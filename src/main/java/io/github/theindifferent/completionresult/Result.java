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

import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class Result<V, E extends Enum<E>> {

  public static <V, E extends Enum<E>> Result<V, E> forValue(@NonNull final V value) {
    requireNonNull(value);
    return new ValueResult<>(value);
  }

  public static <V, E extends Enum<E>> Result<V, E> forError(@NonNull final E error) {
    requireNonNull(error);
    return new ErrorResult<>(error);
  }

  @NonNull
  public abstract V value();

  @NonNull
  public abstract E error();

  public abstract boolean isValue();

  public abstract boolean isError();

  private static class ValueResult<V, E extends Enum<E>> extends Result<V, E> {

    private final V value;

    ValueResult(final V value) {
      this.value = value;
    }

    @NonNull
    @Override
    public V value() {
      return value;
    }

    @NonNull
    @Override
    public E error() {
      throw new IllegalStateException("Successful result does not have error");
    }

    @Override
    public boolean isValue() {
      return true;
    }

    @Override
    public boolean isError() {
      return false;
    }

    @Override
    public boolean equals(@Nullable final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      final ValueResult<?, ?> that = (ValueResult<?, ?>) o;
      return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }

    @Override
    public String toString() {
      return "Result{"
             + "value="
             + value
             + '}';
    }
  }

  private static class ErrorResult<V, E extends Enum<E>> extends Result<V, E> {

    private final E error;

    ErrorResult(final E error) {
      this.error = error;
    }

    @NonNull
    @Override
    public V value() {
      throw new IllegalStateException("Error result does not have value");
    }

    @NonNull
    @Override
    public E error() {
      return error;
    }

    @Override
    public boolean isValue() {
      return false;
    }

    @Override
    public boolean isError() {
      return true;
    }

    @Override
    public boolean equals(@Nullable final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      final ErrorResult<?, ?> that = (ErrorResult<?, ?>) o;
      return Objects.equals(error, that.error);
    }

    @Override
    public int hashCode() {
      return Objects.hash(error);
    }

    @Override
    public String toString() {
      return "Result{"
             + "error="
             + error
             + '}';
    }
  }
}
