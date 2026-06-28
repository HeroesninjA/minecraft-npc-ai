# MCP Runtime Bridge — Design Snapshot File

Actualizat: 2026-06-28

## Problemă

Tool-urile MCP din `ainpc-mcp-service` nu pot accesa date din runtime Paper (NPC-uri, questuri, mapping, story, reputație) pentru că sidecar-ul rulează într-un proces separat și nu comunică înapoi spre plugin.

Pluginul ARE un client MCP (`HttpMcpRuntimeClient`) care poate apela tool-uri, dar MCP service nu are un canal de întoarcere (plugin → MCP funcționează, MCP → plugin nu).

## Soluție Aleasă: Snapshot File

Cel mai simplu și sigur model fără rețea suplimentară: **pluginul scrie un fișier JSON snapshot periodic; MCP service citește fișierul la cerere**.

### Flux

```
Paper Plugin (main thread scheduler)
  │ scrie la fiecare 5-10s
  ▼
data/mcp-runtime-snapshot.json
  │ citește la cerere (cu cache TTL 2s)
  ▼
ainpc-mcp-service (Spring Boot)
  │ expune ca tool MCP
  ▼
Client MCP extern / Codex / etc.
```

### Avantaje

- Zero rețea între procese
- Nu atinge Bukkit API asincron
- Funcționează și după reload plugin
- Fișierul poate fi inspectat manual (debug)
- Fallback natural când fișierul lipsește sau e stale

### Limite

- Datele sunt între 2–10s vechi (suficient pentru dialog NPC și tool-uri read-only)
- Fișierul conține date redactate, nu DB complet

## Schema Snapshot

Versiunea 1 (`schemaVersion: 1`):

```json
{
  "schemaVersion": 1,
  "timestamp": "2026-06-28T12:00:00Z",
  "plugin": {
    "version": "1.0.0",
    "serverType": "Paper",
    "serverVersion": "1.21",
    "tps": 20.0,
    "onlinePlayers": 5,
    "uptimeMinutes": 120
  },
  "features": {
    "ai": true,
    "mcp": true,
    "aiOrchestration": false
  },
  "npc": {
    "totalCount": 12,
    "onlineCount": 10,
    "byRegion": { "demo_sat": 8, "padurea_vrajita": 4 },
    "byRole": { "villager": 8, "guard": 2, "merchant": 2 },
    "recentSamples": [
      {
        "npcId": "npc_demo_sat_001",
        "name": "Gheorghe",
        "profession": "Fierar",
        "regionId": "demo_sat",
        "currentPlace": "fieraria",
        "mood": "HAPPY",
        "relationship": 3
      }
    ]
  },
  "world": {
    "regionCount": 2,
    "placeCount": 14,
    "nodeCount": 28,
    "regions": ["demo_sat", "padurea_vrajita"],
    "places": ["fieraria", "taverna", "piata", "primaria", "biserica"]
  },
  "quests": {
    "activePlayerQuests": 3,
    "activeGlobalQuests": 1,
    "topMechanics": {
      "village_contracts": 1,
      "npc_duties": 2,
      "local_bounties": 1
    }
  },
  "story": {
    "regionsWithState": 1,
    "recentEvents": [
      {
        "eventType": "story_event",
        "place": "fieraria",
        "summary": "Jucatorul a vorbit cu fierarul despre sabie"
      }
    ]
  },
  "reputation": {
    "topRegions": [
      { "regionId": "demo_sat", "score": 50, "level": "NEUTRAL" },
      { "regionId": "padurea_vrajita", "score": -10, "level": "UNFRIENDLY" }
    ]
  },
  "performance": {
    "npcTickMs": 1.2,
    "questTickMs": 0.3,
    "snapshotBuildMs": 0.8
  }
}
```

### Reguli de redactare

Următoarele câmpuri NU ajung în snapshot:

- `token`, `apiKey`, `password`, `secret`, `credential`
- `OPENAI_API_KEY`, `ANTHROPIC_API_KEY`
- `mcp.token`
- conținutul complet al prompturilor AI
- coordonatele exacte (x,y,z) — doar placeId / nodeId
- UUID-urile playerilor — doar username (dacă există)
- orice valoare care conține caracterul `*` în toate pozițiile (placeholder)

## Scriere în Plugin

```kotlin
// ro.ainpc.mcp.bridge.RuntimeSnapshotProducer
class RuntimeSnapshotProducer(
    private val plugin: AINPCPlugin,
    private val scheduler: BukkitScheduler,
    private val snapshotFile: Path = Path.of("data", "mcp-runtime-snapshot.json")
) {
    private var taskId: Int? = null

    fun start(intervalTicks: Long = 100L) // 5s default
    fun stop()
    private fun produceSnapshot(): RuntimeSnapshot
    private fun writeSnapshot(snapshot: RuntimeSnapshot)
}
```

Scrierea atomară: se scrie într-un fișier `.tmp`, apoi se face `rename` atomic.

## Citire în MCP Service

```java
// ro.ainpc.mcp.bridge.SnapshotReader
@Component
public class SnapshotReader {
    private final AtomicReference<SnapshotCache> cache = new AtomicReference<>();
    private final Path snapshotPath;
    private final Duration ttl;

    public RuntimeSnapshot read() {
        SnapshotCache cached = cache.get();
        if (cached != null && !cached.isStale()) {
            return cached.snapshot();
        }
        return reload();
    }
}
```

## Fallback

Când fișierul snapshot lipsește:

1. Tool-urile returnează `available=false, status="bridge_missing"` cu un `detail` explicativ
2. `HealthIndicator` raportează `DOWN` cu `reason: snapshot_file_not_found`
3. Pluginul continuă normal (`features.mcp=false` sau `failOpen=true`)

## Configurație

În `config.yml` ale pluginului se adaugă:

```yaml
mcp:
  snapshot:
    auto: true
    interval_ticks: 100
    max_regions: 10
    max_places: 50
    max_npc_samples: 5
    max_reputation_regions: 5
    max_recent_events: 5
```

În `application.yml` ale MCP service:

```yaml
mcp:
  snapshot:
    path: data/mcp-runtime-snapshot.json
    cache_ttl_seconds: 2
    stale_threshold_seconds: 60
    blocklist_fields:
      - token
      - apiKey
      - password
      - secret
      - credential
      - OPENAI_API_KEY
      - ANTHROPIC_API_KEY
```

## Testare

- Unit test: `RuntimeSnapshotProducerTest` verifică redactarea câmpurilor din blocklist
- Unit test: `SnapshotReaderTest` cu fixture JSON
- Smoke test: scriere snapshot în `data/`, pornire MCP profile `local-bridge`, apel tool
- Paper test: snapshot se actualizează după spawn NPC sau completare quest

## Future: Migration la HTTP direct

Dacă snapshot file devine insuficient (date prea mari, latență prea mare), se poate migra la un endpoint HTTP mic în plugin:

```
Plugin→ endpoint GET http://127.0.0.1:39842/api/mcp/snapshot
```

Păstrând aceeași schemă JSON și același `SnapshotReader` interface.
