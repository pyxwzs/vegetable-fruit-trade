package com.trade.repository;

import com.trade.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long>, JpaSpecificationExecutor<Announcement> {

    @Query("SELECT a FROM Announcement a WHERE a.isTop = true AND a.status = :status ORDER BY a.publishTime DESC")
    List<Announcement> findTop5ByIsTopTrueAndStatusOrderByPublishTimeDesc(@Param("status") Announcement.AnnouncementStatus status);

    @Query("SELECT a FROM Announcement a WHERE a.isTimed = true AND a.publishTime <= :now AND a.status = :status")
    List<Announcement> findByIsTimedTrueAndPublishTimeBeforeAndStatus(@Param("now") LocalDateTime now, @Param("status") Announcement.AnnouncementStatus status);

    @Query("SELECT a FROM Announcement a WHERE a.expireTime < :now AND a.status = :status")
    List<Announcement> findByExpireTimeBeforeAndStatus(@Param("now") LocalDateTime now, @Param("status") Announcement.AnnouncementStatus status);

    Page<Announcement> findByTitleContaining(String title, Pageable pageable);

    @Modifying
    @Query("UPDATE Announcement a SET a.publisher = null WHERE a.publisher.id = :userId")
    void clearPublisherByUserId(@Param("userId") Long userId);
}