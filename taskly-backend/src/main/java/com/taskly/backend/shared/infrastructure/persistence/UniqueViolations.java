package com.taskly.backend.shared.infrastructure.persistence;

public final class UniqueViolations {

  private static final String UNIQUE_VIOLATION_STATE = "23505";

  private static final String[] STATE_ACCESSORS = { "getSqlState", "getCode" };

  private UniqueViolations() {
  }

  public static boolean isUniqueViolation(Throwable error) {
    for (Throwable current = error; current != null; current = current.getCause()) {
      if (UNIQUE_VIOLATION_STATE.equals(sqlState(current))) {
        return true;
      }
      if (current.getCause() == current) {
        return false;
      }
    }
    return false;
  }

  private static String sqlState(Throwable error) {
    for (String accessor : STATE_ACCESSORS) {
      try {
        Object value = error.getClass().getMethod(accessor).invoke(error);
        if (value instanceof String state) {
          return state;
        }
      }
      catch (ReflectiveOperationException | RuntimeException failure) {
        continue;
      }
    }
    return null;
  }
}
