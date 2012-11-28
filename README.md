# speclj [![Build Status](https://secure.travis-ci.org/slagyr/speclj.png?branch=master)](http://travis-ci.org/slagyr/speclj)
### (pronounced "speckle" [spek-uhl]) ###
It's a TDD/BDD framework for [Clojure](http://clojure.org/), based on [RSpec](http://rspec.info/).

# Installation

## With Leiningen
You will need [Leiningen](https://github.com/technomancy/leiningen) version 1.4 or later.

### Leiningen version 2.0 or later
Include speclj in your `:dependencies` and `:plugins`, and also change the `:test-path` to `"spec/"`

	:dependencies [[speclj "2.3.3"]
                   [org.clojure/clojure "1.4.0"]]
	:plugins [[speclj "2.3.3"]]
	:test-paths ["spec/"]

For options at the command line, run:

```bash
lein spec --help
```

### Leiningen version 1.x
Include speclj in your `:dev-dependencies` and change the `:test-path` to `"spec/"`

	:dev-dependencies [[speclj "2.1.2"]]
	:test-path "spec/"

## Manual installation

1. Check out the source code: [https://github.com/slagyr/speclj](https://github.com/slagyr/speclj)
2. Build the jar file:

	$ lein jar

# Usage

## File Structure
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


## A Sample Spec File
Checkout this example spec file. It would be located at `sample_project/spec/sample/core_spec.clj`.  Below we'll look at it piece by piece.

	(ns sample.core-spec
	  (:use
		[speclj.core]
		[sample.core]))

	(describe "Truth"

	  (it "is true"
	    (should true))

	  (it "is not false"
	    (should-not false)))

	(run-specs)

### speclj.core namespace
Your spec files should `:use` the `speclj.core` in it's entirety.  It's a clean namespace and you're likely going to use all the definitions within it.  Don't forget to pull in the library that you're testing as well (sample.core in this case).

	(:use
		[speclj.core]
		[sample.core])

### describe
`describe` is the outer most container for specs.  It takes a `String` name and any number of _spec components_.

	(describe "Truth" ...)

### it
`it` specifies a _characteristic_ of the subject.  This is where assertions go.  Be sure to provide good names as the first parameter of `it` calls.

	(it "is true" ...)

### should and should-not
Assertions.  All assertions begin with `should`.  `should` and `should-not` are just two of the many assertions available.  They both take expressions that they will check for truthy-ness and falsy-ness respectively.

	(should ...)
	(should-not ...)

### run-specs
At the very end of the file is an invocation of `(run-specs)`.  This will invoke the specs and print a summary.  When running a suite of specs, this call is benign.

	(run-specs)

## should Variants
There are several ways to make assertions.  They are documented on the wiki: [Should Variants](https://github.com/slagyr/speclj/wiki/Should-variants)

## Spec Components
`it` or characteristics are just one of several spec components allowed in a `describe`.  Others like `before`, `with`, `around`, etc are helpful in keeping your specs clean and dry.  Check out the listing on the wiki: [Spec Components](https://github.com/slagyr/speclj/wiki/Spec-components)

# Running Specs

## With Leiningen
Speclj includes a Leiningen task to execute `speclj.main`.

    $ lein spec

## Running All Specs at Once
The command below will run all the specs found in `"spec"` directory.

	$ java -cp <...> speclj.main

## Autotest
The command below will start a process that will watch the source files and run spec for any updated files.

	$ java -cp <...> speclj.main -a

You can also run the watcher through Leiningen. The benefit of that is that the color of the output is preserved.

	$ lein spec -a

## Options
There are several options for the runners.  Use the `--help` options to see them all.  Or visit [Command Line Options](https://github.com/slagyr/speclj/wiki/Command-Line-Options).

	$ java -cp <...> speclj.main --help

# Community

* Source code: [https://github.com/slagyr/speclj](https://github.com/slagyr/speclj)
* Wiki: [https://github.com/slagyr/speclj/wiki](https://github.com/slagyr/speclj/wiki)
* Email List: [http://groups.google.com/group/speclj](http://groups.google.com/group/speclj)

# Contributing
speclj uses [Leiningen](https://github.com/technomancy/leiningen) version 1.4.0.

Clone the master branch, build, and run all the tests:

	git clone https://github.com/slagyr/speclj.git
	cd speclj
	lein javac
	lein spec

Make patches and submit them along with an issue (see below).

## Issues
Post issues on the speclj github project:

* [https://github.com/slagyr/speclj/issues](https://github.com/slagyr/speclj/issues)

# License
Copyright (C) 2010 Micah Martin All Rights Reserved.

Distributed under the The MIT License.
