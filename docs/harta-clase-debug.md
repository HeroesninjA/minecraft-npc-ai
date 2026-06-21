# Harta claselor pentru debug

Actualizat: 2026-06-21

Acest document este doar documentatie. Nu schimba runtime-ul si nu modifica ordinea de executie.

Pentru orientare rapida, citeste mai intai [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md), apoi [harta claselor de cod](./harta-clase-cod.md).

## Scop

Aceasta harta urmareste subsistemul `debug`: dump-uri, buffer de evenimente, index semantic pentru mapping, audit si uneltele de inspectie asociate.

Nu este un inventar complet al tuturor claselor de debug. Este o harta de lucru pentru nodurile care produc si citesc diagnosticele importante.

## Noduri principale

- `DebugDumpService` -> scrie dump-uri JSON/text si colecteaza informatii de stare
- `RecentEventsBuffer` -> pastreaza evenimentele recente si le serializa pentru inspectie
- `WorldMappingSemanticIndex` -> index semantic pentru matching de regiuni, places si nodes
- `MappingWandService` -> creeaza drafturi si preview-uri pentru mapping
- `AuditReport` -> colecteaza erori, warnings si infos pentru audit
- `OpenAIDebugSnapshot` -> snapshot de debug pentru starea AI si backoff

## Flux principal

`DebugDumpService` -> colecteaza starea si scrie artefacte de inspectie

`RecentEventsBuffer` -> agregare de evenimente -> dump

`MappingWandService` -> draft -> confirmare -> audit entry

`WorldMappingSemanticIndex` -> inspectie si matching semantic

`OpenAIDebugSnapshot` -> stare AI si backoff pentru diagnostic

## Relatii utile

- `DebugDumpService` aduna date din mai multe subsisteme si le scrie ca artefacte de debug.
- `RecentEventsBuffer` alimenteaza inspectia istoricului recent.
- `WorldMappingSemanticIndex` ajuta la debugging pentru mapping semantic, nu la gameplay direct.
- `MappingWandService` este puntea dintre actiunea de mapping si audit.
- `AuditReport` este obiectul simplu pentru mesajele de audit.

## Cum se citeste

1. Incepe cu `DebugDumpService`.
2. Continua cu `RecentEventsBuffer`.
3. Treci la `WorldMappingSemanticIndex`.
4. Foloseste `MappingWandService` pentru traseul de authoring.
5. Foloseste `OpenAIDebugSnapshot` cand investighezi starea AI.

## Nota

Daca vrei doar traseul intre module si pachete, foloseste harta de pachete. Daca vrei relatiile dintre clasele de debug, acesta este documentul potrivit.
