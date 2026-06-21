package com.trade.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trade.constant.MenuKeyConstants;
import com.trade.exception.BusinessException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class MenuKeyUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MenuKeyUtils() {
    }

    public static List<String> resolveEnabledKeys(String menuKeysJson) {
        if (menuKeysJson == null || menuKeysJson.isBlank()) {
            return new ArrayList<>(MenuKeyConstants.ALL);
        }
        try {
            List<String> keys = MAPPER.readValue(menuKeysJson, new TypeReference<List<String>>() {});
            return keys.stream()
                    .filter(MenuKeyConstants.ALL::contains)
                    .collect(Collectors.toList());
        } catch (JsonProcessingException e) {
            return new ArrayList<>(MenuKeyConstants.ALL);
        }
    }

    public static String toJson(List<String> menuKeys) {
        try {
            return MAPPER.writeValueAsString(menuKeys);
        } catch (JsonProcessingException e) {
            throw new BusinessException("菜单配置保存失败");
        }
    }

    public static List<String> normalizeAndValidate(List<String> menuKeys) {
        if (menuKeys == null || menuKeys.isEmpty()) {
            throw new BusinessException("请至少启用一项功能菜单");
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String key : menuKeys) {
            if (key == null || key.isBlank()) {
                continue;
            }
            if (!MenuKeyConstants.ALL.contains(key)) {
                throw new BusinessException("无效的菜单项: " + key);
            }
            unique.add(key);
        }
        if (unique.isEmpty()) {
            throw new BusinessException("请至少启用一项功能菜单");
        }
        return new ArrayList<>(unique);
    }
}
