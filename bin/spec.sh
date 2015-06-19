#! /bin/sh

# Sometimes an installed version of speclj will conflict with local runs.
# java.lang.IllegalArgumentException: No implementation of method: :report-description of protocol: #'speclj.reporting/Reporter found for class: speclj.report.progress.ProgressReporter
# Try this command when that happens.

java -cp `lein classpath` speclj.main spec/clj target/spec/clj $1 $2 $3 $4 $5