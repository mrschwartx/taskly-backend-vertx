package com.taskly.backend.users.application;

import com.taskly.backend.shared.application.Futures;
import com.taskly.backend.shared.domain.AlreadyExistsException;
import com.taskly.backend.shared.domain.EntityIds;
import com.taskly.backend.shared.domain.NotFoundException;
import com.taskly.backend.shared.domain.ValidationException;
import com.taskly.backend.users.domain.Email;
import com.taskly.backend.users.domain.PasswordHasher;
import com.taskly.backend.users.domain.PlainPassword;
import com.taskly.backend.users.domain.User;
import com.taskly.backend.users.domain.UserRepository;
import com.taskly.backend.users.domain.Username;
import io.vertx.core.Future;
import java.util.Map;
import java.util.UUID;

public class UpdateUser {

  private final UserRepository userRepository;

  private final PasswordHasher passwordHasher;

  public UpdateUser(UserRepository userRepository, PasswordHasher passwordHasher) {
    this.userRepository = userRepository;
    this.passwordHasher = passwordHasher;
  }

  public Future<UserDto> execute(UUID requesterId, String userId, Map<String, Object> changes) {
    return Futures.attempt(() -> {
      OwnAccount.assertOwned(requesterId, userId, "You can only update your own account");
      UUID id = EntityIds.parse(userId, "user");

      boolean hasUsername = changes.containsKey("username");
      boolean hasEmail = changes.containsKey("email");
      boolean hasPassword = changes.containsKey("password");
      if (!hasUsername && !hasEmail && !hasPassword) {
        throw new ValidationException("No updatable fields provided");
      }

      Username username = hasUsername ? Username.create(changes.get("username")) : null;
      Email email = hasEmail ? Email.create(changes.get("email")) : null;
      PlainPassword password = hasPassword ? PlainPassword.create(changes.get("password")) : null;

      return userRepository.findById(id).compose(found -> {
        User user = found.orElseThrow(() -> new NotFoundException("User not found!"));
        return ensureAvailable(id, username, email)
          .compose(done -> hashIfPresent(password))
          .compose(passwordHash -> {
            user.updateProfile(username, email, passwordHash);
            return userRepository.update(user).map(updated -> {
              if (!updated) {
                throw new NotFoundException("User not found!");
              }
              return UserDto.from(user);
            });
          });
      });
    });
  }

  private Future<Void> ensureAvailable(UUID id, Username username, Email email) {
    if (username == null && email == null) {
      return Future.succeededFuture();
    }
    return userRepository.existsByEmailOrUsername(
      email != null ? email.value() : null,
      username != null ? username.value() : null,
      id).compose(taken -> taken
        ? Future.<Void>failedFuture(new AlreadyExistsException("Email or Username is already registered."))
        : Future.<Void>succeededFuture());
  }

  private Future<String> hashIfPresent(PlainPassword password) {
    if (password == null) {
      return Future.succeededFuture(null);
    }
    return passwordHasher.hash(password.value());
  }
}
