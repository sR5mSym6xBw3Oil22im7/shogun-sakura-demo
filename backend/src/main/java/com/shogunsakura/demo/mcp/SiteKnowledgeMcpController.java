package com.shogunsakura.demo.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shogunsakura.demo.service.SiteContentService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SiteKnowledgeMcpController {

  public static final String TOOL_NAME = "get_shogun_sakura_site_content";

  private final SiteContentService siteContentService;
  private final McpAuthentication authentication;
  private final ObjectMapper objectMapper;

  public SiteKnowledgeMcpController(
      SiteContentService siteContentService,
      McpAuthentication authentication,
      ObjectMapper objectMapper) {
    this.siteContentService = siteContentService;
    this.authentication = authentication;
    this.objectMapper = objectMapper;
  }

  @PostMapping(path = "/mcp", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> handle(
      @RequestHeader(name = McpAuthentication.HEADER_NAME, required = false) String token,
      @RequestBody JsonNode request) {
    if (!authentication.isValid(token)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(idOf(request), -32001, "Unauthorized"));
    }

    String method = request.path("method").asText("");
    Object id = idOf(request);

    return switch (method) {
      case "initialize" -> ResponseEntity.ok(success(id, Map.of(
          "protocolVersion", "2025-06-18",
          "serverInfo", Map.of("name", "shogun-sakura-site-knowledge", "version", "1.0.0"),
          "capabilities", Map.of("tools", Map.of())
      )));
      case "tools/list" -> ResponseEntity.ok(success(id, Map.of("tools", tools())));
      case "tools/call" -> handleToolCall(id, request);
      default -> ResponseEntity.badRequest().body(error(id, -32601, "Method not found"));
    };
  }

  private ResponseEntity<Map<String, Object>> handleToolCall(Object id, JsonNode request) {
    String name = request.path("params").path("name").asText("");
    if (!TOOL_NAME.equals(name)) {
      return ResponseEntity.badRequest().body(error(id, -32602, "Unknown tool"));
    }

    Map<String, Object> toolResult = objectMapper.convertValue(siteContentService.getSiteContent(), Map.class);
    return ResponseEntity.ok(success(id, Map.of(
        "content", java.util.List.of(Map.of(
            "type", "text",
            "text", writeJson(toolResult)
        )),
        "isError", false
    )));
  }

  private java.util.List<Map<String, Object>> tools() {
    return java.util.List.of(Map.of(
        "name", TOOL_NAME,
        "description", "SHOGUN SAKURA公式デモサイトの固定許可ページ本文を返します。",
        "inputSchema", Map.of(
            "type", "object",
            "properties", Map.of(),
            "additionalProperties", false
        )
    ));
  }

  private Map<String, Object> success(Object id, Object result) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("jsonrpc", "2.0");
    response.put("id", id);
    response.put("result", result);
    return response;
  }

  private Map<String, Object> error(Object id, int code, String message) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("jsonrpc", "2.0");
    response.put("id", id);
    response.put("error", Map.of("code", code, "message", message));
    return response;
  }

  private Object idOf(JsonNode request) {
    JsonNode id = request == null ? null : request.get("id");
    if (id == null || id.isNull()) {
      return null;
    }
    if (id.isNumber()) {
      return id.longValue();
    }
    return id.asText();
  }

  private String writeJson(Map<String, Object> value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception ex) {
      return "{\"pages\":[],\"errors\":[\"JSON serialization failed\"]}";
    }
  }
}
