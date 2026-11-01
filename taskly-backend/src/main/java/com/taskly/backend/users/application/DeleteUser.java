package com.taskly.backend.users.application;

import com.taskly.backend.shared.application.EventBus;
import com.taskly.backend.shared.application.Futures;
import com.taskly.backend.shared.domain.DomainEvent;
import com.taskly.backend.shared.domain.EntityIds;
import com.taskly.backend.shared.domain.IntegrationEvents;
import com.taskly.backend.shared.domain.NotFoundException;
import com.taskly.backend.users.domain.User;
import com.taskly.backend.users.domain.UserRepository;
import io.vertx.core.Future;
import java.util.Map;
import java.util.UUID;

public class DeleteUser {

  private final UserRepository userRepository;

  private final AvatarStorage avatarStorage;

  private final EventBus eventBus;

  public DeleteUser(UserRepository userRepository, AvatarStorage avatarStorage, EventBus eventBus) {
    this.userRepository = userRepository;
    this.avatarStorage = avatarStorage;
    this.eventBus = eventBus;
  }

  public Future<Void> execute(UUID requesterId, String userId) {
    return Futures.attempt(() -> {
      OwnAccount.assertOwned(requesterId, userId, "You can only delete your own account");
      UUID id = EntityIds.parse(userId, "user");

      return userRepository.findById(id).compose(found -> {
        User user = found.orElseThrow(() -> new NotFoundException("User not found!"));
        return userRepository.deleteById(id)
          .compose(deleted -> deleted
            ? avatarStorage.remove(user.getAvatar())
            : Future.<Void>failedFuture(new NotFoundException("User not found!")))
          .compose(done -> eventBus.publish(DomainEvent.of(IntegrationEvents.USER_DELETED, Map.of("userId", id))));
      });
    });
  }
}
