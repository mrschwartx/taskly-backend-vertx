package com.taskly.backend.users.presentation;

import com.taskly.backend.config.Services;
import com.taskly.backend.shared.infrastructure.http.HttpResponses;
import com.taskly.backend.shared.infrastructure.http.RequestBodies;
import com.taskly.backend.users.application.AuthResult;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class AuthController {

  private final Services services;

  public AuthController(Services services) {
    this.services = services;
  }

  public void mount(Router router) {
    router.post("/api/v1/auth/signup").handler(this::signUp);
    router.post("/api/v1/auth/signin").handler(this::signIn);
    router.get("/api/v1/auth/signout").handler(this::signOut);
  }

  private void signUp(RoutingContext context) {
    withSession(context, services.registerUser.execute(RequestBodies.read(context)));
  }

  private void signIn(RoutingContext context) {
    withSession(context, services.authenticateUser.execute(RequestBodies.read(context)));
  }

  private void signOut(RoutingContext context) {
    HttpResponses.withCookie(context, services.authCookie.clear(),
      new JsonObject().put("message", "Sign out successful").encode());
  }

  private void withSession(RoutingContext context, Future<AuthResult> result) {
    result
      .onSuccess(session -> HttpResponses.withCookie(context, services.authCookie.issue(session.token()),
        UserJson.of(session.user()).encode()))
      .onFailure(context::fail);
  }
}
