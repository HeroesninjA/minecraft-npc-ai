# Codex 250 Demo Paper Evidence Template

Data rularii:
Operator:
Server:
Plugin build:
Player:
Region: demo_sat

PAPER_EVIDENCE_TEMPLATE=true
DO_NOT_STORE_SECRETS=true

## Rezumat

| Field | Value |
| --- | --- |
| Local build | PENDING |
| Paper boot | PENDING |
| Demo status | PENDING |
| Audit all | PENDING |
| Debugdump before | PENDING |
| Restart controlled | PENDING |
| Debugdump after | PENDING |
| Release decision | PENDING |

## Local Build Evidence

Command:

```text
.\gradlew.bat test assemble
```

Observed:

```text
PENDING
```

Artifact paths:

- ainpc-core-plugin/build/libs:
- ainpc-scenario-medieval/build/libs:
- ainpc-api/build/libs:

## Paper Boot Evidence

Commands:

```text
/ainpc demo definition
/ainpc demo status demo_sat
/ainpc demo next demo_sat
```

Observed:

```text
PENDING
```

## World And Settlement Evidence

Commands:

```text
/ainpc world demo create demo_sat
/ainpc world places demo_sat
/ainpc demo smoke demo_sat <player>
```

Observed:

```text
PENDING
```

## NPC, Routine, Quest, Story Evidence

Commands:

```text
/ainpc info nearest
/ainpc routine status nearest
/ainpc progression stored <player>
/ainpc story events demo_sat
/ainpc audit all
```

Observed:

```text
PENDING
```

## Debugdump Evidence

Command:

```text
/ainpc debugdump all
```

Debugdump before path:

```text
PENDING
```

Files inspected:

- config-sanitized.yml:
- openai.txt:
- recent-server-log.txt:
- world-mapping.json:
- npc-world-bindings.json:
- player-progressions.json:
- story-events.json:

Secret scan result:

```text
PENDING
```

## Restart Evidence

Commands/actions:

```text
/ainpc demo restart demo_sat
/ainpc debugdump all
Paper console stop
start Paper normal
/ainpc world places demo_sat
/ainpc list
/ainpc world bindings list 20
/ainpc demo status demo_sat
/ainpc debugdump all
```

Debugdump after path:

```text
PENDING
```

Observed:

```text
PENDING
```

## Release Decision Evidence

Decision: PENDING

Allowed values:

- REMOVE
- KEEP_INTERNAL
- PROMOTE

Reason:

```text
PENDING
```

## Final Gate

| Gate | Result |
| --- | --- |
| No secrets stored in this file | PENDING |
| LIVE gates have command output | PENDING |
| Restart used controlled stop | PENDING |
| Debugdump before/after paths recorded | PENDING |
| Release decision selected | PENDING |
