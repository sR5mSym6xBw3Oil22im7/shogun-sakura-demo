package com.shogunsakura.demo.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.shogunsakura.demo.dto.SiteContentResult;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SiteContentServiceTest {

  private HttpServer server;

  @AfterEach
  void stopServer() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void extractsVisibleTextAndRemovesScriptsAndStyles() throws Exception {
    startServer(200, """
        <html><head><title>商品</title><style>.x{color:red}</style></head>
        <body><h1>桜の押し花と将軍の扇セット</h1><script>alert('secret')</script><p>価格は4,800円です。</p></body></html>
        """);
    SiteContentService service = service();

    SiteContentResult result = service.getSiteContent();

    assertThat(result.pages()).isNotEmpty();
    assertThat(result.pages().getFirst().title()).isEqualTo("商品");
    assertThat(result.pages().getFirst().url()).contains("/frontend/");
    assertThat(result.pages().getFirst().text())
        .contains("価格は4,800円です。")
        .doesNotContain("alert")
        .doesNotContain("color:red");
  }

  @Test
  void rejectsExternalRedirectsAndDoesNotFallback() throws Exception {
    startServer(302, "");
    SiteContentService service = service();

    SiteContentResult result = service.getSiteContent();

    assertThat(result.pages()).isEmpty();
    assertThat(result.errors()).anyMatch(error -> error.contains("リダイレクト"));
  }

  @Test
  void cachesResultsBriefly() throws Exception {
    startServer(200, "<html><head><title>Cached</title></head><body>初回本文</body></html>");
    SiteContentService service = service();

    SiteContentResult first = service.getSiteContent();
    SiteContentResult second = service.getSiteContent();

    assertThat(second).isSameAs(first);
  }

  @Test
  void resolvesPagesUnderConfiguredBasePath() throws Exception {
    startServer(200, "<html><head><title>Base</title></head><body>本番配下の本文</body></html>");
    URI baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/shogun-sakura-demo/frontend/");
    SiteContentService service = new SiteContentService(HttpClient.newHttpClient(), baseUri, Duration.ofSeconds(2));

    SiteContentResult result = service.getSiteContent();

    assertThat(result.pages()).isNotEmpty();
    assertThat(result.pages().getFirst().url()).contains("/shogun-sakura-demo/frontend/");
  }

  private void startServer(int statusCode, String body) throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> {
      if (statusCode >= 300 && statusCode < 400) {
        exchange.getResponseHeaders().add("Location", "https://example.com/");
      }
      byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(statusCode, bytes.length);
      try (OutputStream output = exchange.getResponseBody()) {
        output.write(bytes);
      }
    });
    server.start();
  }

  private SiteContentService service() {
    URI baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/frontend/");
    return new SiteContentService(HttpClient.newHttpClient(), baseUri, Duration.ofSeconds(2));
  }
}
