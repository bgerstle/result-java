package com.bgerstle.result;

public interface CheckedRunnable<E extends Throwable> {
  void run() throws E;
}
