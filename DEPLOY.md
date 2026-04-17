# Release process

The release artifacts live in three places:

- The git tag (created by `clj -T:build deploy`)
- Clojars (pushed by `clj -T:build deploy`, requires `CLOJARS_USERNAME` and
  `CLOJARS_PASSWORD` env vars)
- The codox docs on the `gh-pages` branch (pushed by `bin/doc.sh`)

The `resources/speclj/VERSION` file is the canonical version source. It ships
inside the jar and is read both by `dev/build.clj` (for the jar version and
the git tag) and at runtime by `speclj.cli/get-version` (for `--help` and
`--version`).

ClojureCLR is intentionally **not** part of the release checks: cljr is hard
to set up on dev machines and rarely used by clients.

## Pre-release

1. Run the full spec suite on every supported runtime:

   ```sh
   bb spec-all
   ```

   This runs the suite under babashka, Clojure JVM, and ClojureScript and
   aggregates failures. Do not proceed if any runtime fails.

2. Bump `resources/speclj/VERSION` to the new release number.

3. Update `CHANGES.md` with the user-visible changes for this version.

4. Sanity-check the build locally:

   ```sh
   clj -T:build install
   ```

   This builds the jar and installs it into your local `~/.m2`. In a separate
   scratch project, depend on the freshly installed version and run a few
   specs (both clj and cljs) to confirm nothing is broken.

5. Commit the prep changes:

   ```sh
   git commit -m "prep for release X.Y.Z"
   ```

## Deploy

6. Push the release to Clojars and tag it in git:

   ```sh
   clj -T:build deploy
   ```

   This task:
   - Aborts if the working tree is dirty.
   - Creates the git tag `X.Y.Z` from `resources/speclj/VERSION` and pushes it.
   - Builds the jar and pushes to Clojars (requires `CLOJARS_USERNAME` and
     `CLOJARS_PASSWORD` env vars).

## Post-deploy README update

The README has several version references and one git/sha reference that need
to point at the just-released tag. The git/sha can only be known *after* the
prep commit exists, which is why this is a separate commit.

7. Update the version and sha references in `README.md`:

   - All `speclj` version strings (Leiningen, `:mvn/version` in deps.edn,
     `bb.edn`, etc.).
   - The `:git/tag` and `:git/sha` of the deps.edn git/tag example. Use the
     sha of the "prep for release" commit.

8. Commit and push:

   ```sh
   git commit -m "update README with new tag and sha"
   git push
   ```

## Publish docs

9. Regenerate and publish the codox docs:

   ```sh
   bin/doc.sh
   ```

   **Setup assumption:** `doc/` is a long-lived `git worktree` checked out to
   the `gh-pages` branch. If you have not set it up yet:

   ```sh
   git worktree add doc gh-pages
   ```
