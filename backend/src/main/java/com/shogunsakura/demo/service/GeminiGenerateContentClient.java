package com.shogunsakura.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shogunsakura.demo.config.GeminiApiConfig;
import com.shogunsakura.demo.exception.GeminiApiException;
import com.shogunsakura.demo.mcp.SiteKnowledgeMcpController;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GeminiGenerateContentClient {

  private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
  private static final int MAX_ANSWER_CHARS = 900;

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final GeminiApiConfig config;
  private final URI endpoint;

  public GeminiGenerateContentClient(
      ObjectMapper objectMapper,
      GeminiApiConfig config,
      @Value("${GEMINI_API_BASE_URL:" + DEFAULT_BASE_URL + "}") String baseUrl) {
    this(HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build(), objectMapper, config, URI.create(baseUrl.replaceAll("/+$", "")
            + "/models/" + GeminiApiConfig.MODEL_ID + ":generateContent"));
  }

  GeminiGenerateContentClient(
      HttpClient httpClient,
      ObjectMapper objectMapper,
      GeminiApiConfig config,
      URI endpoint) {
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
    this.config = config;
    this.endpoint = endpoint;
  }

  public String requestFunctionCall(String userMessage, String systemInstruction) {
    Map<String, Object> request = Map.of(
        "systemInstruction", textContent(systemInstruction),
        "contents", List.of(Map.of(
            "role", "user",
            "parts", List.of(Map.of("text", userMessage))
        )),
        "tools", List.of(Map.of(
            "functionDeclarations", List.of(Map.of(
                "name", SiteKnowledgeMcpController.TOOL_NAME,
                "description", "SHOGUN SAKURA公式デモサイトの固定許可ページ本文を取得します。",
                "parameters", Map.of(
                    "type", "OBJECT",
                    "properties", Map.of()
                )
            ))
        )),
        "toolConfig", Map.of(
            "functionCallingConfig", Map.of(
                "mode", "ANY",
                "allowedFunctionNames", List.of(SiteKnowledgeMcpController.TOOL_NAME)
            )
        )
    );

    JsonNode response = post(request);
    String toolName = extractFunctionCallName(response);
    if (!SiteKnowledgeMcpController.TOOL_NAME.equals(toolName)) {
      throw new GeminiApiException("Unexpected Gemini function call.");
    }
    return toolName;
  }

  public String requestFinalAnswer(String userMessage, String systemInstruction, JsonNode siteContent) {
    Map<String, Object> functionCall = Map.of(
        "name", SiteKnowledgeMcpController.TOOL_NAME,
        "args", Map.of()
    );
    Map<String, Object> functionResponse = Map.of(
        "name", SiteKnowledgeMcpController.TOOL_NAME,
        "response", objectMapper.convertValue(siteContent, Map.class)
    );
    Map<String, Object> request = Map.of(
        "systemInstruction", textContent(systemInstruction),
        "contents", List.of(
            Map.of("role", "user", "parts", List.of(Map.of("text", userMessage))),
            Map.of("role", "model", "parts", List.of(Map.of("functionCall", functionCall))),
            Map.of("role", "function", "parts", List.of(Map.of("functionResponse", functionResponse)))
        ),
        "generationConfig", Map.of(
            "temperature", 0,
            "maxOutputTokens", 256
        )
    );

    JsonNode response = post(request);
    String answer = extractText(response);
    if (answer.isBlank()) {
      throw new GeminiApiException("Gemini returned an empty answer.");
    }
    return answer.length() > MAX_ANSWER_CHARS ? answer.substring(0, MAX_ANSWER_CHARS) : answer;
  }

  private JsonNode post(Map<String, Object> payload) {
    try {
      String body = objectMapper.writeValueAsString(payload);
      HttpRequest request = HttpRequest.newBuilder(endpoint)
          .timeout(Duration.ofSeconds(12))
          .header("Content-Type", "application/json")
          .header("x-goog-api-key", config.apiKey())
          .POST(HttpRequest.BodyPublishers.ofString(body))
          .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() == 429) {
        throw new GeminiApiException("Gemini API rate limit.");
      }
      if (response.statusCode() >= 500) {
        throw new GeminiApiException("Gemini API server error.");
      }
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new GeminiApiException("Gemini API request failed.");
      }
      return objectMapper.readTree(response.body());
    } catch (IOException ex) {
      throw new GeminiApiException("Gemini API request failed.", ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new GeminiApiException("Gemini API request interrupted.", ex);
    }
  }

  private Map<String, Object> textContent(String text) {
    return Map.of("parts", List.of(Map.of("text", text)));
  }

  private String extractFunctionCallName(JsonNode response) {
    for (JsonNode candidate : response.path("candidates")) {
      for (JsonNode part : candidate.path("content").path("parts")) {
        String name = part.path("functionCall").path("name").asText("");
        if (!name.isBlank()) {
          return name;
        }
      }
    }
    return "";
  }

  private String extractText(JsonNode response) {
    StringBuilder builder = new StringBuilder();
    for (JsonNode candidate : response.path("candidates")) {
      for (JsonNode part : candidate.path("content").path("parts")) {
        String text = part.path("text").asText("");
        if (!text.isBlank()) {
          builder.append(text);
        }
      }
    }
    return builder.toString().trim();
  }
}
