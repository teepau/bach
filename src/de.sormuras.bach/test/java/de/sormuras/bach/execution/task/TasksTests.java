/*
 * Bach - Java Shell Builder
 * Copyright (C) 2020 Christian Stein
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

package de.sormuras.bach.execution.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.sormuras.bach.execution.NoopToolProvider;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TasksTests {

  @Test
  void checkCreateDirectoriesWithEmptyPath() {
    var path = Path.of("");
    var task = new CreateDirectories(path);
    assertEquals("Create directories " + path, task.title());
    assertFalse(task.parallel());
    assertTrue(task.children().isEmpty());
  }

  @Test
  void checkRunToolProvider() {
    var task = new RunToolProvider(new NoopToolProvider(0, true), "a", "b", "c");
    assertEquals("Run `noop a b ...` (3 arguments)", task.title());
    assertFalse(task.parallel());
    assertTrue(task.children().isEmpty());
  }
}