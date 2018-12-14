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

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Value class holding either value of the operation, or enum code of error.
 *
 * @param <V> the class of the value
 * @param <E> the enum class of the error
 */
public interface Result<V, E extends Enum<E>> {

  /**
   * Returns a {@code Result} with the specified non-null value.
   *
   * @param value value instance to wrap
   * @param <V> the class of the value
   * @param <E> the enum class of the error
   * @return {@code Result} instance holding the specified value instance
   */
  static <V, E extends Enum<E>> Result<V, E> forValue(@NonNull final V value) {
    requireNonNull(value);
    return new ResultValue<>(value);
  }

  /**
   * Returns a {@code Result} with the specified non-null error.
   *
   * @param error error code
   * @param <V> the class of the value
   * @param <E> the enum class of the error
   * @return {@code Result} instance holding the specified error
   */
  static <V, E extends Enum<E>> Result<V, E> forError(@NonNull final E error) {
    requireNonNull(error);
    return new ResultError<>(error);
  }

  /**
   * Returns the wrapped value instance, never {@code null}.
   *
   * @return the value instance, never {@code null}
   * @throws IllegalStateException if {@code Result} is an error
   */
  @NonNull
  V value() throws IllegalStateException;

  /**
   * Returns the wrapped error code, never {@code null}.
   *
   * @return the error code, never {@code null}
   * @throws IllegalStateException if {@code Result} is a value
   */
  @NonNull
  E error() throws IllegalStateException;

  /**
   * Returns {@code true} if the {@code Result} contains a value, {@code false} otherwise.
   * @return {@code true} if the {@code Result} contains a value, {@code false} otherwise
   */
  boolean isValue();

  /**
   * Returns {@code true} if the {@code Result} contains an error code, {@code false} otherwise.
   * @return {@code true} if the {@code Result} contains an error code, {@code false} otherwise
   */
  boolean isError();

}
