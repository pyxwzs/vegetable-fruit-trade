package com.trade.security;

import com.trade.entity.Admin;
import com.trade.entity.User;
import com.trade.repository.AdminRepository;
import com.trade.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private static final long PLATFORM_TENANT_ID = 1L;

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String loginKey) throws UsernameNotFoundException {
        return adminRepository.findByUsername(loginKey)
                .map(this::buildAdminPrincipal)
                .orElseGet(() -> userRepository.findByLoginKey(loginKey)
                        .map(this::buildUserPrincipal)
                        .orElseThrow(() -> new UsernameNotFoundException("账号不存在: " + loginKey)));
    }

    public UserDetails buildAdminPrincipal(Admin admin) {
        Set<SimpleGrantedAuthority> authorities = new LinkedHashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"));
        return UserPrincipal.builder()
                .id(admin.getId())
                .tenantId(PLATFORM_TENANT_ID)
                .username(admin.getUsername())
                .password(admin.getPassword())
                .enabled(admin.getStatus() == Admin.AdminStatus.ENABLED)
                .accountNonLocked(true)
                .authorities(authorities)
                .build();
    }

    public UserDetails buildUserPrincipal(User user) {
        Set<SimpleGrantedAuthority> authorities = new LinkedHashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        return UserPrincipal.builder()
                .id(user.getId())
                .tenantId(user.getTenantId())
                .username(user.getLoginKey())
                .password("")
                .enabled(user.getStatus() != User.UserStatus.DISABLED)
                .accountNonLocked(true)
                .authorities(authorities)
                .build();
    }
}
