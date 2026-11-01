package com.taskly.backend.support;

import com.taskly.backend.users.application.AvatarStorage;
import com.taskly.backend.users.domain.AvatarImageType;
import io.vertx.core.Future;
import java.util.ArrayList;
import java.util.List;

public class FakeAvatarStorage implements AvatarStorage {

  public final List<String> stored = new ArrayList<>();

  public final List<String> removed = new ArrayList<>();

  @Override
  public Future<String> store(AvatarImageType type, byte[] content) {
    String path = "/images/users/avatar-" + stored.size() + type.extension();
    stored.add(path);
    return Future.succeededFuture(path);
  }

  @Override
  public Future<Void> remove(String avatarPath) {
    removed.add(avatarPath);
    return Future.succeededFuture();
  }
}
