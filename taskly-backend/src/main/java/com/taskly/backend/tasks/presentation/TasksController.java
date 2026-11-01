package com.taskly.backend.tasks.presentation;

import com.taskly.backend.config.Services;
import com.taskly.backend.shared.application.AuthenticatedUser;
import com.taskly.backend.shared.infrastructure.http.HttpResponses;
import com.taskly.backend.shared.infrastructure.http.QueryParameters;
import com.taskly.backend.shared.infrastructure.http.RequestBodies;
import com.taskly.backend.users.presentation.JwtAuthenticationHandler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class TasksController {

  private final Services services;

  public TasksController(Services services) {
    this.services = services;
  }

  public void mount(Router router) {
    router.get("/api/v1/tasks/user/:id").handler(this::listByUser);
    router.get("/api/v1/tasks/:id").handler(this::get);
    router.post("/api/v1/tasks/create").handler(this::create);
    router.patch("/api/v1/tasks/:id").handler(this::update);
    router.delete("/api/v1/tasks/:id").handler(this::delete);
  }

  private void listByUser(RoutingContext context) {
    HttpResponses.send(context,
      services.listTasksByUser.execute(requester(context).userId(), context.pathParam("id"),
        QueryParameters.value(context, "status"), QueryParameters.value(context, "orderBy"),
        QueryParameters.value(context, "page")),
      list -> TaskJson.of(list).encode());
  }

  private void get(RoutingContext context) {
    HttpResponses.send(context, services.getTask.execute(requester(context).userId(), context.pathParam("id")),
      task -> TaskJson.of(task).encode());
  }

  private void create(RoutingContext context) {
    HttpResponses.send(context,
      services.createTask.execute(requester(context).userId(), RequestBodies.read(context)),
      task -> TaskJson.of(task).encode());
  }

  private void update(RoutingContext context) {
    HttpResponses.send(context,
      services.updateTask.execute(requester(context).userId(), context.pathParam("id"),
        RequestBodies.read(context)),
      task -> TaskJson.of(task).encode());
  }

  private void delete(RoutingContext context) {
    HttpResponses.send(context, services.deleteTask.execute(requester(context).userId(), context.pathParam("id")),
      done -> new JsonObject().put("message", "Task has been deleted").encode());
  }

  private static AuthenticatedUser requester(RoutingContext context) {
    return context.get(JwtAuthenticationHandler.USER_KEY);
  }
}
