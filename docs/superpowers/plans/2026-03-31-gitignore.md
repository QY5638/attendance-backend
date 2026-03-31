# Git Ignore Rules Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a root `.gitignore` that ignores current Maven and IDE artifacts plus near-term frontend generated files while keeping real project sources visible to Git.

**Architecture:** Use one root-level `.gitignore` so ignore behavior is centralized and easy to audit. Keep the rules intentionally narrow: cover existing backend noise, upcoming frontend generated files, and a small set of common log/cache files without hiding project sources or docs.

**Tech Stack:** Git, Markdown spec, root `.gitignore`, PowerShell and git verification commands

---

## File Structure

- Create: `D:\Graduation project\.gitignore`
  - Responsibility: Centralize ignore rules for the repository.
- Reference: `D:\Graduation project\backend\docs\superpowers\specs\2026-03-31-gitignore-design.md`
  - Responsibility: Defines the approved scope and ignore strategy.
- Reference: `D:\Graduation project\backend\AGENTS.md`
  - Responsibility: Provides project-wide constraints such as not committing unless explicitly requested.

## Implementation Notes

- This repository has just been initialized and does not yet have a baseline commit, so worktree-based isolation is not available for this specific task.
- Do not create a commit as part of this plan unless the user explicitly requests one later.
- Verification must prove both sides of the change: unwanted artifacts are ignored, and wanted project files remain visible.

### Task 1: Create The Root Git Ignore File

**Files:**
- Create: `D:\Graduation project\.gitignore`
- Reference: `D:\Graduation project\backend\docs\superpowers\specs\2026-03-31-gitignore-design.md`

- [ ] **Step 1: Run the failing existence check**

Run: `powershell -NoProfile -Command "Test-Path 'D:\Graduation project\.gitignore'"`
Expected: `False`

- [ ] **Step 2: Write the root ignore rules**

```gitignore
# Backend build outputs
backend/target/

# IDE metadata
backend/.idea/

# Frontend generated files
frontend/node_modules/
frontend/dist/
frontend/.env.local
frontend/.env.*.local

# Logs and temporary files
*.log
*.tmp

# OS cache files
.DS_Store
Thumbs.db
```

- [ ] **Step 3: Verify the file contains every approved rule group**

Run: `powershell -NoProfile -Command "Select-String -Path 'D:\Graduation project\.gitignore' -SimpleMatch 'backend/target/','backend/.idea/','frontend/node_modules/','frontend/dist/','frontend/.env.local','frontend/.env.*.local','*.log','*.tmp','.DS_Store','Thumbs.db' | ForEach-Object { $_.Line }"`
Expected: Output includes all ten ignore lines.

### Task 2: Verify Existing Generated Artifacts Are Ignored

**Files:**
- Verify: `D:\Graduation project\.gitignore`
- Verify: `D:\Graduation project\backend\target\`
- Verify: `D:\Graduation project\backend\.idea\`

- [ ] **Step 1: Check ignore status for existing backend generated directories**

Run: `git check-ignore -v "backend/target/surefire-reports/TEST-com.quyong.attendance.AttendanceApplicationTests.xml" "backend/.idea/workspace.xml"`
Expected: Output shows `.gitignore` rules matching both paths.

- [ ] **Step 2: Confirm standard status output no longer lists backend generated artifacts**

Run: `git status --short`
Expected: Output does not include `backend/target/` or `backend/.idea/`.

- [ ] **Step 3: Confirm ignored output does list backend generated artifacts**

Run: `git status --short --ignored`
Expected: Output includes `!! backend/target/` and `!! backend/.idea/`.

### Task 3: Verify Real Project Files Remain Visible And Future Frontend Rules Are Ready

**Files:**
- Verify: `D:\Graduation project\.gitignore`
- Verify: `D:\Graduation project\backend\AGENTS.md`
- Verify: `D:\Graduation project\backend\pom.xml`
- Verify: `D:\Graduation project\backend\src\`
- Verify: `D:\Graduation project\backend\docs\`
- Verify: `D:\Graduation project\backend\sql\`

- [ ] **Step 1: Verify important project content is still visible to Git**

Run: `git status --short`
Expected: Output still includes untracked project paths such as `AGENTS.md`, `backend/`, `docs/`, and `sql/`.

- [ ] **Step 2: Verify future frontend ignore rules match representative paths**

Run: `git check-ignore -v "frontend/node_modules/vue/index.js" "frontend/dist/assets/index.js" "frontend/.env.local" "frontend/.env.development.local"`
Expected: Output shows `.gitignore` rules matching all four representative frontend paths.

- [ ] **Step 3: Verify the ignore file did not hide the rule source documents**

Run: `powershell -NoProfile -Command "@('D:\Graduation project\backend\AGENTS.md','D:\Graduation project\backend\docs\superpowers\specs\2026-03-31-gitignore-design.md','D:\Graduation project\.gitignore') | ForEach-Object { '{0} => {1}' -f $_, (Test-Path $_) }"`
Expected: Each listed path ends with `=> True`.
