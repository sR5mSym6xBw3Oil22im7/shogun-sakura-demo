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
    String expectedToken = requiredToken();
    if (!StringUtils.hasText(providedToken)) {
      return false;
    }
    return MessageDigest.isEqual(
        expectedToken.getBytes(StandardCharsets.UTF_8),
        providedToken.trim().getBytes(StandardCharsets.UTF_8));
  }

  public String requiredClientToken() {
    return requiredToken();
  }

  private String requiredToken() {
    String token = environment.getRequiredProperty("MCP_INTERNAL_TOKEN");
    return token.trim();
  }
}
