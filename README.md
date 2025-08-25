# speclj 
### (pronounced "speckle" [spek-uhl]) ###
It's a TDD/BDD framework for [Clojure](http://clojure.org/) and [Clojurescript](http://clojurescript.org/), based on [RSpec](http://rspec.info/).

[![Speclj Build](https://github.com/slagyr/speclj/actions/workflows/test.yml/badge.svg)](https://github.com/slagyr/speclj/actions/workflows/test.yml)

[Installation](#installation) | [Clojure](#clojure) | [ClojureScript](#clojurescript) | [Clojure CLR](#clojure-clr) | [Babashka](#babashka)

# Installation
[![Clojars Project](https://img.shields.io/clojars/v/speclj.svg)](https://clojars.org/speclj)

NOTE: Speclj 3.3+ requires Clojure 1.7+.

## From Scratch

```bash
lein new speclj YOUR_PROJECT_NAME
```
[@trptcolin's speclj template](https://github.com/trptcolin/speclj-template) will generate all the files you need.

Or, if you're using ClojureScript:

```bash
lein new specljs YOUR_PROJECT_NAME
```
[@ecmendenhall's specljs template](https://github.com/ecmendenhall/specljs-template) will save you lots of time by getting you started with a running Clojure & ClojureScript setup.

## Using Leiningen (2.0 or later)

Include speclj in your `:dev` profile `:dependencies` and`:plugins`. Then change the `:test-paths` to `"spec"`

```clojure
; - snip
:dependencies [[org.clojure/clojure "1.12.0"]]
:profiles     {:dev {:dependencies [[speclj "3.11.0"]]}}
:plugins      [[speclj "3.11.0"]]
:test-paths   ["spec"]
```

## Manual installation

1. Check out the source code: [https://github.com/slagyr/speclj](https://github.com/slagyr/speclj)
2. Install it:

```bash
$ clj -T:build install
```

# Usage

### [API Documentation](http://micahmartin.com/speclj/)

Start with the `speclj.core` namespace.  That is Speclj's API and it's very unlikely you'll need anything else.

## Clojure

### File Structure
All your `speclj` code should go into a directory named `spec` at the root of your project.  Conventionally, the `spec` directory will mirror the `src` directory structure except that all the `spec` files will have the '_spec.clj' postfix.

	| sample_project
	|-- project.clj
	|-- src
	    |-- sample
	        |-- core.clj
	        | (All your other source code)
	|-- spec
	    |-- sample
	        |-- core_spec.clj
	       	| (All your other test code)


### A Sample Spec File
Checkout this example spec file. It would be located at `sample_project/spec/sample/core_spec.clj`.  Below we'll look at it piece by piece.

```clojure
(ns sample.core-spec
  (:require [speclj.core :refer :all]
            [sample.core :refer :all]))

(describe "Truth"

  (it "is true"
    (should true))

  (it "is not false"
    (should-not false)))

(run-specs)
```

#### speclj.core namespace
Your spec files should `:require` the `speclj.core` in it's entirety.  It's a clean namespace and you're likely going to use all the definitions within it.  Don't forget to pull in the library that you're testing as well (sample.core in this case).

```clojure
(require '[speclj.core :refer :all])
(require '[sample.core :refer :all])
```

#### describe
`describe` is the outermost container for specs.  It takes a `String` name and any number of _spec components_.

```clojure
(describe "Truth" ...)
```

#### it
`it` specifies a _characteristic_ of the subject.  This is where assertions go.  Be sure to provide good names as the first parameter of `it` calls.

```clojure
(it "is true" ...)
```

#### should and should-not
Assertions.  All assertions begin with `should`.  `should` and `should-not` are just two of the many assertions available.  They both take expressions that they will check for truthy-ness and falsy-ness respectively.

```clojure
(should ...)
(should-not ...)
```

#### run-specs
At the very end of the file is an invocation of `(run-specs)`.  This will invoke the specs and print a summary.  When running a suite of specs, this call is benign.

```clojure
(run-specs)
```

### should Variants (Assertions)
There are many ways to make assertions.  Check out the [API Documentation](http://micahmartin.com/speclj/speclj.core.html).  Take note of everything that starts with `should`.

### Spec Components
`it` or characteristics are just one of several spec components allowed in a `describe`.  Others like `before`, `with`, `around`, etc are helpful in keeping your specs clean and dry.  The same [API Documentation](http://micahmartin.com/speclj/speclj.core.html) lists the spec component (everything that doesn't start with `should`).

## Running Specs

### With `deps.edn`
Add a `spec` alias to your `deps.edn`.

```clojure
{
 :aliases {:spec {:main-opts   ["-m" "speclj.main" "-c"]
                  :extra-deps  {speclj/speclj {:mvn/version "3.11.0"}}
                  :extra-paths ["spec"]}}
 }
```

Run specs.
```bash
clj -M:spec     # printing dots
clj -M:spec -a  # auto running with doc output
clj -M:spec <OPTIONS>
```

### With Leiningen
Speclj includes a Leiningen task.

```bash
$ lein spec <OPTIONS>
```

### Using `lein run`
Speclj also includes a Clojure main namespace:

```bash
$ lein run -m speclj.main <OPTIONS>
```

### As a Java command
And sometimes it's just easier to run a Java command, like from an IDE.

```bash
$ java -cp <...> speclj.main <OPTIONS>
$ java -cp `lein classpath` speclj.main
```

### Autotest
The `-a` options invokes the "vigilant" auto-runner.  This command will run all your specs, and then wait.  When you save any test(ed) code, it will run all the affected specs, and wait again.  It's HIGHLY recommended.

```bash
$ lein spec -a
```

### Options
There are several options for the runners.  Use the `--help` options to see them all.

```bash
$ lein spec --help
```
### `:eval-in`
When using `lein spec` you can get a little faster startup by adding `:speclj-eval-in :leiningen` to your project map.  It will prevent Leiningen from spinning up another Java process and instead run the specs in Leiningen's process.  Use at your own risk.

## ClojureScript

### File Structure
All your `speclj` code should go into a a directory named `spec` at the root of your project.  Conventionally, the `spec` directory will mirror the `src` directory structure except that all the `spec` files will have the '_spec.cljs' postfix.

	| sample_project
	|-- project.clj
	|-- bin
	    |-- speclj.js
	|-- src
	    |-- cljs
	    	|-- sample
	        	|-- core.cljs
	        	| (All your other source code)
	|-- spec
	    |-- cljs
	    	|-- sample
	        	|-- core_spec.cljs
	       		| (All your other test code)

### 1. Configure Your project.clj File

[`lein-cljsbuild`](https://github.com/emezeske/lein-cljsbuild) is a Leiningen plugin that'll get you up and running with ClojureScript.  You'll need to add a `:cljsbuild` configuration map to your `project.clj`.

```clojure
:plugins   [[lein-cljsbuild "1.0.3"]]
:cljsbuild {:builds        {:dev  {:source-paths   ["src/cljs" "spec/cljs"]
                                   :compiler       {:output-to "path/to/compiled.js"}
                                   :notify-command ["phantomjs" "bin/speclj" "path/to/compiled.js"]}
                            :prod {:source-paths  ["src/cljs"]
                                   :compiler      {:output-to     "path/to/prod.js"
                                                   :optimizations :simple}}}
            :test-commands {"test" ["phantomjs" "bin/speclj" "path/to/compiled.js"]}}
```
Speclj works by operating on your compiled ClojureScript.  The `:notify-command` will execute the `bin/speclj` command after your cljs is compiled.  The `bin/speclj` command will use speclj to evaluate your compiled ClojureScript.


### 2. Create test runner executable

Create a file named `speclj` in your `bin` directory and copy the code below:

```JavaScript
#! /usr/bin/env phantomjs

var fs = require("fs");
var p = require('webpage').create();
var sys = require('system');

p.onConsoleMessage = function (x) {
  fs.write("/dev/stdout", x, "w");
};

p.injectJs(phantom.args[0]);

var result = p.evaluate(function () {
  speclj.run.standard.arm();
  return speclj.run.standard.run_specs("color", true);
});

phantom.exit(result);
```


### A Sample Spec File
Checkout this example spec file. It would be located at `sample_project/spec/cljs/sample/core_spec.cljs`.  Below we'll look at it piece by piece.

```clojure
(ns sample.core-spec
  (:require-macros [speclj.core :refer [describe it should should-not run-specs]])
  (:require [speclj.core]
            [sample.core :as my-core]))

(describe "Truth"

  (it "is true"
    (should true))

  (it "is not false"
    (should-not false)))

(run-specs)
```

### speclj.core namespace

You'll need to `:require-macros` the `speclj.core` namespace and `:refer` each speclj test word that you want to use.  In the example below, we are using __describe__, __it__, __should__, __should-not__, and __run-spec__.  Yes, this is unfortunate, but unavoidable.  If you wanted to use __context__ you would simply add it to the current `:refer` collection.  For a list of speclj test words go to the [API Documentation](http://micahmartin.com/speclj/speclj.core.html)

Your spec files must `:require` the `speclj.core` too, even though we don't alias it or refer anything. Don't forget this! It loads all the needed speclj namespaces. Also pull in the library that you're testing (sample.core in this case).

As a final note, when requiring your tested namespaces (sample.core in this case), you'll probabaly want to __alias__ it using `:as`.

```clojure
(:require-macros [speclj.core :refer [describe it should should-not run-specs])
(:require [speclj.core]
          [sample.core :as my-core]))
```

### Running ClojureScript Specs

### With Leiningen

```bash
$ lein cljs
```

### Bash
The command below will start a process that will watch the source files and run specs for any updated files.

```bash
$ bin/speclj path/to/compiled.js
```

## Clojure CLR

Install [Clojure CLR](https://github.com/clojure/clojure-clr) and [Clojure CLR CLI](https://github.com/clojure/clr.core.cli).

Add a `spec` alias to your `deps-clr.edn`.

```clojure
{
 :aliases {:spec {:main-opts   ["-m" "speclj.main" "-c"]
                  :extra-deps  {io.github.slagyr/speclj {:git/tag "3.11.0" :git/sha "18b30a0"}}
                  :extra-paths ["spec"]}}
 }
```

Run specs.
```bash
cljr -M:spec     # printing dots
cljr -M:spec -a  # auto running with doc output
cljr -M:spec <OPTIONS>
```

## Babashka

Install [Babashka](https://github.com/babashka/babashka).

Add a `spec` task to your `bb.edn`.

```clojure
{:paths ["src/bb" "spec/bb"]
 :tasks {spec {:extra-deps {speclj/speclj {:mvn/version "3.11.0"}}
               :requires   ([speclj.main :as main])
               :task       (apply main/-main "-c" "spec/bb" *command-line-args*)}}
}
```

Run specs.
```bash
bb spec      # printing dots
bb spec -a   # auto running with doc output
bb spec <OPTIONS>
```

# Code Coverage

Speclj integrated with [Cloverage](https://github.com/cloverage/cloverage) for all your code coverage needs.  Make sure
speclj 3.4.6 or above is included in the classpath.

Here's an example alias for your `deps.edn`.

```clojure
{:aliases {:cov {:main-opts  ["-m" "speclj.cloverage" "--" "-p" "src" "-s" "spec"]
                 :extra-deps {cloverage/cloverage {:mvn/version "1.2.4"}
                              speclj/speclj       {:mvn/version "3.11.0"}}}}}
```

To pass arguments to speclj, include them as you would with `speclj.main` before the double-hyphen `"--"`:

```clojure
{:aliases {:cov {:main-opts ["-m" "speclj.cloverage" "-c" "-t" "~slow" "--" "-p" "src" "-p" "spec"]}}}
```

# Community

* API Documentaiton [http://micahmartin.com/speclj/](http://micahmartin.com/speclj/)
* Source code: [https://github.com/slagyr/speclj](https://github.com/slagyr/speclj)
* Wiki: [https://github.com/slagyr/speclj/wiki](https://github.com/slagyr/speclj/wiki)
* Email List: [http://groups.google.com/group/speclj](http://groups.google.com/group/speclj)

# Contributing
Clone the master branch, build, and run all the tests:

```bash
$ git clone https://github.com/slagyr/speclj.git
$ cd speclj
$ clj -T:build javac
$ clj -M:test:spec
```

To make sure tests pass ClojureScript too, make sure you have npm:

```bash
npm install
clj -T:build clean
clj -M:test:cljs 
```

To include in a local project

```bash
clj -T:build clean
clj -T:build javac
clj -T:build jar
```

In deps.edn

```clojure
{speclj/speclj {:local/root "/path/to/speclj/target/speclj-3.4.6.jar"}}
```

Make patches and submit them along with an issue (see below).

## Issues
Post issues on the speclj github project:

* [https://github.com/slagyr/speclj/issues](https://github.com/slagyr/speclj/issues)

## Compatibility

* Speclj 2.* requires Clojure 1.4.0+
* Clojure 1.3 is not supported by any version of Speclj due to a bug in Clojure 1.3.

# License
Copyright (C) 2010-2023 Micah Martin All Rights Reserved.

Distributed under the The MIT License.
