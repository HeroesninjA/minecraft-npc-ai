# Codex Journal

## 2026-05-20T08:29:15.566Z - MCP conversation journal enabled

Tags: codex-session, mcp, codex, journal, conversation-history

Files: AGENTS.md, mcp/server.js, mcp/scripts/codex-journal.mjs, mcp/package.json, mcp/RUNBOOK.md

Added a project-local Codex journal at .ai/codex-journal.md, exposed MCP memory as project://memory, and wired codex:journal to save session summaries into both the journal file and MCP memory.

## 2026-05-20T08:31:47.912Z - PowerShell context wrappers validated

Tags: codex-session, mcp, codex, powershell, workflow

Files: AGENTS.md, mcp/scripts/codex-context.ps1, mcp/scripts/context-simulate.ps1, mcp/RUNBOOK.md

Added and validated named-parameter PowerShell wrappers for Codex context and cost simulation. Use -NoTests instead of boolean positional parameters to avoid PowerShell/npm argument issues.

## 2026-05-20T08:33:44.726Z - Query-aware MCP snippets

Tags: codex-session, mcp, token-optimization, context

Files: mcp/server.js

Updated MCP compact search output so snippets are centered around query terms instead of always taking the beginning of long files. This improves token efficiency for journals and large source files.

## 2026-05-20T08:51:15.426Z - Context stack doctor and lifecycle scripts

Tags: codex-session, mcp, ops, doctor, autostart

Files: AGENTS.md, mcp/RUNBOOK.md, mcp/package.json, mcp/scripts/start-context-stack.ps1, mcp/scripts/stop-context-stack.ps1, mcp/scripts/context-doctor.ps1, mcp/scripts/install-context-autostart.ps1, mcp/scripts/remove-context-autostart.ps1

Added operational scripts for starting, stopping, repairing, and optionally autostarting the MCP context stack. Validated ctx:doctor -Repair: Docker services healthy, 12 tools, 3 resources, watcher running, and index refreshed incrementally.

## 2026-05-20T08:56:10.301Z - Persistent Chroma storage and context backups

Tags: codex-session, mcp, backup, chroma, persistence

Files: docker-compose.yml, AGENTS.md, mcp/RUNBOOK.md, mcp/package.json, mcp/scripts/backup-context-data.ps1, mcp/scripts/restore-context-data.ps1, mcp/scripts/context-status.mjs

Moved Chroma storage to a host bind mount at data/chroma, added backup/restore scripts for MCP context data, extended ctx:status with Chroma SQLite persistence details, and created a verified backup archive at data/backups/context-backup-20260520-115543.zip.

## 2026-05-20T09:00:47.863Z - Searchable MCP history tools

Tags: codex-session, mcp, history, search, status

Files: AGENTS.md, mcp/server.js, mcp/RUNBOOK.md, mcp/package.json, mcp/scripts/context-history.mjs

Added MCP tools memory_search, changelog_search, rules_search, and project_status, plus the ctx:history CLI. Verified history search against backup/chroma persistence and conversation journal entries; MCP now exposes 16 tools and 4 resources.

## 2026-05-20T09:04:05.443Z - Indexer overlap lock

Tags: codex-session, mcp, indexer, watcher, lock, stability

Files: AGENTS.md, mcp/RUNBOOK.md, mcp/scripts/index-project.mjs, mcp/scripts/context-status.mjs, mcp/server.js

Added a global index lock at data/index-ainpc_code.lock with stale timeout handling to prevent watcher/manual index overlap. Verified normal incremental indexing and simulated active lock behavior; locked runs skip quickly instead of blocking or looping.

## 2026-05-20T09:20:04.518Z - Context audit and stale vector repair

Tags: codex-session, mcp, audit, indexer, chroma, repair

Files: AGENTS.md, mcp/RUNBOOK.md, mcp/package.json, mcp/scripts/context-audit.mjs, mcp/scripts/index-project.mjs, mcp/server.js

Added ctx:audit to compare project files, manifest, keyword cache, and Chroma vector IDs/counts. Fixed indexer stale-vector behavior by deleting a changed file's Chroma vectors before upsert, added --force rebuild support, removed stale probe vector, and verified expected/keyword/Chroma all equal 4089 chunks.

## 2026-05-20T09:35:13.954Z - MCP context maintenance command

Tags: codex-session, mcp, context, maintenance, backup

Files: C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\scripts\maintain-context.ps1, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\scripts\backup-context-data.ps1, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\package.json, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\RUNBOOK.md, C:\Users\HeroesninjA\IdeaProjects\test\AGENTS.md

Added ctx:maintain as a one-shot maintenance command: doctor/repair, audit with extra Chroma ID repair, controlled force reindex only when needed, watcher restart, and backup summary. Fixed backup script output so maintain can report the archive path.

## 2026-05-20T09:42:26.873Z - Codex native MCP registration

Tags: codex-session, mcp, codex, config, status, backup

Files: C:\Users\HeroesninjA\AppData\Local\JetBrains\IntelliJIdea2026.1\aia\codex\config.toml, C:\Users\HeroesninjA\.codex\config.toml, C:\Users\HeroesninjA\IdeaProjects\test\.codex\config.toml, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\scripts\context-status.mjs, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\scripts\backup-context-data.ps1, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\scripts\restore-context-data.ps1, C:\Users\HeroesninjA\IdeaProjects\test\AGENTS.md

Registered ainpc-project-memory as a streamable HTTP MCP server in JetBrains CODEX_HOME, standalone ~/.codex, and project .codex/config.toml. Updated ctx:status to report Codex config registration and updated backup/restore to preserve project .codex/config.toml.

## 2026-05-20T09:48:30.187Z - Codex MCP registration repair script

Tags: codex-session, mcp, codex, repair, startup, backup

Files: C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\scripts\register-codex-mcp.ps1, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\scripts\context-doctor.ps1, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\scripts\start-context-stack.ps1, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\scripts\backup-context-data.ps1, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\package.json, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\RUNBOOK.md, C:\Users\HeroesninjA\IdeaProjects\test\AGENTS.md

Added ctx:register-codex and scripts/register-codex-mcp.ps1 to idempotently repair JetBrains CODEX_HOME, standalone ~/.codex, project .codex/config.toml, and .ai/mcp/mcp.json MCP registration. Wired it into ctx:doctor -- -Repair and ctx:start, and expanded backups to include MCP scripts.

## 2026-05-20T09:57:26.515Z - Codex session history sync

Tags: codex-session, mcp, codex, conversation-history, session-sync, vector-db

Files: C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\scripts\sync-codex-sessions.mjs, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\scripts\context-status.mjs, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\scripts\context-doctor.ps1, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\scripts\backup-context-data.ps1, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\scripts\restore-context-data.ps1, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\package.json, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\RUNBOOK.md, C:\Users\HeroesninjA\IdeaProjects\test\AGENTS.md, C:\Users\HeroesninjA\IdeaProjects\test\.ai\codex-conversations.md

Added codex:sync-sessions and scripts/sync-codex-sessions.mjs to summarize local Codex JSONL sessions for this project into .ai/codex-conversations.md and MCP memory. It filters out reasoning, tool outputs, base instructions, and technical environment payloads. ctx:doctor -- -Repair now runs the session sync before indexing; ctx:status reports session sync counts.

## 2026-05-20T15:11:45.728Z - Codex session sync watcher

Tags: codex-session, mcp, codex, session-sync, watcher, non-blocking

Files: C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\scripts\watch-codex-sessions.mjs, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\scripts\start-session-sync.ps1, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\scripts\stop-session-sync.ps1, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\scripts\sync-codex-sessions.mjs, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\scripts\context-status.mjs, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\scripts\context-doctor.ps1, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\scripts\start-context-stack.ps1, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\scripts\stop-context-stack.ps1, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\package.json, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\RUNBOOK.md, C:\Users\HeroesninjA\IdeaProjects\test\AGENTS.md

Added a hidden Codex session sync watcher with PID/log management. ctx:start now starts code indexing and session sync watchers; ctx:stop stops both; ctx:doctor -- -Repair confirms both without duplicating processes. sync-codex-sessions now avoids rewriting .ai/codex-conversations.md on idle runs, reducing index churn.

## 2026-05-23T06:57:33.621Z - MCP U4 latest index run metrics and Docker PATH hardening

Tags: codex-session, mcp, observability, docker, ctx-status, admin-dashboard

Files: C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\scripts\context-status.mjs, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\server.js, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\public\admin.html, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\scripts\test-mcp-stack.mjs, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\scripts\start-context-stack.ps1, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\scripts\context-doctor.ps1, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\scripts\stop-context-stack.ps1, C:\Users\HeroesninjA\IdeaProjects\test\docs\mcp-docker-server-mvp-si-faze.md

Added latest index refresh metrics to ctx:status and admin dashboard, including status, duration, reason, file counts, chunk counts, and parsed watcher result JSON. Hardened Docker orchestration scripts to prepend Docker Desktop resource paths so docker-credential-desktop is available when Docker is outside PATH. Rebuilt MCP container and validated ctx:test.

## 2026-05-23T07:04:30.357Z - MCP Docker Compose helper hardening

Tags: codex-session, mcp, docker, powershell, ctx-test, hardening

Files: C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\scripts\docker-compose-utils.ps1, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\scripts\start-context-stack.ps1, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\scripts\context-doctor.ps1, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\scripts\stop-context-stack.ps1, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\scripts\test-mcp-stack.mjs, C:\Users\HeroesninjA\Desktop\npc ai mc\mcp-ai-server\mcp\RUNBOOK.md, C:\Users\HeroesninjA\IdeaProjects\test\docs\mcp-docker-server-mvp-si-faze.md

Extracted Docker/Desktop path and compose fallback logic into scripts/docker-compose-utils.ps1. start-context-stack, context-doctor, and stop-context-stack now use the shared helper. The helper prepends Docker Desktop resource/plugin paths and throws on non-zero native Docker/Compose exits so failed builds do not report false success. Added ctx:test coverage for native exit-code propagation and updated docs.

## 2026-05-23T07:53:29.245Z - Admin dashboard log tail limit

Tags: codex-session, mcp, observability, admin-dashboard, logs

Files: server.js, public/admin.html, scripts/test-mcp-stack.mjs, RUNBOOK.md, docs/mcp-docker-server-mvp-si-faze.md

Added tail-limited log reads for /admin/status.json, exposed admin log thresholds in JSON/UI, covered them in ctx:test, and documented MCP_ADMIN_LOG_TAIL_BYTES/MCP_ADMIN_LOG_RECENT_MINUTES.

## 2026-05-23T08:57:30.489Z - Admin dashboard collection parameter

Tags: codex-session, mcp, admin-dashboard, collections, tests

Files: server.js, public/admin.html, scripts/test-mcp-stack.mjs, RUNBOOK.md, docs/mcp-docker-server-mvp-si-faze.md

Made the read-only MCP admin dashboard honor ?collection=<name>, forward that collection to /admin/status.json, display the effective collection, and generate copy-only backup/preflight commands for the requested collection. Added ctx:test coverage and docs.

## 2026-05-23T09:33:19.600Z - Read-only admin collection status

Tags: codex-session, mcp, admin-dashboard, read-only, chroma, tests

Files: server.js, public/admin.html, scripts/test-mcp-stack.mjs, RUNBOOK.md, docs/mcp-docker-server-mvp-si-faze.md

Hardened MCP admin status so read-only status calls use findCollectionId instead of ensureCollection. Missing Chroma collections are reported as collection_exists=false with no implicit creation, the dashboard displays collection status, tests cover the no-side-effect behavior, and docs were updated.

## 2026-05-23T10:14:12.826Z - Read-only MCP search tools

Tags: codex-session, mcp, read-only, chroma, vector-search, context-pack, tests

Files: server.js, scripts/test-mcp-stack.mjs, RUNBOOK.md, docs/mcp-docker-server-mvp-si-faze.md

Hardened MCP read/search paths so vector_search, vector_search_by_file, context_pack, and project_status do not create missing Chroma collections. vector_delete_by_file now skips Chroma deletion when a collection is missing. Added ctx:test coverage for missing-collection read paths and updated docs.

## 2026-05-23T10:49:09.313Z - MCP collection name validation

Tags: codex-session, mcp, chroma, validation, tests

Files: server.js, scripts/test-mcp-stack.mjs, RUNBOOK.md, docs/mcp-docker-server-mvp-si-faze.md

Centralized collection-name validation for MCP tools and admin status. Invalid collection names now return explicit errors instead of silent sanitization, while missing valid collections remain read-only for status/search paths.

## 2026-05-23T11:02:29.797Z - MCP admin collection inventory

Tags: codex-session, mcp, admin-dashboard, chroma, collections, tests

Files: server.js, public/admin.html, scripts/test-mcp-stack.mjs, RUNBOOK.md, docs/mcp-docker-server-mvp-si-faze.md

Added read-only Chroma collection inventory to /admin/status.json and the local admin dashboard, including selected collection status, bounded collection names, and collection naming rules. Extended ctx:test coverage and docs.
