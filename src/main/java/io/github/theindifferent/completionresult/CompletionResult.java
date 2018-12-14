/*
 *  BSD 3-Clause License
 *
 *  Copyright (c) 2018, Stanislav "The Indifferent" Baiduzhyi
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 *  FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package io.github.theindifferent.completionresult;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A promise containing either operation result or error code.
 *
 * <p>It is semantically similar to {@code CompletionStage<Either<V, E>}, but allows mapping of values and errors
 * without the need to unwrap or dereference stages.
 *
 * @param <V> the class of the value
 * @param <E> the enum class of the error
 *
 * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html">CompletionStage</a>
 * @see <a href="https://static.javadoc.io/io.vavr/vavr/0.9.2/io/vavr/control/Either.html">Either</a>
 *
 */
public class CompletionResult<V, E extends Enum<E>> {

  private final CompletionStage<Result<V, E>> stage;

  private CompletionResult(final CompletionStage<Result<V, E>> stage) {
    this.stage = requireNonNull(stage);
  }

  public static <V, E extends Enum<E>> CompletionResult<V, E> forStageResult(final CompletionStage<Result<V, E>> stage) {
    return new CompletionResult<>(stage);
  }

  public static <V, E extends Enum<E>> CompletionResult<V, E> forResult(final Result<V, E> result) {
    return forStageResult(CompletableFuture.completedFuture(result));
  }

  public static <V, E extends Enum<E>> CompletionResult<V, E> forValue(final V value) {
    final Result<V, E> result = Result.forValue(value);
    return forResult(result);
  }

  public static <V, E extends Enum<E>> CompletionResult<V, E> forError(final E error) {
    final Result<V, E> result = Result.forError(error);
    return forResult(result);
  }

  public CompletionResult<V, E> onException(Consumer<Throwable> consumer) {
    return new CompletionResult<>(
        stage.handle((res, t) -> {
          if (t != null) {
            consumer.accept(t);
            if (t instanceof CancellationException) {
              throw (CancellationException) t;
            }
            if (t instanceof CompletionException) {
              throw (CompletionException) t;
            }
            throw new CompletionException(t);
          }
          return res;
        }));
  }

  public CompletionResult<V, E> onResultValue(final Consumer<V> valueConsumer) {
    return new CompletionResult<>(
        stage.thenApply(result -> {
          if (result.isValue()) {
            valueConsumer.accept(result.value());
          }
          return result;
        }));
  }

  public CompletionResult<V, E> onResultError(final Consumer<E> errorConsumer) {
    return new CompletionResult<>(
        stage.thenApply(result -> {
          if (result.isError()) {
            errorConsumer.accept(result.error());
          }
          return result;
        }));
  }

  @SuppressWarnings("unchecked")
  public <T> CompletionResult<T, E> thenApplyValue(final Function<V, T> valueMapping) {
    requireNonNull(valueMapping);
    return new CompletionResult<>(
        stage.thenApply(res -> {
          if (res.isValue()) {
            return Result.forValue(valueMapping.apply(res.value()));
          }
          return (Result<T, E>) res;
        }));
  }

  public <T> CompletionResult<T, E> thenComposeValue(final Function<V, CompletionResult<T, E>> valueMapping) {
    requireNonNull(valueMapping);
    final CompletableFuture<Result<T, E>> future = new CompletableFuture<>();
    stage.whenComplete((res, throwable) -> composeValueImpl(future, valueMapping, res, throwable));
    return new CompletionResult<>(future);
  }

  @SuppressWarnings("unchecked")
  public <F extends Enum<F>> CompletionResult<V, F> thenApplyError(final Function<E, F> errorMapping) {
    requireNonNull(errorMapping);
    return new CompletionResult<>(
        stage.thenApply(res -> {
          if (res.isError()) {
            return Result.forError(errorMapping.apply(res.error()));
          }
          return (Result<V, F>) res;
        }));
  }

  public <F extends Enum<F>> CompletionResult<V, F> thenComposeError(final Function<E, CompletionResult<V, F>> errorMapping) {
    requireNonNull(errorMapping);
    final CompletableFuture<Result<V, F>> future = new CompletableFuture<>();
    stage.whenComplete((res, throwable) -> composeErrorImpl(future, errorMapping, res, throwable));
    return new CompletionResult<>(future);
  }

  public <T, F extends Enum<F>> CompletionResult<T, F> thenCompose(final Function<Result<V, E>, CompletionResult<T, F>> mapping) {
    requireNonNull(mapping);
    final CompletableFuture<Result<T, F>> future = new CompletableFuture<>();
    stage.whenComplete((res, throwable) -> composeImpl(future, mapping, res, throwable));
    return new CompletionResult<>(future);
  }

  @SuppressWarnings("unchecked")
  private <T> void composeValueImpl(@NonNull final CompletableFuture<Result<T, E>> future,
                                    @NonNull final Function<V, CompletionResult<T, E>> valueMapping,
                                    @Nullable final Result<V, E> result,
                                    @Nullable final Throwable throwable) {
    // got exception:
    if (throwable != null) {
      composeExceptionImpl(future, throwable);
      return;
    }
    // no exception but null result:
    if (result == null) {
      future.complete(null);
      return;
    }
    // throwable is null, result is not null:
    if (result.isError()) {
      future.complete((Result<T, E>) result);
      return;
    }
    // result has value:
    try {
      final CompletionResult<T, E> mapped = valueMapping.apply(result.value());
      mapped.stage.whenComplete((mappedResult, mappedThrowable) -> {
        if (mappedResult != null) {
          future.complete(mappedResult);
        } else {
          future.completeExceptionally(mappedThrowable);
        }
      });
    } catch (final Throwable mappingThrowable) {
      composeExceptionImpl(future, mappingThrowable);
    }
  }

  @SuppressWarnings("unchecked")
  private <F extends Enum<F>> void composeErrorImpl(@NonNull final CompletableFuture<Result<V, F>> future,
                                                    @NonNull final Function<E, CompletionResult<V, F>> errorMapping,
                                                    @Nullable final Result<V, E> result,
                                                    @Nullable final Throwable throwable) {
    // got exception:
    if (throwable != null) {
      composeExceptionImpl(future, throwable);
      return;
    }
    // no exception but null result:
    if (result == null) {
      future.complete(null);
      return;
    }
    // throwable is null, result is not null:
    if (result.isValue()) {
      future.complete((Result<V, F>) result);
      return;
    }
    // result has error:
    try {
      final CompletionResult<V, F> mapped = errorMapping.apply(result.error());
      mapped.stage.whenComplete((mappedResult, mappedThrowable) -> {
        if (mappedResult != null) {
          future.complete(mappedResult);
        } else {
          future.completeExceptionally(mappedThrowable);
        }
      });
    } catch (final Throwable mappingThrowable) {
      composeExceptionImpl(future, mappingThrowable);
    }
  }

  private <T, F extends Enum<F>> void composeImpl(@NonNull final CompletableFuture<Result<T, F>> future,
                                                  @NonNull final Function<Result<V, E>, CompletionResult<T, F>> mapping,
                                                  @Nullable final Result<V, E> result,
                                                  @Nullable final Throwable throwable) {
    // got exception:
    if (throwable != null) {
      composeExceptionImpl(future, throwable);
      return;
    }
    // no exception but null result:
    if (result == null) {
      future.complete(null);
      return;
    }
    // throwable is null, result is not null:
    try {
      final CompletionResult<T, F> mapped = mapping.apply(result);
      mapped.stage.whenComplete((mappedResult, mappedThrowable) -> {
        if (mappedResult != null) {
          future.complete(mappedResult);
        } else {
          future.completeExceptionally(mappedThrowable);
        }
      });
    } catch (final Throwable mappingThrowable) {
      composeExceptionImpl(future, mappingThrowable);
    }
  }

  private void composeExceptionImpl(@NonNull final CompletableFuture<?> future,
                                    @NonNull final Throwable throwable) {
    if (throwable instanceof CancellationException) {
      future.cancel(true);
      return;
    }
    if (throwable instanceof CompletionException) {
      future.completeExceptionally(throwable.getCause());
      return;
    }
    future.completeExceptionally(throwable);
  }

  Result<V, E> getBlocking() {
    return stage.toCompletableFuture().join();
  }
}
