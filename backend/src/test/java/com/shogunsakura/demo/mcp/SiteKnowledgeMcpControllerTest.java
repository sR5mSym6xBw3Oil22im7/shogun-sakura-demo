package com.shogunsakura.demo.mcp;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.shogunsakura.demo.dto.SiteContentResult;
import com.shogunsakura.demo.dto.SitePageContent;
import com.shogunsakura.demo.service.SiteContentService;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SiteKnowledgeMcpController.class)
@Import(McpAuthentication.class)
@TestPropertySource(properties = "MCP_INTERNAL_TOKEN=test-token")
class SiteKnowledgeMcpControllerTest {

  @Autowired
  MockMvc mockMvc;

  @MockBean
  SiteContentService siteContentService;

  @Test
  void rejectsMissingToken() throws Exception {
    mockMvc.perform(post("/mcp")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void rejectsWrongToken() throws Exception {
    mockMvc.perform(post("/mcp")
            .contentType(MediaType.APPLICATION_JSON)
            .header(McpAuthentication.HEADER_NAME, "wrong")
            .content("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void listsSiteContentTool() throws Exception {
    mockMvc.perform(post("/mcp")
            .contentType(MediaType.APPLICATION_JSON)
            .header(McpAuthentication.HEADER_NAME, "test-token")
            .content("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.tools[0].name").value(SiteKnowledgeMcpController.TOOL_NAME));
  }

  @Test
  void callsSiteContentTool() throws Exception {
    when(siteContentService.getSiteContent()).thenReturn(new SiteContentResult(
        List.of(new SitePageContent("商品", "http://127.0.0.1/frontend/", "価格は4,800円です。")),
        OffsetDateTime.parse("2026-07-19T10:00:00+09:00"),
        List.of()));

    mockMvc.perform(post("/mcp")
            .contentType(MediaType.APPLICATION_JSON)
            .header(McpAuthentication.HEADER_NAME, "test-token")
            .content("{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":\"get_shogun_sakura_site_content\",\"arguments\":{}}}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.content[0].text").value(org.hamcrest.Matchers.containsString("価格は4,800円です。")));
  }
}
