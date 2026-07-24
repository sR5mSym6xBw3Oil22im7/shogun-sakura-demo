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
import org.springframework.beans.factory.annotation.Autowired;
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

  @Autowired
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

  public GeminiFunctionCall requestFunctionCall(String userMessage, String systemInstruction) {
    Map<String, Object> request = Map.of(
        "systemInstruction", textContent(systemInstruction),
        "contents", List.of(Map.of(
            "role", "USER",
            "parts", List.of(Map.of("text", userMessage))
        )),
        "tools", List.of(Map.of(
            "name", SiteKnowledgeMcpController.TOOL_NAME,
            "description", "SHOGUN SAKURA公式デモサイトの固定許可ページ本文を取得します。",
            "parameters", Map.of(
                "type", "OBJECT",
                "properties", Map.of()
            )
        )),
        "toolConfig", Map.of(
            "functionCallingConfig", Map.of(
                "mode", "AUTO",
                "allowedFunctionNames", List.of(SiteKnowledgeMcpController.TOOL_NAME)
            )
        )
    );

    JsonNode response = post(request);
    JsonNode modelContent = response.path("candidates").path(0).path("content");
    JsonNode functionCall = extractFunctionCall(modelContent);
    String toolName = functionCall.path("name").asText("");
    if (!SiteKnowledgeMcpController.TOOL_NAME.equals(toolName) || !functionCall.path("args").isObject()) {
      throw new GeminiApiException("Unexpected Gemini function call.");
    }
    return new GeminiFunctionCall(modelContent.deepCopy(), functionCall.deepCopy());
  }

  public String requestFinalAnswer(
      String userMessage,
      String systemInstruction,
      GeminiFunctionCall functionCall,
      JsonNode siteContent) {
    Map<String, Object> functionResponse = new java.util.LinkedHashMap<>();
    functionResponse.put("name", SiteKnowledgeMcpController.TOOL_NAME);
    String id = functionCall.functionCall().path("id").asText("");
    if (!id.isBlank()) {
      functionResponse.put("id", id);
    }
    functionResponse.put("response", objectMapper.convertValue(siteContent, Map.class));
    Map<String, Object> request = Map.of(
        "systemInstruction", textContent(systemInstruction),
        "contents", List.of(
            Map.of("role", "user", "parts", List.of(Map.of("text", userMessage))),
            objectMapper.convertValue(functionCall.modelContent(), Map.class),
            Map.of("role", "user", "parts", List.of(Map.of("functionResponse", functionResponse)))
        ),
        "tools", List.of(Map.of(
            "name", SiteKnowledgeMcpController.TOOL_NAME,
            "description", "SHOGUN SAKURA公式デモサイトの固定許可ページ本文を取得します。",
            "parameters", Map.of(
                "type", "OBJECT",
                "properties", Map.of()
            )
        )),
        "toolConfig", Map.of(
            "functionCallingConfig", Map.of(
                "mode", "AUTO",
                "allowedFunctionNames", List.of(SiteKnowledgeMcpController.TOOL_NAME)
            )
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

  public String requestDirectAnswer(String userMessage, String systemInstruction, JsonNode siteContent) {
    String prompt = buildDirectPrompt(userMessage, siteContent);
    Map<String, Object> request = Map.of(
        "systemInstruction", textContent(systemInstruction),
        "contents", List.of(Map.of(
            "role", "USER",
            "parts", List.of(Map.of("text", prompt))
        )),
        "generationConfig", Map.of(
            "temperature", 0,
            "maxOutputTokens", 256
        )
    );

    JsonNode response = post(request);
    String answer = extractText(response);
    if (answer.isBlank()) {
      throw new GeminiApiException("Gemini returned an empty direct answer.");
    }
    return answer;
  }

  private String buildDirectPrompt(String userMessage, JsonNode siteContent) {
    StringBuilder builder = new StringBuilder();
    builder.append("以下のサイト本文を参照して、質問に答えてください。\n");
    builder.append("サイト本文:\n");
    for (JsonNode page : siteContent.path("pages")) {
      String title = page.path("title").asText("");
      String url = page.path("url").asText("");
      String text = page.path("text").asText("");
      if (!title.isBlank()) {
        builder.append("タイトル: ").append(title).append("\n");
      }
      if (!url.isBlank()) {
        builder.append("URL: ").append(url).append("\n");
      }
      if (!text.isBlank()) {
        builder.append(text).append("\n");
      }
      builder.append("\n");
    }
    builder.append("質問: ").append(userMessage).append("\n");
    builder.append("サイト本文に該当する回答がなければ、\"このサイト内に記載がないため、お答えできません。\"だけを回答してください。\n");
    return builder.toString();
  }

  private String siteContentText(JsonNode siteContent) {
    StringBuilder builder = new StringBuilder();
    builder.append("サイト本文:\n");
    for (JsonNode page : siteContent.path("pages")) {
      String title = page.path("title").asText("");
      String url = page.path("url").asText("");
      String text = page.path("text").asText("");
      if (!title.isBlank()) {
        builder.append("タイトル: ").append(title).append("\n");
      }
      if (!url.isBlank()) {
        builder.append("URL: ").append(url).append("\n");
      }
      if (!text.isBlank()) {
        builder.append(text).append("\n");
      }
      builder.append("\n");
    }
    if (siteContent.has("errors") && siteContent.path("errors").isArray()) {
      for (JsonNode error : siteContent.path("errors")) {
        if (!error.asText().isBlank()) {
          builder.append("ERROR: ").append(error.asText()).append("\n");
        }
      }
    }
    return builder.toString();
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

  private JsonNode extractFunctionCall(JsonNode modelContent) {
    if (!"model".equals(modelContent.path("role").asText(""))) {
      throw new GeminiApiException("Gemini did not return model content.");
    }
    for (JsonNode part : modelContent.path("parts")) {
      JsonNode functionCall = part.path("functionCall");
      if (functionCall.isObject() && !functionCall.path("name").asText("").isBlank()) {
        return functionCall;
      }
    }
    throw new GeminiApiException("Gemini did not return a function call.");
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

  public record GeminiFunctionCall(JsonNode modelContent, JsonNode functionCall) {
  }
}
