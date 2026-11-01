package com.taskly.backend.integration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

abstract class IntegrationTestSupport {

  static final Path PUBLIC_DIR = TestServer.INSTANCE.publicDir;

  ApiClient client() {
    return new ApiClient("http://127.0.0.1:" + TestServer.INSTANCE.port());
  }

  record Registered(ApiClient client, String id, String username, String email, String password) {
  }

  Registered register() {
    ApiClient client = client();
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String username = "user_" + suffix;
    String email = suffix + "@mail.com";
    String password = "Password123!";
    ApiResponse response = client.post("/api/v1/auth/signup",
      ApiClient.json("username", username, "email", email, "password", password));
    if (response.status() != 200) {
      throw new IllegalStateException("Signup failed: " + response.body());
    }
    return new Registered(client, response.json("$._id"), username, email, password);
  }

  String taskJson(String name, String status) {
    return ApiClient.json("name", name, "description", "Finish reading", "priority", "low", "due",
      "2026-12-08T10:00:00Z", "status", status);
  }

  long avatarFileCount() {
    try (var files = Files.list(PUBLIC_DIR.resolve("images").resolve("users"))) {
      return files.filter(file -> file.getFileName().toString().startsWith("avatar-")).count();
    }
    catch (java.nio.file.NoSuchFileException error) {
      return 0;
    }
    catch (IOException error) {
      throw new UncheckedIOException(error);
    }
  }
}
