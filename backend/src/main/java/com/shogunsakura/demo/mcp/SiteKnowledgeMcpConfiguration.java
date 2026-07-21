package com.shogunsakura.demo.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shogunsakura.demo.service.SiteContentService;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.server.transport.ServerTransportSecurityException;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SiteKnowledgeMcpConfiguration {

  @Bean
  McpJsonMapper mcpJsonMapper(ObjectMapper objectMapper) {
    return new JacksonMcpJsonMapper(objectMapper);
  }

  @Bean
  HttpServletStreamableServerTransportProvider siteKnowledgeMcpTransportProvider(
      McpJsonMapper mcpJsonMapper,
      McpAuthentication authentication) {
    return HttpServletStreamableServerTransportProvider.builder()
        .jsonMapper(mcpJsonMapper)
        .mcpEndpoint("/mcp")
        .securityValidator(headers -> {
          if (!authentication.isValid(firstHeader(headers, McpAuthentication.HEADER_NAME))) {
            throw new ServerTransportSecurityException(401, "Unauthorized");
          }
          if (!isAllowedOrigin(firstHeader(headers, "Origin"))) {
            throw new ServerTransportSecurityException(403, "Forbidden");
          }
        })
        .build();
  }

  @Bean
  ServletRegistrationBean<HttpServletStreamableServerTransportProvider> siteKnowledgeMcpServlet(
      HttpServletStreamableServerTransportProvider transportProvider) {
    ServletRegistrationBean<HttpServletStreamableServerTransportProvider> registration =
        new ServletRegistrationBean<>(transportProvider, "/mcp");
    registration.setName("siteKnowledgeMcpServlet");
    registration.setLoadOnStartup(1);
    return registration;
  }

  @Bean
  FilterRegistrationBean<Filter> siteKnowledgeMcpSecurityFilter(McpAuthentication authentication) {
    FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
    registration.setFilter((request, response, chain) -> {
      jakarta.servlet.http.HttpServletRequest httpRequest = (jakarta.servlet.http.HttpServletRequest) request;
      HttpServletResponse httpResponse = (HttpServletResponse) response;
      if ("GET".equalsIgnoreCase(httpRequest.getMethod())) {
        httpResponse.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        return;
      }
      if (!authentication.isValid(httpRequest.getHeader(McpAuthentication.HEADER_NAME))) {
        httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return;
      }
      if (!isAllowedOrigin(httpRequest.getHeader("Origin"))) {
        httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
        return;
      }
      chain.doFilter(request, response);
    });
    registration.addUrlPatterns("/mcp");
    registration.setOrder(-100);
    return registration;
  }

  @Bean(destroyMethod = "close")
  McpSyncServer siteKnowledgeMcpServer(
      HttpServletStreamableServerTransportProvider transportProvider,
      SiteContentService siteContentService,
      ObjectMapper objectMapper) {
    return McpServer.sync(transportProvider)
        .serverInfo("shogun-sakura-site-knowledge", "1.0.0")
        .capabilities(McpSchema.ServerCapabilities.builder()
            .tools(true)
            .build())
        .tools(siteContentTool(siteContentService, objectMapper))
        .build();
  }

  private SyncToolSpecification siteContentTool(SiteContentService siteContentService, ObjectMapper objectMapper) {
    McpSchema.Tool tool = McpSchema.Tool.builder(SiteKnowledgeMcpController.TOOL_NAME)
        .description("SHOGUN SAKURA公式デモサイトの固定許可ページ本文を返します。")
        .inputSchema(Map.of(
            "type", "object",
            "properties", Map.of(),
            "additionalProperties", false))
        .build();
    return SyncToolSpecification.builder()
        .tool(tool)
        .callHandler((exchange, request) -> {
          if (!SiteKnowledgeMcpController.TOOL_NAME.equals(request.name()) || !request.arguments().isEmpty()) {
            return McpSchema.CallToolResult.builder()
                .addTextContent("{\"pages\":[],\"errors\":[\"Unknown tool\"]}")
                .isError(true)
                .build();
          }
          Map<String, Object> toolResult = objectMapper.convertValue(siteContentService.getSiteContent(), Map.class);
          return McpSchema.CallToolResult.builder()
              .content(List.of(new McpSchema.TextContent(writeJson(objectMapper, toolResult))))
              .isError(false)
              .build();
        })
        .build();
  }

  private String writeJson(ObjectMapper objectMapper, Map<String, Object> value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception ex) {
      return "{\"pages\":[],\"errors\":[\"JSON serialization failed\"]}";
    }
  }

  private static String firstHeader(Map<String, List<String>> headers, String name) {
    for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
      if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name) && !entry.getValue().isEmpty()) {
        return entry.getValue().getFirst();
      }
    }
    return null;
  }

  private static boolean isAllowedOrigin(String origin) {
    if (origin == null || origin.isBlank()) {
      return true;
    }
    return origin.equals("http://127.0.0.1")
        || origin.equals("http://127.0.0.1:8080")
        || origin.equals("http://localhost:8080");
  }
}
