# speclj #
### (pronounced "speckle" [spek-uhl]) ###
It's a TDD/BDD framework for [Clojure](http://clojure.org/).  Based quite loyally on [RSpec](http://rspec.info/).

# Installation

TODO

# Usage

## Speclj 101
Checkout this example.  Below we'll look at it piece by piece.

	(ns basics-spec
	  (:use [speclj.core]))

	(describe "Truth"

	  (it "is true"
	    (should true))

	  (it "is not false"
	    (should-not false)))

	(conclude-single-file-run)

### speclj.core namespace
Your spec files should `:use` the `speclj.core` in it's entirety.  It's a clean namespace and you're likely going to use all the definitions within it.

	(:use [speclj.core])

### describe
`describe` is the outer most container for specs.  It takes a `String` name and any number of _spec components_.

	(describe "Truth" ...)

### it
`it` specifies a _characteristic_ of the subject.  This is where assertions go.  Be sure to provide good names as the first parameter of `it` calls.

	(it "is true" ...)
	
### should and should-not
Assertions.  All assertions begin with `should`.  `should` and `should-not` are just two of the many matchers available.  They both take expressions that they will check for truthy-ness and falsy-ness respectively.

	(should ...)
	(should-not ...)
	
### What's that thing at the end?
At the very end of the file is an invocation of `(conclude-single-file-run)`.  This is a benign call that will execute all the specs if the file is evaluated in isolation.  It provides a convenience that editors and IDEs don't provide yet for speclj. 
	
	(conclude-single-file-run)

# Contributing
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
