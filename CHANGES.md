# 3.7.0
* Displays the total number of assertions made in the test results

# 3.6.0
* Replaces `SpecFailure` and `SpecPending` classes with `ex-info`
* Replaces `mmargs` library with a Clojure implementation
* Adds support for Clojure CLR
* Upgrades dependencies
* Removes clojure/java.data dependency

# 3.5.0

* migrates from deprecated make-fresh library to clojure.tools.namespace
* fixes bug where changes to src files in a directory that includes a space char does not register updates to the vigilant runner

# 3.4.8

* can now be built and executed under `:advanced` ClojureScript optimizations
  * exports functions needed by JavaScript
  * refactors functions that would otherwise fail with `:advanced` optimizations
  * extends `run-specs` to accepts string keys in addition to keyword keys
  * exports `speclj.run.standard.arm()` function to JavaScript (synonym for `speclj.run.standard.armed = true`)
  * exports `speclj.run.standard.disarm()` function to JavaScript (synonym for `speclj.run.standard.armed = false`)
  * `Runner` can now `get-descriptions`
  * `Runner` can now `filter-descriptions` based on a hash-map of string/boolean namespace pairs
    * `{"speclj.core" true "speclj.reporting" false"}`

# 3.4.7

* fixes bug where `should-throw` would expand string predicates improperly under cljs whitespace optimizations.
* fixes `should<`, `should<=`, `should>`, `should>=`, and other macros to only evaluate forms once.
* upgrades to clojure 1.11.3
* upgrades to clojurescript 1.11.132

# 3.4.6

* adds cloverage support

# 3.4.5

* 3.4.4 was a failed deploy... no compiled java code.

# 3.4.4

* fixes bug where `it` blocks were not executing within the namespace where they were declared.

# 3.4.3

* redefs-around

# 3.4.2

* should-have-count
* should-not-have-count
* stub/clear!

# 3.4.1

* fix Leiningen plugin

# 3.4.0

* focus - run only the specs that have focus and ignore all others
  * focus-it
  * focus-context
  * focus-describe
* should>
* should>=
* should<
* should<=
* should-start-with
* should-end-with
* should-not-start-with
* should-not-end-with
* upgrade to clojure 1.11.1
* upgrade to clojurescript 1.11.4
* clojurescript specs run on headless chrome

# 3.3.2

* upgrade to clojure 1.8.0
* upgrade clojurescript to 1.8.40
* .cljc support by @eyelidlessness https://github.com/slagyr/speclj/pull/150
* better platform support by @eyelidlessness https://github.com/slagyr/speclj/pull/152

# 3.3.1

* fixes reader conditionals in require form 

# 3.3.0

* Uses Clojure 1.7 (RC2) with reader conditionals
* upgrade to clojurescript 0.0.3308
* dev - no more cljx or cljsbuild
* Can ommit pending specs from output by @arlandism - https://github.com/slagyr/speclj/pull/125
* should-throw allow arbitrary exception validation by @tjarvstrand - https://github.com/slagyr/speclj/pull/132
* merge pull request by @ryankinderman - https://github.com/slagyr/speclj/pull/129 

# 3.2.0

* cljs specs catch Strings thrown as exception - https://github.com/slagyr/speclj/issues/122
* fixes (run-spec) error in cljs - https://github.com/slagyr/speclj/issues/89
* upgrade dependencies: clojurescript 0.0-3030, cljsbuild 1.0.5, cljx 0.6.0
* Add `around-all` @ryankinderman https://github.com/slagyr/speclj/pull/86
* Allow `--speclj` as alternative help option @arlandism https://github.com/slagyr/speclj/pull/103
* Allow `lein spec --help` in lein 2.4+
* Support alternate test-paths (besides `spec`) more fully. https://github.com/slagyr/speclj/issues/101
* Avoid using `load-file` for versioning. https://github.com/slagyr/speclj/issues/87
* Avoid the dreaded `ClassNotFoundException: speclj.components` in the REPL. https://github.com/slagyr/speclj/issues/79
* Make stubs work in multithreaded or lazy situations @finn1317 https://github.com/slagyr/speclj/pull/114

# 3.1.0

* upgrade to clojure 1.6
* upgrade clojurescript to 0.0-2234 and cljsbuild to 1.0.3
* should-throw matches regex. @TakaGoto https://github.com/slagyr/speclj/pull/95
* `lein spec` no longer hangs when exiting if agents were used. @sdegutis https://github.com/slagyr/speclj/issues/94
* Fix for inproper exit-code on spec failures. @CraZySacX https://github.com/slagyr/speclj/pull/90
* Rerun all tests by pressing <Enter> in vigilant runner. @kevinbuch https://github.com/slagyr/speclj/issues/41
* remove cljs? debug message. @kevinbuch https://github.com/slagyr/speclj/pull/84

# 3.0.2

* Fixes 'ClassNotFoundException speclj.run.standard' when running Vigilant.

# 3.0.1

* Fixes exception throwing so that correct line numbers are displayed
* CLJS upgrade to 2173
* Resolves CLJS warning: "WARNING: Use of undeclared Var" when `with` used

# 3.0.0

* Merges CLJ and CLJS into one jar using CLJX (specljs is no more)
* `lein spec` tasks defaults to `:eval-in :subprocess` rather than `:eval-in leiningen`

# 2.9.1

* adds `:*` and fn matchers to stub invokation checking

# 2.9.0

* vigilant runner prints load error stacktraces
* Adds stubs

# 2.8.1

* Faster lein task. @mylesmegyesi https://github.com/slagyr/speclj/pull/67
* clojure-test reporter. @alpian
* can use reporter objects as well as names. @alpian https://github.com/slagyr/speclj/pull/69
* reverts back to speclj.SpecFailure for errors.
* lein spec task uses :test-paths project setting to specify spec location

# 2.8.0

* Removes check for java.lang.Object when installing SpecCompoenent, because it blocks multimethods
* Only call exit on fail. @glenjamin https://github.com/slagyr/speclj/pull/47
* Diff for maps. @edtsech https://github.com/slagyr/speclj/pull/53
* Fix for localized specs. @nilnor
* -no-color switch. @dudymas https://github.com/slagyr/speclj/pull/64
* Remove (format) to work in recent cljs. @philipsdoctor https://github.com/slagyr/speclj/pull/66
* CLJS upgrade to 0.0-1978

# 2.7.5

* should-contain and should-not-contain gracefully handle nil containers

# 2.7.4

* ???

# 2.7.3

* Adds speclj.core/run-spec as speclj.run.standard/run-spec (clj only) for backward compatibility
* Adds should-be, such that (should-be empty? [1 2 3]) offers a useful report
* Adds should-not-be, such that (should-not-be empty? []) offers a useful report
* Simplifies failure report syntax

# 2.7.2

* Specljs no longer adds keys to js/Object
* Speclj doesn't leave orphaned sub process when the main process crashes @trptcolin
* Speclj flushes output @trptcolin
* speclj.core requires used name spaced for convenience.

# 2.7.1

* Fixes problem with vigilant runner

# 2.7.0

* Refactored to accomodate multiple platform (cljs)
* BREAKING-CHANGE: (run-specs) moved from speclj.core to speclj.run.standard
* Specljs (speclj on ClojureScript)

# 2 6.1

* Java src is compiles with 1.5 target instead of 1.7 (which was used in 2.6.0)

# 2.6.0

* Uses Clojure 1.5.1
* Added with! and with-all!.  Courtesy of @spadin.
* Compile errors are caught and reported. Courtesy of @glenjamin.  https://github.com/slagyr/speclj/pull/39
* should-contain and should== converted to pure macro so that line numbers are accurate.  https://github.com/slagyr/speclj/issues/42
* failures result from java.lang.AssertionError to be compatible with 3rd party assertion libraries. https://github.com/slagyr/speclj/issues/43
* auto runner should no longer barf on compile errors. https://github.com/slagyr/speclj/issues/33
* removed 'lein new speclj' task.  Instead use 'lein new speclj <name>'. https://github.com/slagyr/speclj/pull/23

# 2.5.0

* should== for loose equality and collection containment equality
* should-not== opposite of should==

# 2.4.0

* should-contain works with regular expressions, maps, and sequences
* should-not-contain, opposite of should-contain
* Adds leinigen task help/doc string.

# 2.3.2

* Vars (helpers fns and such) can be delcared inside contexts.  Good suggestion by @mylesmegyesi.

# 2.3.1

* Standard runner clears descriptions after each run.

# 2.3.0

* Includes lein template by @bcmcgavin to create new speclj projects. https://github.com/slagyr/speclj/pull/23
* Updates 'lein spec' task by @mylesmegyesi to be compatible with lein2 deprecations. https://github.com/slagyr/speclj/pull/24
* Running specs in the REPL doesnt accumulate results any longer.

# 2.2.0

* Support for Leiningen 2

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
