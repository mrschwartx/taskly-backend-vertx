package com.taskly.backend.config;

import com.taskly.backend.shared.application.EventBus;
import com.taskly.backend.shared.infrastructure.events.InMemoryEventBus;
import com.taskly.backend.tasks.application.CreateTask;
import com.taskly.backend.tasks.application.DeleteTask;
import com.taskly.backend.tasks.application.DeleteTasksByOwner;
import com.taskly.backend.tasks.application.GetTask;
import com.taskly.backend.tasks.application.ListTasksByUser;
import com.taskly.backend.tasks.application.UpdateTask;
import com.taskly.backend.tasks.domain.TaskRepository;
import com.taskly.backend.tasks.infrastructure.PgTaskRepository;
import com.taskly.backend.tasks.infrastructure.UserDeletedSubscriber;
import com.taskly.backend.users.application.AuthenticateUser;
import com.taskly.backend.users.application.AvatarStorage;
import com.taskly.backend.users.application.ChangeUserAvatar;
import com.taskly.backend.users.application.DeleteUser;
import com.taskly.backend.users.application.GetUser;
import com.taskly.backend.users.application.ListUsers;
import com.taskly.backend.users.application.RegisterUser;
import com.taskly.backend.users.application.UpdateUser;
import com.taskly.backend.users.application.VerifyAccessToken;
import com.taskly.backend.users.domain.PasswordHasher;
import com.taskly.backend.users.domain.TokenService;
import com.taskly.backend.users.domain.UserRepository;
import com.taskly.backend.users.infrastructure.BcryptPasswordHasher;
import com.taskly.backend.users.infrastructure.JwtTokenService;
import com.taskly.backend.users.infrastructure.LocalAvatarStorage;
import com.taskly.backend.users.infrastructure.PgUserRepository;
import com.taskly.backend.users.presentation.AuthCookie;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;

public final class Services {

  public final RegisterUser registerUser;

  public final AuthenticateUser authenticateUser;

  public final VerifyAccessToken verifyAccessToken;

  public final GetUser getUser;

  public final ListUsers listUsers;

  public final UpdateUser updateUser;

  public final DeleteUser deleteUser;

  public final ChangeUserAvatar changeUserAvatar;

  public final CreateTask createTask;

  public final GetTask getTask;

  public final ListTasksByUser listTasksByUser;

  public final UpdateTask updateTask;

  public final DeleteTask deleteTask;

  public final AuthCookie authCookie;

  private Services(Vertx vertx, Pool pool, TasklyConfig config) {
    TasklyConfig.Auth auth = config.auth();
    PasswordHasher passwordHasher = new BcryptPasswordHasher(vertx, auth.bcryptRounds());
    TokenService tokenService = new JwtTokenService(auth.secret(), auth.tokenTtl());
    AvatarStorage avatarStorage = new LocalAvatarStorage(vertx, config.storage().publicDir());
    EventBus eventBus = new InMemoryEventBus();
    UserRepository userRepository = new PgUserRepository(pool);
    TaskRepository taskRepository = new PgTaskRepository(pool);

    this.registerUser = new RegisterUser(userRepository, passwordHasher, tokenService);
    this.authenticateUser = new AuthenticateUser(userRepository, passwordHasher, tokenService);
    this.verifyAccessToken = new VerifyAccessToken(tokenService);
    this.getUser = new GetUser(userRepository);
    this.listUsers = new ListUsers(userRepository);
    this.updateUser = new UpdateUser(userRepository, passwordHasher);
    this.deleteUser = new DeleteUser(userRepository, avatarStorage, eventBus);
    this.changeUserAvatar = new ChangeUserAvatar(userRepository, avatarStorage);

    this.createTask = new CreateTask(taskRepository);
    this.getTask = new GetTask(taskRepository);
    this.listTasksByUser = new ListTasksByUser(taskRepository);
    this.updateTask = new UpdateTask(taskRepository);
    this.deleteTask = new DeleteTask(taskRepository);

    new UserDeletedSubscriber(eventBus, new DeleteTasksByOwner(taskRepository));

    this.authCookie = new AuthCookie(auth.cookieName(), auth.tokenTtl(), auth.cookieDomain(),
      config.app().production());
  }

  public static Services create(Vertx vertx, Pool pool, TasklyConfig config) {
    return new Services(vertx, pool, config);
  }
}
