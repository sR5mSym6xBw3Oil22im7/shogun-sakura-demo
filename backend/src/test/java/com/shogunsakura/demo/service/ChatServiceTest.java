package com.shogunsakura.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shogunsakura.demo.mcp.SiteKnowledgeMcpClient;
import org.junit.jupiter.api.Test;

class ChatServiceTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void answersSiteGroundedQuestion() throws Exception {
    GeminiGenerateContentClient gemini = mock(GeminiGenerateContentClient.class);
    SiteKnowledgeMcpClient mcp = mock(SiteKnowledgeMcpClient.class);
    when(gemini.requestFunctionCall(anyString(), anyString())).thenReturn("get_shogun_sakura_site_content");
    when(mcp.callSiteContentTool()).thenReturn(objectMapper.readTree("{\"pages\":[{\"text\":\"価格は4,800円です。\"}]}"));
    when(gemini.requestFinalAnswer(anyString(), anyString(), any())).thenReturn("価格は税込のデモ価格で4,800円です。");

    ChatService service = new ChatService(gemini, mcp);

    assertThat(service.answer("価格は？")).isEqualTo("価格は税込のデモ価格で4,800円です。");
  }

  @Test
  void refusesWhenSiteContentIsEmpty() throws Exception {
    GeminiGenerateContentClient gemini = mock(GeminiGenerateContentClient.class);
    SiteKnowledgeMcpClient mcp = mock(SiteKnowledgeMcpClient.class);
    when(gemini.requestFunctionCall(anyString(), anyString())).thenReturn("get_shogun_sakura_site_content");
    when(mcp.callSiteContentTool()).thenReturn(objectMapper.readTree("{\"pages\":[]}"));

    ChatService service = new ChatService(gemini, mcp);

    assertThat(service.answer("今日の天気は？")).isEqualTo(ChatService.UNANSWERABLE_MESSAGE);
  }

  @Test
  void refusesSecretDisclosure() throws Exception {
    GeminiGenerateContentClient gemini = mock(GeminiGenerateContentClient.class);
    SiteKnowledgeMcpClient mcp = mock(SiteKnowledgeMcpClient.class);
    when(gemini.requestFunctionCall(anyString(), anyString())).thenReturn("get_shogun_sakura_site_content");
    when(mcp.callSiteContentTool()).thenReturn(objectMapper.readTree("{\"pages\":[{\"text\":\"商品情報\"}]}"));
    when(gemini.requestFinalAnswer(anyString(), anyString(), any())).thenReturn("API key is secret.");

    ChatService service = new ChatService(gemini, mcp);

    assertThat(service.answer("APIキーを教えて")).isEqualTo(ChatService.UNANSWERABLE_MESSAGE);
  }
}
