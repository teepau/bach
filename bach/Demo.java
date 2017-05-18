/*
 * Java Shell Builder
 * Copyright (C) 2017 Christian Stein
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

import java.net.URI;
import java.nio.file.Paths;
import java.util.logging.Level;

public class Demo {

  public static void main(String... args) throws Exception {
    System.out.printf("%n%s%n%n", "BASIC");
    new Bach(Level.FINE, Layout.BASIC)
        .set(Folder.SOURCE, Paths.get("demo/basic"))
        .set(Folder.TARGET, Paths.get("target/bach/basic"))
        .format()
        .compile()
        .run("com.greetings", "com.greetings.Main");

    System.out.printf("%n%s%n%n", "COMMON");
    new Bach(Level.INFO, Layout.COMMON)
        .set(Folder.SOURCE, Paths.get("demo/common"))
        .set(Folder.TARGET, Paths.get("target/bach/common"))
        .format()
        .compile()
        .run("com.greetings", "com.greetings.Main");

    System.out.printf("%n%s%n%n", "IDEA");
    new Bach(Level.INFO, Layout.IDEA)
        .set(Folder.SOURCE, Paths.get("demo/idea"))
        .set(Folder.TARGET, Paths.get("target/bach/idea"))
        .format()
        .load("org.junit.jupiter.api", URI.create("http://central.maven.org/maven2/org/junit/jupiter/junit-jupiter-api/5.0.0-M4/junit-jupiter-api-5.0.0-M4.jar"))
        .load("org.junit.platform.commons", URI.create("http://central.maven.org/maven2/org/junit/platform/junit-platform-commons/1.0.0-M4/junit-platform-commons-1.0.0-M4.jar"))
        .compile()
        .test()
        .run("com.greetings", "com.greetings.Main");
  }
}
