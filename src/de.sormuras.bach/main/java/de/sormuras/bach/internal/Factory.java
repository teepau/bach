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

package de.sormuras.bach.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A factory method creates a new instance of the class that encloses the annotated element. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Factory {

  /** A description of the input sources the factory method. */
  enum Kind {

    /**
     * A static factory method that creates a new instance using any parameters.
     *
     * <pre>{@code
     * record Foo(Bar bar, String text) {
     *
     *   @Factory
     *   static Foo of() {
     *     return new Foo(Bar.of(), "Lorem");
     *   }
     *
     *   @Factory(Kind.STATIC)
     *   static Foo ofSystem() {
     *     return new Foo(Bar.of(), System.getProperty(...));
     *   }
     * }
     * }</pre>
     */
    STATIC,

    /**
     * An instance factory method that replaces a single component of the instance.
     *
     * <pre>{@code
     * record Foo(Bar bar, String text) {
     *
     *   @Factory(Kind.SETTER)
     *   Foo bar(Bar bar) {
     *     return new Foo(bar, this.text);
     *   }
     *
     *   @Factory(Kind.SETTER)
     *   Foo text(String text) {
     *     return new Foo(this.bar, text);
     *   }
     * }
     * }</pre>
     */
    SETTER,

    /**
     * An instance factory method that modifies one or more components with the given arguments.
     *
     * <pre>{@code
     * record Foo(Bar bar, String text) {
     *
     *   @Factory(Kind.OPERATOR)
     *   Foo withSuffix(String suffix) {
     *     return new Foo(bar, text + suffix);
     *   }
     * }
     * }</pre>
     */
    OPERATOR
  }

  /**
   * Return the factory kind.
   *
   * @return a kind constant.
   */
  Kind value() default Kind.STATIC;
}
