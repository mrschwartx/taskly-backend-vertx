package com.taskly.backend.config;

import java.util.Map;

public final class LoggingBootstrap {

  private static final String DEFAULT_LOG_FILE = "logs/taskly-api.log";

  private static final String DEFAULT_LEVEL = "debug";

  private LoggingBootstrap() {
  }

  public static void configure(Map<String, String> source) {
    String level = source.getOrDefault("LOG_LEVEL", DEFAULT_LEVEL);
    System.setProperty("LOG_LEVEL", level.isBlank() ? DEFAULT_LEVEL : level.strip());
    String logFile = source.containsKey("LOG_FILE") ? source.get("LOG_FILE").strip() : DEFAULT_LOG_FILE;
    if (logFile.isEmpty()) {
      System.setProperty("logback.configurationFile", "logback-console.xml");
      return;
    }
    System.setProperty("LOG_FILE", logFile);
    System.setProperty("logback.configurationFile", "logback-file.xml");
  }
}
