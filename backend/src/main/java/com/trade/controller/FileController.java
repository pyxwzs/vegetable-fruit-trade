package com.trade.controller;

import com.trade.dto.FileMetadataDTO;
import com.trade.entity.FileMetadata;
import com.trade.service.FileService;
import com.trade.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

    /** 文件列表：需要文件管理权限 */
    @GetMapping
    @PreAuthorize("hasAuthority('file:manage')")
    public ApiResponse<Page<FileMetadataDTO>> listFiles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String businessType,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(fileService.listFiles(keyword, businessType, pageable));
    }

    /** 上传文件：所有已登录用户均可上传（业务单据附件） */
    @PostMapping("/upload")
    public ApiResponse<FileMetadataDTO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) Long businessId) {
        FileMetadata metadata = fileService.uploadFile(file, businessType, businessId);
        return ApiResponse.success(fileService.toDto(metadata));
    }

    /** 刷新访问链接：所有已登录用户均可获取（查看业务附件） */
    @GetMapping("/{fileId}/access-url")
    public ApiResponse<String> refreshAccessUrl(@PathVariable String fileId) {
        return ApiResponse.success(fileService.refreshAccessUrl(fileId));
    }

    /** 下载文件：所有已登录用户均可下载（查看业务附件） */
    @GetMapping("/download/{fileId}")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable String fileId) {
        InputStream inputStream = fileService.downloadFile(fileId);
        String filename = fileService.findFileNameForDownload(fileId);

        HttpHeaders headers = new HttpHeaders();
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded);

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(inputStream));
    }

    /** 删除文件：需要文件管理权限 */
    @DeleteMapping("/{fileId}")
    @PreAuthorize("hasAuthority('file:manage')")
    public ApiResponse<Void> deleteFile(@PathVariable String fileId) {
        fileService.deleteFile(fileId);
        return ApiResponse.success(null);
    }
}
