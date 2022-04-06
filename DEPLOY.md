# Step for deployment

1) Run specs `lein spec`
2) Run cljs specs `lein cljs`
3) Increase version in project.clj
4) Update CHANGES.md with changes in version
5) `lein install`, update some other project to use the new version, try it out (clj and cljs)
6) `git commit` changes
7) `git tag -a X.Y.Z -m ''`
8) `git push origin master --tags`
9) `lein deploy clojars` (need pgp config, micah may need to do this)
10) `bin/doc.sh` - generate and publish new documentation
