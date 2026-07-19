package com.shogunsakura.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shogunsakura.demo.config.GeminiApiConfig;
import com.shogunsakura.demo.exception.GeminiApiException;
import com.shogunsakura.demo.mcp.SiteKnowledgeMcpController;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Queue;
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
        {"candidates":[{"content":{"parts":[{"functionCall":{"name":"get_shogun_sakura_site_content","args":{}}}]}}]}
        """);

    assertThat(client.requestFunctionCall("価格は？", "system"))
        .isEqualTo(SiteKnowledgeMcpController.TOOL_NAME);
  }

  @Test
  void rejectsUnexpectedFunctionCall() throws Exception {
    GeminiGenerateContentClient client = clientWithResponses(200, """
        {"candidates":[{"content":{"parts":[{"functionCall":{"name":"other_tool","args":{}}}]}}]}
        """);

    assertThatThrownBy(() -> client.requestFunctionCall("価格は？", "system"))
        .isInstanceOf(GeminiApiException.class)
        .hasMessageNotContaining(new GeminiApiConfig().apiKey());
  }

  @Test
  void parsesFinalAnswer() throws Exception {
    GeminiGenerateContentClient client = clientWithResponses(200, """
        {"candidates":[{"content":{"parts":[{"text":"セット内容は押し花1点と将軍の扇1点です。"}]}}]}
        """);

    String answer = client.requestFinalAnswer("セット内容は？", "system", new ObjectMapper().readTree("{\"pages\":[{\"text\":\"セット内容\"}]}"));

    assertThat(answer).isEqualTo("セット内容は押し花1点と将軍の扇1点です。");
  }

  @Test
  void mapsRateLimitWithoutLeakingApiKey() throws Exception {
    GeminiGenerateContentClient client = clientWithResponses(429, "{}");

    assertThatThrownBy(() -> client.requestFunctionCall("価格は？", "system"))
        .isInstanceOf(GeminiApiException.class)
        .hasMessageNotContaining(new GeminiApiConfig().apiKey());
  }

  @Test
  void rejectsEmptyFinalAnswer() throws Exception {
    GeminiGenerateContentClient client = clientWithResponses(200, """
        {"candidates":[{"content":{"parts":[{"text":""}]}}]}
        """);

    assertThatThrownBy(() -> client.requestFinalAnswer("価格は？", "system", new ObjectMapper().readTree("{\"pages\":[{\"text\":\"価格\"}]}")))
        .isInstanceOf(GeminiApiException.class);
  }

  private GeminiGenerateContentClient clientWithResponses(int statusCode, String body) throws IOException {
    Queue<Response> responses = new ArrayDeque<>();
    responses.add(new Response(statusCode, body));
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> {
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
        new GeminiApiConfig(),
        endpoint);
  }

  private record Response(int statusCode, String body) {
  }
}
