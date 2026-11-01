package com.taskly.backend.tasks.infrastructure;

import com.taskly.backend.shared.domain.EntityIds;
import com.taskly.backend.shared.infrastructure.persistence.Rows;
import com.taskly.backend.tasks.domain.DueDate;
import com.taskly.backend.tasks.domain.Task;
import com.taskly.backend.tasks.domain.TaskDescription;
import com.taskly.backend.tasks.domain.TaskName;
import com.taskly.backend.tasks.domain.TaskPage;
import com.taskly.backend.tasks.domain.TaskPriority;
import com.taskly.backend.tasks.domain.TaskRepository;
import com.taskly.backend.tasks.domain.TaskStatus;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class PgTaskRepository implements TaskRepository {

  private static final String COLUMNS = "id, owner_id, name, description, priority, due, status, created_at, updated_at";

  private static final Map<String, String> SORT_COLUMNS = Map.of(
    "name", "name",
    "description", "description",
    "priority", "priority",
    "due", "due",
    "status", "status",
    "createdAt", "created_at",
    "updatedAt", "updated_at");

  private final Pool pool;

  public PgTaskRepository(Pool pool) {
    this.pool = pool;
  }

  @Override
  public UUID nextId() {
    return EntityIds.generate();
  }

  @Override
  public Future<Void> add(Task task) {
    return pool.preparedQuery("INSERT INTO tasks (" + COLUMNS + ") VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)")
      .execute(Rows.tuple(task.getId(), task.getOwnerId(), task.getName().value(),
        task.getDescription().value(), task.getPriority().value(), Rows.timestamp(task.getDue().value()),
        task.getStatus().value(), Rows.timestamp(task.getCreatedAt()), Rows.timestamp(task.getUpdatedAt())))
      .mapEmpty();
  }

  @Override
  public Future<Boolean> update(Task task) {
    return pool.preparedQuery("UPDATE tasks SET name = $2, description = $3, priority = $4, due = $5, "
      + "status = $6, updated_at = $7 WHERE id = $1")
      .execute(Rows.tuple(task.getId(), task.getName().value(), task.getDescription().value(),
        task.getPriority().value(), Rows.timestamp(task.getDue().value()), task.getStatus().value(),
        Rows.timestamp(task.getUpdatedAt())))
      .map(rows -> rows.rowCount() == 1);
  }

  @Override
  public Future<Optional<Task>> findById(UUID id) {
    return pool.preparedQuery("SELECT " + COLUMNS + " FROM tasks WHERE id = $1")
      .execute(Rows.tuple(id))
      .map(rows -> Rows.first(rows, PgTaskRepository::toDomain));
  }

  @Override
  public Future<TaskPage> findByOwner(UUID ownerId, String status, String orderBy, int page, int pageSize) {
    List<Object> filterParameters = new ArrayList<>();
    filterParameters.add(ownerId);
    StringBuilder where = new StringBuilder(" WHERE owner_id = $1");
    if (status != null) {
      filterParameters.add(status);
      where.append(" AND status = $2");
    }
    String countSql = "SELECT count(*) FROM tasks" + where;
    String pageSql = "SELECT " + COLUMNS + " FROM tasks" + where + " ORDER BY " + orderClause(orderBy)
      + " LIMIT $" + (filterParameters.size() + 1) + " OFFSET $" + (filterParameters.size() + 2);

    List<Object> pageParameters = new ArrayList<>(filterParameters);
    pageParameters.add((long) pageSize);
    pageParameters.add((long) (page - 1) * pageSize);

    return pool.preparedQuery(countSql).execute(Rows.tuple(filterParameters.toArray()))
      .compose(counted -> {
        long total = counted.iterator().next().getLong(0);
        return pool.preparedQuery(pageSql).execute(Rows.tuple(pageParameters.toArray()))
          .map(rows -> new TaskPage(Rows.all(rows, PgTaskRepository::toDomain), total));
      });
  }

  @Override
  public Future<Boolean> deleteById(UUID id) {
    return pool.preparedQuery("DELETE FROM tasks WHERE id = $1")
      .execute(Rows.tuple(id))
      .map(rows -> rows.rowCount() == 1);
  }

  @Override
  public Future<Integer> deleteByOwner(UUID ownerId) {
    return pool.preparedQuery("DELETE FROM tasks WHERE owner_id = $1")
      .execute(Rows.tuple(ownerId))
      .map(rows -> rows.rowCount());
  }

  private static String orderClause(String orderBy) {
    Set<String> columns = new LinkedHashSet<>();
    String requested = orderBy != null ? SORT_COLUMNS.get(orderBy) : null;
    if (requested != null) {
      columns.add(requested);
    }
    columns.add("created_at");
    columns.add("id");
    return String.join(" ASC, ", columns) + " ASC";
  }

  private static Task toDomain(Row row) {
    return Task.restore(
      row.getUUID("id"),
      row.getUUID("owner_id"),
      TaskName.restore(row.getString("name")),
      TaskDescription.restore(row.getString("description")),
      TaskPriority.restore(row.getString("priority")),
      DueDate.restore(Rows.instant(row, "due")),
      TaskStatus.restore(row.getString("status")),
      Rows.instant(row, "created_at"),
      Rows.instant(row, "updated_at"));
  }
}
