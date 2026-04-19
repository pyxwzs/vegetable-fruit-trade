package com.trade.controller;

import com.trade.config.AuthProperties;
import com.trade.dto.DataScopeRequestDTO;
import com.trade.dto.DataScopeResponseDTO;
import com.trade.dto.ForgotPasswordRequest;
import com.trade.dto.LoginMfaVerifyRequest;
import com.trade.dto.LoginRequest;
import com.trade.dto.RefreshTokenRequest;
import com.trade.dto.RegisterChallengeRequest;
import com.trade.dto.ResetPasswordRequest;
import com.trade.dto.UserSecuritySettingsDTO;
import com.trade.entity.User;
import com.trade.service.MfaLoginService;
import com.trade.service.PasswordResetService;
import com.trade.service.RegisterService;
import com.trade.service.UserService;
import com.trade.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final PasswordResetService passwordResetService;
    private final MfaLoginService mfaLoginService;
    private final RegisterService registerService;
    private final AuthProperties authProperties;

    /**
     * 系统是否允许使用「登录二次验证」功能（application.yml auth.mfa-login-enabled）。
     * 具体是否走二次验证还取决于账号自身的 mfaLoginEnabled。
     */
    @GetMapping("/config")
    public ApiResponse<Map<String, Object>> authConfig() {
        Map<String, Object> data = new HashMap<>();
        data.put("mfaFeatureEnabled", authProperties.isMfaLoginEnabled());
        return ApiResponse.success(data);
    }

    /**
     * 登录：若系统与账号均开启二次验证则返回 mfaRequired + sessionId；否则直接返回 token。
     */
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ApiResponse.success(userService.login(loginRequest));
    }

    @PostMapping("/login/mfa-challenge")
    public ApiResponse<Map<String, Object>> loginMfaChallenge(@Valid @RequestBody LoginRequest loginRequest) {
        return ApiResponse.success(mfaLoginService.challenge(loginRequest));
    }

    @PostMapping("/login/mfa-verify")
    public ApiResponse<Map<String, String>> loginMfaVerify(@Valid @RequestBody LoginMfaVerifyRequest request) {
        return ApiResponse.success(mfaLoginService.verify(request));
    }

    /** 注册第一步：填写信息并发送邮箱验证码（此时不创建账号） */
    @PostMapping("/register/challenge")
    public ApiResponse<Map<String, Object>> registerChallenge(@Valid @RequestBody RegisterChallengeRequest request) {
        return ApiResponse.success(registerService.challenge(request));
    }

    /** 注册第二步：校验邮箱验证码后创建账号 */
    @PostMapping("/register/verify")
    public ApiResponse<User> registerVerify(@Valid @RequestBody LoginMfaVerifyRequest request) {
        return ApiResponse.success(registerService.verify(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<Map<String, String>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(userService.refreshAccessToken(request.getRefreshToken()));
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.sendResetCode(request.getEmail().trim());
        return ApiResponse.success();
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(
                request.getEmail().trim(),
                request.getCode(),
                request.getNewPassword()
        );
        return ApiResponse.success();
    }

    @GetMapping("/me")
    public ApiResponse<User> getCurrentUser() {
        User user = userService.getCurrentUser();
        return ApiResponse.success(user);
    }

    /**
     * 查询当前管理员的数据视角（代查用户），仅存服务端；JWT 不含该信息，不可篡改。
     */
    @GetMapping("/data-scope")
    public ApiResponse<DataScopeResponseDTO> getDataScope() {
        return ApiResponse.success(userService.getDataScopeForCurrentUser());
    }

    /**
     * 设置管理员数据视角：targetUserId 为空表示恢复全量数据；非管理员调用将报错。
     */
    @PutMapping("/data-scope")
    public ApiResponse<Void> setDataScope(@Valid @RequestBody DataScopeRequestDTO dto) {
        userService.setAdminDataScope(dto.getTargetUserId());
        return ApiResponse.success(null);
    }

    /** 当前用户修改二次验证、登录邮件提醒等安全选项 */
    @PatchMapping("/me/security")
    public ApiResponse<User> updateMySecurity(@Valid @RequestBody UserSecuritySettingsDTO dto) {
        return ApiResponse.success(userService.updateSecuritySettings(dto));
    }
}
