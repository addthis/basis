# benchy

## What's this?

`benchy` is a simple template-like mini-module that can be put into repos
to let you easily create and maintain benchmarks/ tests using
[jmh](http://openjdk.java.net/projects/code-tools/jmh/)/
[jol](http://openjdk.java.net/projects/code-tools/jol/).

## Building

You must build/ install the enclosing project first, or otherwise ensure that the version
`benchy` refers to is correct and available. Then run `mvn clean package` from the
base of the benchy subdirectory. eg. from the benchy root directory:

```bash
cd ..
mvn clean install
cd benchy
mvn clean package
```

## Use

Run `java -jar target/microbenchmarks.jar -h` to get command line help from jmh. To
run individual benchmarks, `java -jar target/microbenchmarks.jar <regex>` will run
all benchmarks matching the given regex. Otherwise, see the comments in individual
benchmarks or use the programmatic jmh api (outside the scope of this readme).

See the links provided at the top for JMH and JOL for details and additional links to
examples on how to use them.

`benchy` is not intended be published to maven central or other repositories and is mainly
for local, experimental use with the enclosing project. To use it in a new project, copy
this README.md and the pom.xml file into a new directory "benchy" in that repository. The
pom.xml file has three properties that need to be changed that are clearly marked near
the top.

## Administrative

### License

benchy is released under the Apache License Version 2.0.  See
[Apache](http://www.apache.org/licenses/LICENSE-2.0) or the LICENSE for details.
