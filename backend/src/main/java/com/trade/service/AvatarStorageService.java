package com.trade.service;

import com.trade.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

@Service
public class AvatarStorageService {

    private static final long MAX_AVATAR_BYTES = 512 * 1024;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public String saveUserAvatar(Long tenantId, Long userId, String avatarBase64) {
        if (!StringUtils.hasText(avatarBase64)) {
            return null;
        }
        byte[] bytes = decodeBase64(avatarBase64.trim());
        if (bytes.length > MAX_AVATAR_BYTES) {
            throw new BusinessException("头像大小不能超过 512KB");
        }

        try {
            Path dir = Paths.get(uploadDir, String.valueOf(tenantId), "avatars");
            Files.createDirectories(dir);
            Path target = dir.resolve("user_" + userId + ".jpg");
            Files.write(target, bytes);
            return "/uploads/" + tenantId + "/avatars/user_" + userId + ".jpg";
        } catch (IOException e) {
            throw new BusinessException("头像保存失败");
        }
    }

    private byte[] decodeBase64(String raw) {
        String content = raw;
        int comma = raw.indexOf(',');
        if (raw.startsWith("data:") && comma > 0) {
            content = raw.substring(comma + 1);
        }
        try {
            return Base64.getDecoder().decode(content);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("头像格式不正确");
        }
    }
}
