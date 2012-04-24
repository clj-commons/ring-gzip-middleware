## ring-gzip-middleware

Gzips [Ring](http://github.com/mmcgrana/ring) responses for user agents which can handle it.

### Usage

Apply the Ring middleware function `ring.middleware.gzip/wrap-gzip` to
your Ring handler, typically at the top level (i.e. as the last bit of
middleware in a `->` form).


### Installation

Add `[amalloy/ring-gzip-middleware "0.1.1"]` to your Leingingen dependencies.

### License

Copyright (C) 2010 Michael Stephens and other contributors.

Distributed under an MIT-style license (see LICENSE for details).
