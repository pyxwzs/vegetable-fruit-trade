package com.trade.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trade.config.WeChatMiniProgramProperties;
import com.trade.dto.WxSessionDTO;
import com.trade.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class WeChatMiniProgramService {

    private static final String CODE2SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";

    private final WeChatMiniProgramProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public WxSessionDTO code2Session(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BusinessException("微信登录凭证无效");
        }
        if (useMock()) {
            WxSessionDTO mock = new WxSessionDTO();
            mock.setOpenid("dev_" + DigestUtils.md5Hex(code).substring(0, 20));
            mock.setSessionKey("mock_session_key");
            return mock;
        }

        String url = UriComponentsBuilder.fromHttpUrl(CODE2SESSION_URL)
                .queryParam("appid", properties.getAppId())
                .queryParam("secret", properties.getAppSecret())
                .queryParam("js_code", code.trim())
                .queryParam("grant_type", "authorization_code")
                .build(true)
                .toUriString();

        String body;
        try {
            body = restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            throw new BusinessException("微信登录失败，请稍后重试");
        }

        WxSessionDTO session;
        try {
            session = objectMapper.readValue(body, WxSessionDTO.class);
        } catch (Exception e) {
            throw new BusinessException("微信登录失败，请稍后重试");
        }

        if (session.getErrcode() != null && session.getErrcode() != 0) {
            throw new BusinessException("微信登录失败：" + (session.getErrmsg() != null ? session.getErrmsg() : "未知错误"));
        }
        if (!StringUtils.hasText(session.getOpenid())) {
            throw new BusinessException("微信登录失败，未获取到用户标识");
        }
        return session;
    }

    private boolean useMock() {
        return properties.isMockEnabled() && !StringUtils.hasText(properties.getAppSecret());
    }
}
