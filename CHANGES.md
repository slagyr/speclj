# 1.4.0

* added pending macro to mark characteristics as pending
* empty characteristics are now considered pending
* xit is a shortcut to mark a characteristic as pending
* added shortcuts for runners and reporters
* renamed specdoc to documentation
* output improved
* with-all spec component

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
* Lazy seqs print nicely in output (@trptcolin)

# 1.0.3

* First release

