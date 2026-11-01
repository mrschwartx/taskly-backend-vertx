package com.taskly.backend.users.presentation;

import com.taskly.backend.users.application.UserDto;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;

public final class UserJson {

  private UserJson() {
  }

  public static JsonObject of(UserDto user) {
    return new JsonObject()
      .put("_id", user.id())
      .put("username", user.username())
      .put("email", user.email())
      .put("avatar", user.avatar())
      .put("createdAt", user.createdAt())
      .put("updatedAt", user.updatedAt());
  }

  public static JsonArray of(List<UserDto> users) {
    JsonArray array = new JsonArray();
    users.forEach(user -> array.add(of(user)));
    return array;
  }
}
