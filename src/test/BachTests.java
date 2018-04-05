/*
 * Bach - Java Shell Builder
 * Copyright (C) 2018 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;

@ExtendWith(BachContext.class)
class BachTests {

  @Test
  void log(BachContext context) {
    context.bach.info("log %s", "1");
    context.bach.vars.level = System.Logger.Level.WARNING;
    context.bach.info("log %s", "2");
    assertLinesMatch(List.of("log 1"), context.recorder.all);
  }

  @Test
  void debug(BachContext context) {
    assertTrue(context.bach.debug());
    context.bach.debug("debug %s", "1");
    context.bach.vars.level = System.Logger.Level.OFF;
    assertFalse(context.bach.debug());
    context.bach.debug("debug %s", "2");
    context.bach.vars.level = System.Logger.Level.INFO;
    assertFalse(context.bach.debug());
    context.bach.debug("debug %s", "3");
    assertEquals(1, context.recorder.all.size());
    assertLinesMatch(List.of("debug 1"), context.recorder.all);
  }

  @Test
  void runExecutable(BachContext context) {
    assertThrows(Error.class, () -> context.bach.run("command", "a", "b", "3"));
    assertEquals("[run] command [a, b, 3]", context.recorder.all.get(0));
  }

  @Test
  void runExecutableInQuietMode(BachContext context) {
    context.bach.vars.level = System.Logger.Level.OFF;
    assertThrows(Error.class, () -> context.bach.run("command", "a", "b", "3"));
    assertTrue(context.recorder.all.isEmpty());
  }

  @Test
  void runStreamSequentially(BachContext context) {
    var tasks = context.tasks(3);
    var result = context.bach.run("run stream sequentially", tasks);
    assertEquals(0, result);
    var expected =
        List.of(
            "[run] run stream sequentially...",
            "1 begin",
            "1 done. .+",
            "2 begin",
            "2 done. .+",
            "3 begin",
            "3 done. .+",
            "[run] run stream sequentially done.");
    assertLinesMatch(expected, context.recorder.all);
  }

  @Test
  void runStreamParallel(BachContext context) {
    var tasks = context.tasks(3).parallel();
    var result = context.bach.run("run stream in parallel", tasks);
    assertEquals(0, result);
    var expected =
        List.of(
            "[run] run stream in parallel...",
            ". begin",
            ". begin",
            ". begin",
            ". done. .+",
            ". done. .+",
            ". done. .+",
            "[run] run stream in parallel done.");
    assertLinesMatch(expected, context.recorder.all);
  }

  @Test
  void runVarArgs(BachContext context) {
    var result = context.bach.run("run varargs", () -> context.task("A"), () -> context.task("B"));
    assertEquals(0, result);
    var expected =
        List.of(
            "[run] run varargs...",
            ". begin",
            ". begin",
            ". done. .+",
            ". done. .+",
            "[run] run varargs done.");
    assertLinesMatch(expected, context.recorder.all);
  }

  @Test
  void runThrowsIllegalStateExceptionOnNoneZeroResult(BachContext context) {
    Supplier<Integer> nine = () -> context.task("42", () -> 9);
    Executable executable = () -> context.bach.run("error", Stream.of(nine));
    var exception = assertThrows(IllegalStateException.class, executable);
    assertEquals("0 expected, but got: 9", exception.getMessage());
  }

  @Test
  void runCommand(BachContext context) {
    var bach = context.bach;
    var bytes = new ByteArrayOutputStream(2000);
    var out = new PrintStream(bytes);
    var command = new JdkTool.Java().toCommand(bach).add("--version");
    command.setStandardStreams(out, out);
    var result = bach.run("java --version", command);
    assertEquals(0, result);
    assertTrue(bytes.toString().contains(Runtime.version().toString()));
    var expected =
        List.of(
            "[run] java --version...",
            "running java with 1 argument(s)",
            "java\n--version",
            "replaced executable `java` with program `" + bach.util.getJdkCommand("java") + "`",
            "[run] java --version done.");
    assertLinesMatch(expected, context.recorder.all);
  }
}
