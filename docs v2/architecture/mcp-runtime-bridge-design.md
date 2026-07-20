# MCP runtime bridge

Status: contract canonic al implementarii curente; smoke-ul producer-reader si hardening-ul write tools raman deschise.
Actualizat: 2026-07-18.

Bridge-ul are un canal read-only prin snapshot si un canal separat, optional, prin fisiere de comanda.

## Canalul read-only

1. `RuntimeSnapshotProducer` ruleaza asincron in plugin.
2. Intervalul implicit este `100` ticks si este limitat la `20..6000`.
3. Producerul scrie temporar si apoi redenumeste `data/mcp-runtime-snapshot.json`.
4. Snapshot-ul Paper are `schemaVersion=3` si include plugin, features, NPC, world, quests, build mode si health bounded.
5. `SnapshotReader` din sidecar citeste fisierul la cerere numai in mod non-offline.
6. Profilul `local-bridge` foloseste cache TTL de 2 secunde si prag stale de 60 de secunde.

## Compatibilitatea schemei

`SnapshotReader` accepta versiunile `1..3`, iar fixture-urile sidecar curente sunt v3. Snapshot-urile v1/v2 raman citibile, dar nu contin obiectul `health`; versiunile mai noi de 3 sunt respinse explicit. Existenta fisierului nu este suficienta: starea poate fi `STALE`, `INVALID`, `MISSING` sau `OFFLINE`.

## Redactare, stare si audit

- `RedactingSnapshotFilter` copiaza numai modelul Java cunoscut sidecar-ului, inclusiv proiectia health v3;
- modelul Java oglindeste campurile v3 pastrate de producer;
- filtrul mascheaza valori sensibile numai in anumite campuri de sample;
- `McpSnapshotService` auditeaza durata, disponibilitatea si marimea payload-ului;
- transformarea prin `McpSnapshotService` reconstruieste rezultatul si poate pierde distinctia `STALE`, raportand-o ca `CACHED`;
- health indicator-ul citeste direct `SnapshotReader` si vede starea stale originala.

## Canalul de comenzi

- sidecar-ul scrie `*.cmd.json` prin `AinpcWriteTools`;
- pluginul citeste coada pe main thread la 5 secunde numai cand `mcp.write_tools_enabled=true`;
- rezultatul este scris separat in `*.result.json`;
- cai relative identice nu garanteaza acelasi director daca procesele au workdir diferit;
- canalul de comenzi nu este parte din contractul read-only si nu are autentificare proprie verificata.

## Cerinte de configurare

- foloseste cai absolute comune pentru snapshot si command queue;
- porneste sidecar-ul cu profilul `local-bridge` si aceeasi cale absoluta ca producerul;
- nu folosi actuator `UP` ca dovada ca snapshot-ul este valid;
- verifica `ainpc.debug.health` si un tool bazat pe snapshot;
- pastreaza write tools neconsumate pana exista autentificare, allowlist si audit end-to-end.

## Surse in cod

- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/mcp/bridge/RuntimeSnapshotProducer.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/mcp/bridge/RuntimeSnapshot.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/mcp/bridge/McpCommandQueue.kt`
- `ainpc-mcp-service/src/main/java/ro/ainpc/mcp/bridge/SnapshotReader.java`
- `ainpc-mcp-service/src/main/java/ro/ainpc/mcp/bridge/McpSnapshotService.java`
- `ainpc-mcp-service/src/main/java/ro/ainpc/mcp/bridge/RedactingSnapshotFilter.java`

## Legaturi

- `architecture/spring-ai-mcp-serviciu-intern.md`
- `reference/mcp-runtime-gap-checklist.md`
- `operations/mcp-serviciu-imbunatatiri.md`
