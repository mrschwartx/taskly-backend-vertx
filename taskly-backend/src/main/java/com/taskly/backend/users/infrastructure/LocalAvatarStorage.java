package com.taskly.backend.users.infrastructure;

import com.taskly.backend.users.application.AvatarStorage;
import com.taskly.backend.users.domain.Avatar;
import com.taskly.backend.users.domain.AvatarImageType;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import java.nio.file.Path;
import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalAvatarStorage implements AvatarStorage {

  private static final Logger log = LoggerFactory.getLogger(LocalAvatarStorage.class);

  private static final int RANDOM_BOUND = 1_000_000_000;

  private final FileSystem fileSystem;

  private final Path publicDir;

  private final Path avatarsDir;

  private final SecureRandom random = new SecureRandom();

  public LocalAvatarStorage(Vertx vertx, Path publicDir) {
    this.fileSystem = vertx.fileSystem();
    this.publicDir = publicDir.toAbsolutePath().normalize();
    this.avatarsDir = this.publicDir.resolve("images").resolve("users");
  }

  @Override
  public Future<String> store(AvatarImageType type, byte[] content) {
    String filename = "avatar-" + System.currentTimeMillis() + "-" + random.nextInt(RANDOM_BOUND)
      + type.extension();
    Path target = avatarsDir.resolve(filename);
    return fileSystem.mkdirs(avatarsDir.toString())
      .compose(done -> fileSystem.writeFile(target.toString(), Buffer.buffer(content)))
      .map(done -> Avatar.toPath(filename));
  }

  @Override
  public Future<Void> remove(String avatarPath) {
    if (!Avatar.isCustom(avatarPath)) {
      return Future.succeededFuture();
    }
    Path target = publicDir.resolve(avatarPath.substring(1)).normalize();
    if (!target.startsWith(avatarsDir)) {
      return Future.succeededFuture();
    }
    String location = target.toString();
    return fileSystem.exists(location)
      .compose(exists -> exists ? fileSystem.delete(location) : Future.<Void>succeededFuture())
      .recover(error -> {
        log.warn("Failed to remove avatar file {}: {}", avatarPath, error.getMessage());
        return Future.<Void>succeededFuture();
      });
  }
}
