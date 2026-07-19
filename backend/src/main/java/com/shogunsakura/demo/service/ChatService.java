package com.shogunsakura.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.shogunsakura.demo.exception.GeminiApiException;
import com.shogunsakura.demo.exception.SiteContentUnavailableException;
import com.shogunsakura.demo.mcp.SiteKnowledgeMcpClient;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

  public static final String UNANSWERABLE_MESSAGE = "このサイト内に記載がないため、お答えできません。";

  private final GeminiGenerateContentClient geminiClient;
  private final SiteKnowledgeMcpClient mcpClient;
  private final String systemInstruction;

  public ChatService(GeminiGenerateContentClient geminiClient, SiteKnowledgeMcpClient mcpClient) {
    this.geminiClient = geminiClient;
    this.mcpClient = mcpClient;
    this.systemInstruction = loadSystemInstruction();
  }

  public String answer(String rawMessage) {
    String message = rawMessage == null ? "" : rawMessage.trim();
    try {
      geminiClient.requestFunctionCall(message, systemInstruction);
      JsonNode siteContent = mcpClient.callSiteContentTool();
      if (!hasUsableSiteContent(siteContent)) {
        return UNANSWERABLE_MESSAGE;
      }
      String answer = geminiClient.requestFinalAnswer(message, systemInstruction, siteContent).trim();
      if (answer.isBlank() || revealsRestrictedInformation(answer)) {
        return UNANSWERABLE_MESSAGE;
      }
      return answer;
    } catch (GeminiApiException | SiteContentUnavailableException | IllegalStateException ex) {
      return UNANSWERABLE_MESSAGE;
    }
  }

  private boolean hasUsableSiteContent(JsonNode siteContent) {
    for (JsonNode page : siteContent.path("pages")) {
      if (!page.path("text").asText("").isBlank()) {
        return true;
      }
    }
    return false;
  }

  private boolean revealsRestrictedInformation(String answer) {
    String lower = answer.toLowerCase();
    return lower.contains("api key")
        || lower.contains("x-goog-api-key")
        || lower.contains("mcp_internal_token")
        || lower.contains("spring_datasource_password");
  }

  private String loadSystemInstruction() {
    try {
      return new ClassPathResource("chatbot-system-prompt.txt")
          .getContentAsString(StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new IllegalStateException("chatbot-system-prompt.txt could not be loaded.", ex);
    }
  }
}
