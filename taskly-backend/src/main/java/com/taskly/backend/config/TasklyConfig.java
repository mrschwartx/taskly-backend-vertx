package com.taskly.backend.config;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record TasklyConfig(
  Server server,
  App app,
  String clientUrl,
  Database database,
  Auth auth,
  Storage storage,
  Seed seed) {

  private static final int MIN_SECRET_LENGTH = 32;

  public record Server(String name, String host, int port) {
  }

  public record App(String level) {

    public boolean production() {
      return "production".equals(level);
    }
  }

  public record Database(String url, String user, String password, int poolMax, int timeoutMillis,
    boolean baselineOnMigrate, String baselineVersion) {

    public String jdbcUrl() {
      if (url.startsWith("jdbc:")) {
        return url;
      }
      return "jdbc:" + url.replaceFirst("^postgres://", "postgresql://");
    }

    public String vertxUri() {
      return jdbcUrl().substring("jdbc:".length());
    }
  }

  public record Auth(String secret, Duration tokenTtl, String cookieName, String cookieDomain, int bcryptRounds) {
  }

  public record Storage(Path publicDir, long avatarMaxBytes) {

    public long avatarMaxMegabytes() {
      return avatarMaxBytes / (1024 * 1024);
    }
  }

  public record Seed(boolean enabled, boolean allowProduction) {
  }

  public static TasklyConfig from(Map<String, String> source) {
    List<String> errors = new ArrayList<>();
    Reader reader = new Reader(source, errors);

    Server server = new Server(
      reader.text("APP_NAME", "taskly-api"),
      reader.text("APP_HOST", "0.0.0.0"),
      reader.integer("APP_PORT", 8080, 0, 65535));
    App app = new App(reader.text("APP_LEVEL", "development"));
    String clientUrl = reader.text("CLIENT_URL", "http://localhost:3000");
    Database database = new Database(
      reader.required("DATABASE_URL"),
      reader.required("DATABASE_USER"),
      reader.text("DATABASE_PASSWORD", ""),
      reader.integer("DATABASE_POOL_MAX", 10, 1, 1000),
      reader.integer("DATABASE_TIMEOUT", 30000, 1, Integer.MAX_VALUE),
      reader.bool("FLYWAY_BASELINE_ON_MIGRATE", false),
      reader.text("FLYWAY_BASELINE_VERSION", "2"));
    String secret = reader.text("AUTH_SECRET", "");
    if (secret.isBlank()) {
      errors.add("AUTH_SECRET is required");
    }
    else if (secret.length() < MIN_SECRET_LENGTH) {
      errors.add("AUTH_SECRET must be at least " + MIN_SECRET_LENGTH + " characters long");
    }
    Auth auth = new Auth(
      secret,
      Duration.ofHours(1),
      "taskly_token",
      reader.text("COOKIE_DOMAIN", ""),
      reader.integer("BCRYPT_ROUNDS", 10, 4, 31));
    Storage storage = new Storage(
      Path.of(reader.text("PUBLIC_DIR", "public")).toAbsolutePath().normalize(),
      2L * 1024 * 1024);
    Seed seed = new Seed(reader.bool("SEED_ENABLED", false), reader.bool("SEED_ALLOW_PRODUCTION", false));

    if (!errors.isEmpty()) {
      throw new IllegalArgumentException("Invalid configuration: " + String.join("; ", errors));
    }
    return new TasklyConfig(server, app, clientUrl, database, auth, storage, seed);
  }

  private static final class Reader {

    private final Map<String, String> source;

    private final List<String> errors;

    Reader(Map<String, String> source, List<String> errors) {
      this.source = source;
      this.errors = errors;
    }

    String text(String key, String fallback) {
      String value = source.get(key);
      return value == null || value.isBlank() ? fallback : value.strip();
    }

    String required(String key) {
      String value = source.get(key);
      if (value == null || value.isBlank()) {
        errors.add(key + " is required");
        return "";
      }
      return value.strip();
    }

    int integer(String key, int fallback, int min, int max) {
      String value = source.get(key);
      if (value == null || value.isBlank()) {
        return fallback;
      }
      try {
        int parsed = Integer.parseInt(value.strip());
        if (parsed < min || parsed > max) {
          errors.add(key + " must be between " + min + " and " + max);
          return fallback;
        }
        return parsed;
      }
      catch (NumberFormatException error) {
        errors.add(key + " must be an integer");
        return fallback;
      }
    }

    boolean bool(String key, boolean fallback) {
      String value = source.get(key);
      if (value == null || value.isBlank()) {
        return fallback;
      }
      return Boolean.parseBoolean(value.strip());
    }
  }
}
