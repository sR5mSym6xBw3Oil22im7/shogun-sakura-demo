package com.shogunsakura.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.shogunsakura.demo.dto.SiteContentResult;
import com.shogunsakura.demo.exception.GeminiApiException;
import com.shogunsakura.demo.exception.SiteContentUnavailableException;
import com.shogunsakura.demo.mcp.SiteKnowledgeMcpClient;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

  public static final String UNANSWERABLE_MESSAGE = "このサイト内に記載がないため、お答えできません。";

  private static final Pattern PRICE_PATTERN = Pattern.compile("(?:\\d{1,3}(?:,\\d{3})+|\\d+)");

  private final GeminiGenerateContentClient geminiClient;
  private final SiteKnowledgeMcpClient mcpClient;
  private final SiteContentService siteContentService;
  private final String systemInstruction;

  public ChatService(GeminiGenerateContentClient geminiClient, SiteKnowledgeMcpClient mcpClient) {
    this(geminiClient, mcpClient, null);
  }

  public ChatService(GeminiGenerateContentClient geminiClient, SiteKnowledgeMcpClient mcpClient,
      SiteContentService siteContentService) {
    this.geminiClient = geminiClient;
    this.mcpClient = mcpClient;
    this.siteContentService = siteContentService;
    this.systemInstruction = loadSystemInstruction();
  }

  public String answer(String rawMessage) {
    String message = rawMessage == null ? "" : rawMessage.trim();
    if (message.isEmpty()) {
      return UNANSWERABLE_MESSAGE;
    }

    try {
      JsonNode siteContent = resolveSiteContent();
      if (!hasUsableSiteContent(siteContent)) {
        return UNANSWERABLE_MESSAGE;
      }

      try {
        String answer = geminiClient.requestDirectAnswer(message, systemInstruction, siteContent);
        if (answer != null && !answer.isBlank() && !revealsRestrictedInformation(answer)) {
          return answer.trim();
        }
        return fallbackAnswerFromSiteContent(siteContent, message);
      } catch (GeminiApiException | IllegalStateException ex) {
        System.err.println("ChatService answer fail: " + ex.getMessage());
        ex.printStackTrace(System.err);
        return fallbackAnswerFromSiteContent(siteContent, message);
      }
    } catch (SiteContentUnavailableException | IllegalStateException ex) {
      System.err.println("ChatService site content unavailable: " + ex.getMessage());
      ex.printStackTrace(System.err);
      return fallbackAnswerFromSiteContent(null, message);
    }
  }

  private JsonNode resolveSiteContent() {
    try {
      return mcpClient.callSiteContentTool();
    } catch (SiteContentUnavailableException | IllegalStateException ex) {
      if (siteContentService == null) {
        throw ex;
      }
      SiteContentResult result = siteContentService.getSiteContent();
      if (!result.hasUsableText()) {
        throw ex;
      }
      ObjectNode root = objectMapper().createObjectNode();
      ArrayNode pages = root.putArray("pages");
      for (com.shogunsakura.demo.dto.SitePageContent page : result.pages()) {
        ObjectNode pageNode = pages.addObject();
        pageNode.put("title", page.title());
        pageNode.put("url", page.url());
        pageNode.put("text", page.text());
      }
      return root;
    }
  }

  private String fallbackAnswerFromSiteContent(JsonNode siteContent, String message) {
    if (siteContent == null || !hasUsableSiteContent(siteContent)) {
      return UNANSWERABLE_MESSAGE;
    }

    String lowerMessage = message.toLowerCase();
    if (lowerMessage.contains("価格") || lowerMessage.contains("値段") || lowerMessage.contains("いくら")) {
      for (JsonNode page : siteContent.path("pages")) {
        String pageText = page.path("text").asText("");
        Matcher matcher = PRICE_PATTERN.matcher(pageText);
        if (matcher.find()) {
          String amount = matcher.group();
          return "税込のデモ価格で" + amount + "円です。";
        }
      }
    }

    return UNANSWERABLE_MESSAGE;
  }

  private ObjectMapper objectMapper() {
    return new ObjectMapper();
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
