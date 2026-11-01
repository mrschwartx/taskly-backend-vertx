package com.taskly.backend.users.application;

import com.taskly.backend.users.domain.AvatarImageType;
import io.vertx.core.Future;

public interface AvatarStorage {

  Future<String> store(AvatarImageType type, byte[] content);

  Future<Void> remove(String avatarPath);
}
