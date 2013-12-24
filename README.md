Unofficial Bugsnag Notifier for Clojure
=======================================

This is a wrapper for Bugsnag's [Java notifier](https://bugsnag.com/docs/notifiers/java).

This wrapper works, but is still experimental and subject to change.

Installation
------------

This library isn't intended for distribution yet.  If you want to give it a try anyway:

* Drop bugsnag.clj in your leiningen project's src/ directory.
* Add the following dependency to your project.clj file: 

```clojure
[com.bugsnag/bugsnag "1.2.1"]
```

Configuration
-------------

Configuration is being read from environment variables by the notifier's code.

```bash
export RELEASE_STAGE=production
export BUGSNAG_ID=your-api-key
```

Usage
-----

```clojure
(ns my-app.core
  (:require [bugsnag]))

; You can send an error at any time using bugsnag/notify.
(bugsnag/notify "Test message")

; The ring middleware automatically reports 4xx-5xx errors as well as any exceptions.
(defn handler [request]
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body "Hello world!"})

(def app (bugsnag/wrap-bugsnag handler))
```

TODO
----

There's a few odd decisions that deserve a quick explanation and deserve attention in the future.

**Configuration**

Configuration, immutability, and a singleton bugsnag instance don't play very well together.  As a result, the configuration is only accepted via the expected environment variables and not via code.  It's a quick and dirty way of ensuring we have the values in before the defs, and should be revised to a permanent solution.

**Conditional bugsnag instantiation**

When you instantiate the bugsnag object, it will begin capturing errors while supressing their output to stderr.  If you are in development mode, this error suppression is undesirable.  To handle this case, we never instantiate bugsnag in development and use no-op stubs instead.

**Middleware Reporting Conditions**

The middleware sends notifications if:

1. An exception is caught
2. An unsuccessful HTTP status code is returned (4xx-5xx)

The second is overzealous and might catch unimportant errors (403s etc), but for our purposes more error data was better.

**Middleware Intercepts Body InputStream**

The middleware steals the request body's inputstream and replaces it with a new one every time it needs to be read.  The whole process is a bit mangled; there might be a cleaner way to give everyone access to this information.

**Missing Bugsnag Methods**

We haven't needed all of the standard bugsnag methods yet, so I haven't bothered implementing them.

