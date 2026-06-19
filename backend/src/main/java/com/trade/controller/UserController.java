package com.trade.controller;

import com.trade.dto.UserDTO;
import com.trade.entity.User;
import com.trade.service.UserService;
import com.trade.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<User> getMe() {
        return ApiResponse.success(userService.getCurrentUser());
    }

    @PutMapping("/me")
    public ApiResponse<User> updateMe(@Valid @RequestBody UserDTO userDTO) {
        User current = userService.getCurrentUser();
        return ApiResponse.success(userService.updateUser(current.getId(), userDTO));
    }
}
