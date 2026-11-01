package com.taskly.backend.users.presentation;

import com.taskly.backend.shared.application.AuthenticatedUser;
import com.taskly.backend.shared.domain.DomainException;
import com.taskly.backend.users.application.VerifyAccessToken;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import java.util.List;

public class JwtAuthenticationHandler implements Handler<RoutingContext> {

  public static final String USER_KEY = "authenticatedUser";

  private static final List<String> PROTECTED_PREFIXES = List.of("/api/v1/users", "/api/v1/tasks");

  private final VerifyAccessToken verifyAccessToken;

  private final AuthCookie authCookie;

  public JwtAuthenticationHandler(VerifyAccessToken verifyAccessToken, AuthCookie authCookie) {
    this.verifyAccessToken = verifyAccessToken;
    this.authCookie = authCookie;
  }

  @Override
  public void handle(RoutingContext context) {
    String path = context.request().path();
    boolean protectedPath = PROTECTED_PREFIXES.stream()
      .anyMatch(prefix -> path.equals(prefix) || path.startsWith(prefix + "/"));
    if (!protectedPath) {
      context.next();
      return;
    }
    try {
      AuthenticatedUser user = verifyAccessToken.execute(authCookie.read(context.request()));
      context.put(USER_KEY, user);
    }
    catch (DomainException error) {
      context.fail(error);
      return;
    }
    context.next();
  }
}
