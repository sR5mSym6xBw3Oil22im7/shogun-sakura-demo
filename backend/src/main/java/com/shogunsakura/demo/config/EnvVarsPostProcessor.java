package com.shogunsakura.demo.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Ensures selected environment variables are available as Spring properties early in startup.
 */
public class EnvVarsPostProcessor implements EnvironmentPostProcessor, Ordered {

  private static final String PROPERTY_SOURCE_NAME = "envVarsPostProcessor";

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    Map<String, Object> props = new HashMap<>();

    // Import GEMINI_API_KEY if present
    String gemini = System.getenv("GEMINI_API_KEY");
    if (gemini != null && !gemini.isBlank()) {
      props.put("GEMINI_API_KEY", gemini);
    }

    // Import any MCP_ prefixed environment variables
    Map<String, String> env = System.getenv();
    for (Map.Entry<String, String> e : env.entrySet()) {
      String k = e.getKey();
      if (k != null && k.startsWith("MCP_")) {
        props.put(k, e.getValue());
      }
    }

    if (!props.isEmpty()) {
      MapPropertySource source = new MapPropertySource(PROPERTY_SOURCE_NAME, props);
      environment.getPropertySources().addFirst(source);
    }
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }
}
