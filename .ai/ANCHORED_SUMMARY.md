## Goal
Finish Kotlin conversion of the 3 remaining Java files (AINPCCommand.java, ScenarioEngine.java, NPCManager.java) by extracting cohesive groups into Kotlin sidecar files.

## Constraints & Preferences
- Sidecar pattern: keep Java class as entry point, delegate to Kotlin top-level functions via `@file:JvmName`
- No rewriting of business logic; preserve original command routing, error handling, and Bukkit/Paper compatibility
- Each extraction must compile green before next step
- Document progress in `docs/kotlin-migration-tracker.md`

## Status
- **KOT-299** ✅ NPCManager warning fixes
- **KOT-300** ✅ NPCManagerAnchors.kt extraction (11 anchor methods)
- **KOT-301** ✅ AINPCCommandDisplay.kt extraction (12 usage + sendHelp)
- **KOT-302** ✅ AINPCCommandStory.kt extraction (5 story handlers + 7 helpers + 2 constants)
- **KOT-303** ✅ AINPCCommandMisc.kt extraction (11 misc handlers + sendHelp removal)

## Current Java LOC
- AINPCCommand.java: ~5482 (was 6267)
- ScenarioEngine.java: 4414 (unchanged)
- NPCManager.java: 1824 (was 2566)
- Total: ~11,720 Java lines

## Next Steps
- Extract next group from AINPCCommand.java (Map/Wand ~568 lines, Delete/Repair/Info ~778 lines, or Quest ~1070 lines)
- Update `docs/kotlin-migration-tracker.md` with all KOT entries
- Consider tackling ScenarioEngine.java (4414 LOC) which hasn't been touched yet
