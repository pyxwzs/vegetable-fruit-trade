package com.trade.controller;

import com.trade.entity.Role;
import com.trade.repository.RoleRepository;
import com.trade.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleRepository roleRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('user:manage')")
    public ApiResponse<List<Role>> listRoles() {
        return ApiResponse.success(roleRepository.findAll());
    }
}
