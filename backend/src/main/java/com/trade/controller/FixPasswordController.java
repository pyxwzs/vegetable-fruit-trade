package com.trade.controller;

import com.trade.entity.User;
import com.trade.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 运维/初始化辅助接口：仅系统管理员（user:manage）可调用。
 */
@Slf4j
@RestController
@RequestMapping("/fix")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('user:manage')")
public class FixPasswordController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/admin-password")
    public Map<String, Object> fixAdminPassword() {
        Map<String, Object> result = new HashMap<>();

        try {
            User admin = userRepository.findByUsername("admin")
                    .orElseGet(() -> {
                        User newAdmin = new User();
                        newAdmin.setUsername("admin");
                        newAdmin.setEmail("admin@example.com");
                        newAdmin.setRealName("系统管理员");
                        newAdmin.setStatus(User.UserStatus.ENABLED);
                        return newAdmin;
                    });

            String rawPassword = "admin123";
            String encodedPassword = passwordEncoder.encode(rawPassword);
            admin.setPassword(encodedPassword);

            userRepository.save(admin);

            boolean matches = passwordEncoder.matches(rawPassword, admin.getPassword());

            result.put("success", true);
            result.put("message", "密码修复成功");
            result.put("username", "admin");
            result.put("rawPassword", rawPassword);
            result.put("encodedPassword", encodedPassword);
            result.put("verification", matches);

            log.info("密码修复成功: username=admin, verification={}", matches);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "修复失败: " + e.getMessage());
            log.error("密码修复失败", e);
        }

        return result;
    }

    @GetMapping("/check-password")
    public Map<String, Object> checkPassword(@RequestParam String username,
                                             @RequestParam String password) {
        Map<String, Object> result = new HashMap<>();

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            result.put("exists", false);
            result.put("message", "用户不存在");
            return result;
        }

        result.put("exists", true);
        result.put("username", user.getUsername());
        result.put("storedPassword", user.getPassword());
        result.put("isBCrypt", user.getPassword().startsWith("$2a$"));

        boolean matches = passwordEncoder.matches(password, user.getPassword());
        result.put("passwordMatches", matches);
        result.put("testedPassword", password);

        return result;
    }
}
