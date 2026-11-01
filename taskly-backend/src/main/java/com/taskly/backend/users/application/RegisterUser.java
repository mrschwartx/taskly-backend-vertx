package com.taskly.backend.users.application;

import com.taskly.backend.shared.application.Futures;
import com.taskly.backend.shared.domain.AlreadyExistsException;
import com.taskly.backend.users.domain.Email;
import com.taskly.backend.users.domain.PasswordHasher;
import com.taskly.backend.users.domain.PlainPassword;
import com.taskly.backend.users.domain.TokenService;
import com.taskly.backend.users.domain.User;
import com.taskly.backend.users.domain.UserRepository;
import com.taskly.backend.users.domain.Username;
import io.vertx.core.Future;
import java.util.Map;

public class RegisterUser {

  private final UserRepository userRepository;

  private final PasswordHasher passwordHasher;

  private final TokenService tokenService;

  public RegisterUser(UserRepository userRepository, PasswordHasher passwordHasher, TokenService tokenService) {
    this.userRepository = userRepository;
    this.passwordHasher = passwordHasher;
    this.tokenService = tokenService;
  }

  public Future<AuthResult> execute(Map<String, Object> input) {
    return Futures.attempt(() -> {
      Username username = Username.create(input.get("username"));
      Email email = Email.create(input.get("email"));
      PlainPassword password = PlainPassword.create(input.get("password"));

      return userRepository.existsByEmailOrUsername(email.value(), username.value(), null)
        .compose(exists -> {
          if (exists) {
            return Future.<String>failedFuture(new AlreadyExistsException("Email or Username is already registered."));
          }
          return passwordHasher.hash(password.value());
        })
        .compose(passwordHash -> {
          User user = User.register(userRepository.nextId(), username, email, passwordHash);
          return userRepository.add(user)
            .map(done -> new AuthResult(UserDto.from(user), tokenService.issue(user.getId())));
        });
    });
  }
}
