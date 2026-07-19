package com.shogunsakura.demo.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record SiteContentResult(
    List<SitePageContent> pages,
    OffsetDateTime retrievedAt,
    List<String> errors
) {

  public boolean hasUsableText() {
    return pages != null && pages.stream().anyMatch(page -> page.text() != null && !page.text().isBlank());
  }
}
