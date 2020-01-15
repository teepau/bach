//usr/bin/env jshell --execution local --show-version "$0" "$@"; exit $?

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

/*
 * Zero-installation "bach build" script.
 */

// The "/open" directive below this comment requires a constant String literal as its argument.
// Due to this restriction, the URL points to the "master" version.

/open https://github.com/sormuras/bach/raw/2.1-M1/src/bach/Bach.java

System.setProperty("Bach.java/transferIO", "true")
var code = 0
try {
  Bach.main();
} catch (Throwable throwable) {
  System.err.println(throwable.getMessage());
  code = 1;
}

/exit code
