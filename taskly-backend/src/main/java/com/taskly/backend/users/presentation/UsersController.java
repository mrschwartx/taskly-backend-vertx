package com.taskly.backend.users.presentation;

import com.taskly.backend.config.Services;
import com.taskly.backend.config.TasklyConfig;
import com.taskly.backend.shared.application.AuthenticatedUser;
import com.taskly.backend.shared.domain.ValidationException;
import com.taskly.backend.shared.infrastructure.http.HttpResponses;
import com.taskly.backend.shared.infrastructure.http.QueryParameters;
import com.taskly.backend.shared.infrastructure.http.RequestBodies;
import com.taskly.backend.users.application.AvatarUpload;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class UsersController {

  private static final String AVATAR_FIELD = "avatar";

  private final Vertx vertx;

  private final Services services;

  private final TasklyConfig.Storage storage;

  public UsersController(Vertx vertx, Services services, TasklyConfig.Storage storage) {
    this.vertx = vertx;
    this.services = services;
    this.storage = storage;
  }

  public void mount(Router router) {
    router.get("/api/v1/users").handler(this::list);
    router.get("/api/v1/users/").handler(this::list);
    router.get("/api/v1/users/:id").handler(this::get);
    router.patch("/api/v1/users/update/:id").handler(this::update);
    router.post("/api/v1/users/upload/:id").handler(this::uploadAvatar);
    router.delete("/api/v1/users/delete/:id").handler(this::delete);
  }

  private void list(RoutingContext context) {
    HttpResponses.send(context,
      services.listUsers.execute(QueryParameters.value(context, "username"),
        QueryParameters.value(context, "email")),
      users -> UserJson.of(users).encode());
  }

  private void get(RoutingContext context) {
    HttpResponses.send(context, services.getUser.execute(context.pathParam("id")),
      user -> UserJson.of(user).encode());
  }

  private void update(RoutingContext context) {
    HttpResponses.send(context,
      services.updateUser.execute(requester(context).userId(), context.pathParam("id"),
        RequestBodies.read(context)),
      user -> UserJson.of(user).encode());
  }

  private void uploadAvatar(RoutingContext context) {
    AuthenticatedUser requester = requester(context);
    String userId = context.pathParam("id");
    HttpResponses.send(context,
      readUpload(context).compose(upload -> services.changeUserAvatar.execute(requester.userId(), userId, upload)),
      user -> UserJson.of(user).encode());
  }

  private void delete(RoutingContext context) {
    services.deleteUser.execute(requester(context).userId(), context.pathParam("id"))
      .onSuccess(done -> HttpResponses.withCookie(context, services.authCookie.clear(),
        new JsonObject().put("message", "User has been deleted").encode()))
      .onFailure(context::fail);
  }

  private Future<AvatarUpload> readUpload(RoutingContext context) {
    FileUpload file = null;
    for (FileUpload candidate : context.fileUploads()) {
      if (AVATAR_FIELD.equals(candidate.name())) {
        file = candidate;
        break;
      }
    }
    if (file == null) {
      return Future.succeededFuture(null);
    }
    if (file.size() > storage.avatarMaxBytes()) {
      return Future.failedFuture(
        new ValidationException("File size exceeds " + storage.avatarMaxMegabytes() + "MB"));
    }
    String contentType = file.contentType();
    return vertx.fileSystem().readFile(file.uploadedFileName())
      .map(content -> new AvatarUpload(contentType, content.getBytes()));
  }

  private static AuthenticatedUser requester(RoutingContext context) {
    return context.get(JwtAuthenticationHandler.USER_KEY);
  }
}
