package com.trade.controller;

import com.trade.dto.LoginRequest;
import com.trade.dto.PasswordChangeDTO;
import com.trade.dto.RefreshTokenRequest;
import com.trade.entity.User;
import com.trade.exception.BusinessException;
import com.trade.repository.UserRepository;
import com.trade.service.UserService;
import com.trade.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ApiResponse.success(userService.login(loginRequest));
    }

    @PostMapping("/refresh")
    public ApiResponse<Map<String, String>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(userService.refreshAccessToken(request.getRefreshToken()));
    }

    @GetMapping("/me")
    public ApiResponse<User> getCurrentUser() {
        return ApiResponse.success(userService.getCurrentUser());
    }

    @Transactional
    @PutMapping("/me/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody PasswordChangeDTO dto) {
        User user = userService.getCurrentUser();
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
        return ApiResponse.success();
    }
}
