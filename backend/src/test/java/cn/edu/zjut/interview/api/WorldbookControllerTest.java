package cn.edu.zjut.interview.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 世界书只读接口（issue #1 世界书半）：两本书、详情带条目、未知 code 404。
 *
 * <p>数据由启动扫描 {@code game_data/worldbooks/} 灌入，所以这组用例同时验证了 B2。
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class WorldbookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listReturnsTwoBooks() throws Exception {
        mockMvc.perform(get("/api/worldbooks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].code", containsInAnyOrder("java", "frontend")))
                .andExpect(jsonPath("$[0].entryCount").value(4))
                .andExpect(jsonPath("$[1].entryCount").value(4))
                // 列表不吐条目正文
                .andExpect(jsonPath("$[0].entries").doesNotExist());
    }

    @Test
    void detailReturnsEntriesInFileOrder() throws Exception {
        mockMvc.perform(get("/api/worldbooks/java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("java"))
                .andExpect(jsonPath("$.name").isNotEmpty())
                .andExpect(jsonPath("$.entries", hasSize(4)))
                .andExpect(jsonPath("$.entries[0].key").value("company"))
                .andExpect(jsonPath("$.entries[0].activationType").value("always"))
                .andExpect(jsonPath("$.entries[0].content").isNotEmpty())
                .andExpect(jsonPath("$.entries[*].key", hasItem("stack")))
                .andExpect(jsonPath("$.entries[2].activationType").value("keyword"))
                .andExpect(jsonPath("$.entries[2].activationKeys", hasItem("Spring")));
    }

    @Test
    void unknownCodeReturns404() throws Exception {
        mockMvc.perform(get("/api/worldbooks/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"))
                .andExpect(jsonPath("$.code").value("nope"));
    }
}
