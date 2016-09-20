fasthat, a faster heap dump analyser
====================================

Overview
--------

fasthat is a fork of OpenJDK 8's [jhat][jhat] that enhances our ability
to analyse large heap dumps (typically 4 to 8 GB) that we frequently
work with at On-Site.

Features
--------

Above and beyond the features already present in [jhat][jhat], fasthat
is enhanced in the following ways:

+ Performance:
    + Faster loading of large heap dumps, by using a deque for the
      tree-walking phase (which uses a FIFO), rather than a vector.
+ Functionality:
    + **Referrer chains**: The histogram now has the ability to look
      at only objects that refer to instances of a specific class.
      For example, if your heap dump is full of `String`s, you can
      now look at a histogram of just the objects that refer to any
      `String` objects.
    + **Language-specific models**: Various object/collection types
      from OpenJDK, specific versions of JRuby, and Guava can now have
      their contents viewed conveniently in the object view.

As I solve more challenges in analysing our heap dumps, I'll be adding
more features.

Dependencies
------------

fasthat uses the following libraries:

+ [Guava][guava]

I always try to keep up-to-date with the latest released versions. At
the time of current writing, I'm using Guava 18.0.

I develop and build using Eclipse. [Mike Virata-Stone][smellsblue] has created
an Ant `build.xml` that you may find useful. If you are set up with ant and JDK
8, you can run `ant` to build the jar, and then run via the following command
(make sure to adjust your max heap to allow for the size of your heap dump):
```
$ java -Xmx8g -jar bin/fasthat.jar path/to/your/heap.dump
```

Future directions
-----------------

Since this isn't my full-time project, there are many things I'd like
to improve on that I haven't yet got around to:

+ Performance:
    + Look into ways to make heap dump loading more concurrent. Right
      now, on our 16-core machine, sometimes the CPU usage is 1300%
      and sometimes it's 100%. The more of those 100% we can turn into
      1300%, the better.
    + Audit the code to find and fix any weird concurrency bugs.
    + Figure out what is shareable per-thread in Nashorn, and what must
      be distinct. Currently I've taken the very conservative approach
      of creating a new Nashorn instance for each OQL query, but that is
      probably way over the top.
    + Make all the model and script operations interruptible, so that
      they can stop running as soon as the user hits the Stop button.
      Right now, the interruption only happens when the page is being
      written out (after all the computation has already been done and
      squandered).
+ Language-specific models:
    + Allow real tracing through JRuby classes, etc. In particular,
      this means having JRuby classes be selectable via the histogram.
    + Allow inspection of JRuby stack traces.
    + Make the object views use language-specific models much more
      pervasively.
    + Enable OQL queries on language-specific object properties.
    + Implement unpacking of more object types, especially for JRuby
      and Guava.
    + Implement models for OpenJDK 7 and 8, and JRuby 1.7.
    + Implement better support for detecting Guava data structures
      given that [Guava doesn't have a version signature][guava-ver]
      in the heap dump.

Contact and licensing
---------------------

fasthat is maintained by [Chris Jester-Young][cky] and [Mike Virata-Stone][smellsblue].

All of the code from OpenJDK are licensed under GPLv2 with Classpath
Exception. All of the new code (not originating from OpenJDK) are
licensed under GPLv2 or later, with Classpath Exception.

[jhat]: http://docs.oracle.com/javase/6/docs/technotes/tools/share/jhat.html
[guava]: https://github.com/google/guava
[smellsblue]: https://github.com/smellsblue
[guava-ver]: http://stackoverflow.com/q/7694468/13
[jsr-223]: http://www.jcp.org/en/jsr/detail?id=223
[cky]: https://github.com/cky
