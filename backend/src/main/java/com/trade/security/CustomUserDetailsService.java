package com.trade.security;

import com.trade.entity.User;
import com.trade.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)  // 确保事务存在
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("加载用户信息: {}", username);

        User user = userRepository.findByUsernameWithRoles(username)
                .orElseThrow(() -> {
                    log.error("用户不存在: {}", username);
                    return new UsernameNotFoundException("用户不存在: " + username);
                });

        log.info("找到用户: {}, 角色数: {}", user.getUsername(), user.getRoles().size());

        // 收集所有权限
        var authorities = user.getRoles().stream()
                .peek(role -> log.debug("角色: {}", role.getName()))
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> {
                    log.debug("权限: {}", permission.getCode());
                    return new SimpleGrantedAuthority(permission.getCode());
                })
                .collect(Collectors.toSet());

        log.info("用户 {} 有 {} 个权限", username, authorities.size());

        boolean enabled = user.getStatus() != User.UserStatus.DISABLED;
        boolean nonLocked = user.getStatus() != User.UserStatus.LOCKED;

        return UserPrincipal.builder()
                .id(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .email(user.getEmail())
                .enabled(enabled)
                .accountNonLocked(nonLocked)
                .authorities(authorities)
                .build();
    }
}