package com.trade.service;

import com.trade.config.AuthProperties;
import com.trade.dto.DataScopeResponseDTO;
import com.trade.dto.LoginRequest;
import com.trade.dto.UserDTO;
import com.trade.dto.UserSecuritySettingsDTO;
import com.trade.entity.AdminDataScope;
import com.trade.entity.Role;
import com.trade.entity.User;
import com.trade.exception.BusinessException;
import com.trade.repository.AdminDataScopeRepository;
import com.trade.repository.AnnouncementRepository;
import com.trade.repository.FileMetadataRepository;
import com.trade.repository.PurchaseOrderRepository;
import com.trade.repository.RoleRepository;
import com.trade.repository.SalesOrderRepository;
import com.trade.repository.UserRepository;
import com.trade.security.JwtTokenProvider;
import com.trade.util.BeanCopyUtils;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String DEFAULT_ROLE_NAME = "PURCHASER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AdminDataScopeRepository adminDataScopeRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final AnnouncementRepository announcementRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final AuthProperties authProperties;
    private final MfaLoginService mfaLoginService;
    private final LoginAlertService loginAlertService;

    /**
     * 一步登录：根据系统开关与账号「二次验证」设置，返回 token 或 MFA 会话信息。
     */
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
            SecurityContextHolder.clearContext();
            throw new BusinessException("账号已禁用或锁定");
        }

        boolean globalMfa = authProperties.isMfaLoginEnabled();
        boolean userMfa = Boolean.TRUE.equals(user.getMfaLoginEnabled());

        if (!globalMfa || !userMfa) {
            try {
                SecurityContextHolder.getContext().setAuthentication(authentication);
                Map<String, Object> tokens = buildTokenPayload(authentication, loginRequest.getUsername(),
                        Boolean.TRUE.equals(loginRequest.getRememberMe()));
                loginAlertService.sendIfEnabled(user);
                return tokens;
            } finally {
                SecurityContextHolder.clearContext();
            }
        }

        SecurityContextHolder.clearContext();
        return mfaLoginService.prepareChallenge(user, loginRequest);
    }

    private Map<String, Object> buildTokenPayload(Authentication authentication, String username, boolean rememberMe) {
        Map<String, Object> response = new HashMap<>();
        response.put("token", tokenProvider.generateAccessToken(authentication));
        response.put("refreshToken", tokenProvider.generateRefreshToken(username, rememberMe));
        response.put("type", "Bearer");
        response.put("tokenType", "Bearer");
        return response;
    }

    @Transactional
    public User updateSecuritySettings(UserSecuritySettingsDTO dto) {
        User user = getCurrentUser();
        if (dto.getMfaLoginEnabled() != null) {
            if (Boolean.TRUE.equals(dto.getMfaLoginEnabled())) {
                if (!authProperties.isMfaLoginEnabled()) {
                    throw new BusinessException("系统未开启登录二次验证功能，无法开启");
                }
                if (user.getEmail() == null || user.getEmail().isBlank()) {
                    throw new BusinessException("开启二次验证前请先绑定邮箱");
                }
            }
            user.setMfaLoginEnabled(dto.getMfaLoginEnabled());
        }
        if (dto.getLoginAlertEmailEnabled() != null) {
            user.setLoginAlertEmailEnabled(dto.getLoginAlertEmailEnabled());
        }
        return userRepository.save(user);
    }

    @Transactional
    public User createUser(UserDTO userDTO) {
        if (userDTO.getUsername() == null || userDTO.getUsername().isBlank()) {
            throw new BusinessException("用户名不能为空");
        }
        if (userDTO.getPassword() == null || userDTO.getPassword().isEmpty()) {
            throw new BusinessException("密码不能为空");
        }
        if (userRepository.existsByUsername(userDTO.getUsername().trim())) {
            throw new BusinessException("用户名已存在");
        }
        if (userDTO.getEmail() != null && !userDTO.getEmail().isBlank()
                && userRepository.existsByEmail(userDTO.getEmail().trim())) {
            throw new BusinessException("邮箱已存在");
        }

        String statusStr = userDTO.getStatus();
        userDTO.setStatus(null);
        User user = BeanCopyUtils.copyProperties(userDTO, User.class);
        user.setUsername(userDTO.getUsername().trim());
        if (userDTO.getEmail() != null) {
            user.setEmail(userDTO.getEmail().trim().isEmpty() ? null : userDTO.getEmail().trim());
        }
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setMfaLoginEnabled(false);
        user.setLoginAlertEmailEnabled(true);
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                user.setStatus(User.UserStatus.valueOf(statusStr.trim()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("无效的用户状态");
            }
        } else {
            user.setStatus(User.UserStatus.ENABLED);
        }

        user.setRoles(resolveRoles(userDTO.getRoleIds()));
        return userRepository.save(user);
    }

    private Set<Role> resolveRoles(Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            Role role = roleRepository.findByName(DEFAULT_ROLE_NAME)
                    .orElseThrow(() -> new BusinessException("系统未配置默认角色 " + DEFAULT_ROLE_NAME));
            return new HashSet<>(List.of(role));
        }
        Set<Role> roles = new HashSet<>();
        for (Long rid : roleIds) {
            Role role = roleRepository.findById(rid)
                    .orElseThrow(() -> new BusinessException("角色不存在: " + rid));
            roles.add(role);
        }
        return roles;
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
                throw new BusinessException("账号已禁用或锁定");
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
        return userRepository.findByUsernameWithRoles(username)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    /** 是否系统管理员角色（name=ADMIN） */
    public boolean isAdmin(User user) {
        if (user == null || user.getRoles() == null) {
            return false;
        }
        return user.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getName()));
    }

    /**
     * 业务数据归属用户：
     * - 管理员：无代查记录时全量（null），有记录时为代查用户 id
     * - 财务（FINANCE）：全量（null），需要看所有采购/销售订单才能付款收款
     * - 仓管员（WAREHOUSE_KEEPER）：全量（null），需要看所有待入库/待出库订单
     * - 其他非管理员（采购员、销售员）：仅自己的订单
     */
    public Long resolveBizDataScopeUserId() {
        User me = getCurrentUser();
        if (isAdmin(me) || isFinance(me) || isWarehouseKeeper(me)) {
            if (isAdmin(me)) {
                return adminDataScopeRepository.findById(me.getId())
                        .map(AdminDataScope::getTargetUserId)
                        .orElse(null);
            }
            return null; // 财务/仓管全量
        }
        return me.getId();
    }

    public boolean isFinance(User user) {
        if (user == null || user.getRoles() == null) return false;
        return user.getRoles().stream().anyMatch(r -> "FINANCE".equals(r.getName()));
    }

    public boolean isWarehouseKeeper(User user) {
        if (user == null || user.getRoles() == null) return false;
        return user.getRoles().stream().anyMatch(r -> "WAREHOUSE_KEEPER".equals(r.getName()));
    }

    public DataScopeResponseDTO getDataScopeForCurrentUser() {
        User me = getCurrentUser();
        if (!isAdmin(me)) {
            return new DataScopeResponseDTO(false, null, null, null);
        }
        return adminDataScopeRepository.findById(me.getId())
                .map(s -> {
                    User t = getUser(s.getTargetUserId());
                    String rn = t.getRealName();
                    String disp = rn != null && !rn.isBlank() ? rn : t.getUsername();
                    return new DataScopeResponseDTO(true, t.getId(), t.getUsername(), disp);
                })
                .orElse(new DataScopeResponseDTO(true, null, null, null));
    }

    @Transactional
    public void setAdminDataScope(Long targetUserId) {
        User me = getCurrentUser();
        if (!isAdmin(me)) {
            throw new BusinessException("仅管理员可切换数据视角");
        }
        if (targetUserId == null) {
            adminDataScopeRepository.deleteById(me.getId());
            return;
        }
        if (targetUserId.equals(me.getId())) {
            throw new BusinessException("不能以自己为代查对象");
        }
        User target = getUser(targetUserId);
        if (isAdmin(target)) {
            throw new BusinessException("不能以管理员账号为数据视角");
        }
        AdminDataScope row = adminDataScopeRepository.findById(me.getId()).orElse(new AdminDataScope());
        row.setAdminUserId(me.getId());
        row.setTargetUserId(targetUserId);
        adminDataScopeRepository.save(row);
    }

    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    public Page<User> getUsers(String keyword, User.UserStatus status, Pageable pageable) {
        return userRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.trim().isEmpty()) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("username"), like),
                        cb.like(root.get("realName"), like),
                        cb.like(root.get("email"), like),
                        cb.like(root.get("phone"), like)
                ));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
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
        if (userDTO.getEmail() != null) {
            String em = userDTO.getEmail().trim();
            if (em.isEmpty()) {
                user.setEmail(null);
            } else {
                if (!em.equalsIgnoreCase(user.getEmail() != null ? user.getEmail() : "")
                        && userRepository.existsByEmailAndIdNot(em, id)) {
                    throw new BusinessException("邮箱已被其他用户使用");
                }
                user.setEmail(em);
            }
        }
        if (userDTO.getPhone() != null) {
            user.setPhone(userDTO.getPhone().trim().isEmpty() ? null : userDTO.getPhone().trim());
        }
        if (userDTO.getRealName() != null) {
            user.setRealName(userDTO.getRealName().trim().isEmpty() ? null : userDTO.getRealName().trim());
        }

        String statusStr = userDTO.getStatus();
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                user.setStatus(User.UserStatus.valueOf(statusStr.trim()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("无效的用户状态");
            }
        }

        if (userDTO.getRoleIds() != null) {
            user.setRoles(resolveRoles(userDTO.getRoleIds()));
        }

        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }

        return userRepository.save(user);
    }

    @Transactional
    public void adminResetPassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public User updateUserStatus(Long id, User.UserStatus newStatus) {
        User target = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (target.getUsername().equals(currentUsername) && newStatus != User.UserStatus.ENABLED) {
            throw new BusinessException("不能将自己的账号设为禁用或锁定");
        }
        target.setStatus(newStatus);
        return userRepository.save(target);
    }

    @Transactional
    public void deleteUser(Long id) {
        User target = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (target.getUsername().equals(currentUsername)) {
            throw new BusinessException("不能删除当前登录账号");
        }
        purchaseOrderRepository.clearPurchaserByUserId(id);
        salesOrderRepository.clearSalesmanByUserId(id);
        announcementRepository.clearPublisherByUserId(id);
        fileMetadataRepository.clearUploaderByUserId(id);
        userRepository.deleteById(id);
    }
}
