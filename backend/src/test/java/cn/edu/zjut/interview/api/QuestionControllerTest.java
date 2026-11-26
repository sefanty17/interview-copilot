package cn.edu.zjut.interview.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 题库只读接口验收（issue #1 的「题库」那一半）。
 *
 * <p>种子是 java / frontend 各 6 题（{@code questions-h2.sql}），所以断言用
 * 「≥ 实际条数」而不是写死 20：本用例验的是接口行为，题量扩充由种子文件负责，
 * 两者解耦后加题不用改测试。
 *
 * <p>但「不传 limit 时至少要够展示」这条仍按 {@code DEFAULT_LIMIT} 断言，
 * 因为它是接口契约的一部分（issue 的口径是「每岗位 ≥20 题」）。
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class QuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 按岗位取题返回该岗位的题目() throws Exception {
        mockMvc.perform(get("/api/questions").param("jobCode", "java").param("limit", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].jobCode").value("java"))
                .andExpect(jsonPath("$[0].text").isNotEmpty())
                .andExpect(jsonPath("$[0].followUps").isArray());
    }

    @Test
    void 前端岗位也能取到题() throws Exception {
        mockMvc.perform(get("/api/questions").param("jobCode", "frontend").param("limit", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].jobCode").value("frontend"));
    }

    /** 英文 code 与中文标签都要认：外部调用方两种写法都可能出现。 */
    @Test
    void 按英文题型过滤() throws Exception {
        mockMvc.perform(get("/api/questions")
                        .param("jobCode", "java")
                        .param("type", "technical")
                        .param("limit", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].typeCode").value(everyItem(is("technical"))));
    }

    @Test
    void 按中文题型过滤() throws Exception {
        mockMvc.perform(get("/api/questions")
                        .param("jobCode", "java")
                        .param("type", "场景题")
                        .param("limit", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].type").value(everyItem(is("场景题"))));
    }

    /** 缺 jobCode 是调用方错误：400，而不是「整库返回」。 */
    @Test
    void 缺jobCode返回400() throws Exception {
        mockMvc.perform(get("/api/questions"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 未知题型返回400() throws Exception {
        mockMvc.perform(get("/api/questions").param("jobCode", "java").param("type", "不存在的题型"))
                .andExpect(status().isBadRequest());
    }

    /** 没录题的岗位是空数组而不是 404：接口存在，只是这个岗位还没有题。 */
    @Test
    void 没有题库的岗位返回空数组() throws Exception {
        mockMvc.perform(get("/api/questions").param("jobCode", "no-such-job"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    /** limit 必须真的截断，否则前端「只要 5 道」的请求会白拿整库。 */
    @Test
    void limit生效() throws Exception {
        mockMvc.perform(get("/api/questions").param("jobCode", "java").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    /** offset 配合 limit 做分页，两页不重叠。 */
    @Test
    void offset生效且两页不重叠() throws Exception {
        String first = mockMvc.perform(get("/api/questions")
                        .param("jobCode", "java").param("limit", "1").param("offset", "0"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(get("/api/questions")
                        .param("jobCode", "java").param("limit", "1").param("offset", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertNotEquals(first, second,
                "offset=0 与 offset=1 应返回不同的题");
    }

    /** 超上限的 limit 被截到 100，不会一次拉走整库。 */
    @Test
    void limit超过上限被截断() throws Exception {
        mockMvc.perform(get("/api/questions").param("jobCode", "java").param("limit", "9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.lessThanOrEqualTo(100)));
    }
}
