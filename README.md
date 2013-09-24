## ring-gzip-middleware

Gzips [Ring](http://github.com/ring-clojure/ring) responses for user agents
which can handle it.

### Usage

Apply the Ring middleware function `ring.middleware.gzip/wrap-gzip` to
your Ring handler, typically at the top level (i.e. as the last bit of
middleware in a `->` form).

### Installation

Add `[amalloy/ring-gzip-middleware "0.1.3"]` to your Leingingen dependencies.

### Compression of seq bodies

In JDK versions <=6, [`java.util.zip.GZIPOutputStream.flush()` does not actually
flush data compressed so
far](http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4813885), which means
that every gzip response must be complete before any bytes hit the wire.  While
of marginal importance when compressing static files and other resources that
are consumed in an all-or-nothing manner (i.e. virtually everything that is sent
in a Ring response), lazy sequences are impacted negatively by this.  In
particular, long-polling or server-sent event responses backed by lazy
sequences, when gzipped under <=JDK6, must be fully consumed before the client
receives any data at all.

So, _this middleware does not gzip-compress Ring seq response bodies unless the
JDK in use is 7+_, in which case it takes advantage of the new `flush`-ability
of `GZIPOutputStream` there.

### License

Copyright (C) 2010 Michael Stephens and other contributors.

Distributed under an MIT-style license (see LICENSE for details).
