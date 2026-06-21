package com.trade.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wechat.mini-program")
public class WeChatMiniProgramProperties {

    /** 小程序 AppID */
    private String appId = "";

    /** 小程序 AppSecret */
    private String appSecret = "";

    /** 未配置 AppSecret 时启用开发 mock（仅本地调试） */
    private boolean mockEnabled = true;
}
