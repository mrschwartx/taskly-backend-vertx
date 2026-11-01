package com.taskly.backend.tasks.application;

import static com.taskly.backend.support.Await.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.taskly.backend.shared.domain.ForbiddenException;
import com.taskly.backend.shared.domain.NotFoundException;
import com.taskly.backend.shared.domain.ValidationException;
import com.taskly.backend.support.InMemoryTaskRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TaskUseCasesTest {

  private InMemoryTaskRepository tasks;

  private CreateTask createTask;

  private GetTask getTask;

  private ListTasksByUser listTasks;

  private UpdateTask updateTask;

  private DeleteTask deleteTask;

  private DeleteTasksByOwner deleteByOwner;

  private final UUID owner = UUID.randomUUID();

  private final UUID intruder = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    tasks = new InMemoryTaskRepository();
    createTask = new CreateTask(tasks);
    getTask = new GetTask(tasks);
    listTasks = new ListTasksByUser(tasks);
    updateTask = new UpdateTask(tasks);
    deleteTask = new DeleteTask(tasks);
    deleteByOwner = new DeleteTasksByOwner(tasks);
  }

  private static Map<String, Object> payload(String name, String status) {
    Map<String, Object> input = new HashMap<>();
    input.put("name", name);
    input.put("description", "Finish reading");
    input.put("priority", "low");
    input.put("due", "2026-12-08T10:00:00Z");
    if (status != null) {
      input.put("status", status);
    }
    return input;
  }

  @Test
  void createsATaskOwnedByTheRequester() {
    TaskDto task = await(createTask.execute(owner, payload("Read", "todo")));

    assertThat(task.owner()).isEqualTo(owner.toString());
    assertThat(task.due()).isEqualTo("2026-12-08T10:00:00.000Z");
    assertThat(task.createdAt()).isEqualTo(task.updatedAt());
  }

  @Test
  void createDefaultsStatusAndDescriptionAndIgnoresOwnerInput() {
    Map<String, Object> input = payload("Read", null);
    input.remove("description");
    input.put("owner", intruder.toString());

    TaskDto task = await(createTask.execute(owner, input));

    assertThat(task.status()).isEqualTo("todo");
    assertThat(task.description()).isEmpty();
    assertThat(task.owner()).isEqualTo(owner.toString());
  }

  @Test
  void createRejectsInvalidPayloads() {
    assertThatThrownBy(() -> await(createTask.execute(owner, Map.of()))).isInstanceOf(ValidationException.class);
    assertThatThrownBy(() -> await(createTask.execute(owner, payload("", "todo")))).isInstanceOf(ValidationException.class);
    Map<String, Object> nullStatus = payload("Read", null);
    nullStatus.put("status", null);
    assertThatThrownBy(() -> await(createTask.execute(owner, nullStatus))).hasMessage("Task status is required");
  }

  @Test
  void getEnforcesOwnership() {
    TaskDto task = await(createTask.execute(owner, payload("Read", "todo")));

    assertThat(await(getTask.execute(owner, task.id()))).isEqualTo(task);
    assertThatThrownBy(() -> await(getTask.execute(intruder, task.id()))).isInstanceOf(ForbiddenException.class);
    assertThatThrownBy(() -> await(getTask.execute(owner, UUID.randomUUID().toString())))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("Task not found!");
    assertThatThrownBy(() -> await(getTask.execute(owner, "nope"))).hasMessage("Invalid task ID format");
  }

  @Test
  void listsFourTasksPerPage() {
    for (int index = 1; index <= 6; index++) {
      await(createTask.execute(owner, payload("Task " + index, "todo")));
    }

    TaskListDto first = await(listTasks.execute(owner, owner.toString(), null, null, null));
    TaskListDto second = await(listTasks.execute(owner, owner.toString(), null, null, "2"));

    assertThat(first.taskCount()).isEqualTo(6);
    assertThat(first.tasks()).hasSize(4);
    assertThat(second.tasks()).extracting(TaskDto::name).containsExactly("Task 5", "Task 6");
  }

  @Test
  void listFiltersAndOrders() {
    await(createTask.execute(owner, payload("b-task", "done")));
    await(createTask.execute(owner, payload("a-task", "done")));
    await(createTask.execute(owner, payload("c-task", "todo")));

    TaskListDto result = await(listTasks.execute(owner, owner.toString(), "done", "name", null));

    assertThat(result.taskCount()).isEqualTo(2);
    assertThat(result.tasks()).extracting(TaskDto::name).containsExactly("a-task", "b-task");
  }

  @Test
  void listNormalizesPageNumbersLikeParseInt() {
    for (int index = 1; index <= 5; index++) {
      await(createTask.execute(owner, payload("Task " + index, "todo")));
    }
    String id = owner.toString();

    assertThat(await(listTasks.execute(owner, id, null, null, "abc")).tasks()).hasSize(4);
    assertThat(await(listTasks.execute(owner, id, null, null, "-3")).tasks()).hasSize(4);
    assertThat(await(listTasks.execute(owner, id, null, null, "2xyz")).tasks()).hasSize(1);
    assertThat(await(listTasks.execute(owner, id, null, null, "99999999999999999999")).tasks()).isEmpty();
  }

  @Test
  void listValidatesRequestAndOwnership() {
    String id = owner.toString();

    assertThatThrownBy(() -> await(listTasks.execute(intruder, id, null, null, null)))
        .isInstanceOf(ForbiddenException.class);
    assertThatThrownBy(() -> await(listTasks.execute(owner, "bad", null, null, null))).hasMessage("Invalid user ID format");
    assertThatThrownBy(() -> await(listTasks.execute(owner, id, null, "owner", null))).hasMessage("Invalid orderBy field");
    assertThatThrownBy(() -> await(listTasks.execute(owner, id, List.of("a", "b"), null, null)))
        .hasMessage("Status filter must be a string");
    assertThat(await(listTasks.execute(owner, id, null, null, null))).isEqualTo(new TaskListDto(List.of(), 0));
  }

  @Test
  void updatesATask() {
    TaskDto created = await(createTask.execute(owner, payload("Read", "todo")));

    TaskDto updated = await(updateTask.execute(owner, created.id(),
        Map.of("name", "Read - Updated", "status", "in_progress", "owner", intruder.toString())));

    assertThat(updated.name()).isEqualTo("Read - Updated");
    assertThat(updated.status()).isEqualTo("in_progress");
    assertThat(updated.owner()).isEqualTo(owner.toString());
    assertThat(updated.createdAt()).isEqualTo(created.createdAt());
  }

  @Test
  void updateRejectsForeignEmptyAndInvalidChanges() {
    TaskDto created = await(createTask.execute(owner, payload("Read", "todo")));

    assertThatThrownBy(() -> await(updateTask.execute(intruder, created.id(), Map.of("name", "Hacked"))))
        .isInstanceOf(ForbiddenException.class);
    assertThatThrownBy(() -> await(updateTask.execute(owner, created.id(), Map.of())))
        .hasMessage("No updatable fields provided");
    assertThatThrownBy(() -> await(updateTask.execute(owner, created.id(), Map.of("due", "never"))))
        .isInstanceOf(ValidationException.class);
    assertThat(await(getTask.execute(owner, created.id())).name()).isEqualTo("Read");
  }

  @Test
  void deletesATaskOnlyForItsOwner() {
    TaskDto created = await(createTask.execute(owner, payload("Read", "todo")));

    assertThatThrownBy(() -> await(deleteTask.execute(intruder, created.id()))).isInstanceOf(ForbiddenException.class);
    await(deleteTask.execute(owner, created.id()));

    assertThatThrownBy(() -> await(getTask.execute(owner, created.id()))).isInstanceOf(NotFoundException.class);
    assertThatThrownBy(() -> await(deleteTask.execute(owner, UUID.randomUUID().toString())))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void deletesAllTasksOfAnOwner() {
    await(createTask.execute(owner, payload("One", "todo")));
    await(createTask.execute(owner, payload("Two", "todo")));
    await(createTask.execute(intruder, payload("Other", "todo")));

    assertThat(await(deleteByOwner.execute(owner.toString()))).isEqualTo(2);

    assertThat(await(listTasks.execute(owner, owner.toString(), null, null, null)).taskCount()).isZero();
    assertThat(await(listTasks.execute(intruder, intruder.toString(), null, null, null)).taskCount()).isEqualTo(1);
  }
}
