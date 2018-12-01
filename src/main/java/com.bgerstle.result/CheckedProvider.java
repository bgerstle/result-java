package com.bgerstle.result;

@FunctionalInterface
public interface CheckedProvider<V, E extends Throwable> {
  V get() throws E;
}
