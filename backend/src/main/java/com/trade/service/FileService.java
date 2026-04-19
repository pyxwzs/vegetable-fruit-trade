package com.trade.service;

import com.trade.entity.FileMetadata;
import com.trade.entity.User;
import com.trade.exception.BusinessException;
import com.trade.repository.FileMetadataRepository;
import com.trade.util.FileUtils;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.PostConstruct;
import javax.persistence.criteria.Predicate;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.trade.dto.FileMetadataDTO;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final MinioClient minioClient;
    private final FileMetadataRepository fileMetadataRepository;
    private final UserService userService;

    @Value("${minio.bucket-name:}")
    private String bucketName;

    @Value("${minio.enabled:true}")
    private boolean minioEnabled;

    @PostConstruct
    public void init() {
        if (!minioEnabled) {
            log.warn("MinIO 服务已禁用，文件上传功能不可用");
            return;
        }

        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("创建桶: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("初始化MinIO失败", e);
            throw new RuntimeException("初始化文件存储失败", e);
        }
    }

    @Transactional
    public FileMetadata uploadFile(MultipartFile file, String businessType, Long businessId) {
        if (!minioEnabled) {
            throw new BusinessException("文件服务未启用，请配置MinIO");
        }
        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isBlank()) {
                throw new BusinessException("文件名无效");
            }
            byte[] bytes = file.getBytes();
            String fileId = UUID.randomUUID().toString();
            String extension = FileUtils.getExtension(originalFilename);
            String bt = (businessType != null && !businessType.isBlank()) ? businessType.trim() : "other";
            String objectName = bt + "/" + fileId + "/" + originalFilename;

            String md5Hex;
            try (ByteArrayInputStream md5In = new ByteArrayInputStream(bytes)) {
                md5Hex = FileUtils.calculateMD5(md5In);
            }

            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                    .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                    .build();

            minioClient.putObject(args);

            User currentUser = userService.getCurrentUser();

            FileMetadata metadata = new FileMetadata();
            metadata.setFileId(fileId);
            metadata.setFileName(originalFilename);
            metadata.setFileType(extension);
            metadata.setFileSize(file.getSize());
            metadata.setFilePath(objectName);
            metadata.setUrl(getFileUrl(objectName));
            metadata.setUploader(currentUser);
            metadata.setBusinessType(bt);
            metadata.setBusinessId(businessId);
            metadata.setCategory(bt);
            metadata.setMd5(md5Hex);

            return fileMetadataRepository.save(metadata);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("上传文件失败", e);
            throw new BusinessException("上传文件失败: " + e.getMessage());
        }
    }

    public Page<FileMetadataDTO> listFiles(String keyword, String businessType, Pageable pageable) {
        Specification<FileMetadata> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                ps.add(cb.like(root.get("fileName"), "%" + keyword.trim() + "%"));
            }
            if (businessType != null && !businessType.isBlank()) {
                ps.add(cb.equal(root.get("businessType"), businessType.trim()));
            }
            ps.add(cb.equal(root.get("status"), FileMetadata.FileStatus.ACTIVE));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return fileMetadataRepository.findAll(spec, pageable).map(this::toDto);
    }

    /** 重新生成预签名地址（列表/预览前调用，避免过期） */
    public String refreshAccessUrl(String fileId) {
        FileMetadata metadata = fileMetadataRepository.findByFileId(fileId)
                .orElseThrow(() -> new BusinessException("文件不存在"));
        if (!minioEnabled || metadata.getFilePath() == null) {
            return metadata.getUrl();
        }
        String url = getFileUrl(metadata.getFilePath());
        metadata.setUrl(url);
        fileMetadataRepository.save(metadata);
        return url;
    }

    public FileMetadataDTO toDto(FileMetadata m) {
        FileMetadataDTO d = new FileMetadataDTO();
        d.setId(m.getId());
        d.setFileId(m.getFileId());
        d.setFileName(m.getFileName());
        d.setFileType(m.getFileType());
        d.setFileSize(m.getFileSize());
        d.setFilePath(m.getFilePath());
        if (m.getFileSize() != null) {
            d.setFileSizeDisplay(FileUtils.formatFileSize(m.getFileSize()));
        }
        d.setMd5(m.getMd5());
        if (m.getUploader() != null) {
            d.setUploaderId(m.getUploader().getId());
            String name = m.getUploader().getRealName();
            d.setUploaderName(name != null && !name.isBlank() ? name : m.getUploader().getUsername());
        }
        d.setBusinessType(m.getBusinessType());
        d.setBusinessId(m.getBusinessId());
        d.setCategory(m.getCategory());
        d.setVersion(m.getVersion());
        d.setIsLatest(m.getIsLatest());
        d.setParentFileId(m.getParentFileId());
        d.setDescription(m.getDescription());
        d.setStatus(m.getStatus() != null ? m.getStatus().name() : null);
        d.setCreateTime(m.getCreateTime());
        d.setUpdateTime(m.getUpdateTime());
        if (minioEnabled && m.getFilePath() != null) {
            d.setUrl(getFileUrl(m.getFilePath()));
        } else {
            d.setUrl(m.getUrl());
        }
        return d;
    }

    public String findFileNameForDownload(String fileId) {
        return fileMetadataRepository.findByFileId(fileId)
                .map(FileMetadata::getFileName)
                .orElse("download.bin");
    }

    public InputStream downloadFile(String fileId) {
        try {
            FileMetadata metadata = fileMetadataRepository.findByFileId(fileId)
                    .orElseThrow(() -> new BusinessException("文件不存在"));

            GetObjectArgs args = GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(metadata.getFilePath())
                    .build();

            return minioClient.getObject(args);

        } catch (Exception e) {
            log.error("下载文件失败", e);
            throw new BusinessException("下载文件失败");
        }
    }

    public String getFileUrl(String objectName) {
        try {
            GetPresignedObjectUrlArgs args = GetPresignedObjectUrlArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .method(Method.GET)
                    .expiry(7, TimeUnit.DAYS)
                    .build();

            return minioClient.getPresignedObjectUrl(args);
        } catch (Exception e) {
            log.error("获取文件URL失败", e);
            return null;
        }
    }

    @Transactional
    public void deleteFile(String fileId) {
        try {
            FileMetadata metadata = fileMetadataRepository.findByFileId(fileId)
                    .orElseThrow(() -> new BusinessException("文件不存在"));

            // 删除MinIO中的文件
            RemoveObjectArgs args = RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(metadata.getFilePath())
                    .build();

            minioClient.removeObject(args);

            // 删除元数据
            fileMetadataRepository.delete(metadata);

        } catch (Exception e) {
            log.error("删除文件失败", e);
            throw new BusinessException("删除文件失败");
        }
    }
}