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

jmh is a benchmarking harness that provides tools to prevent and/ or account for
jvm/ jit optimizations -- eg. dead code elimination that naive benchmarks frequently encounter.
It has most of the usual niceties as well, so it is pretty okay once you get past the initial
learning curve.

jol is a tool for analyzing the in-memory representation for objects. It can sometimes
reveal fun jvm inefficiencies like byte alignment padding -- wasted heap space that
inflates your class's actual size. The good news is that sometimes you can move things
around to reclaim that space, and even if not, knowing you have x "free" bytes to spend
can help you make informed decisions about space trade-offs.

See the links provided at the top for jmh and jol for details and additional links to
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
