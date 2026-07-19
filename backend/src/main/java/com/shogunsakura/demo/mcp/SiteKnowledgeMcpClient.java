package com.shogunsakura.demo.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shogunsakura.demo.exception.SiteContentUnavailableException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class SiteKnowledgeMcpClient {

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final McpAuthentication authentication;
  private final URI mcpUri;
  private final AtomicLong ids = new AtomicLong(1);

  public SiteKnowledgeMcpClient(ObjectMapper objectMapper, McpAuthentication authentication, Environment environment) {
    this(HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .build(), objectMapper, authentication, resolveMcpUri(environment));
  }

  SiteKnowledgeMcpClient(
      HttpClient httpClient,
      ObjectMapper objectMapper,
      McpAuthentication authentication,
      URI mcpUri) {
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
    this.authentication = authentication;
    this.mcpUri = mcpUri;
  }

  public JsonNode callSiteContentTool() {
    request("initialize", Map.of(
        "protocolVersion", "2025-06-18",
        "clientInfo", Map.of("name", "shogun-sakura-chat", "version", "1.0.0"),
        "capabilities", Map.of()
    ));

    JsonNode listResult = request("tools/list", Map.of());
    boolean hasTool = false;
    for (JsonNode tool : listResult.path("tools")) {
      if (SiteKnowledgeMcpController.TOOL_NAME.equals(tool.path("name").asText())) {
        hasTool = true;
        break;
      }
    }
    if (!hasTool) {
      throw new SiteContentUnavailableException("MCP tool is unavailable.");
    }

    JsonNode callResult = request("tools/call", Map.of(
        "name", SiteKnowledgeMcpController.TOOL_NAME,
        "arguments", Map.of()
    ));
    String text = callResult.path("content").path(0).path("text").asText("");
    if (text.isBlank()) {
      throw new SiteContentUnavailableException("MCP tool returned no content.");
    }

    try {
      return objectMapper.readTree(text);
    } catch (IOException ex) {
      throw new SiteContentUnavailableException("MCP tool returned invalid JSON.");
    }
  }

  private JsonNode request(String method, Map<String, Object> params) {
    try {
      String body = objectMapper.writeValueAsString(Map.of(
          "jsonrpc", "2.0",
          "id", ids.getAndIncrement(),
          "method", method,
          "params", params
      ));
      HttpRequest request = HttpRequest.newBuilder(mcpUri)
          .timeout(Duration.ofSeconds(8))
          .header("Content-Type", "application/json")
          .header(McpAuthentication.HEADER_NAME, authentication.requiredClientToken())
          .POST(HttpRequest.BodyPublishers.ofString(body))
          .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new SiteContentUnavailableException("MCP request failed.");
      }
      JsonNode json = objectMapper.readTree(response.body());
      if (json.has("error")) {
        throw new SiteContentUnavailableException("MCP returned an error.");
      }
      return json.path("result");
    } catch (IOException ex) {
      throw new SiteContentUnavailableException("MCP request failed.");
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new SiteContentUnavailableException("MCP request interrupted.");
    }
  }

  private static URI resolveMcpUri(Environment environment) {
    String configured = environment.getProperty("MCP_BASE_URL");
    if (configured != null && !configured.isBlank()) {
      return URI.create(configured.replaceAll("/+$", "") + "/mcp");
    }
    String port = environment.getProperty("local.server.port",
        environment.getProperty("PORT", environment.getProperty("server.port", "8080")));
    return URI.create("http://127.0.0.1:" + port + "/mcp");
  }
}
