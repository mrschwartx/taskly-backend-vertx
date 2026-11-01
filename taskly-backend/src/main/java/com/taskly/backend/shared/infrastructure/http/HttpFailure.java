package com.taskly.backend.shared.infrastructure.http;

public class HttpFailure extends RuntimeException {

  private final int status;

  public HttpFailure(int status, String message) {
    super(message);
    this.status = status;
  }

  public int status() {
    return status;
  }
}
