package com.bgerstle.result;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.quicktheories.core.Gen;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.Generate.oneOf;
import static org.quicktheories.generators.SourceDSL.strings;

public class ResultTests {
  public static Gen<? extends Result<Object, Exception>> results() {
    Gen<String> strs =  strings().allPossible().ofLengthBetween(1, 5);
    return
        oneOf(strs.map(s -> (Object)s),
              strs.map(s -> new Exception(s)).map(e -> (Object)e))
            .map(Result::of);
  }

  @Test
  void emptyOptionalTransformationExample() {
    assertThrows(NoSuchElementException.class, () ->
        Result.<String, Exception>attempt(Optional.<String>empty()::get)
              .flatMapAttempt(URI::new)
              .orElseThrow());
  }

  @Test
  void optionalInvalidURITransformationExample() {
    assertThrows(URISyntaxException.class, () ->
        Result.<String, Exception>attempt(Optional.of("://")::get)
            .flatMapAttempt(URI::new)
            .orElseThrow());
  }

  @Test
  void optionalValidURITransformationExample() throws Exception {
    URI apiBaseUrl =
        Result.<String, Exception>attempt(Optional.of("http://example.com")::get)
            .flatMapAttempt(URI::new)
            .orElseThrow();
    assertThat(apiBaseUrl, equalTo(new URI("http://example.com")));
  }

  @Test
  void wrapsValues() {
    assertThat(
        Result.attempt(() -> "foo").orElseAssert(),
        is("foo")
    );
  }

  @ParameterizedTest
  @ValueSource(classes = {RuntimeException.class,
                          IOException.class,
                          Exception.class})
  void wrapsExceptions(Class<Exception> exceptionClass) {
    assertThrows(exceptionClass, () ->
        Result
            .attempt(() -> {
              throw exceptionClass.getConstructor().newInstance();
            })
            .orElseThrow()
    );
  }

  @Test
  void wrapsSubtypeExceptions() {
    assertThrows(RuntimeException.class, () ->
        Result.<Object, Exception>attempt(() -> {
          throw new RuntimeException();
        })
            .orElseThrow()
    );
  }

  @Test
  void flatmapsNewValue() {
    qt().forAll(strings().allPossible().ofLengthBetween(1, 10),
                strings().allPossible().ofLengthBetween(1, 10))
        .checkAssert((s1, s2) ->
      assertThat(
          Result.attempt(() -> s1)
                .flatMap((f) -> Result.success(f + s2))
                .orElseAssert(),
          is(s1 + s2)
      )
    );
  }

  @Test
  void flatmapsNewError() {
    assertThrows(StringIndexOutOfBoundsException.class, () ->
        Result.success("foo")
              .flatMapAttempt((f) -> f.charAt(f.length()))
              .orElseThrow()
    );
  }

  @Test
  void errorsSuppressSubsequentMaps() {
    assertThrows(StringIndexOutOfBoundsException.class, () ->
        Result.success("foo")
              .flatMapAttempt((f) -> f.charAt(f.length()))
              .map((c) -> {
                fail();
                return Character.toUpperCase(c);
              })
              .orElseThrow()
    );
  }

  @Test
  void errorResultsAreEmpty() {
    assertThat(Result.failure(new Exception()).toOptional().isPresent(), is(false));
  }

  @Test
  void successResultsArePresent() {
    assertThat(Result.success(0).toOptional().isPresent(), is(true));
  }

  @Test
  void resultEquality() {
    qt().forAll(results(), results().toOptionals(50))
        .checkAssert((r1, r2) -> {
          assertThat(
              // all result equality-based method results
              (r1.equals(r2.orElse(null))
               && r1.toString().equals(r2.map(Result::toString).orElse(null))
               && r1.hashCode() == r2.map(Result::hashCode).orElse(null)),
              // are identical to the equality of their underlying value/error
              is(r1.getEither().equals(r2.map(Result::getEither).orElse(null))));
        });
  }

  @Test
  void resultIdentity() {
    qt().forAll(results())
        .checkAssert((r) -> {
          assertThat(
              (r.equals(r)
               && r.toString().equals(r.toString())
               && r.hashCode() == r.hashCode()),
              is(true));
        });
  }
}
