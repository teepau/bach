/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
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

package de.sormuras.bach;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.spi.ToolProvider;

/** Provide Bach.java as a service. */
public class BachToolProvider implements ToolProvider {

  @Override
  public String name() {
    return "bach";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var bach = new Bach(out, err, false, Project.of(Path.of("")));
    try {
      bach.main(List.of(args));
      return 0;
    } catch (Throwable t) {
      t.printStackTrace(err);
      return 1;
    }
  }
}
