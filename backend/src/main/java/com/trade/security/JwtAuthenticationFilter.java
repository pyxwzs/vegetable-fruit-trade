package com.trade.security;

import com.trade.tenant.TenantContext;
import com.trade.tenant.TenantFilterManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final TenantFilterManager tenantFilterManager;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        log.debug("处理请求: {}, 方法: {}", requestURI, request.getMethod());

        if (requestURI.contains("/auth/config")
                || requestURI.contains("/auth/platform/login")
                || requestURI.contains("/auth/wx/login")
                || requestURI.contains("/auth/wx/register")
                || requestURI.contains("/auth/refresh")) {
            log.debug("放行认证相关匿名请求: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = getJwtFromRequest(request);
            log.debug("从请求中获取JWT: {}", jwt != null ? "存在" : "不存在");

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt) && tokenProvider.isAccessToken(jwt)) {
                String username = tokenProvider.getUsernameFromToken(jwt);
                Long tenantId = tokenProvider.getTenantIdFromToken(jwt);
                log.debug("JWT有效，租户: {}, 用户名: {}", tenantId, username);

                if (tenantId != null) {
                    TenantContext.set(tenantId);
                    tenantFilterManager.enableIfPresent();
                }

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()) {
                    log.warn("用户 {} 已禁用或锁定，拒绝访问", username);
                    filterChain.doFilter(request, response);
                    return;
                }
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("用户认证成功: {}", username);
            } else {
                log.debug("JWT无效或不存在");
            }
        } catch (Exception ex) {
            log.error("JWT认证失败: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        log.debug("Authorization头: {}", bearerToken);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
