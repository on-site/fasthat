fasthat, a faster heap dump analyser
====================================

Overview
--------

fasthat is a fork of OpenJDK 6's [jhat][jhat] that enhances our ability
to analyse large heap dumps (typically 4 to 8 GB) that we frequently
work with at On-Site.

Features
--------

Above and beyond the features already present in [jhat][jhat], fasthat
is enhanced in the following ways:

+ Performance:
    + Faster loading of large heap dumps, by using a deque for the
      tree-walking phase (which uses a FIFO), rather than a vector.
    + Faster execution of OQL, by using real Rhino rather than the
      in-JDK Rhino.
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
+ [Rhino][rhino]

I always try to keep up-to-date with the latest released versions. At
the time of current writing, I'm using Guava 12.0 and Rhino 1.7R4.

I develop and build using Eclipse. [Mike Virata-Stone][mjvs] has created
an Ant `build.xml` that you may find useful, but I have not tested it.
In particular, you may need to update it to work with the latest Guava
jars.

Future directions
-----------------

Since this isn't my full-time project, there are many things I'd like
to improve on that I haven't yet got around to:

+ Performance:
    + Look into ways to make heap dump loading more concurrent. Right
      now, on our 16-core machine, sometimes the CPU usage is 1300%
      and sometimes it's 100%. The more of those 100% we can turn into
      1300%, the better.
    + Make all the model and script operations interruptible, so that
      they can stop running as soon as the user hits the Stop button.
      Right now, the interruption only happens when the page is being
      written out (after all the computation has already been done and
      squandered).
+ Language-specific models:
    + Allow real tracing through JRuby classes, etc.
    + Allow inspection of JRuby stack traces.
    + Make the object views use language-specific models much more
      pervasively.
    + Implement unpacking of more object types, especially for JRuby
      and Guava.
    + Implement models for OpenJDK 7 and JRuby 1.7.
    + Implement better support for detecting Guava data structures
      given that [Guava doesn't have a version signature][guava-ver]
      in the heap dump.
+ Miscellaneous:
    + Merge in changes from OpenJDK 7's jhat.
    + Merge in changes to the Rhino [JSR-223][jsr-223] engine from
      [scripting.java.net][scripting] and OpenJDK 7, if any.

Contact and licensing
---------------------

fasthat is maintained by [Chris Jester-Young][cky].

All of the code from OpenJDK are licensed under GPLv2 with Classpath
Exception. All of the new code (not originating from OpenJDK) are
licensed under GPLv2 or later, with Classpath Exception.

[jhat]: http://docs.oracle.com/javase/6/docs/technotes/tools/share/jhat.html
[guava]: http://code.google.com/p/guava-libraries/
[rhino]: http://www.mozilla.org/rhino/
[mjvs]: http://github.com/mikestone
[guava-ver]: http://stackoverflow.com/q/7694468/13
[jsr-223]: http://www.jcp.org/en/jsr/detail?id=223
[scripting]: http://java.net/projects/scripting/
[cky]: http://github.com/cky
