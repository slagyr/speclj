# clj-kondo

... is a linting tool tha plays nicely with Speclj.

## Using the `clj-kondo` Command.

When you use the `clj-kondo` commandline utility, be sure to include the speclj dependencies in the command.

For example if you use `deps.edn` with the conventional `test` alias that has all your development dependencies,
you can lint everything with this command:

    clj-kondo --lint "$(clojure -A:test -Spath)"

## Alias command

Perhaps the best way add clj-kondo to your project is using a targeted alias, like so:

``` clojure
{
 :paths   ["src"]
 :deps    {}
 :aliases {
           :test  {:extra-deps  {speclj/speclj {:mvn/version "3.6.0"}}
                   :extra-paths ["spec"]}
           :spec  {:main-opts ["-m" "speclj.main" "-c"]}
           :kondo {:extra-deps {clj-kondo/clj-kondo {:mvn/version "2025.02.20"}
                                speclj/speclj       {:mvn/version "3.6.0"}}
                   :main-opts  ["-m" "clj-kondo.main" "--lint" "src:spec"]}
           }
 }
```

And run it with the command:

    clj -M:kondo

## Supporting Speclj Macros

There are a few macros in Speclj that need to be described to Kondo.  
Simply add the following to `<project dir>/.clj-kondo/config.edn`.

``` clojure 
{:lint-as {speclj.core/around clojure.core/fn
           speclj.core/around-all clojure.core/fn
           speclj.core/with clojure.core/def
           speclj.core/with! clojure.core/def
           speclj.core/with-all clojure.core/def
           speclj.core/with-all! clojure.core/def}}
```