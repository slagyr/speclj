# Step for deployment

1) Run specs `clj -M:test:spec`
2) Run cljs specs `clj -M:test:cljs`
3) Increase version in project.clj
4) Update CHANGES.md with changes in version
5) `clj -T:build install`, update some other project to use the new version, try it out (clj and cljs)
6) `git commit` changes
7) `clj -T:build deploy`
10) `bin/doc.sh` - generate and publish new documentation
