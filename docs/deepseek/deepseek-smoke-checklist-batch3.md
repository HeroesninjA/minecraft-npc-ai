# Smoke Checklist — Batch L151-L200

Rulează înainte de a marca batch-ul ca finalizat:

- [x] Build: `./gradlew test` — BUILD SUCCESSFUL, 0 erori
- [x] Scope: `ScopeRegistry` normalizează region/place + aliasuri
- [x] Warnings: scope lipsă/invalid produce WARNING, nu ERROR
- [x] Debounce: evenimente identice în <500ms sunt ignorate
- [x] Cleanup: `cleanupOrphanedObjectives()` curăță state-ul offline
- [x] File source: mesajele de warning includ numele fișierului YAML
- [x] Objective tree: `loaded-quest-definitions.json` include `objective_tree`
- [x] Reload: `/ainpc quest reload <id>` reîncarcă conținutul
- [x] Backup: `/ainpc quest backup` salvează pack-urile în ZIP
- [x] Reindex: `/ainpc quest reindex` regenerează indexul
- [x] Schema version: `FeaturePack.schemaVersion` = 1
- [x] Batch guide: `./deepseek-batch-guide.md` creat

