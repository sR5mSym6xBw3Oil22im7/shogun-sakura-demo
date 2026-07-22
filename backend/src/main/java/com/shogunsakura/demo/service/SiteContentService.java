package com.shogunsakura.demo.service;

import com.shogunsakura.demo.dto.SiteContentResult;
import com.shogunsakura.demo.dto.SitePageContent;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SiteContentService {

  static final String DEFAULT_BASE_URL = "https://sr5msym6xbw3oil22im7.github.io/shogun-sakura-demo/frontend/";
  static final List<String> ALLOWED_PAGE_SUFFIXES = List.of(
      "",
      "index.html",
      "orderhistory.html",
      "system_diagrams.html",
      "orderConfirmation.html",
      "orderConfirmed.html"
  );

  private static final int MAX_PAGE_CHARS = 5000;
  private static final int MAX_TOTAL_CHARS = 18000;
  private static final Duration CACHE_TTL = Duration.ofMinutes(5);

  private final HttpClient httpClient;
  private final URI baseUri;
  private final Duration timeout;
  private SiteContentResult cachedResult;
  private OffsetDateTime cachedAt;

  @Autowired
  public SiteContentService(
      @Value("${SITE_SOURCE_BASE_URL:" + DEFAULT_BASE_URL + "}") String baseUrl) {
    this(HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NEVER)
        .connectTimeout(Duration.ofSeconds(5))
        .build(), URI.create(baseUrl), Duration.ofSeconds(6));
  }

  SiteContentService(HttpClient httpClient, URI baseUri, Duration timeout) {
    this.httpClient = httpClient;
    this.baseUri = normalizeBaseUri(baseUri);
    this.timeout = timeout;
  }

  public synchronized SiteContentResult getSiteContent() {
    OffsetDateTime now = OffsetDateTime.now();
    if (cachedResult != null && cachedAt != null && Duration.between(cachedAt, now).compareTo(CACHE_TTL) < 0) {
      return cachedResult;
    }

    SiteContentResult result = retrieveSiteContent(now);
    cachedResult = result;
    cachedAt = now;
    return result;
  }

  private SiteContentResult retrieveSiteContent(OffsetDateTime retrievedAt) {
    List<SitePageContent> pages = new ArrayList<>();
    List<String> errors = new ArrayList<>();
    int totalChars = 0;

    for (String pageSuffix : ALLOWED_PAGE_SUFFIXES) {
      if (totalChars >= MAX_TOTAL_CHARS) {
        break;
      }

      URI pageUri = baseUri.resolve(pageSuffix);
      if (!isAllowed(pageUri)) {
        errors.add("許可外URLを拒否しました: " + pageUri);
        continue;
      }

      try {
        HttpRequest request = HttpRequest.newBuilder(pageUri)
            .timeout(timeout)
            .GET()
            .header("Accept", "text/html")
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 300 && response.statusCode() < 400) {
          errors.add("リダイレクトを拒否しました: " + pageUri);
          continue;
        }

        if (response.statusCode() == 404) {
          continue;
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
          errors.add("取得に失敗しました: " + pageUri);
          continue;
        }

        SitePageContent page = parsePage(pageUri, response.body(), MAX_TOTAL_CHARS - totalChars);
        if (!page.text().isBlank()) {
          pages.add(page);
          totalChars += page.text().length();
        }
      } catch (IOException ex) {
        errors.add("取得に失敗しました: " + pageUri);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        errors.add("取得が中断されました: " + pageUri);
        break;
      }
    }

    return new SiteContentResult(List.copyOf(pages), retrievedAt, List.copyOf(errors));
  }

  private SitePageContent parsePage(URI pageUri, String html, int remainingChars) {
    Document document = Jsoup.parse(html, pageUri.toString());
    document.select("script,style,noscript,template,svg").remove();
    String title = document.title().trim();
    String text = document.body() == null ? "" : document.body().text().replaceAll("\\s+", " ").trim();
    int limit = Math.max(0, Math.min(MAX_PAGE_CHARS, remainingChars));
    if (text.length() > limit) {
      text = text.substring(0, limit);
    }
    return new SitePageContent(title, pageUri.toString(), text);
  }

  private boolean isAllowed(URI uri) {
    if (!baseUri.getScheme().equalsIgnoreCase(uri.getScheme())) {
      return false;
    }
    if (!baseUri.getHost().equalsIgnoreCase(uri.getHost())) {
      return false;
    }
    if (effectivePort(baseUri) != effectivePort(uri)) {
      return false;
    }
    for (String pageSuffix : ALLOWED_PAGE_SUFFIXES) {
      if (baseUri.resolve(pageSuffix).getPath().equals(uri.getPath())) {
        return true;
      }
    }
    return false;
  }

  private static URI normalizeBaseUri(URI uri) {
    if (uri.getScheme() == null || uri.getHost() == null) {
      throw new IllegalArgumentException("SITE_SOURCE_BASE_URL must be an absolute URL.");
    }
    String path = uri.getPath();
    if (path == null || path.isBlank()) {
      path = "/";
    }
    if (!path.endsWith("/")) {
      path = path + "/";
    }
    return URI.create(uri.getScheme() + "://" + uri.getAuthority() + path);
  }

  private int effectivePort(URI uri) {
    if (uri.getPort() != -1) {
      return uri.getPort();
    }
    return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
  }
}
