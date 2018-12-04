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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

@SuppressWarnings("unchecked")
public class CompletionResultTest {

  @Rule
  public Timeout timeout = new Timeout(10, TimeUnit.SECONDS);

  @Test
  public void testOnExceptionOnly() {
    final Consumer<TestError> errorConsumer = mock(Consumer.class);
    final Consumer<String> valueConsumer = mock(Consumer.class);
    final Consumer<Throwable> exceptionConsumer = mock(Consumer.class);
    final CustomException expectedException = new CustomException();
    final CompletionResult<String, TestError> completionResult =
        CompletionResult.forStageResult(exceptionallyCompletedFuture(expectedException));
    completionResult
        .onException(exceptionConsumer)
        .onResultError(errorConsumer)
        .onResultValue(valueConsumer);
    // wait for it to finish:
    try {
      completionResult.getBlocking();
    } catch (final Throwable ignore) {
      // ignore
    }
    verify(exceptionConsumer, times(1)).accept(eq(expectedException));
    verify(errorConsumer, never()).accept(any());
    verify(valueConsumer, never()).accept(any());
    verifyNoMoreInteractions(exceptionConsumer, valueConsumer, errorConsumer);
  }

  @Test
  public void testOnErrorOnly() {
    final Consumer<TestError> errorConsumer = mock(Consumer.class);
    final Consumer<String> valueConsumer = mock(Consumer.class);
    final Consumer<Throwable> exceptionConsumer = mock(Consumer.class);
    final CompletionResult<String, TestError> completionResult =
        CompletionResult.forError(TestError.RANDOM_ERROR);
    completionResult
        .onException(exceptionConsumer)
        .onResultValue(valueConsumer)
        .onResultError(errorConsumer);
    // wait for it to finish:
    try {
      completionResult.getBlocking();
    } catch (final Throwable ignore) {
      // ignore
    }
    verify(errorConsumer, times(1)).accept(eq(TestError.RANDOM_ERROR));
    verify(valueConsumer, never()).accept(any());
    verify(exceptionConsumer, never()).accept(any());
    verifyNoMoreInteractions(exceptionConsumer, valueConsumer, errorConsumer);
  }

  @Test
  public void testOnValueOnly() {
    final String expectedString = "the quick brown fox jumps over the lazy dog";
    final Consumer<TestError> errorConsumer = mock(Consumer.class);
    final Consumer<String> valueConsumer = mock(Consumer.class);
    final Consumer<Throwable> exceptionConsumer = mock(Consumer.class);
    final CompletionResult<String, TestError> completionResult =
        CompletionResult.forValue(expectedString);
    completionResult
        .onException(exceptionConsumer)
        .onResultError(errorConsumer)
        .onResultValue(valueConsumer);
    // wait for it to finish:
    try {
      completionResult.getBlocking();
    } catch (final Throwable ignore) {
      // ignore
    }
    verify(valueConsumer, times(1)).accept(eq(expectedString));
    verify(errorConsumer, never()).accept(any());
    verify(exceptionConsumer, never()).accept(any());
    verifyNoMoreInteractions(exceptionConsumer, valueConsumer, errorConsumer);
  }

  @Test
  public void testApplyValue() {
    final Integer inputValue = 13;
    final CompletionResult<Integer, TestError> completionResult = CompletionResult.forValue(inputValue);
    final Result<String, TestError> result = completionResult
        .thenApplyError(error -> TestError.SECOND_ERROR)
        .thenApplyValue(String::valueOf)
        .getBlocking();
    assertTrue(result.isValue());
    assertEquals("13", result.value());
  }

  @Test
  public void testApplyError() {
    final CompletionResult<Integer, TestError> completionResult = CompletionResult.forError(TestError.RANDOM_ERROR);
    final Result<String, TestError> result = completionResult
        .thenApplyError(error -> TestError.SECOND_ERROR)
        .thenApplyValue(String::valueOf)
        .getBlocking();
    assertTrue(result.isError());
    assertEquals(TestError.SECOND_ERROR, result.error());
  }

  @Test
  public void testComposeValue() {
    final String expectedString = "the quick brown fox jumps over the lazy dog";
    final CompletionResult<Integer, TestError> completionResult = CompletionResult.forValue(13);
    final CompletionResult<String, TestError> mappedValue = CompletionResult.forValue(expectedString);
    final CompletionResult<String, TestError2> mappedError = CompletionResult.forError(TestError2.ANOTHER_ERROR_TYPE);
    final Result<String, TestError2> result = completionResult
        .thenComposeValue(i -> mappedValue)
        .thenComposeError(e -> mappedError)
        .getBlocking();
    assertTrue(result.isValue());
    assertEquals(expectedString, result.value());
  }

  @Test
  public void testComposeError() {
    final CompletionResult<Integer, TestError> completionResult = CompletionResult.forError(TestError.RANDOM_ERROR);
    final CompletionResult<String, TestError> mappedValue = CompletionResult.forValue("10");
    final CompletionResult<String, TestError2> mappedError = CompletionResult.forError(TestError2.ANOTHER_ERROR_TYPE);
    final Result<String, TestError2> result = completionResult
        .thenComposeValue(i -> mappedValue)
        .thenComposeError(e -> mappedError)
        .getBlocking();
    assertTrue(result.isError());
    assertEquals(TestError2.ANOTHER_ERROR_TYPE, result.error());
  }

  @Test
  public void testCompose() {
    final CompletionResult<Integer, TestError> completionResult = CompletionResult.forError(TestError.RANDOM_ERROR);
    final Result<String, TestError2> expectedResult = Result.forError(TestError2.ANOTHER_ERROR_TYPE);
    final CompletionResult<String, TestError2> mapped = CompletionResult.forResult(expectedResult);
    final Result<String, TestError2> result = completionResult
        .thenCompose(res -> mapped)
        .getBlocking();
    assertEquals(expectedResult, result);
  }

  @Test
  public void testCancellation() {
    final Consumer<Throwable> exceptionConsumer = mock(Consumer.class);
    final CompletableFuture<Result<String, TestError>> future = new CompletableFuture<>();
    future.cancel(true);
    CompletionResult.forStageResult(future)
        .onException(exceptionConsumer);
    verify(exceptionConsumer, times(1)).accept(isA(CancellationException.class));
    verifyNoMoreInteractions(exceptionConsumer);
  }

  @Test
  public void testCancellationAfterComposeValue() {
    final Consumer<Throwable> exceptionConsumer = mock(Consumer.class);
    final CompletableFuture<Result<String, TestError>> future = new CompletableFuture<>();
    future.cancel(true);
    CompletionResult.forStageResult(future)
        .thenComposeValue(str -> CompletionResult.forValue("1"))
        .onException(exceptionConsumer);
    verify(exceptionConsumer, times(1)).accept(isA(CancellationException.class));
    verifyNoMoreInteractions(exceptionConsumer);
  }

  @Test
  public void testCancellationAfterComposeError() {
    final Consumer<Throwable> exceptionConsumer = mock(Consumer.class);
    final CompletableFuture<Result<String, TestError>> future = new CompletableFuture<>();
    future.cancel(true);
    CompletionResult.forStageResult(future)
        .thenComposeError(e -> CompletionResult.forError(TestError2.ANOTHER_ERROR_TYPE))
        .onException(exceptionConsumer);
    verify(exceptionConsumer, times(1)).accept(isA(CancellationException.class));
    verifyNoMoreInteractions(exceptionConsumer);
  }

  @Test
  public void testCancellationAfterComposeResult() {
    final Consumer<Throwable> exceptionConsumer = mock(Consumer.class);
    final CompletableFuture<Result<String, TestError>> future = new CompletableFuture<>();
    future.cancel(true);
    CompletionResult.forStageResult(future)
        .thenCompose(res -> CompletionResult.forError(TestError2.ANOTHER_ERROR_TYPE))
        .onException(exceptionConsumer);
    verify(exceptionConsumer, times(1)).accept(isA(CancellationException.class));
    verifyNoMoreInteractions(exceptionConsumer);
  }

  @Test
  public void testComposeNotWrappingException() {
    final CustomException expected = new CustomException();
    final Consumer<Throwable> exceptionConsumer = mock(Consumer.class);
    final CompletableFuture<Result<String, TestError>> future = new CompletableFuture<>();
    future.completeExceptionally(expected);
    CompletionResult.forStageResult(future)
        .thenComposeValue(str -> CompletionResult.forValue("1"))
        .onException(exceptionConsumer);
    verify(exceptionConsumer, times(1)).accept(eq(expected));
    verifyNoMoreInteractions(exceptionConsumer);
  }

  @Test
  public void testApplyThrowsException() {
    final Consumer<Throwable> exceptionConsumer = mock(Consumer.class);
    CompletionResult.forValue(1)
        .thenApplyValue(str -> {
          throw new CustomException();
        })
        .onException(exceptionConsumer);
    verify(exceptionConsumer, times(1)).accept(isA(CompletionException.class));
    verifyNoMoreInteractions(exceptionConsumer);
  }

  @Test
  public void testComposeValueThrowsException() {
    final Consumer<Throwable> exceptionConsumer = mock(Consumer.class);
    CompletionResult.forValue(1)
        .thenComposeValue(val -> {
          throw new CustomException();
        })
        .onException(exceptionConsumer);
    verify(exceptionConsumer, times(1)).accept(isA(CustomException.class));
    verifyNoMoreInteractions(exceptionConsumer);
  }

  @Test
  public void testComposeErrorThrowsException() {
    final Consumer<Throwable> exceptionConsumer = mock(Consumer.class);
    CompletionResult.forError(TestError.RANDOM_ERROR)
        .thenComposeError(error -> {
          throw new CustomException();
        })
        .onException(exceptionConsumer);
    verify(exceptionConsumer, times(1)).accept(isA(CustomException.class));
    verifyNoMoreInteractions(exceptionConsumer);
  }

  @Test
  public void testComposeResultThrowsException() {
    final Consumer<Throwable> exceptionConsumer = mock(Consumer.class);
    CompletionResult.forValue(1)
        .thenCompose(result -> {
          throw new CustomException();
        })
        .onException(exceptionConsumer);
    verify(exceptionConsumer, times(1)).accept(isA(CustomException.class));
    verifyNoMoreInteractions(exceptionConsumer);
  }

  @Test
  public void testComposeValueOnNullStage() {
    final CompletableFuture<Result<String, TestError>> future = new CompletableFuture<>();
    future.complete(null);
    final CompletionResult<String, TestError> composeFrom = CompletionResult.forStageResult(future);
    final Result<Integer, TestError> result = composeFrom
        .thenComposeValue(i -> CompletionResult.forValue(1))
        .getBlocking();
    assertNull(result);
  }

  @Test
  public void testComposeErrorOnNullStage() {
    final CompletableFuture<Result<String, TestError>> future = new CompletableFuture<>();
    future.complete(null);
    final CompletionResult<String, TestError> composeFrom = CompletionResult.forStageResult(future);
    final Result<String, TestError2> result = composeFrom
        .thenComposeError(e -> CompletionResult.forError(TestError2.ANOTHER_ERROR_TYPE))
        .getBlocking();
    assertNull(result);
  }

  @Test
  public void testComposeResultOnNullStage() {
    final CompletableFuture<Result<String, TestError>> future = new CompletableFuture<>();
    future.complete(null);
    final CompletionResult<String, TestError> composeFrom = CompletionResult.forStageResult(future);
    final CompletionResult<Integer, TestError> composeTo = CompletionResult.forValue(1);
    final Result<Integer, TestError> result = composeFrom
        .thenCompose(res -> composeTo)
        .getBlocking();
    assertNull(result);
  }

  @Test
  public void testComposeValueExceptionally() {
    final Consumer<Throwable> exceptionConsumer = mock(Consumer.class);
    final CustomException expectedException = new CustomException();
    final CompletableFuture<Result<String, TestError>> future = new CompletableFuture<>();
    future.completeExceptionally(expectedException);
    final CompletionResult<String, TestError> composeTo = CompletionResult.forStageResult(future);
    final CompletionResult<Integer, TestError> composeFrom = CompletionResult.forValue(1);
    composeFrom
        .thenComposeValue(i -> composeTo)
        .onException(exceptionConsumer);
    verify(exceptionConsumer, times(1)).accept(eq(expectedException));
    verifyNoMoreInteractions(exceptionConsumer);
  }

  @Test
  public void testComposeErrorExceptionally() {
    final Consumer<Throwable> exceptionConsumer = mock(Consumer.class);
    final CustomException expectedException = new CustomException();
    final CompletableFuture<Result<String, TestError>> future = new CompletableFuture<>();
    future.completeExceptionally(expectedException);
    final CompletionResult<String, TestError> composeTo = CompletionResult.forStageResult(future);
    final CompletionResult<String, TestError> composeFrom = CompletionResult.forError(TestError.RANDOM_ERROR);
    composeFrom
        .thenComposeError(error -> composeTo)
        .onException(exceptionConsumer);
    verify(exceptionConsumer, times(1)).accept(eq(expectedException));
    verifyNoMoreInteractions(exceptionConsumer);
  }

  @Test
  public void testComposeResultExceptionally() {
    final Consumer<Throwable> exceptionConsumer = mock(Consumer.class);
    final CustomException expectedException = new CustomException();
    final CompletableFuture<Result<String, TestError>> future = new CompletableFuture<>();
    future.completeExceptionally(expectedException);
    final CompletionResult<String, TestError> composeTo = CompletionResult.forStageResult(future);
    final CompletionResult<Integer, TestError> composeFrom = CompletionResult.forValue(1);
    composeFrom
        .thenCompose(res -> composeTo)
        .onException(exceptionConsumer);
    verify(exceptionConsumer, times(1)).accept(eq(expectedException));
    verifyNoMoreInteractions(exceptionConsumer);
  }

  @Test
  public void testComposeOnExceptionallyCompleted() {
    final CustomException expectedException = new CustomException();
    final CompletionStage<Result<String, TestError>> inputStage = CompletableFuture.completedFuture(1)
        .thenApply(i -> {
          throw expectedException;
        });
    final CompletionResult<String, TestError> composeFrom = CompletionResult.forStageResult(inputStage);
    final CompletionResult<Integer, TestError> composeTo = CompletionResult.forValue(2);
    try {
      composeFrom
          .thenComposeValue(str -> composeTo)
          .getBlocking();
      fail("Composing from exceptionally completed stage should result in exception");
    } catch (final CompletionException ex) {
      assertSame(expectedException, ex.getCause());
    }
  }

  private <T> CompletionStage<T> exceptionallyCompletedFuture(final Throwable throwable) {
    final CompletableFuture<T> future = new CompletableFuture<>();
    future.completeExceptionally(throwable);
    return future;
  }

  public enum TestError {
    RANDOM_ERROR,
    SECOND_ERROR
  }

  public enum TestError2 {
    ANOTHER_ERROR_TYPE
  }

  private static class CustomException extends RuntimeException {
  }

}
