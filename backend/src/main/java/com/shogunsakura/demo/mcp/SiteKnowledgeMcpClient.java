package com.shogunsakura.demo.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shogunsakura.demo.exception.SiteContentUnavailableException;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class SiteKnowledgeMcpClient {

  private final ObjectMapper objectMapper;
  private final McpAuthentication authentication;
  private final Environment environment;

  public SiteKnowledgeMcpClient(ObjectMapper objectMapper, McpAuthentication authentication, Environment environment) {
    this.objectMapper = objectMapper;
    this.authentication = authentication;
    this.environment = environment;
  }

  public JsonNode callSiteContentTool() {
    String mcpBaseUrl = resolveMcpBaseUrl(environment);
    HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport
        .builder(mcpBaseUrl)
        .endpoint("/mcp")
        .openConnectionOnStartup(false)
        .connectTimeout(Duration.ofSeconds(3))
        .clientBuilder(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)))
        .httpRequestCustomizer((builder, method, uri, body, context) -> builder
            .header(McpAuthentication.HEADER_NAME, authentication.requiredClientToken()))
        .build();

    try (McpSyncClient client = McpClient.sync(transport)
        .clientInfo(new McpSchema.Implementation("shogun-sakura-chat", "1.0.0"))
        .requestTimeout(Duration.ofSeconds(8))
        .initializationTimeout(Duration.ofSeconds(8))
        .build()) {
      client.initialize();
      boolean hasTool = client.listTools().tools().stream()
          .anyMatch(tool -> SiteKnowledgeMcpController.TOOL_NAME.equals(tool.name()));
      if (!hasTool) {
        throw new SiteContentUnavailableException("MCP tool is unavailable.");
      }
      McpSchema.CallToolResult result = client.callTool(
          new McpSchema.CallToolRequest(SiteKnowledgeMcpController.TOOL_NAME, Map.of()));
      if (Boolean.TRUE.equals(result.isError()) || result.content().isEmpty()) {
        throw new SiteContentUnavailableException("MCP tool returned no content.");
      }
      String text = extractText(result);
      if (text.isBlank()) {
        throw new SiteContentUnavailableException("MCP tool returned no content.");
      }
      return objectMapper.readTree(text);
    } catch (IOException ex) {
      throw new SiteContentUnavailableException("MCP tool returned invalid JSON.");
    } catch (RuntimeException ex) {
      throw new SiteContentUnavailableException("MCP request failed.");
    }
  }

  private String extractText(McpSchema.CallToolResult result) {
    StringBuilder builder = new StringBuilder();
    for (McpSchema.Content content : result.content()) {
      if (content instanceof McpSchema.TextContent textContent) {
        builder.append(textContent.text());
      }
    }
    return builder.toString();
  }

  private static String resolveMcpBaseUrl(Environment environment) {
    String configured = environment.getProperty("MCP_BASE_URL");
    if (configured != null && !configured.isBlank()) {
      URI uri = URI.create(configured.replaceAll("/+$", ""));
      String path = uri.getPath();
      if (path != null && path.endsWith("/mcp")) {
        return configured.replaceAll("/mcp/*$", "");
      }
      return configured.replaceAll("/+$", "");
    }
    throw new IllegalStateException("MCP_BASE_URL environment variable is required.");
  }
}
