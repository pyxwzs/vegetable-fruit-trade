package com.trade.service;

import com.trade.dto.SiteSettingsDTO;
import com.trade.entity.SiteSettings;
import com.trade.exception.BusinessException;
import com.trade.repository.SiteSettingsRepository;
import com.trade.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SiteSettingsService {

    private static final Set<String> ALLOWED_EXT = Set.of("png", "jpg", "jpeg", "webp", "gif", "svg");
    private static final long MAX_LOGO_BYTES = 2 * 1024 * 1024;

    private final SiteSettingsRepository repository;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Transactional(readOnly = true)
    public SiteSettingsDTO getSettings() {
        return toDto(getOrCreate());
    }

    /** 登录页等匿名场景：未指定租户时的默认展示 */
    public SiteSettingsDTO getPublicDefaultSettings() {
        SiteSettingsDTO dto = new SiteSettingsDTO();
        dto.setSiteName("果蔬配送经营管理系统");
        dto.setLogoPath(null);
        return dto;
    }

    @Transactional
    public SiteSettingsDTO updateSiteName(SiteSettingsDTO dto) {
        SiteSettings settings = getOrCreate();
        settings.setSiteName(dto.getSiteName().trim());
        return toDto(repository.save(settings));
    }

    @Transactional
    public SiteSettingsDTO uploadLogo(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择图片文件");
        }
        if (file.getSize() > MAX_LOGO_BYTES) {
            throw new BusinessException("Logo 大小不能超过 2MB");
        }

        Long tenantId = TenantContext.requireTenantId();
        String ext = resolveExtension(file);
        Path siteDir = Paths.get(uploadDir, String.valueOf(tenantId), "site");
        Files.createDirectories(siteDir);

        Path target = siteDir.resolve("logo." + ext);
        Files.write(target, file.getBytes());

        SiteSettings settings = getOrCreate();
        settings.setLogoPath("/uploads/" + tenantId + "/site/logo." + ext);
        return toDto(repository.save(settings));
    }

    @Transactional
    public SiteSettingsDTO updateLogoUrl(String logoUrl) {
        SiteSettings settings = getOrCreate();
        if (logoUrl == null || logoUrl.isBlank()) {
            settings.setLogoPath(null);
            return toDto(repository.save(settings));
        }

        String trimmed = logoUrl.trim();
        validateLogoUrl(trimmed);
        settings.setLogoPath(trimmed);
        return toDto(repository.save(settings));
    }

    private void validateLogoUrl(String url) {
        if (url.length() > 500) {
            throw new BusinessException("Logo 链接过长");
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new BusinessException("Logo 链接须以 http:// 或 https:// 开头");
        }
        try {
            new URI(url);
        } catch (URISyntaxException e) {
            throw new BusinessException("Logo 链接格式不正确");
        }
    }

    private SiteSettings getOrCreate() {
        Long tenantId = TenantContext.requireTenantId();
        return repository.findByTenantId(tenantId)
                .orElseGet(() -> {
                    SiteSettings s = new SiteSettings();
                    s.setTenantId(tenantId);
                    s.setSiteName("果蔬批发");
                    return repository.save(s);
                });
    }

    private String resolveExtension(MultipartFile file) {
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        }
        if (ext.isEmpty()) {
            String contentType = file.getContentType();
            if (contentType != null) {
                if (contentType.contains("png")) ext = "png";
                else if (contentType.contains("jpeg") || contentType.contains("jpg")) ext = "jpg";
                else if (contentType.contains("webp")) ext = "webp";
                else if (contentType.contains("gif")) ext = "gif";
                else if (contentType.contains("svg")) ext = "svg";
            }
        }
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException("仅支持 png、jpg、webp、gif、svg 格式");
        }
        return ext;
    }

    private SiteSettingsDTO toDto(SiteSettings settings) {
        SiteSettingsDTO dto = new SiteSettingsDTO();
        dto.setSiteName(settings.getSiteName());
        dto.setLogoPath(settings.getLogoPath());
        return dto;
    }
}
