# speclj

## Toolbox

This project uses [toolbox](https://raw.githubusercontent.com/slagyr/toolbox/main/SKILL.md)
to manage agent components. If `.toolbox/` doesn't exist, fetch the toolbox
SKILL.md from the URL above and follow its instructions. Once bootstrapped:

- **Skills:** Load from `.toolbox/skills/{name}/SKILL.md` when their descriptions match the task at hand.
- **Commands:** When the user invokes a command by name (e.g., "/plan"), read and follow `.toolbox/commands/{name}.md`.

### Skills

- [tdd](https://raw.githubusercontent.com/slagyr/agent-lib/main/skills/tdd/SKILL.md)
- [crap4clj](https://raw.githubusercontent.com/unclebob/crap4clj/master/SKILL.md)
- [clj-mutate](https://raw.githubusercontent.com/slagyr/clj-mutate/master/SKILL.md)
- [speclj-structure-check](https://raw.githubusercontent.com/unclebob/speclj-structure-check/master/.claude/skills/speclj-structure-check/SKILL.md)
- [gherclj](https://raw.githubusercontent.com/slagyr/agent-lib/main/skills/gherclj/SKILL.md)

### Commands

- [plan](https://raw.githubusercontent.com/slagyr/agent-lib/main/commands/plan.md)
- [todo](https://raw.githubusercontent.com/slagyr/agent-lib/main/commands/todo.md)
- [work](https://raw.githubusercontent.com/slagyr/agent-lib/main/commands/work.md)
- [plan-with-features](https://raw.githubusercontent.com/slagyr/agent-lib/main/commands/plan-with-features.md)

## Release process

The full release process lives in [DEPLOY.md](DEPLOY.md). Read it before
doing anything release-related — these notes only cover the agent-specific
boundaries.

**You may:**

- Run pre-release checks: `bb spec-all`
- Bump `VERSION`
- Update `CHANGES.md`
- Run the local install smoke-test: `clj -T:build install`
- Stage and offer the "prep for release X.Y.Z" commit
- After the user has deployed, update `README.md`'s version and `:git/sha`
  references and offer the "update README with new tag and sha" commit

**You must NOT run any of these — they push to shared infrastructure:**

- `clj -T:build deploy` — tags, pushes tags to GitHub, and uploads to Clojars
- `bin/doc.sh` — pushes generated docs to the `gh-pages` branch
- `git push` of release tags or release commits

When everything looks good, stop at the "prep for release" commit, summarize
what's ready, and output the exact next command verbatim so the user can
copy-paste and execute it themselves. Do the same for the docs publish step.

If `CLOJARS_USERNAME` / `CLOJARS_PASSWORD` are not set in the environment,
mention it — `clj -T:build deploy` will fail without them.
