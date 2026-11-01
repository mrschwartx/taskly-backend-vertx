package com.taskly.backend.users.application;

import com.taskly.backend.shared.application.Futures;
import com.taskly.backend.shared.domain.AuthenticationFailedException;
import com.taskly.backend.shared.domain.NotFoundException;
import com.taskly.backend.shared.domain.TextRules;
import com.taskly.backend.shared.domain.ValidationException;
import com.taskly.backend.users.domain.PasswordHasher;
import com.taskly.backend.users.domain.TokenService;
import com.taskly.backend.users.domain.User;
import com.taskly.backend.users.domain.UserRepository;
import io.vertx.core.Future;
import java.util.Map;

public class AuthenticateUser {

  private final UserRepository userRepository;

  private final PasswordHasher passwordHasher;

  private final TokenService tokenService;

  public AuthenticateUser(UserRepository userRepository, PasswordHasher passwordHasher, TokenService tokenService) {
    this.userRepository = userRepository;
    this.passwordHasher = passwordHasher;
    this.tokenService = tokenService;
  }

  public Future<AuthResult> execute(Map<String, Object> input) {
    return Futures.attempt(() -> {
      if (!(input.get("email") instanceof String email) || !(input.get("password") instanceof String password)) {
        throw new ValidationException("Email and password are required");
      }

      return userRepository.findByEmail(TextRules.trim(email)).compose(found -> {
        User user = found.orElseThrow(() -> new NotFoundException("User not found!"));
        return passwordHasher.matches(password, user.getPasswordHash()).map(matches -> {
          if (!matches) {
            throw new AuthenticationFailedException("Wrong password!");
          }
          return new AuthResult(UserDto.from(user), tokenService.issue(user.getId()));
        });
      });
    });
  }
}
