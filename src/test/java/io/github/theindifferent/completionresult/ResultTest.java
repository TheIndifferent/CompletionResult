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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ResultTest {

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Test
  public void testValue() {
    final String input = "the quick brown fox jumps over the lazy dog";
    final Result<String, TestError> result = Result.forValue(input);
    assertTrue(result.isValue());
    assertFalse(result.isError());
    assertEquals(input, result.value());

    expected.expect(IllegalStateException.class);
    result.error();
    fail("Error was returned for value result");
  }

  @Test
  public void testError() {
    final TestError error = TestError.RANDOM_ERROR;
    final Result<Object, TestError> result = Result.forError(error);
    assertTrue(result.isError());
    assertFalse(result.isValue());
    assertEquals(TestError.RANDOM_ERROR, result.error());

    expected.expect(IllegalStateException.class);
    result.value();
    fail("Value was returned for error result");
  }

  @Test
  public void testIdentity() {
    final Map<Result<String, TestError>, Integer> map = new HashMap<>();
    map.put(Result.forValue("the quick brown fox jumps over the lazy dog"), 1);
    map.put(Result.forError(TestError.RANDOM_ERROR), 2);

    assertEquals(Integer.valueOf(1), map.get(Result.forValue("the quick brown fox jumps over the lazy dog")));
    assertEquals(Integer.valueOf(2), map.get(Result.forError(TestError.RANDOM_ERROR)));
  }

  public enum TestError {
    RANDOM_ERROR
  }
}
