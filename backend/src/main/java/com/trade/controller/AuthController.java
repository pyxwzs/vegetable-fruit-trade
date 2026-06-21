package com.trade.controller;

import com.trade.dto.LoginRequest;
import com.trade.dto.MenuKeysDTO;
import com.trade.dto.RefreshTokenRequest;
import com.trade.dto.WxAuthResultDTO;
import com.trade.dto.WxLoginRequest;
import com.trade.dto.WxRegisterRequest;
import com.trade.entity.User;
import com.trade.service.AdminService;
import com.trade.service.TenantRegistrationService;
import com.trade.service.UserService;
import com.trade.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AdminService adminService;
    private final TenantRegistrationService tenantRegistrationService;

    @PostMapping("/wx/login")
    public ApiResponse<WxAuthResultDTO> wxLogin(@Valid @RequestBody WxLoginRequest request) {
        return ApiResponse.success(userService.wxLogin(request));
    }

    @PostMapping("/wx/register")
    public ApiResponse<WxAuthResultDTO> wxRegister(@Valid @RequestBody WxRegisterRequest request) {
        User user = tenantRegistrationService.registerWx(request);
        return ApiResponse.success(userService.buildWxAuthResult(user, false));
    }

    @PostMapping("/platform/login")
    public ApiResponse<Map<String, Object>> platformLogin(@Valid @RequestBody LoginRequest loginRequest) {
        return ApiResponse.success(adminService.platformLogin(loginRequest));
    }

    @PostMapping("/refresh")
    public ApiResponse<Map<String, String>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(userService.refreshAccessToken(request.getRefreshToken()));
    }

    @GetMapping("/me")
    public ApiResponse<Object> getCurrentAccount() {
        return ApiResponse.success(userService.getCurrentAccount());
    }

    @Transactional
    @PutMapping("/me/menus")
    public ApiResponse<User> updateMenuKeys(@Valid @RequestBody MenuKeysDTO dto) {
        return ApiResponse.success(userService.updateMenuKeys(dto));
    }
}
