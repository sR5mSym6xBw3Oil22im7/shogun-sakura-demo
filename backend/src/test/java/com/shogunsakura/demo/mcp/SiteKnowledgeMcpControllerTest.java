package com.shogunsakura.demo.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.shogunsakura.demo.dto.SiteContentResult;
import com.shogunsakura.demo.dto.SitePageContent;
import com.shogunsakura.demo.service.SiteContentService;
import java.time.OffsetDateTime;
import java.util.List;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {
        SiteKnowledgeMcpConfiguration.class,
        SiteKnowledgeMcpClient.class,
        McpAuthentication.class,
        org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration.class,
        SiteKnowledgeMcpControllerTest.TestConfig.class
    })
@TestPropertySource(properties = {
    "MCP_INTERNAL_TOKEN=test-token",
    "MCP_BASE_URL=http://127.0.0.1:${local.server.port}"
})
class SiteKnowledgeMcpControllerTest {

  @LocalServerPort
  int port;

  @Autowired
  SiteKnowledgeMcpClient mcpClient;

  @Autowired
  TestRestTemplate restTemplate;

  @MockBean
  SiteContentService siteContentService;

  @Test
  void sdkClientInitializesListsToolAndCallsSiteContentToolOverHttp() {
    when(siteContentService.getSiteContent()).thenReturn(new SiteContentResult(
        List.of(new SitePageContent("商品", "http://127.0.0.1/frontend/", "価格は4,800円です。")),
        OffsetDateTime.parse("2026-07-19T10:00:00+09:00"),
        List.of()));

    JsonNode result = mcpClient.callSiteContentTool();

    assertThat(result.path("pages").path(0).path("text").asText()).contains("価格は4,800円です。");
  }

  @Test
  void rejectsMissingToken() {
    ResponseEntity<String> response = restTemplate.exchange(
        "http://127.0.0.1:" + port + "/mcp",
        HttpMethod.POST,
        new HttpEntity<>("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}", streamableHeaders(null, null)),
        String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void rejectsWrongToken() {
    ResponseEntity<String> response = restTemplate.exchange(
        "http://127.0.0.1:" + port + "/mcp",
        HttpMethod.POST,
        new HttpEntity<>("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}", streamableHeaders("wrong", null)),
        String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void rejectsBrowserOrigin() throws Exception {
    HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/mcp"))
        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .header("Accept", "application/json, text/event-stream")
        .header("Origin", "http://127.0.0.1:5500")
        .header(McpAuthentication.HEADER_NAME, "test-token")
        .POST(HttpRequest.BodyPublishers.ofString("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}"))
        .build();
    HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

    assertThat(response.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
  }

  @Test
  void rejectsGetBecauseSseIsNotProvided() {
    ResponseEntity<String> response = restTemplate.exchange(
        "http://127.0.0.1:" + port + "/mcp",
        HttpMethod.GET,
        new HttpEntity<>(streamableHeaders("test-token", null)),
        String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
  }

  private HttpHeaders streamableHeaders(String token, String origin) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM));
    if (token != null) {
      headers.set(McpAuthentication.HEADER_NAME, token);
    }
    if (origin != null) {
      headers.set("Origin", origin);
    }
    return headers;
  }

  @TestConfiguration
  static class TestConfig {

    @Bean
    com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
      return new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
    }
  }
}
