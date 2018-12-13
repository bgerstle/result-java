package com.bgerstle.result;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class Result<V, E extends Throwable> {
  private final V value;
  private final E error;

  private Result(V value, E error) {
    this.value = value;
    this.error = error;
  }

  public static <V, E extends Throwable> Result<V, E> failure(E error) {;
    return new Result<>(null, Objects.requireNonNull(error));
  }

  public static <V, E extends Throwable> Result<V, E> success(V value) {
    return new Result<>(Objects.requireNonNull(value), null);
  }

  public Object getEither() {
    return value != null ? value : error;
  }

  public Optional<V> getValue() {
    return Optional.ofNullable(value);
  }

  public Optional<E> getError() {
    return Optional.ofNullable(error);
  }

  public static <E extends Throwable>
  Result<Optional<Object>, E> attempt(CheckedRunnable<? extends E> r) {
    return attempt(() -> {
      r.run();
      return Optional.empty();
    });
  }

  public static <V, E extends Throwable>
  Result<V, E> attempt(CheckedSupplier<? extends V, ? extends E> p) {

    try {
      return Result.success(p.get());
    } catch (Throwable e) {
      @SuppressWarnings("unchecked")
      E err = (E)e;
      return Result.failure(err);
    }
  }

  @SuppressWarnings("unchecked")
  public static <V, E extends Throwable> Result<V, E> of(Object o) {
    if (o instanceof Throwable) {
      return Result.failure((E)o);
    }
    return Result.success((V)o);
  }

  public <T> Result<T, E> map(Function<? super V, ? extends T> mapper) {
    return Result.of(getValue().<Object>map(mapper).orElse(error));
  }

  public <T> Result<T, E> flatMapAttempt(CheckedFunction<? super V, ? extends T, ? super E> cf) {
    return flatMap(v -> Result.attempt(() -> cf.apply(v)));
  }


  public <T> Result<T, E> flatMap(Function<? super V, ? extends Result<? extends T, ? super E>> mapper) {
    return getValue()
        .map((v) -> {
          @SuppressWarnings("unchecked")
          Result<T, E> r = (Result<T, E>) mapper.apply(v);
          return Objects.requireNonNull(r);
        })
        .orElseGet(() -> Result.failure(error));
  }

  /**
   * Recover from a specific exception (and its subclasses), providing a fallback value.
   * @param eClass    The exception class to "catch," along with its subclasses.
   * @param recovery  A function returning a fallback value in case of the specified exception(s).
   * @param <E2>      Type parameter constraining eClass to exceptions.
   * @return A Result which either has the receiver's data, or the fallback value in case the specified exception class was caught.
   */
  public <E2 extends Throwable> Result<V, E> recover(Class<E2> eClass, Supplier<V> recovery) {
    if (getError().flatMap(e -> eClass.isInstance(e) ? Optional.of(e) : Optional.empty()).isPresent()) {
      return Result.success(recovery.get());
    }
    return this;
  }

  public V orElseThrow() throws E {
    return orElseThrowAs(e -> e);
  }

  public <E2 extends Throwable> V orElseThrowAs(Function<E, E2> emapper) throws E2 {
    return Optional.ofNullable(value).orElseThrow(() -> emapper.apply(error));
  }

  public V orElseAssert() {
    return orElseThrowAs(AssertionError::new);
  }

  public Optional<V> toOptional() {
    return Optional.ofNullable(value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Result<?, ?> result = (Result<?, ?>) o;
    return Objects.equals(value, result.value) &&
           Objects.equals(error, result.error);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, error);
  }

  @Override
  public String toString() {
    return "Result{" +
           "value=" + value +
           ", error=" + error +
           '}';
  }
}
