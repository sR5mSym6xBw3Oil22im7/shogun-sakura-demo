package com.shogunsakura.demo.frontend;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
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

class ComposeChatbotFlowTest {

  private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(10);
  private static final String FRONTEND_URL = "http://localhost:5500/frontend/index.html";

  private WebDriver driver;
  private WebDriverWait wait;

  @BeforeEach
  void setUp() {
    Assumptions.assumeTrue(isReachable(FRONTEND_URL), "Docker Compose frontend is not reachable.");

    ChromeOptions options = new ChromeOptions();
    options.addArguments("--headless=new");
    options.addArguments("--window-size=1440,1200");

    driver = new ChromeDriver(options);
    wait = new WebDriverWait(driver, WAIT_TIMEOUT);
  }

  @AfterEach
  void tearDown() {
    if (driver != null) {
      driver.quit();
    }
  }

  @Test
  void chatbotShowsNoAnswerWhenLiveComposeBackendHasNoAnswer() {
    driver.get(FRONTEND_URL);
    waitForDocumentReady();

    click(By.cssSelector("[data-chatbot-toggle]"));
    driver.findElement(By.cssSelector("[data-chatbot-input]")).sendKeys("価格を教えてください");
    click(By.cssSelector("[data-chatbot-send]"));

    wait.until(ExpectedConditions.textToBePresentInElementLocated(
        By.cssSelector("[data-chatbot-messages]"),
        "応答なし"));

    String pageText = driver.findElement(By.tagName("body")).getText();
    assertThat(pageText).contains("価格を教えてください");
    assertThat(pageText).contains("応答なし");
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

  private boolean isReachable(String url) {
    try {
      HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
      connection.setConnectTimeout(1500);
      connection.setReadTimeout(1500);
      connection.setRequestMethod("HEAD");
      return connection.getResponseCode() >= 200 && connection.getResponseCode() < 500;
    } catch (IOException ex) {
      return false;
    }
  }
}
