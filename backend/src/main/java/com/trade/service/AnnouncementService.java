package com.trade.service;

import com.trade.dto.AnnouncementDTO;
import com.trade.entity.Announcement;
import com.trade.entity.User;
import com.trade.entity.UserNotification;
import com.trade.exception.BusinessException;
import com.trade.repository.AnnouncementRepository;
import com.trade.repository.UserNotificationRepository;
import com.trade.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AnnouncementService {
    private final AnnouncementRepository announcementRepository;
    private final UserNotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final NotificationService notificationService;

    @Transactional
    public Announcement createAnnouncement(AnnouncementDTO dto) {
        Announcement announcement = new Announcement();
        announcement.setTitle(dto.getTitle());
        announcement.setContent(dto.getContent());
        announcement.setPublisher(userService.getCurrentUser());
        announcement.setTargetRoles(dto.getTargetRoles() != null ? dto.getTargetRoles() : Collections.emptySet());
        if (dto.getPriority() != null && !dto.getPriority().isBlank()) {
            announcement.setPriority(Announcement.Priority.valueOf(dto.getPriority().trim().toUpperCase()));
        }
        announcement.setIsTop(dto.getIsTop());
        announcement.setAttachments(dto.getAttachments());
        announcement.setIsTimed(dto.getIsTimed());

        if (dto.getIsTimed()) {
            announcement.setPublishTime(dto.getPublishTime());
            announcement.setStatus(Announcement.AnnouncementStatus.DRAFT);
        } else {
            announcement.setPublishTime(LocalDateTime.now());
            announcement.setStatus(Announcement.AnnouncementStatus.PUBLISHED);
        }

        announcement.setExpireTime(dto.getExpireTime());

        Announcement saved = announcementRepository.save(announcement);

        // 如果不是定时发布，立即推送
        if (!dto.getIsTimed()) {
            pushAnnouncementToUsers(saved);
        }

        return saved;
    }

    @Async
    public void pushAnnouncementToUsers(Announcement announcement) {
        // 获取目标用户

        Set<String> roles = announcement.getTargetRoles();
        List<User> targetUsers = roles == null || roles.isEmpty()
                ? List.of()
                : userRepository.findByRoles_NameInSet(roles);

        for (User user : targetUsers) {
            UserNotification notification = new UserNotification();
            notification.setUser(user);
            notification.setAnnouncement(announcement);
            notification.setTitle(announcement.getTitle());
            notification.setContent(announcement.getContent());
            notification.setType(UserNotification.NotificationType.ANNOUNCEMENT);
            notification.setIsRead(false);

            notificationRepository.save(notification);

            // 发送邮件通知
            if (user.getEmail() != null && announcement.getPriority() == Announcement.Priority.URGENT) {
                notificationService.sendEmail(user.getEmail(),
                        "紧急公告：" + announcement.getTitle(),
                        announcement.getContent());
            }
        }
    }

    public Page<Announcement> getAnnouncements(String keyword, Pageable pageable) {
        return announcementRepository.findAll((root, query, cb) -> {
            var predicates = cb.conjunction();

            if (keyword != null && !keyword.isEmpty()) {
                predicates = cb.and(predicates,
                        cb.like(root.get("title"), "%" + keyword + "%")
                );
            }

            return predicates;
        }, pageable);
    }

    public List<Announcement> getTopAnnouncements() {
        return announcementRepository.findTop5ByIsTopTrueAndStatusOrderByPublishTimeDesc(
                Announcement.AnnouncementStatus.PUBLISHED);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        UserNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException("通知不存在"));

        notification.setIsRead(true);
        notification.setReadTime(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Scheduled(fixedDelay = 60000) // 每分钟执行一次
    @Transactional
    public void processTimedAnnouncements() {
        LocalDateTime now = LocalDateTime.now();
        List<Announcement> timedAnnouncements = announcementRepository
                .findByIsTimedTrueAndPublishTimeBeforeAndStatus(
                        now, Announcement.AnnouncementStatus.DRAFT);

        for (Announcement announcement : timedAnnouncements) {
            announcement.setStatus(Announcement.AnnouncementStatus.PUBLISHED);
            announcementRepository.save(announcement);
            pushAnnouncementToUsers(announcement);
        }
    }

    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    @Transactional
    public void processExpiredAnnouncements() {
        LocalDateTime now = LocalDateTime.now();
        List<Announcement> expiredAnnouncements = announcementRepository
                .findByExpireTimeBeforeAndStatus(now, Announcement.AnnouncementStatus.PUBLISHED);

        for (Announcement announcement : expiredAnnouncements) {
            announcement.setStatus(Announcement.AnnouncementStatus.EXPIRED);
            announcementRepository.save(announcement);
        }
    }
}