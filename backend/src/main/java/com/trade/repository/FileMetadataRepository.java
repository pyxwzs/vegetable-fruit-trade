package com.trade.repository;

import com.trade.entity.FileMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long>, JpaSpecificationExecutor<FileMetadata> {

    Optional<FileMetadata> findByFileId(String fileId);

    List<FileMetadata> findByBusinessTypeAndBusinessId(String businessType, Long businessId);

    List<FileMetadata> findByParentFileId(Long parentFileId);

    @Modifying
    @Query("UPDATE FileMetadata f SET f.uploader = null WHERE f.uploader.id = :userId")
    void clearUploaderByUserId(@Param("userId") Long userId);
}