package com.shogunsakura.demo.frontend;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.openqa.selenium.chrome.ChromeOptions;

final class SeleniumTestSupport {

  private SeleniumTestSupport() {
  }

  static ChromeOptions createChromeOptions() {
    ChromeOptions options = new ChromeOptions();
    options.addArguments("--headless=new");
    options.addArguments("--window-size=1440,1200");

    String chromeBin = System.getProperty("chrome.bin", System.getenv("CHROME_BIN"));
    if (chromeBin != null && !chromeBin.isBlank()) {
      options.setBinary(chromeBin);
    }
    return options;
  }

  static Path resolveFrontendDir() {
    String configured = System.getProperty("frontend.dir");
    if (configured != null && !configured.isBlank()) {
      return resolvePath(configured, "frontend.dir");
    }

    Path cwd = Path.of(".").toAbsolutePath().normalize();
    for (Path candidate : List.of(
        cwd.resolve("frontend"),
        cwd.getParent().resolve("frontend"))) {
      if (isFrontendDirectory(candidate)) {
        return candidate.toAbsolutePath().normalize();
      }
    }

    Path projectDir = findProjectDirectory(cwd);
    if (projectDir != null) {
      Path candidate = projectDir.resolve("frontend");
      if (isFrontendDirectory(candidate)) {
        return candidate.toAbsolutePath().normalize();
      }
      Path parentCandidate = projectDir.getParent().resolve("frontend");
      if (isFrontendDirectory(parentCandidate)) {
        return parentCandidate.toAbsolutePath().normalize();
      }
    }

    throw new IllegalStateException(
        "Unable to resolve frontend directory. Tried: " + cwd + ", " + cwd.getParent() + ", project root candidates");
  }

  static String resolveFrontendTestUrl() {
    String configured = Optional.ofNullable(System.getProperty("frontend.test.url")).orElseGet(() -> System.getenv("FRONTEND_TEST_URL"));
    if (configured != null && !configured.isBlank()) {
      return configured;
    }
    return "http://localhost:5500/frontend/index.html";
  }

  private static Path resolvePath(String value, String propertyName) {
    Path resolved = Path.of(value).normalize();
    if (!resolved.isAbsolute()) {
      resolved = Path.of(".").toAbsolutePath().normalize().resolve(resolved).normalize();
    }
    if (!isFrontendDirectory(resolved)) {
      throw new IllegalStateException("Configured " + propertyName + " does not point to an existing frontend directory: " + resolved);
    }
    return resolved;
  }

  private static boolean isFrontendDirectory(Path path) {
    Path normalized = path.normalize();
    if (!Files.exists(normalized) || !Files.isDirectory(normalized)) {
      return false;
    }
    return Files.exists(normalized.resolve("index.html"));
  }

  private static Path findProjectDirectory(Path cwd) {
    Path current = cwd;
    while (current != null) {
      if (Files.exists(current.resolve("pom.xml")) && Files.exists(current.resolve("src"))) {
        return current;
      }
      current = current.getParent();
    }
    return null;
  }
}
