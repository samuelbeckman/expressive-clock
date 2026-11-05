# Expressive Clock - Development Instructions

## 1. Project Overview

Expressive Clock is a native Android application built with Kotlin and Jetpack Compose.

The project uses:
- Kotlin
- Jetpack Compose
- Material 3 Expressive
- AndroidX
- Preferences DataStore
- Gradle

Preserve the existing application architecture and visual design system unless a task explicitly requires changes.

## 2. Git and Branch Safety

These rules are mandatory for every development task.

Before starting any task:
- Run `git status --short --branch`.
- Identify the current branch.
- Inspect existing staged, unstaged and untracked changes.
- Identify whether requested files overlap with existing user modifications.

Never perform any of the following without explicit user approval:
- Switch branches.
- Create or delete branches.
- Create commits.
- Push to GitHub.
- Merge branches.
- Reset the repository.
- Run `git clean`.
- Stash existing changes.
- Discard or restore existing modifications.
- Revert user changes.

Never merge anything into `main` unless explicitly requested.

Treat all existing uncommitted and untracked changes as user-owned work.

Never overwrite, delete, stage or modify unrelated existing changes.

If the requested task overlaps with existing uncommitted changes:
1. Inspect the relevant diff.
2. Explain the overlap.
3. Ask for approval before proceeding if there is a risk of overwriting existing work.

A new conversation thread does not automatically grant permission to change branches or discard existing work.

## 3. Scope and Implementation Discipline

- Make only the changes necessary to complete the requested task.
- Avoid unnecessary refactoring.
- Do not modify unrelated screens, components or functionality.
- Preserve existing application behaviour unless explicitly asked to change it.
- Preserve existing animations, transitions, interactions and visual styling.
- Follow established Kotlin and Jetpack Compose patterns.
- Reuse existing components and architecture where appropriate.
- Do not introduce unnecessary dependencies.
- Do not change application version names or version codes unless explicitly requested.
- Do not change Gradle, Kotlin, Compose or Android SDK versions unless explicitly requested.

Before making significant architectural changes, explain the proposed approach and obtain approval.

## 4. Application Architecture

Respect the existing architecture.

Important considerations:
- ClockViewModel is the central application ViewModel.
- ClockStore and Preferences DataStore are the persistence source of truth.
- Alarms, timers, stopwatch and world clock functionality rely on persisted state.
- Timer and stopwatch calculations use SystemClock.elapsedRealtime().
- AlarmManager scheduling and cancellation must remain coordinated with persisted alarm state.
- Manifest permissions and component declarations are important to application functionality.
- Widget configuration is stored independently by widget ID.
- Palettes.kt is generated and must not be edited manually. Use the existing generation script when changes are required.

Do not replace established architectural patterns without explicit justification.

## 5. Build and Verification

After completing an implementation task:

1. Run the appropriate Gradle build.
2. For normal development tasks, run:

   `./gradlew assembleDebug`

3. Resolve any build errors caused by your changes.
4. Report whether the build succeeded or failed.
5. Do not claim that functionality was device-tested unless it was actually tested.

Unless the task is explicitly an audit, investigation or release-only task, produce an installable debug APK.

The expected APK location is:

`~/Downloads/ExpressiveClock-debug.apk`

If necessary, copy the generated APK from:

`app/build/outputs/apk/debug/app-debug.apk`

Do not overwrite an existing APK without checking first. If the destination already exists, use a clearly identified alternative filename or request approval.

Request any required permissions for writing outside the repository.

## 6. Final Development Report

At the end of every implementation task, provide a concise report containing:

- Summary of what was implemented.
- Files modified.
- Files created.
- Files deleted, if any.
- Important implementation decisions.
- Build command and result.
- Verification performed.
- Debug APK location, when applicable.
- Final Git branch.
- Final Git working-tree status.

Clearly distinguish between verified results and anything that could not be tested.

## 7. Communication

- Explain significant implementation decisions.
- Ask questions when requirements are ambiguous.
- Flag potential regressions before making risky changes.
- Do not silently expand the scope of a task.
- Prioritise correctness and preservation of existing functionality over unnecessary changes.

## 8. Approval and Autonomy

You may independently inspect files, analyse code, implement clearly requested changes and run appropriate non-destructive builds.

You must obtain explicit approval before destructive Git operations, branch changes, commits, pushes, merges, or changes that could overwrite unrelated user work.

A request to implement a feature does not imply permission to perform unrelated repository maintenance.

---
