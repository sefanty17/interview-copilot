package cn.edu.zjut.interview.dto.worldbook;

import cn.edu.zjut.interview.domain.WorldbookEntry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/** 世界书条目视图：{@code activationKeys} 由 JSON 字符串还原成数组。 */
public record WorldbookEntryView(String key, String name, boolean enabled, int priority,
                                 String activationType, List<String> activationKeys, String content) {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    public static WorldbookEntryView from(WorldbookEntry entry) {
        return new WorldbookEntryView(
                entry.getEntryKey(),
                entry.getName(),
                Boolean.TRUE.equals(entry.getEnabled()),
                entry.getPriority() == null ? 0 : entry.getPriority(),
                entry.getActivationType(),
                parseKeys(entry.getActivationKeys()),
                entry.getContent());
    }

    private static List<String> parseKeys(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return List.copyOf(MAPPER.readValue(json, STRING_LIST));
        } catch (Exception e) {
            // 坏 JSON 不该让详情接口 500：当成没有关键词
            return List.of();
        }
    }
}
