package com.trade.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;

@Data
@Builder
public class UserPrincipal implements UserDetails {
    private Long id;
    private String username;

    @JsonIgnore
    private String email;

    @JsonIgnore
    private String password;

    /** 与业务 User.UserStatus 一致：仅 ENABLED、且未 LOCKED 时可正常访问 */
    @Builder.Default
    private boolean accountNonLocked = true;

    @Builder.Default
    private boolean enabled = true;

    private Collection<? extends GrantedAuthority> authorities;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
