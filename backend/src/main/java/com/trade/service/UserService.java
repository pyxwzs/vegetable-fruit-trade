package com.trade.service;

import com.trade.dto.LoginRequest;
import com.trade.dto.UserDTO;
import com.trade.entity.User;
import com.trade.exception.BusinessException;
import com.trade.repository.UserRepository;
import com.trade.security.JwtTokenProvider;
import com.trade.util.BeanCopyUtils;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public Map<String, Object> login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new BusinessException("用户不存在"));
        if (user.getStatus() != User.UserStatus.ENABLED) {
            throw new BusinessException("账号已禁用");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("token", tokenProvider.generateAccessToken(authentication));
        response.put("refreshToken", tokenProvider.generateRefreshToken(
                loginRequest.getUsername(),
                Boolean.TRUE.equals(loginRequest.getRememberMe())));
        response.put("tokenType", "Bearer");
        return response;
    }

    public Map<String, String> refreshAccessToken(String refreshToken) {
        try {
            if (!tokenProvider.validateToken(refreshToken) || !tokenProvider.validateRefreshToken(refreshToken)) {
                throw new BusinessException("刷新令牌无效或已过期");
            }
            String username = tokenProvider.getUsernameFromToken(refreshToken);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new BusinessException("用户不存在"));
            if (user.getStatus() != User.UserStatus.ENABLED) {
                throw new BusinessException("账号已禁用");
            }
            return tokenProvider.rotateTokens(refreshToken);
        } catch (BusinessException e) {
            throw e;
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException("刷新令牌无效或已过期");
        }
    }

    public User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    /** 单管理员模式：始终返回 null（查看全量数据） */
    public Long resolveBizDataScopeUserId() {
        return null;
    }

    @Transactional
    public User updateUser(Long id, UserDTO userDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (userDTO.getUsername() != null && !userDTO.getUsername().isBlank()) {
            String nu = userDTO.getUsername().trim();
            if (!nu.equals(user.getUsername()) && userRepository.existsByUsernameAndIdNot(nu, id)) {
                throw new BusinessException("用户名已存在");
            }
            user.setUsername(nu);
        }
        if (userDTO.getPhone() != null) {
            user.setPhone(userDTO.getPhone().trim().isEmpty() ? null : userDTO.getPhone().trim());
        }
        if (userDTO.getRealName() != null) {
            user.setRealName(userDTO.getRealName().trim().isEmpty() ? null : userDTO.getRealName().trim());
        }
        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }

        return userRepository.save(user);
    }

    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }
}
