package com.trade.controller;

import com.trade.dto.AdminResetPasswordDTO;
import com.trade.dto.UserDTO;
import com.trade.dto.UserStatusUpdateDTO;
import com.trade.entity.User;
import com.trade.service.UserService;
import com.trade.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('user:manage')")
    public ApiResponse<Page<User>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        User.UserStatus st = null;
        if (status != null && !status.isBlank()) {
            try {
                st = User.UserStatus.valueOf(status.trim());
            } catch (IllegalArgumentException ignored) {
                // 非法状态值时忽略筛选
            }
        }
        Page<User> users = userService.getUsers(keyword, st, pageable);
        return ApiResponse.success(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:manage')")
    public ApiResponse<User> getUser(@PathVariable Long id) {
        return ApiResponse.success(userService.getUser(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('user:manage')")
    public ApiResponse<User> createUser(@Valid @RequestBody UserDTO userDTO) {
        User user = userService.createUser(userDTO);
        return ApiResponse.success(user);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:manage')")
    public ApiResponse<User> updateUser(@PathVariable Long id, @Valid @RequestBody UserDTO userDTO) {
        User user = userService.updateUser(id, userDTO);
        return ApiResponse.success(user);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:manage')")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResponse.success();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('user:manage')")
    public ApiResponse<User> updateUserStatus(@PathVariable Long id, @Valid @RequestBody UserStatusUpdateDTO dto) {
        User user = userService.updateUserStatus(id, dto.getStatus());
        return ApiResponse.success(user);
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('user:manage')")
    public ApiResponse<Void> resetUserPassword(@PathVariable Long id, @Valid @RequestBody AdminResetPasswordDTO dto) {
        userService.adminResetPassword(id, dto.getNewPassword());
        return ApiResponse.success();
    }
}