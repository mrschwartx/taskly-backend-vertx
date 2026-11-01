package com.taskly.backend.config;

import com.taskly.backend.shared.infrastructure.http.CorsHandler;
import com.taskly.backend.shared.infrastructure.http.GlobalErrorHandler;
import com.taskly.backend.shared.infrastructure.http.HttpResponses;
import com.taskly.backend.shared.infrastructure.http.RequestLoggingHandler;
import com.taskly.backend.shared.infrastructure.http.SecurityHeadersHandler;
import com.taskly.backend.tasks.presentation.TasksController;
import com.taskly.backend.users.presentation.AuthController;
import com.taskly.backend.users.presentation.JwtAuthenticationHandler;
import com.taskly.backend.users.presentation.UsersController;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.FileSystemAccess;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.sqlclient.Pool;
import java.nio.file.Path;

public final class HttpRouter {

  private static final long MAX_REQUEST_BYTES = 3L * 1024 * 1024;

  private HttpRouter() {
  }

  public static Router create(Vertx vertx, Pool pool, Services services, TasklyConfig config) {
    Router router = Router.router(vertx);

    router.route().handler(new RequestLoggingHandler());
    router.route().handler(new SecurityHeadersHandler());
    router.route().handler(new CorsHandler(config.clientUrl()));
    router.route("/api/v1/*").handler(new JwtAuthenticationHandler(services.verifyAccessToken, services.authCookie));
    router.route("/api/v1/*").handler(BodyHandler.create(uploadsDirectory())
      .setBodyLimit(MAX_REQUEST_BYTES)
      .setDeleteUploadedFilesOnEnd(true));

    router.get("/").handler(context ->
      HttpResponses.message(context, 200, "Welcome to the Taskly API"));
    router.get("/actuator/health").handler(context -> pool.query("SELECT 1").execute()
      .onSuccess(rows -> HttpResponses.json(context, 200, "{\"status\":\"UP\"}"))
      .onFailure(error -> HttpResponses.json(context, 503, "{\"status\":\"DOWN\"}")));

    new AuthController(services).mount(router);
    new UsersController(vertx, services, config.storage()).mount(router);
    new TasksController(services).mount(router);

    router.route().handler(StaticHandler.create(FileSystemAccess.ROOT, config.storage().publicDir().toString())
      .setCachingEnabled(false)
      .setIncludeHidden(false)
      .setDirectoryListing(false));
    router.route().handler(context -> HttpResponses.message(context, 404, "Not found"));
    router.route().failureHandler(new GlobalErrorHandler(config.storage().avatarMaxMegabytes()));
    return router;
  }

  private static String uploadsDirectory() {
    return Path.of(System.getProperty("java.io.tmpdir"), "taskly-uploads").toString();
  }
}
