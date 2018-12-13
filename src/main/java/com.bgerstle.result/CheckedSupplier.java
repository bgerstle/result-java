package com.bgerstle.result;

@FunctionalInterface
public interface CheckedSupplier<V, E extends Throwable> {
  V get() throws E;
}
