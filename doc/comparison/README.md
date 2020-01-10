# Build Tool Comparison

## Minimal

Single module project with sole `module-info.java` compilation unit:

```java
module de.sormuras.bach.doc.minimal {}
```

The goal of each build tool, is to generate 3 JAR files:

- Java module
- `-sources.jar`
- `-javadoc.jar`

All build tools are able to generate those 3 required JAR files.
See https://github.com/sormuras/bach/runs/375630551 for details and their build logs.

Here's a summary of the most interesting numbers.

| Tool           | Project Files | Raw Build Time | Total Time | Tool Files | Tool Size in MB |
|----------------| ------------- | -------------- | ---------- | ---------- | --------------- |
| Bach.java 2.0  |             1 |             2  |         11 |          1 |             0.1 |
| Maven 3.6.3    |             6 |            14  |         18 |       1394 |            44   |
| Gradle 6.0.1   |             7 |            43  |         44 |        447 |           330   |


### Bach.java

```text
└───src
    └───de.sormuras.bach.doc.minimal
            module-info.java
```

1 file, the module compilation unit, weighing in 12,327 bytes before build.

`jshell https://bit.ly/bach-build`

14 seconds later (including 1.5 s raw build time), the directory contains 1,936,297 bytes.

### Maven

```text
│   mvnw
│   mvnw.cmd
│   pom.xml
│
├───.mvn
│   └───wrapper
│           maven-wrapper.properties
│           MavenWrapperDownloader.java
│
└───src
    └───main
        └───java
                module-info.java

```

6 files, the module compilation unit, the [pom.xml](minimal/maven/pom.xml) file and assets of the Maven Wrapper.
All of them weighing in 48,756 bytes before build.

`mvn verify`

18 seconds later (including 13.9 s raw build time), the directory contains 1,905,905 bytes.

In addition, `~/.m2` was created. That directory contains 1394 files with 44,353,411 bytes.

### Gradle

```text
│   build.gradle.kts
│   gradlew
│   gradlew.bat
│   settings.gradle.kts
│
├───gradle
│   └───wrapper
│           gradle-wrapper.jar
│           gradle-wrapper.properties
│
└───src
    └───main
        └───java
                module-info.java
```

7 files weighing in 92,583 bytes before build, including the main build file: [build.gradle.kts](minimal/gradle/build.gradle.kts)

`./gradlew build`

47 seconds later (45 s raw (?!) build time), the directory contains 2,056,204 bytes.

In addition, `~/.gradle` was created. That directory contains 444 files with 330,224,076 bytes.

Local `.gradle` directory containing hashes, caches, properties, etc, ...  was ignored.
Same for the `~/.tooling/gradle` directory.