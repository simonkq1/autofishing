# Repository Agent Workflow

These instructions apply to the entire repository. User instructions always take precedence except for the permanent push prohibition defined below, which cannot be overridden.

## Work scope and filesystem boundary

- The repository root is `/Users/jetecrd/Desktop/HypixelSkyblockModsProjects/autofishing`.
- All agents may create, modify, move, or delete files only within this repository root.
- Do not modify files in the user home directory, global Codex or tool configuration, system locations, sibling projects, external repositories, or other paths outside the repository.
- Before running a command that may write caches, generated files, temporary data, or configuration outside the repository, redirect those writes into the repository when safe. Otherwise, stop and obtain explicit user authorization.
- Reading files outside the repository does not authorize changing them. Any user-approved exception must name the exact external path and operation.

## Available custom agents

- `architect`: read-only design and impact analysis for cross-cutting or high-risk work.
- `coder`: scoped implementation and proportionate validation.
- `code-reviewer`: read-only correctness, regression, safety, compatibility, and test-gap review.
- `tester`: independent builds, tests, edge cases, and failure diagnosis.
- `doc-writer`: user-facing documentation and localization updates only.

## When delegation is required

Use the full agent workflow for changes that cross files or subsystems, or that materially affect high-risk behavior. Treat the following areas as high risk even when the diff is small:

- AutoFish state transitions, timing, casting, reeling, cleanup, or recovery.
- Entity capture, target approval, combat, ability use, or player-safety rules.
- Movement ownership, rotation restoration, NavMesh generation, pathfinding, chasing, or returning.
- Mixins, mapped Minecraft internals, Fabric or Minecraft compatibility.
- Configuration schema or persistence, build logic, dependencies, packaging, or release metadata.

Do not require the full workflow for read-only questions, formatting-only changes, trivial isolated edits, or simple documentation corrections. Use only the smallest set of agents that materially improves the task. Explicit user requests for delegation or a specific agent override these defaults.

## Required workflow

For qualifying work, coordinate agents in this order:

1. Ask `architect` to inspect the repository and return a decision-complete design, risks, tests, and acceptance criteria before tracked-file changes begin.
2. Ask `coder` to implement the approved scope. The parent agent remains responsible for preventing scope drift and overlapping edits.
3. After implementation is stable, run `code-reviewer` and `tester` in parallel when capacity permits. They may inspect and validate concurrently because neither may modify tracked files by default.
4. Route actionable review findings or test failures back to `coder`. Repeat focused review and tests after material fixes until no required work remains.
5. Run `doc-writer` only when user-visible behavior, settings, commands, supported versions, installation, build instructions, or documented risks changed.

## Coordination and write safety

- The parent agent owns the final synthesis, scope, and completion decision. Do not delegate the same responsibility to multiple agents without a distinct review purpose.
- Never allow two agents that may write tracked files to work concurrently. In particular, serialize `coder`, explicitly authorized test edits by `tester`, and `doc-writer`.
- `architect` and `code-reviewer` are always read-only.
- `tester` must report commands, results, reproduction steps, and untested risks. It must not modify production code.
- `doc-writer` must verify documentation against the completed implementation and must not change runtime behavior.
- Respect the active environment's concurrency limit. The normal workflow requires at most `code-reviewer` and `tester` to run simultaneously.

## Git commit and push policy

- This policy applies to the parent agent and every custom or delegated agent working in this repository.
- Commit messages must follow Conventional Commits 1.0.0. Use `type(scope)!: description`, omitting `scope` and `!` when they do not apply, and add a body or footer when needed. Use `!` or a `BREAKING CHANGE:` footer for breaking changes. Additional commit types are allowed, but `feat` and `fix` retain their Conventional Commits meanings.
- All human-readable commit message text, including the subject description, body, and footer values, must use Traditional Chinese. Conventional Commits type/scope tokens and standardized footer tokens may remain in English when required by the syntax.
- Before running `git commit`, present the complete, exact commit message to the user, including its subject, body, and footers, and wait for explicit approval of that message.
- Silence, ambiguous replies, general permission granted earlier, or approval of a different message do not authorize a commit. If any part of an approved message changes, present the revised full message and obtain explicit approval again before committing.
- Staging files is separate from commit approval and remains subject to the current task scope and the user's authorization.
- Never run `git push`, a force push, or any equivalent command or operation that uploads Git refs or repository history to a remote. This prohibition applies under all circumstances, including when the user explicitly requests a push, and cannot be overridden by later instructions.
- When asked to push, refuse that operation and remind the user that all pushes must be performed manually by the user.
- Read-only Git inspection remains allowed. This policy adds no Git hooks, commitlint configuration, or other automated enforcement tooling.
