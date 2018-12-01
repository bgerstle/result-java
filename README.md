![badget](https://travis-ci.org/bgerstle/result-java.svg?branch=master)

# Result
A simple `Result` type in Java for functional exception handling. Inspired by Haskell's `Either`, [antitypical/Result](https://github.com/antitypical/Result) in Swift, and [cyclops' `Try`](https://github.com/aol/cyclops/blob/master/cyclops/src/main/java/cyclops/control/Try.java).

## Why should I use it?
**TL;DR;** `Result` allows you to call methods that throw checked exceptions in lambas without messy workarounds like nested try/catch or runtime exceptions.

With the advent of functional APIs in Java such as `Optional` and `Stream`, it's not uncommon to call methods which can throw a checked (or unchecked) exception in your lambdas. For instance, when parsing a URL from the app's environment:

``` java
Optional<URI> apiBaseURL = Optional.ofNullable(System.getenv("apiBaseURL")).map(baseUrlString -> {
  // Compiler error, unhandled URISyntaxException
  return new URI(baseUrlString);
});
```

In order to handle that `URISyntaxException` and unpack the optional, you might wind up with code looking like this:

``` java
URI apiBaseURL;
try {
  apiBaseURL = Optional
      .ofNullable(System.getenv("apiBaseURL"))
      .map(baseUrlString -> {
        try {
          return new URI(baseUrlString);
        } catch (URISyntaxException e) {
          // wrap as unchecked exception, catch outside of lambda
          throw new RuntimeException("uri parsing error", e);
        }
      })
      .get();
} catch (NoSuchElementException e) {
  throw new IllegalStateException("Missing apiBaseURL env var");
} catch (RuntimeException e) {
  // catch and re-throw the wrapped URISyntaxException
  if (e.getCause() instanceof URISyntaxException) {
    throw (URISyntaxException)e.getCause();
  } else {
    // something else weird happened, rethrow
    throw e;
  }
}
```

Variable reassignment, nested try/catch blocks, and runtime exceptions—oh my! Let's clean this up using `Result`:

``` java
URI apiBaseURI = Result
  .<String, Exception>attempt(Optional.ofNullable(System.getenv("apiBaseURL")::get) // 1
  .flatMapAttempt(URI::new) // 2
  .orElseThrow(); // 3
```

Here's the play by play:

1. `attempt` to `get` the optional value of `System.getenv(...)`, this yields a `Result<String, Exception>` which either contains a `String` or an exception (in this case, a `NoSuchElementException`)
2. `flatMap` the initial `Result` by trying to construct a `URI`, which returns a `Result<URI, Exception>`, containing either a `URI` or an exception
3. Unpack the final `Result`, which will either throw one of the captured exceptions or return the transformed value

Other great use cases for `Result` include:

- Rethrowing exceptions as an assertion: `Result.attempt(...).orElseAssert()` (similar to `try!` in Swift)
- Returning an empty optional on error: `Result<T, E>.attempt(...).getValue() // Optional<T>` (similar to `try?` in Swift)

## How do I use it?
`Result` isn't published in any repositories, so [Jitpack](https://jitpack.io/) is probably your best bet:

``` groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    ...
    compile 'com.github.bgerstle:result-java:master-SNAPSHOT'
}
```

You can substitute a tag or commit with `master-SNAPSHOT` if you don't to be on master.

## How do I build it?
With Java 11 or above installed, from the command line:

``` shell
./gradlew assemble
```

## How do I test it?
With Java 11 or above installed, from the command line:

``` shell
./gradlew check -i
```

Or, run all the tests in IntelliJ using a JUnit Run Configuration.
