package com.trade.service;

import com.trade.entity.Inventory;
import com.trade.entity.User;
import com.trade.entity.UserNotification;
import com.trade.repository.UserNotificationRepository;
import com.trade.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final UserNotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Async
    public void sendExpiringAlert(Inventory inventory) {
        log.info("发送过期预警: {}", inventory.getProduct().getName());

        // 获取有库存管理权限的用户
        List<User> managers = userRepository.findByRoles_NameIn(List.of("ADMIN", "WAREHOUSE_KEEPER"));

        for (User user : managers) {
            UserNotification notification = new UserNotification();
            notification.setUser(user);
            notification.setTitle("库存过期预警");
            notification.setContent(String.format("商品【%s】批次【%s】即将过期，请及时处理",
                    inventory.getProduct().getName(), inventory.getBatchNo()));
            notification.setType(UserNotification.NotificationType.ALERT);

            notificationRepository.save(notification);
        }
    }

    @Async
    public void sendLowStockAlert(Inventory inventory) {
        log.info("发送低库存预警: {}", inventory.getProduct().getName());

        List<User> managers = userRepository.findByRoles_NameIn(List.of("ADMIN", "WAREHOUSE_KEEPER", "PURCHASER"));

        for (User user : managers) {
            UserNotification notification = new UserNotification();
            notification.setUser(user);
            notification.setTitle("库存不足预警");
            notification.setContent(String.format("商品【%s】库存不足，当前可用库存：%s，请及时补货",
                    inventory.getProduct().getName(), inventory.getAvailableQuantity()));
            notification.setType(UserNotification.NotificationType.ALERT);

            notificationRepository.save(notification);
        }
    }
}