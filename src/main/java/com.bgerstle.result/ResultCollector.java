package com.bgerstle.result;

import java.util.ArrayList;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class ResultCollector<V, E extends Throwable>
    implements Collector<Result<V, E>, Result<ArrayList<V>, E>, Result<ArrayList<V>, E>> {
  private Result<ArrayList<V>, E> finalListResult;

  public ResultCollector() {
    finalListResult = Result.success(new ArrayList<>());
  }

  @Override
  public Supplier<Result<ArrayList<V>, E>> supplier() {
    return () -> finalListResult;
  }

  private void accumulate(Result<ArrayList<V>, E> unused, Result<V, E> result) {
    this.finalListResult = finalListResult.flatMap(values -> {
      return result.map(value -> {
        values.add(value);
        return values;
      });
    });
  }

  @Override
  public BiConsumer<Result<ArrayList<V>, E>, Result<V, E>> accumulator() {
    return this::accumulate;
  }

  @Override
  public BinaryOperator<Result<ArrayList<V>, E>> combiner() {
    return (rs1, rs2) -> {
      assert rs1 == rs2;
      assert rs2 == finalListResult;
      return finalListResult;
    };
  }

  @Override
  public Function<Result<ArrayList<V>, E>, Result<ArrayList<V>, E>> finisher() {
    return (results) -> finalListResult;
  }

  @Override
  public Set<Characteristics> characteristics() {
    return Set.of();
  }
}
