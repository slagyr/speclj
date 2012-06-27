# 2.1.3

* Throwables are now caught by vigilant runner when loading files.  Errors used to fail silently.

# 2.1.2

* Errors (as opposed to Exceptions) no longer abort test runs

# 2.1.1

* 2.1.0 is broken for some reason

# 2.1.0

* Vigilant runner will remember failing tests and rerun them until they pass
* leiningen spec task will always run in project root
* tags in the command line work again

# 2.0.1

* before-all's can use values from with-all's

# 2.0.0

* Works with Clojure 1.4.0
* Does NOT work with Clojure 1.3 dues to bug: http://dev.clojure.org/jira/browse/CLJ-876

# 1.5.0

* Improved stacktrace eliding for pagages starting with clojure./java./speclj.
* Multiple reporters supported
* new assertions: should-be-nil, should-not-be-nil (Thanks to [pgr0ss](https://github.com/pgr0ss))
* made reporting functional resuable

# 1.4.0

* added pending macro to mark characteristics as pending
* empty characteristics are now considered pending
* xit is a shortcut to mark a characteristic as pending
* added shortcuts for runners and reporters
* renamed specdoc to documentation
* output improved
* with-all spec component
* tag examples. declared in describe blocks. filtered on run.
* exclude hidden files in runs (https://github.com/slagyr/speclj/pull/4)

# 1.3.0

* using raw Runtime.exec in 'lein spec' command to avoid ant's output mangling and lag.
* should-be-same or some assertion that checks object reference equality
* allow runners to run files, not just directories
* options for (run-specs)
* fix file path displayed with failed spec
* refactored Vigilant running to use fresh library (embedded locally)

# 1.2.0

* around components now execute around befores and afters
* removed AOT compilation of clojure src which caused problems on other versions of clojure
* fixed typo that prevented src from properly reloaded by Vigilant Runner
* spec files are sorted before running
* stack traces are elided and a switch was added to output full stack traces

# 1.1.0

* fixed problem where vigilant runner would crash with :reload or :verbose in ns
* colorize output
* standard runner won't evaluate specs until they're all loaded
* `with` components are only bound within their context
* nested describe/context
* `after`s are invoked even after failures or errors
* should= support for doubles (use delta)
* Lazy seqs print nicely in output ([trptcolin](https://github.com/trptcolin))

# 1.0.3

* First release

