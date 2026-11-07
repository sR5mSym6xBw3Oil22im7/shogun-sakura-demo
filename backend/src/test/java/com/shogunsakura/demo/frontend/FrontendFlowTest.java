package com.shogunsakura.demo.frontend;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

class FrontendFlowTest {

  private static final String TEST_HOST = "localhost";
  private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(10);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

  // 本番と同様に、画面と API を同一オリジン（1 つのサーバー）で配信する。
  private HttpServer server;
  private int serverPort;
  private String serverOrigin;
  private WebDriver driver;
  private WebDriverWait wait;
  private final List<JsonNode> orderRequests = new CopyOnWriteArrayList<>();

  @BeforeEach
  void setUp() throws Exception {
    Path staticDir = SeleniumTestSupport.resolveFrontendDir();
    server = HttpServer.create(new InetSocketAddress(TEST_HOST, 0), 0);
    serverPort = server.getAddress().getPort();
    serverOrigin = "http://" + TEST_HOST + ":" + serverPort;

    server.createContext("/", new StaticFrontendHandler(staticDir));
    server.createContext("/api/orders", new OrderApiHandler(orderRequests, serverOrigin));
    server.createContext("/api/chat", new ChatApiHandler(serverOrigin));

    server.start();

    ChromeOptions options = SeleniumTestSupport.createChromeOptions();
    driver = new ChromeDriver(options);
    wait = new WebDriverWait(driver, WAIT_TIMEOUT);
  }

  @AfterEach
  void tearDown() {
    if (driver != null) {
      driver.quit();
    }
    if (server != null) {
      server.stop(0);
    }
    orderRequests.clear();
  }

  @Test
  void purchaseFormShowsValidationErrorsForInvalidEmail() {
    openPage("/index.html");

    WebElement emailInput = driver.findElement(By.id("email"));
    emailInput.clear();
    emailInput.sendKeys("invalid-email");

    WebElement form = driver.findElement(By.cssSelector("[data-order-form]"));
    ((JavascriptExecutor) driver).executeScript("arguments[0].requestSubmit();", form);

    wait.until(
        ExpectedConditions.textToBe(By.cssSelector("[data-error-for='email']"), "Please enter a valid email address."));
    assertThat(driver.getCurrentUrl()).endsWith("/index.html");
    assertThat(text("[data-error-for='email']")).isEqualTo("Please enter a valid email address.");
    assertThat(text("[data-form-status]")).isEqualTo("Please check the form fields.");
  }

  @Test
  void purchaseFlowNavigatesToConfirmationAndCompletionPages() {
    openPage("/index.html");
    WebElement form = driver.findElement(By.cssSelector("[data-order-form]"));
    ((JavascriptExecutor) driver).executeScript("arguments[0].requestSubmit();", form);

    wait.until(ExpectedConditions.urlContains("/orderConfirmation.html"));
    assertThat(driver.getPageSource()).contains("注文確認");

    click(By.cssSelector("[data-confirm-order-button]"));

    wait.until(ExpectedConditions.urlContains("/orderConfirmed.html"));
    assertThat(driver.getPageSource()).contains("ご注文ありがとうございます");
    assertThat(orderRequests).hasSize(1);

    JsonNode payload = orderRequests.get(0);
    assertThat(payload.path("quantity").asInt()).isEqualTo(1);
    assertThat(payload.path("name").asText()).startsWith("テスト氏名");
    assertThat(payload.path("address").asText()).startsWith("テスト住所");
    assertThat(payload.path("postalCode").asText()).matches("\\d{3}-\\d{4}");
    assertThat(payload.path("email").asText()).contains("@");
    assertThat(payload.path("note").asText()).startsWith("テスト備考");
  }

  @Test
  void orderSubmissionUsesLocalApiWhenPageIsOpenedFromFileUrl() throws Exception {
    Path confirmationPage = SeleniumTestSupport.resolveFrontendDir().resolve("orderConfirmation.html").toAbsolutePath();

    Path frontendDir = confirmationPage.getParent();
    Path preloader = frontendDir.resolve("__preload_for_test.html");
    String preloadHtml = "<!doctype html><html><head><meta charset=\"utf-8\"></head><body>" +
        "<script>window.sessionStorage.setItem('pendingOrderQuantity','1');location.href='orderConfirmation.html';</script>" +
        "</body></html>";
    try {
      java.nio.file.Files.writeString(preloader, preloadHtml);

      // Open the preloader which sets sessionStorage for the file origin and redirects
      driver.get(preloader.toUri().toString());
      waitForDocumentReady();

      wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("[data-confirm-order-button]")));
      click(By.cssSelector("[data-confirm-order-button]"));

        // After clicking, the file:// page should navigate to orderConfirmed.html (file URL).
        wait.until(ExpectedConditions.urlContains("orderConfirmed.html"));

        // Verify the real backend (localhost:8080) has stored the demo order.
        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
          .uri(java.net.URI.create("http://localhost:8080/api/orders"))
          .GET()
          .build();
        java.net.http.HttpResponse<String> resp = httpClient.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
        com.fasterxml.jackson.databind.JsonNode orders = OBJECT_MAPPER.readTree(resp.body());
        assertThat(orders.isArray() ? orders.size() : 0).isGreaterThan(0);
    } finally {
      try {
        java.nio.file.Files.deleteIfExists(preloader);
      } catch (Exception ignore) {
      }
    }
  }

  @Test
  void orderHistoryPageRendersFetchedOrders() {
    openPage("/orderhistory.html");

    wait.until(ExpectedConditions.textToBe(By.cssSelector("[data-history-status]"), "2 件の注文履歴を表示しています。"));

    String pageText = driver.findElement(By.tagName("body")).getText();
    assertThat(pageText).contains("2 件の注文履歴を表示しています。");
    assertThat(pageText).contains("SHOGUN SAKURA SET");
    assertThat(pageText).contains("テスト太郎");
    assertThat(pageText).contains("テスト花子");
    assertThat(pageText).contains("¥4,800");
    assertThat(pageText).contains("¥9,600");
  }

  @Test
  void chatbotSendsQuestionAndAppendsAnswer() {
    openPage("/index.html");

    click(By.cssSelector("[data-chatbot-toggle]"));
    WebElement input = driver.findElement(By.cssSelector("[data-chatbot-input]"));
    input.sendKeys("価格を教えてください");

    click(By.cssSelector("[data-chatbot-send]"));

    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector("[data-chatbot-messages]"),
        "税込のデモ価格で4,800円です。"));

    String pageText = driver.findElement(By.tagName("body")).getText();
    assertThat(pageText).contains("価格を教えてください");
    assertThat(pageText).contains("税込のデモ価格で4,800円です。");
  }

  private void openPage(String path) {
    driver.get(serverOrigin + path);
    waitForDocumentReady();
  }

  private String text(String cssSelector) {
    return driver.findElement(By.cssSelector(cssSelector)).getText();
  }

  private void click(By by) {
    WebElement element = driver.findElement(by);
    ((JavascriptExecutor) driver).executeScript(
        "arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});",
        element);
    wait.until(ExpectedConditions.elementToBeClickable(by));
    element.click();
  }

  private void waitForDocumentReady() {
    wait.until(
        webDriver -> "complete".equals(((JavascriptExecutor) webDriver).executeScript("return document.readyState")));
  }

  private static final class StaticFrontendHandler implements HttpHandler {

    private final Path root;

    private StaticFrontendHandler(Path root) {
      this.root = root;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      URI uri = exchange.getRequestURI();
      String rawPath = uri.getPath();
      String relativePath = rawPath.startsWith("/") ? rawPath.substring(1) : rawPath;
      if (relativePath.isBlank()) {
        relativePath = "index.html";
      }

      Path target = root.resolve(relativePath).normalize();
      if (Files.isDirectory(target)) {
        target = target.resolve("index.html");
      }

      if (!target.startsWith(root) || !Files.exists(target)) {
        sendResponse(exchange, 404, "Not Found", "text/plain; charset=UTF-8");
        return;
      }

      byte[] body = Files.readAllBytes(target);
      sendResponse(exchange, 200, body, detectContentType(target));
    }

    private String detectContentType(Path path) throws IOException {
      String detected = Files.probeContentType(path);
      if (detected != null) {
        return detected;
      }
      String filename = path.getFileName().toString();
      if (filename.endsWith(".js")) {
        return "text/javascript; charset=UTF-8";
      }
      if (filename.endsWith(".css")) {
        return "text/css; charset=UTF-8";
      }
      if (filename.endsWith(".html")) {
        return "text/html; charset=UTF-8";
      }
      if (filename.endsWith(".png")) {
        return "image/png";
      }
      return "application/octet-stream";
    }
  }

  private static final class OrderApiHandler implements HttpHandler {

    private final List<JsonNode> orderRequests;
    private final String allowedOrigin;

    private OrderApiHandler(List<JsonNode> orderRequests, String allowedOrigin) {
      this.orderRequests = orderRequests;
      this.allowedOrigin = allowedOrigin;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
        sendResponse(exchange, 204, new byte[0], "text/plain; charset=UTF-8", allowedOrigin);
        return;
      }

      if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
        byte[] requestBody = exchange.getRequestBody().readAllBytes();
        orderRequests.add(OBJECT_MAPPER.readTree(requestBody));
        sendJson(exchange, 201, """
            {
              "orderId": 101,
              "message": "注文を受け付けました。",
              "createdAt": "2026-07-23T12:00:00+09:00"
            }
            """, allowedOrigin);
        return;
      }

      if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
        sendJson(exchange, 200, """
            [
              {
                "orderId": 101,
                "productName": "SHOGUN SAKURA SET",
                "customerName": "テスト太郎",
                "email": "taro@example.com",
                "postalCode": "100-0001",
                "address": "東京都千代田区1-1",
                "quantity": 1,
                "totalAmount": 4800,
                "note": "demo order",
                "createdAt": "2026-07-23T10:30:00+09:00"
              },
              {
                "orderId": 102,
                "productName": "SHOGUN SAKURA SET",
                "customerName": "テスト花子",
                "email": "hanako@example.com",
                "postalCode": "150-0001",
                "address": "東京都渋谷区1-2-3",
                "quantity": 2,
                "totalAmount": 9600,
                "note": "gift",
                "createdAt": "2026-07-23T11:45:00+09:00"
              }
            ]
            """, allowedOrigin);
        return;
      }

      sendResponse(exchange, 405, "Method Not Allowed", "text/plain; charset=UTF-8", allowedOrigin);
    }
  }

  private static final class ChatApiHandler implements HttpHandler {

    private final String allowedOrigin;

    private ChatApiHandler(String allowedOrigin) {
      this.allowedOrigin = allowedOrigin;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
        sendResponse(exchange, 204, new byte[0], "text/plain; charset=UTF-8", allowedOrigin);
        return;
      }

      if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
        sendResponse(exchange, 405, "Method Not Allowed", "text/plain; charset=UTF-8", allowedOrigin);
        return;
      }

      sendJson(exchange, 200, """
          {
            "answer": "税込のデモ価格で4,800円です。"
          }
          """, allowedOrigin);
    }
  }

  private static void sendJson(HttpExchange exchange, int status, String body, String allowedOrigin)
      throws IOException {
    sendResponse(exchange, status, body, "application/json; charset=UTF-8", allowedOrigin);
  }

  private static void sendResponse(HttpExchange exchange, int status, String body, String contentType)
      throws IOException {
    sendResponse(exchange, status, body.getBytes(StandardCharsets.UTF_8), contentType, null);
  }

  private static void sendResponse(HttpExchange exchange, int status, byte[] body, String contentType)
      throws IOException {
    sendResponse(exchange, status, body, contentType, null);
  }

  private static void sendResponse(HttpExchange exchange, int status, String body, String contentType,
      String allowedOrigin)
      throws IOException {
    sendResponse(exchange, status, body.getBytes(StandardCharsets.UTF_8), contentType, allowedOrigin);
  }

  private static void sendResponse(HttpExchange exchange, int status, byte[] body, String contentType,
      String allowedOrigin)
      throws IOException {
    Headers headers = exchange.getResponseHeaders();
    headers.set("Content-Type", contentType);
    if (allowedOrigin != null && !allowedOrigin.isBlank()) {
      headers.set("Access-Control-Allow-Origin", allowedOrigin);
      headers.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
      headers.set("Access-Control-Allow-Headers", "Content-Type, Accept");
    }
    exchange.sendResponseHeaders(status, body.length);
    try (OutputStream outputStream = exchange.getResponseBody()) {
      outputStream.write(body);
    }
  }
}
