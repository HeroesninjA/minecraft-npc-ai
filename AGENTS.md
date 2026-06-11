# Codex Project Rules

This project has a local MCP context server. Use it before loading broad repo context.

- MCP server: `streamable_http` at `http://127.0.0.1:3000/mcp`.
- Serena coding MCP sidecar: `streamable_http` at `http://127.0.0.1:9121/mcp`; use it for symbol-level code navigation, references, diagnostics, and refactors.
- Serena complements Chroma/MCP project memory. It does not replace `ainpc-project-memory`, Chroma, backup, audit, rules, changelog, or context history.
- At the start of coding sessions, activate the current project with Serena and read its initial instructions before relying on broad file reads.
- Docker publishes MCP, Chroma, and Postgres on `127.0.0.1` only; do not change to broad host binds unless implementing the explicit remote-access phase.
- MCP write tools append compact metadata to `data/write-audit.json`; do not put secrets or full content in audit entries.
- Embedding status is reported by `project_status.embeddings`; backup manifests record provider/model/url/signature without storing API key values.
- If `ctx:status` or `ctx:audit` reports an embedding signature mismatch, back up context data first, then run `npm run ctx:index -- "C:\Users\HeroesninjA\IdeaProjects\test" ainpc_code --force` from the MCP server root.
- Project Codex config: `.codex/config.toml` registers `ainpc-project-memory` for Codex-native MCP loading.
- Main code collection: `ainpc_code`.
- Repair Codex-native and JetBrains MCP registration with `npm run ctx:register-codex -- -ProjectRoot "C:\Users\HeroesninjA\IdeaProjects\test"` from the MCP server root.
- If JetBrains ACP fails with `no rollout found for thread id ...`, run `npm run ctx:repair-jetbrains-acp` to inspect stale sessions, then `npm run ctx:repair-jetbrains-acp -- -ChatId "<jetbrains-chat-id>" -Repair` to back up and quarantine the stale `.agentsession` for one chat. Use `-AllStale` only for deliberate cleanup.
- Check or repair the context stack from `C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp` with `npm run ctx:doctor` or `npm run ctx:doctor -- -Repair`.
- For one-shot safe maintenance, use `npm run ctx:maintain -- -ProjectRoot "C:\Users\HeroesninjA\IdeaProjects\test" -Collection "ainpc_code"`. Add `-IncludeChroma` when the vector DB files should be included in the backup.
- Back up MCP memory/rules/changelog/journal with `npm run ctx:backup -- -ProjectRoot "C:\Users\HeroesninjA\IdeaProjects\test"`; add `-IncludeChroma` only when the vector SQLite files should be included too.
- Test restore safely with `npm run ctx:test-restore -- "C:\Users\HeroesninjA\IdeaProjects\test"`; it restores into `data/tmp` and does not overwrite live project/MCP/Codex/JetBrains files.
- Search prior decisions/history with `npm run ctx:history -- "<query>" all 8 1200` before re-solving an old problem.
- The indexer uses `data/index-ainpc_code.lock`; if a manual index says `locked: true`, another index process is active and the run was intentionally skipped.
- Use `npm run ctx:audit -- "C:\Users\HeroesninjA\IdeaProjects\test" ainpc_code` to verify manifest, keyword cache, Chroma vector count, and lock state.
- Use `npm run ctx:status -- "C:\Users\HeroesninjA\IdeaProjects\test"` to verify MCP health, watcher state, Chroma persistence, and Codex config registration.
- `ctx:status` also reports Codex session sync state for `.ai/codex-conversations.md`.
- The local read-only MCP dashboard is `http://127.0.0.1:3000/admin`; JSON status is `http://127.0.0.1:3000/admin/status.json`.
- If audit reports extra Chroma IDs not present in keyword cache, use `npm run ctx:audit -- "C:\Users\HeroesninjA\IdeaProjects\test" ainpc_code --repair-extra`.
- Prefer `context_pack` with `retrieval: hybrid` for normal work. Use `profile: code` for implementation, `profile: debug` for bug work, and `profile: planning` for roadmap/design questions.
- For token optimization, use staged budgets: start with `900/8`, retry with `1400/12` if context is thin, and use `2200/14` only for cross-module or ambiguous tasks.
- From this repo root, prefer local staged wrapper:
  `powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-staged.ps1 -Query "<task or symbol query>" -Profile code -NoTests`
- Shortcut for common code tasks:
  `powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-code.ps1 -Query "<task or symbol query>" -NoTests`
- Optional: persist latest selected context output:
  `powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-staged.ps1 -Query "<task or symbol query>" -Profile code -NoTests -OutFile ".ai\last-mcp-context.txt"`
- Optional: persist attempt summary JSON:
  `powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-staged.ps1 -Query "<task or symbol query>" -Profile code -NoTests -SummaryJson ".ai\last-mcp-context-summary.json"`
- Optional: analyze accumulated summary files for token efficiency:
  `powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-summary-report.ps1 -InputPath ".ai" -FilePattern "*mcp-context*summary*.json*" -OutFile ".ai\mcp-context-report.json"`
- Optional: export report CSVs for spreadsheet analysis:
  `powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-summary-report.ps1 -InputPath ".ai" -FilePattern "*mcp-context*summary*.json*" -OutFile ".ai\mcp-context-report.json" -RowsCsvOutFile ".ai\mcp-context-report-rows.csv" -ProfilesCsvOutFile ".ai\mcp-context-report-profiles.csv"`
- Optional: enable adaptive thresholds from report data:
  `powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-code.ps1 -Query "<task or symbol query>" -NoTests -UseAdaptiveThresholds -AdaptiveReportPath ".ai\mcp-context-report.json" -AdaptiveMinRuns 5`
- Optional nightly batch over query file:
  `powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-nightly.ps1 -QueryFile ".ai\mcp-nightly-queries.txt" -OutputDir ".ai" -Profile code -NoTests -UseAdaptiveThresholds`
- Optional multi-profile nightly matrix:
  `powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-nightly-matrix.ps1 -OutputRoot ".ai\nightly" -ReportRoot ".ai" -Profiles code,debug,planning -NoTests -UseAdaptiveThresholds -EnsureQueryTemplates -WriteCsvReports`
- Optional launcher with baked defaults (short command for scheduler):
  `powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-nightly-matrix-launcher.ps1`
- Optional scheduler registration (preview/apply):
  `powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-register-schedule.ps1 -TaskName "AINPC-MCP-Context-Nightly" -StartTime "02:30"` and add `-Apply` to create/update.
- Optional scheduler diagnostics:
  `powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-scheduler-diagnose.ps1 -OutFile ".ai\scheduler-diagnose.json"`
- Optional startup-folder fallback registration:
  `powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-register-startup.ps1` and add `-Apply` to create/update (or `-Remove -Apply` to remove).
- Optional unified automation health-check:
  `powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-automation-status.ps1 -ProjectRoot "." -AiDir ".ai" -FreshHours 30 -OutFile "mcp-context-automation-status.json"`
- Optional strict gate (non-zero when unhealthy):
  `powershell -ExecutionPolicy Bypass -File .\scripts\mcp-context-automation-status.ps1 -ProjectRoot "." -AiDir ".ai" -FreshHours 30 -OutFile "mcp-context-automation-status.json" -TextSummaryOutFile "mcp-context-automation-status.txt" -FailOnUnhealthy`
- By default the wrapper auto-scopes include/exclude paths by profile for token efficiency. Use `-DisableAutoScope` only when broad context is required.
- When using the CLI fallback, run from `C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp`:
  `npm run mcp:context -- "<query>" 900 8 ainpc_code prompt code "" "" true hybrid`
- For a session preflight with status, config-drift detection, optional non-destructive repair, and prompt context, run from the MCP server root:
  `npm run codex:preflight -- -ProjectRoot "C:\Users\HeroesninjA\IdeaProjects\test" -Collection ainpc_code -Query "<task or symbol query>" -Repair`
- For context-only fallback, run:
  `npm run codex:context -- "<task or symbol query>" code 900 8 "" "" true hybrid ainpc_code`
- On PowerShell, prefer the named-parameter wrapper when using path filters:
  `powershell -ExecutionPolicy Bypass -File "C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\scripts\codex-context.ps1" -Query "<task or symbol query>" -Profile code -TokenBudget 900 -TopK 8 -Include "ainpc-core-plugin/src/main/kotlin/" -Exclude "ainpc-core-plugin/src/test/" -NoTests -Retrieval hybrid`
- To persist durable notes without manual JSON, use:
  `npm run codex:log -- memory "<topic>" "<summary>" "tag1,tag2"`
- To persist a session journal into both `.ai/codex-journal.md` and MCP memory, use:
  `npm run codex:journal -- "C:\Users\HeroesninjA\IdeaProjects\test" "<title>" "<summary>" "tag1,tag2" "file1,file2"`
- To sync compact local Codex conversation summaries into project context, use:
  `npm run codex:sync-sessions -- "C:\Users\HeroesninjA\IdeaProjects\test" 25 10`
- Keep context small. Query targeted symbols, files, package names, or feature names instead of reading the whole repository.
- Do not index or inspect generated folders unless explicitly needed: `target`, `build`, `.gradle`, `.kotlin`, `.idea`, `node_modules`.
- Store durable project decisions with `memory_add`, implementation summaries with `changelog_add`, and reusable rules/context with `rules_set`.
- The live watcher is expected to keep Chroma and `data/keyword-index.json` updated after code changes.
- The session-sync watcher is expected to keep `.ai/codex-conversations.md` updated from local Codex JSONL sessions; `ctx:status` reports `sessionSyncWatcher`.
- Before large edits, check the existing dirty worktree and preserve unrelated user changes.
