# speclj [![Build Status](https://secure.travis-ci.org/slagyr/speclj.png?branch=master)](http://travis-ci.org/slagyr/speclj)
### (pronounced "speckle" [spek-uhl]) ###
It's a TDD/BDD framework for [Clojure](http://clojure.org/) and [Clojurescript](http://clojurescript.org/), based on [RSpec](http://rspec.info/).

[Installation](#installation) | [Clojure](#clojure) | [ClojureScript](#clojurescript)

# Installation

Latest version: `3.1.0`

## From Scratch

```bash
lein new speclj YOUR_PROJECT_NAME
```
See [@trptcolin's speclj template](https://github.com/trptcolin/speclj-template)

## Using Leiningen (2.0 or later)

Include speclj in your `:dev` profile `:dependencies` and`:plugins`. Then change the `:test-paths` to `"spec"`

```clojure
; - snip
:dependencies [[org.clojure/clojure "1.6.0"]]
:profiles {:dev {:dependencies [[speclj "3.1.0"]]}}
:plugins [[speclj "3.1.0"]]
:test-paths ["spec"]
```

## Manual installation

1. Check out the source code: [https://github.com/slagyr/speclj](https://github.com/slagyr/speclj)
2. Install it:

```bash
$ lein install
```

# Usage

## [API Documentation](http://micahmartin.com/speclj/)

## Usage with Clojure

### File Structure
All your `speclj` code should go into a a directory named `spec` at the root of your project.  Conventionally, the `spec` directory will mirror the `src` directory structure except that all the `spec` files will have the '_spec.clj' postfix.

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
(use 'speclj.core)
(use 'sample.core)
```

#### describe
`describe` is the outer most container for specs.  It takes a `String` name and any number of _spec components_.

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
There are several ways to make assertions.  They are documented on the wiki: [Should Variants](https://github.com/slagyr/speclj/wiki/Should-variants)

### Spec Components
`it` or characteristics are just one of several spec components allowed in a `describe`.  Others like `before`, `with`, `around`, etc are helpful in keeping your specs clean and dry.  Check out the listing on the wiki: [Spec Components](https://github.com/slagyr/speclj/wiki/Spec-components)

## Running Specs

### With Leiningen
Speclj includes a Leiningen task to execute `speclj.main`.

```bash
$ lein spec
```

### Using `lein run`
The command below will run all the specs found in `"spec"` directory.

```bash
$ lein run -m speclj.main
```

### As a Java command
The command below will run all the specs found in `"spec"` directory.

```bash
$ java -cp <...> speclj.main
```

### Autotest
The command below will start a process that will watch the source files and run specs for any updated files.

```bash
$ lein spec -a
```

### Options
There are several options for the runners.  Use the `--help` options to see them all.  Or visit [Command Line Options](https://github.com/slagyr/speclj/wiki/Command-Line-Options).

```bash
$ lein spec --help
```

### `:eval-in`
To make your tests start up slightly faster, you can add `:speclj-eval-in :leiningen` to your project map.


## Usage with ClojureScript

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
:plugins [[lein-cljsbuild "1.0.3"]]
:cljsbuild {:builds        {:dev  {:source-paths ["src/cljs" "spec/cljs"]
                                   :compiler     {:output-to "path/to/compiled.js"}
                                   :notify-command ["phantomjs" "bin/speclj" "path/to/compiled.js"]}
                            :prod {:source-paths  ["src/cljs"]
                                   :compiler      {:output-to "path/to/prod.js"
                                                   :optimizations :simple}}}
            :test-commands {"test" ["phantomjs"  "bin/speclj" "path/to/compiled.js"]}}
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
  speclj.run.standard.armed = true;
  return speclj.run.standard.run_specs(
     cljs.core.keyword("color"), true
  );
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
Your spec files should `:require` the `speclj.core` just like in clojure. Don't forget to pull in the library that you're testing as well (sample.core in this case).  

You'll also need to `:require-macros` the `speclj.core` and `:refer` each speclj test word that you want to use.  In the example below, we are using __describe__, __it__, __should__, __should-not__, and __run-spec__.
If you wanted to use __context__ you would simply add it to the current `:refer` collection.  For a list of speclj test words go to the [speclj documentation](http://speclj.com/docs)

As a final note, your own library must be __aliased__ using `:as`.  This is a current ClojureScript requirement.

```clojure
(:require-macros [speclj.core :refer [describe it should should-not run-specs])
(:require [speclj.core]
          [sample.core :as my-core]))
```

### Running ClojureScript Specs

### With Leiningen
We defer to `cljsbuild` to run our test command.

```bash
$ lein cljsbuild test
```

### Bash
The command below will start a process that will watch the source files and run specs for any updated files.

```bash
$ bin/speclj path/to/compiled.js
```

# Community

* API Documentaiton [http://micahmartin.com/speclj/](http://micahmartin.com/speclj/)
* Source code: [https://github.com/slagyr/speclj](https://github.com/slagyr/speclj)
* Wiki: [https://github.com/slagyr/speclj/wiki](https://github.com/slagyr/speclj/wiki)
* Email List: [http://groups.google.com/group/speclj](http://groups.google.com/group/speclj)

# Contributing
speclj uses [Leiningen](https://github.com/technomancy/leiningen) version 2.0.0 or later.

Clone the master branch, build, and run all the tests:

```bash
$ git clone https://github.com/slagyr/speclj.git
$ cd speclj
$ lein spec
```

To make sure tests pass ClojureScript too, make sure you have [phantomjs](http://phantomjs.org/download.html) installed and then run:

```bash
lein cljsbuild clean
lein cljsbuild once
```

Make patches and submit them along with an issue (see below).

## Issues
Post issues on the speclj github project:

* [https://github.com/slagyr/speclj/issues](https://github.com/slagyr/speclj/issues)

# Compatibility

* Speclj 2.* requires Clojure 1.4.0+
* Clojure 1.3 is not supported by any version of Speclj due to a bug in Clojure 1.3.

# License
Copyright (C) 2010-2014 Micah Martin All Rights Reserved.

Distributed under the The MIT License.
