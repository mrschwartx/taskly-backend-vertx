package com.taskly.backend.users.application;

import com.taskly.backend.shared.application.Futures;
import com.taskly.backend.shared.domain.EntityIds;
import com.taskly.backend.shared.domain.NotFoundException;
import com.taskly.backend.shared.domain.ValidationException;
import com.taskly.backend.users.domain.AvatarImageType;
import com.taskly.backend.users.domain.User;
import com.taskly.backend.users.domain.UserRepository;
import io.vertx.core.Future;
import java.util.UUID;

public class ChangeUserAvatar {

  private final UserRepository userRepository;

  private final AvatarStorage avatarStorage;

  public ChangeUserAvatar(UserRepository userRepository, AvatarStorage avatarStorage) {
    this.userRepository = userRepository;
    this.avatarStorage = avatarStorage;
  }

  public Future<UserDto> execute(UUID requesterId, String userId, AvatarUpload upload) {
    return Futures.attempt(() -> {
      OwnAccount.assertOwned(requesterId, userId, "You can only upload an avatar for your own account");
      UUID id = EntityIds.parse(userId, "user");

      if (upload == null) {
        throw new ValidationException("No file uploaded");
      }
      AvatarImageType type = AvatarImageType.fromContentType(upload.contentType())
        .orElseThrow(() -> new ValidationException("Only JPEG, PNG, WEBP and GIF images are allowed"));

      return userRepository.findById(id).compose(found -> {
        User user = found.orElseThrow(() -> new NotFoundException("User not found!"));
        return avatarStorage.store(type, upload.content()).compose(storedPath -> replace(user, storedPath));
      });
    });
  }

  private Future<UserDto> replace(User user, String storedPath) {
    String previousAvatar = user.getAvatar();
    user.changeAvatar(storedPath);
    return userRepository.update(user)
      .compose(updated -> updated
        ? Future.<Void>succeededFuture()
        : Future.<Void>failedFuture(new NotFoundException("User not found!")))
      .recover(error -> avatarStorage.remove(storedPath).compose(done -> Future.<Void>failedFuture(error)))
      .compose(done -> avatarStorage.remove(previousAvatar))
      .map(done -> UserDto.from(user));
  }
}
