---
name: openspec-apply-change
description: Implement and test patches from an OpenSpec change. Use when the user wants to start implementing patches, continue implementation, or work through the iterative patch-test cycle.
license: MIT
compatibility: Requires openspec CLI.
metadata:
  author: openspec
  version: "1.0"
  generatedBy: "1.2.0"
---

Implement patches from an OpenSpec change with iterative testing after each patch.

**Input**: Optionally specify a change name. If omitted, check if it can be inferred from conversation context. If vague or ambiguous you MUST prompt for available changes.

**Steps**

1. **Select the change**

   If a name is provided, use it. Otherwise:
   - Infer from conversation context if the user mentioned a change
   - Auto-select if only one active change exists
   - If ambiguous, run `openspec list --json` to get available changes and use the **AskUserQuestion tool** to let the user select

   Always announce: "Using change: <name>" and how to override (e.g., `/opsx-apply <other>`).

2. **Check status to understand the schema**
   ```bash
   openspec status --change "<name>" --json
   ```
   Parse the JSON to understand:
   - `schemaName`: The workflow being used
   - Which artifact contains the tasks (typically "tasks" for spec-driven)

3. **Get apply instructions**

   ```bash
   openspec instructions apply --change "<name>" --json
   ```

   This returns:
   - Context file paths
   - Progress (total, complete, remaining)
   - Task list with status
   - Dynamic instruction based on current state

   **Handle states:**
   - If `state: "blocked"` (missing artifacts): show message, suggest using openspec-continue-change
   - If `state: "all_done"`: congratulate, suggest archive
   - Otherwise: proceed to implementation

4. **Read context files**

   Read the files listed in `contextFiles` from the apply instructions output.
   Pay special attention to:
   - `design.md` — target classes, smali patterns, patch approach
   - `docs/<appname>/` — prior research and debugging notes

5. **Show current progress**

   Display:
   - Schema being used
   - Progress: "N/M tasks complete"
   - Remaining tasks overview
   - Dynamic instruction from CLI

6. **Implement patches with iterative testing (loop until done or blocked)**

   **IMPORTANT**: Unlike normal software engineering, APK patching requires testing AFTER each patch, not just at the end. The cycle is:

   ```
   Write patch → Build .mpp → Apply to APK → Test on device → Document result → Next patch
   ```

   For each task in the tasks file:

   a. **Implement the patch code**
      - Create/edit the patch file in `patches/src/main/kotlin/app/template/patches/<app>/<category>/`
      - Follow the design.md for target classes and smali patterns
      - Use the morphe-patching skill for guidance on syntax and conventions
      - Keep the patch minimal and focused

   b. **Build the patch package**
      ```bash
      ANDROID_HOME="$HOME/Android/Sdk" GITHUB_ACTOR="$(gh api user --jq '.login')" GITHUB_TOKEN="$(gh auth token)" ./gradlew :patches:buildAndroid
      ```

   c. **Test the patch**
      - Apply the .mpp to the target APK using Morphe CLI
      - Install the patched APK on a device/emulator via ADB
      - Verify the expected behavior change
      - Check for crashes, unexpected behavior, or side effects

   d. **Document the result**
      - If the patch works: mark task complete, note success in docs
      - If the patch fails: document the failure, analyze why, iterate
      - Update `docs/<appname>/` with findings

   e. **Mark task complete**
      - Update task checkbox in the tasks file: `- [ ]` → `- [x]`
      - Continue to next task

   **Pause if:**
   - Task is unclear → ask for clarification
   - Patch causes a crash → analyze logcat, find root cause, iterate
   - Target class/method not found → re-analyze APK with JADX CLI
   - Implementation reveals a design issue → suggest updating artifacts
   - User interrupts

7. **On completion or pause, show status**

   Display:
   - Tasks completed this session
   - Overall progress: "N/M tasks complete"
   - If all done: suggest archive
   - If paused: explain why and wait for guidance

**Output During Implementation**

```
## Implementing: <change-name> (schema: <schema-name>)

Working on task 3/7: <task description>
[...writing patch code...]
[...building .mpp...]
[...testing on device...]
✓ Patch works — root detection bypassed successfully

Working on task 4/7: <task description>
[...writing patch code...]
[...building .mpp...]
[...testing on device...]
✗ Patch crashes on launch — analyzing logcat...
```

**Output On Completion**

```
## Implementation Complete

**Change:** <change-name>
**Schema:** <schema-name>
**Progress:** 7/7 tasks complete ✓

### Completed This Session
- [x] Task 1 — patch written, built, tested ✓
- [x] Task 2 — patch written, built, tested ✓
...

All patches tested and working! Ready to archive this change.
```

**Output On Pause (Issue Encountered)**

```
## Implementation Paused

**Change:** <change-name>
**Schema:** <schema-name>
**Progress:** 4/7 tasks complete

### Issue Encountered
<description: crash log, missing class, failed test, etc.>

**Options:**
1. Iterate on the patch (adjust target or smali)
2. Re-analyze the APK with JADX CLI
3. Update the design with a new approach
4. Other approach

What would you like to do?
```

**Guardrails**
- **Test after EVERY patch** — don't batch patches without testing in between
- **Keep changes minimal** — only modify what's needed for the current task
- **Document failures** — every failed patch attempt goes in `docs/<appname>/`
- **Use JADX CLI to verify** — always confirm class/method existence before writing patches
- **Pause on crashes** — don't guess at fixes; analyze logcat and understand the root cause
- **Read context files before starting** — especially design.md and prior docs
- **If task is ambiguous, pause and ask** — wrong patches can crash the app
- **Update task checkbox immediately** after completing each task
- **Use contextFiles from CLI output**, don't assume specific file names

**Iterative Patch-Test Cycle**

This skill enforces the patch-test-repeat cycle that APK patching requires:

```
  ┌─────────────┐     ┌──────────┐     ┌───────────┐     ┌────────────┐
  │ Write Patch │────▶│ Build .mpp│────▶│ Apply &   │────▶│ Test on    │
  │   Code      │     │           │     │ Install   │     │  Device    │
  └─────────────┘     └──────────┘     └───────────┘     └─────┬──────┘
       ▲                                                        │
       │                          ┌──────────────┐              │
       │                          │  Document &  │◀─────────────┘
       │                          │   Iterate    │
       └──────────────────────────┴──────────────┘
              (if test fails)          │
                                       ▼
                                 (if test passes)
                                 Mark task complete
```

**Fluid Workflow Integration**

This skill supports the "actions on a change" model:

- **Can be invoked anytime**: Before all artifacts are done (if tasks exist), after partial implementation, interleaved with other actions
- **Allows artifact updates**: If implementation reveals design issues, suggest updating artifacts — not phase-locked, work fluidly
- **Testing is part of implementation**: A task isn't complete until the patch is tested on a real device/emulator
