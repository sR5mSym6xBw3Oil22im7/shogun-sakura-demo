package com.shogunsakura.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shogunsakura.demo.config.GeminiApiConfig;
import com.shogunsakura.demo.exception.GeminiApiException;
import com.shogunsakura.demo.mcp.SiteKnowledgeMcpController;
import com.shogunsakura.demo.service.GeminiGenerateContentClient.GeminiFunctionCall;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class GeminiGenerateContentClientTest {

  private HttpServer server;

  @AfterEach
  void stopServer() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void parsesForcedFunctionCall() throws Exception {
    GeminiGenerateContentClient client = clientWithResponses(200, """
        {"candidates":[{"content":{"role":"model","parts":[{"functionCall":{"name":"get_shogun_sakura_site_content","args":{}}}]}}]}
        """);

    GeminiFunctionCall functionCall = client.requestFunctionCall("価格は？", "system");

    assertThat(functionCall.functionCall().path("name").asText())
        .isEqualTo(SiteKnowledgeMcpController.TOOL_NAME);
    assertThat(functionCall.modelContent().path("role").asText()).isEqualTo("model");
  }

  @Test
  void sendsToolDeclarationWithName() throws Exception {
    AtomicReference<String> requestBody = new AtomicReference<>();
    GeminiGenerateContentClient client = clientWithResponses(requestBody, 200, """
        {"candidates":[{"content":{"role":"model","parts":[{"functionCall":{"name":"get_shogun_sakura_site_content","args":{}}}]}}]}
        """);

    client.requestFunctionCall("価格は？", "system");

    JsonNode sent = new ObjectMapper().readTree(requestBody.get());
    assertThat(sent.path("tools").get(0).path("name").asText())
        .isEqualTo(SiteKnowledgeMcpController.TOOL_NAME);
    assertThat(sent.path("tools").get(0).path("description").asText())
        .isEqualTo("SHOGUN SAKURA公式デモサイトの固定許可ページ本文を取得します。");
  }

  @Test
  void rejectsUnexpectedFunctionCall() throws Exception {
    GeminiGenerateContentClient client = clientWithResponses(200, """
        {"candidates":[{"content":{"role":"model","parts":[{"functionCall":{"name":"other_tool","args":{}}}]}}]}
        """);

    assertThatThrownBy(() -> client.requestFunctionCall("価格は？", "system"))
        .isInstanceOf(GeminiApiException.class)
        .hasMessageNotContaining(new GeminiApiConfig("test-key").apiKey());
  }

  @Test
  void parsesFinalAnswerAndSendsOfficialFunctionResponseShape() throws Exception {
    AtomicReference<String> requestBody = new AtomicReference<>();
    GeminiGenerateContentClient client = clientWithResponses(requestBody, 200, """
        {"candidates":[{"content":{"parts":[{"text":"セット内容は押し花1点と将軍の扇1点です。"}]}}]}
        """);
    GeminiFunctionCall functionCall = new GeminiFunctionCall(
        new ObjectMapper().readTree("{\"role\":\"model\",\"parts\":[{\"functionCall\":{\"name\":\"get_shogun_sakura_site_content\",\"id\":\"call-1\",\"args\":{}}}]}"),
        new ObjectMapper().readTree("{\"name\":\"get_shogun_sakura_site_content\",\"id\":\"call-1\",\"args\":{}}"));

    String answer = client.requestFinalAnswer(
        "セット内容は？",
        "system",
        functionCall,
        new ObjectMapper().readTree("{\"pages\":[{\"text\":\"セット内容\"}]}"));
    JsonNode sent = new ObjectMapper().readTree(requestBody.get());

    assertThat(answer).isEqualTo("セット内容は押し花1点と将軍の扇1点です。");
    assertThat(sent.path("contents").path(0).path("role").asText()).isEqualTo("user");
    assertThat(sent.path("contents").path(1).path("role").asText()).isEqualTo("model");
    assertThat(sent.path("contents").path(1).path("parts").path(0).path("functionCall").path("id").asText()).isEqualTo("call-1");
    assertThat(sent.path("contents").path(2).path("role").asText()).isEqualTo("user");
    assertThat(sent.path("contents").path(2).path("parts").path(0).path("functionResponse").path("id").asText()).isEqualTo("call-1");
  }

  @Test
  void mapsRateLimitWithoutLeakingApiKey() throws Exception {
    GeminiGenerateContentClient client = clientWithResponses(429, "{}");

    assertThatThrownBy(() -> client.requestFunctionCall("価格は？", "system"))
        .isInstanceOf(GeminiApiException.class)
        .hasMessageNotContaining(new GeminiApiConfig("test-key").apiKey());
  }

  @Test
  void rejectsEmptyFinalAnswer() throws Exception {
    GeminiGenerateContentClient client = clientWithResponses(200, """
        {"candidates":[{"content":{"parts":[{"text":""}]}}]}
        """);

    assertThatThrownBy(() -> client.requestFinalAnswer("価格は？", "system", functionCall(), new ObjectMapper().readTree("{\"pages\":[{\"text\":\"価格\"}]}")))
        .isInstanceOf(GeminiApiException.class);
  }

  private GeminiGenerateContentClient clientWithResponses(int statusCode, String body) throws IOException {
    return clientWithResponses(new AtomicReference<>(), statusCode, body);
  }

  private GeminiGenerateContentClient clientWithResponses(
      AtomicReference<String> requestBody,
      int statusCode,
      String body) throws IOException {
    Queue<Response> responses = new ArrayDeque<>();
    responses.add(new Response(statusCode, body));
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> {
      requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
      Response response = responses.remove();
      byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(response.statusCode(), bytes.length);
      try (OutputStream output = exchange.getResponseBody()) {
        output.write(bytes);
      }
    });
    server.start();
    URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/generateContent");
    return new GeminiGenerateContentClient(
        HttpClient.newHttpClient(),
        new ObjectMapper(),
        new GeminiApiConfig("test-key"),
        endpoint);
  }

  private GeminiFunctionCall functionCall() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    return new GeminiFunctionCall(
        mapper.readTree("{\"role\":\"model\",\"parts\":[{\"functionCall\":{\"name\":\"get_shogun_sakura_site_content\",\"args\":{}}}]}"),
        mapper.readTree("{\"name\":\"get_shogun_sakura_site_content\",\"args\":{}}"));
  }

  private record Response(int statusCode, String body) {
  }
}
