package com.taskly.backend.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class DotEnv {

  private DotEnv() {
  }

  public static Map<String, String> read(Path file) {
    Map<String, String> values = new HashMap<>();
    if (!Files.isRegularFile(file)) {
      return values;
    }
    try {
      for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
        parseLine(line, values);
      }
    }
    catch (IOException error) {
      throw new UncheckedIOException("Failed to read " + file, error);
    }
    return values;
  }

  public static Map<String, String> merged(Path file, Map<String, String> environment) {
    Map<String, String> values = read(file);
    values.putAll(environment);
    return values;
  }

  private static void parseLine(String line, Map<String, String> values) {
    String trimmed = line.strip();
    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
      return;
    }
    int separator = trimmed.indexOf('=');
    if (separator <= 0) {
      return;
    }
    String key = trimmed.substring(0, separator).strip();
    String value = trimmed.substring(separator + 1).strip();
    if (value.length() >= 2 && isQuoted(value)) {
      value = value.substring(1, value.length() - 1);
    }
    values.put(key, value);
  }

  private static boolean isQuoted(String value) {
    char first = value.charAt(0);
    return (first == '"' || first == '\'') && value.charAt(value.length() - 1) == first;
  }
}
