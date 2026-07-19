package com.shogunsakura.demo.mcp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class McpAuthentication {

  public static final String HEADER_NAME = "X-MCP-Internal-Token";

  private final Environment environment;

  public McpAuthentication(Environment environment) {
    this.environment = environment;
  }

  public boolean isValid(String providedToken) {
    String expectedToken = environment.getProperty("MCP_INTERNAL_TOKEN");
    if (!StringUtils.hasText(expectedToken) || !StringUtils.hasText(providedToken)) {
      return false;
    }
    return MessageDigest.isEqual(
        expectedToken.trim().getBytes(StandardCharsets.UTF_8),
        providedToken.trim().getBytes(StandardCharsets.UTF_8));
  }

  public String requiredClientToken() {
    String token = environment.getProperty("MCP_INTERNAL_TOKEN");
    if (!StringUtils.hasText(token)) {
      throw new IllegalStateException("MCP_INTERNAL_TOKEN is not configured.");
    }
    return token.trim();
  }
}
