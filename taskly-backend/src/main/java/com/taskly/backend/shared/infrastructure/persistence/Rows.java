package com.taskly.backend.shared.infrastructure.persistence;

import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class Rows {

  private Rows() {
  }

  public static Tuple tuple(Object... values) {
    return Tuple.from(new ArrayList<Object>(Arrays.asList(values)));
  }

  public static OffsetDateTime timestamp(Instant instant) {
    return instant.atOffset(ZoneOffset.UTC);
  }

  public static Instant instant(Row row, String column) {
    return row.getOffsetDateTime(column).toInstant();
  }

  public static <T> Optional<T> first(RowSet<Row> rows, Function<Row, T> mapper) {
    for (Row row : rows) {
      return Optional.of(mapper.apply(row));
    }
    return Optional.empty();
  }

  public static <T> List<T> all(RowSet<Row> rows, Function<Row, T> mapper) {
    List<T> result = new ArrayList<>();
    for (Row row : rows) {
      result.add(mapper.apply(row));
    }
    return result;
  }
}
