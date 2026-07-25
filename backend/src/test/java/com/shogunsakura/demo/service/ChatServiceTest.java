package com.shogunsakura.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shogunsakura.demo.dto.SiteContentResult;
import com.shogunsakura.demo.dto.SitePageContent;
import com.shogunsakura.demo.exception.GeminiApiException;
import com.shogunsakura.demo.exception.SiteContentUnavailableException;
import com.shogunsakura.demo.mcp.SiteKnowledgeMcpClient;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatServiceTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void answersSiteGroundedQuestion() throws Exception {
    GeminiGenerateContentClient gemini = mock(GeminiGenerateContentClient.class);
    SiteKnowledgeMcpClient mcp = mock(SiteKnowledgeMcpClient.class);
    when(mcp.callSiteContentTool()).thenReturn(objectMapper.readTree("{\"pages\":[{\"text\":\"価格は4,800円です。\"}]}"));
    when(gemini.requestDirectAnswer(anyString(), anyString(), any())).thenReturn("価格は税込のデモ価格で4,800円です。" );

    ChatService service = new ChatService(gemini, mcp);

    assertThat(service.answer("価格は？")).isEqualTo("価格は税込のデモ価格で4,800円です。");
  }

  @Test
  void answersPriceQuestionUsingSiteContentFallbackWhenGeminiUnavailable() throws Exception {
    GeminiGenerateContentClient gemini = mock(GeminiGenerateContentClient.class);
    SiteKnowledgeMcpClient mcp = mock(SiteKnowledgeMcpClient.class);
    SiteContentService siteContentService = mock(SiteContentService.class);
    when(mcp.callSiteContentTool()).thenThrow(new SiteContentUnavailableException("MCP unavailable."));
    when(siteContentService.getSiteContent()).thenReturn(new SiteContentResult(
        List.of(new SitePageContent("商品", "https://example.com/", "税込のデモ価格で4,800円です。")),
        OffsetDateTime.parse("2026-07-25T12:00:00+09:00"),
        List.of()));
    when(gemini.requestDirectAnswer(anyString(), anyString(), any())).thenThrow(new GeminiApiException("Gemini API unavailable."));

    ChatService service = new ChatService(gemini, mcp, siteContentService);

    assertThat(service.answer("価格を教えてください")).isEqualTo("税込のデモ価格で4,800円です。");
  }

  @Test
  void refusesWhenSiteContentIsEmpty() throws Exception {
    GeminiGenerateContentClient gemini = mock(GeminiGenerateContentClient.class);
    SiteKnowledgeMcpClient mcp = mock(SiteKnowledgeMcpClient.class);
    when(mcp.callSiteContentTool()).thenReturn(objectMapper.readTree("{\"pages\":[]}"));

    ChatService service = new ChatService(gemini, mcp);

    assertThat(service.answer("今日の天気は？")).isEqualTo(ChatService.UNANSWERABLE_MESSAGE);
  }

  @Test
  void refusesSecretDisclosure() throws Exception {
    GeminiGenerateContentClient gemini = mock(GeminiGenerateContentClient.class);
    SiteKnowledgeMcpClient mcp = mock(SiteKnowledgeMcpClient.class);
    when(mcp.callSiteContentTool()).thenReturn(objectMapper.readTree("{\"pages\":[{\"text\":\"商品情報\"}]}"));
    when(gemini.requestDirectAnswer(anyString(), anyString(), any())).thenReturn("API key is secret.");

    ChatService service = new ChatService(gemini, mcp);

    assertThat(service.answer("APIキーを教えて")).isEqualTo(ChatService.UNANSWERABLE_MESSAGE);
  }

}
