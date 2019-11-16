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

package it;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

class Log extends de.sormuras.bach.Log {

  private final StringWriter out, err;

  Log() {
    this(new StringWriter(), new StringWriter());
  }

  private Log(StringWriter out, StringWriter err) {
    super(new PrintWriter(out, true), new PrintWriter(err, true), true);
    this.out = out;
    this.err = err;
  }

  List<String> lines() {
    return Strings.lines(out);
  }

  List<String> errors() {
    return Strings.lines(err);
  }
}