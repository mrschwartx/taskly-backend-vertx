package com.taskly.backend.users.infrastructure;

import com.taskly.backend.shared.domain.AlreadyExistsException;
import com.taskly.backend.shared.domain.EntityIds;
import com.taskly.backend.shared.infrastructure.persistence.Rows;
import com.taskly.backend.shared.infrastructure.persistence.UniqueViolations;
import com.taskly.backend.users.domain.Email;
import com.taskly.backend.users.domain.User;
import com.taskly.backend.users.domain.UserRepository;
import com.taskly.backend.users.domain.Username;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PgUserRepository implements UserRepository {

  private static final String DUPLICATE_MESSAGE = "Email or Username is already registered.";

  private static final String COLUMNS = "id, username, email, password_hash, avatar_file, created_at, updated_at";

  private final Pool pool;

  public PgUserRepository(Pool pool) {
    this.pool = pool;
  }

  @Override
  public UUID nextId() {
    return EntityIds.generate();
  }

  @Override
  public Future<Void> add(User user) {
    return pool.preparedQuery("INSERT INTO users (" + COLUMNS + ") VALUES ($1, $2, $3, $4, $5, $6, $7) "
      + "ON CONFLICT DO NOTHING")
      .execute(Rows.tuple(user.getId(), user.getUsername().value(), user.getEmail().value(),
        user.getPasswordHash(), user.getAvatar(), Rows.timestamp(user.getCreatedAt()),
        Rows.timestamp(user.getUpdatedAt())))
      .compose(rows -> rows.rowCount() == 0
        ? Future.<Void>failedFuture(new AlreadyExistsException(DUPLICATE_MESSAGE))
        : Future.<Void>succeededFuture());
  }

  @Override
  public Future<Boolean> update(User user) {
    return pool.preparedQuery("UPDATE users SET username = $2, email = $3, password_hash = $4, "
      + "avatar_file = $5, updated_at = $6 WHERE id = $1")
      .execute(Rows.tuple(user.getId(), user.getUsername().value(), user.getEmail().value(),
        user.getPasswordHash(), user.getAvatar(), Rows.timestamp(user.getUpdatedAt())))
      .recover(PgUserRepository::translate)
      .map(rows -> rows.rowCount() == 1);
  }

  @Override
  public Future<Optional<User>> findById(UUID id) {
    return pool.preparedQuery("SELECT " + COLUMNS + " FROM users WHERE id = $1")
      .execute(Rows.tuple(id))
      .map(rows -> Rows.first(rows, PgUserRepository::toDomain));
  }

  @Override
  public Future<Optional<User>> findByEmail(String email) {
    return pool.preparedQuery("SELECT " + COLUMNS + " FROM users WHERE lower(email) = lower($1) LIMIT 1")
      .execute(Rows.tuple(email))
      .map(rows -> Rows.first(rows, PgUserRepository::toDomain));
  }

  @Override
  public Future<Boolean> existsByEmailOrUsername(String email, String username, UUID excludeId) {
    List<Object> parameters = new ArrayList<>();
    List<String> matches = new ArrayList<>();
    if (email != null) {
      parameters.add(email);
      matches.add("lower(email) = lower($" + parameters.size() + ")");
    }
    if (username != null) {
      parameters.add(username);
      matches.add("lower(username) = lower($" + parameters.size() + ")");
    }
    if (matches.isEmpty()) {
      return Future.succeededFuture(false);
    }
    StringBuilder sql = new StringBuilder("SELECT 1 FROM users WHERE (")
      .append(String.join(" OR ", matches))
      .append(")");
    if (excludeId != null) {
      parameters.add(excludeId);
      sql.append(" AND id <> $").append(parameters.size());
    }
    sql.append(" LIMIT 1");
    return pool.preparedQuery(sql.toString())
      .execute(Rows.tuple(parameters.toArray()))
      .map(rows -> rows.size() > 0);
  }

  @Override
  public Future<List<User>> search(String username, String email) {
    List<Object> parameters = new ArrayList<>();
    List<String> filters = new ArrayList<>();
    String usernamePattern = likePattern(username);
    if (usernamePattern != null) {
      parameters.add(usernamePattern);
      filters.add("username ILIKE $" + parameters.size());
    }
    String emailPattern = likePattern(email);
    if (emailPattern != null) {
      parameters.add(emailPattern);
      filters.add("email ILIKE $" + parameters.size());
    }
    String where = filters.isEmpty() ? "" : " WHERE " + String.join(" AND ", filters);
    return pool.preparedQuery("SELECT " + COLUMNS + " FROM users" + where + " ORDER BY created_at ASC, id ASC")
      .execute(Rows.tuple(parameters.toArray()))
      .map(rows -> Rows.all(rows, PgUserRepository::toDomain));
  }

  @Override
  public Future<Boolean> deleteById(UUID id) {
    return pool.preparedQuery("DELETE FROM users WHERE id = $1")
      .execute(Rows.tuple(id))
      .map(rows -> rows.rowCount() == 1);
  }

  private static User toDomain(Row row) {
    return User.restore(
      row.getUUID("id"),
      Username.restore(row.getString("username")),
      Email.restore(row.getString("email")),
      row.getString("password_hash"),
      row.getString("avatar_file"),
      Rows.instant(row, "created_at"),
      Rows.instant(row, "updated_at"));
  }

  private static String likePattern(String value) {
    if (value == null || value.isEmpty()) {
      return null;
    }
    return "%" + value.replaceAll("[\\\\%_]", "\\\\$0") + "%";
  }

  private static Future<RowSet<Row>> translate(Throwable error) {
    if (UniqueViolations.isUniqueViolation(error)) {
      return Future.failedFuture(new AlreadyExistsException(DUPLICATE_MESSAGE));
    }
    return Future.failedFuture(error);
  }
}
