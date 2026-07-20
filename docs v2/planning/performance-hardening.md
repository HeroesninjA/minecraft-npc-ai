# Roadmap de performance hardening

Status: roadmap activ compatibil, neimplementat complet.
Actualizat: 2026-07-18.

Nu exista un baseline masurat si versionat pentru tick time, latenta comenzilor, DB sau debug export. Prin urmare, aceasta pagina descrie riscuri si criterii, nu rezultate de benchmark.

## Riscuri confirmate structural

- `/ainpc debugdump all|npc` ruleaza sincron din comanda admin;
- tail-ul de log este bounded la 512 KiB si 250 de linii; snapshot-urile de domeniu raman construite sincron;
- exporturile pot serializa multe randuri si fisiere pe acelasi apel;
- scanarea vanilla este incrementala si bugetata pe main thread, dar accesul individual la bloc/chunk poate ramane costisitor; fixture-ul runtime are operatii sincrone;
- backend-ul DB curent serializeaza accesul printr-o singura conexiune;
- task-urile periodice, reload-ul, AI si mapping-ul pot concura pentru timp pe server.

Prezenta unui risc in cod nu demonstreaza o regresie masurata. Profilarea trebuie facuta pe workload reproductibil.

## Masuratori propuse

- MSPT/TPS si spike-uri pe tick;
- p50/p95/p99 pentru comenzi admin si actiuni de player;
- durata si marimea debugdump-ului pe seturi mici/medii/mari;
- timp si randuri pentru operatiile DB critice;
- memorie si allocari pentru snapshot-uri si serializare;
- startup, reload, save si restart;
- latenta AI separata de timpul executorului determinist.

## Directii compatibile

- [x] tail bounded fara incarcarea intregului log;
- snapshot/export in etape, cu limite si executie sigura in afara tick-ului;
- [x] retentie si quota pentru directoarele managed de debug dump;
- query-uri paginate si timeout-uri explicite;
- [x] bugete comune declarate pentru task-urile recurente, cu serii fixe per scheduler; stagger-ul ramane de evaluat;
- [x] coada comuna pentru scanarea vanilla, cu buget configurabil de blocuri per tick si fara acces Bukkit off-thread;
- cache invalidation masurabila;
- [x] metrici si health signals bounded cu cardinalitate controlata pentru tick, DB, comenzi, schedulere, AI si export.

## Criterii de acceptare

- fixture si workload versionate;
- baseline inainte/dupa pe acelasi mediu;
- bugete declarate si gate automat unde este stabil;
- lipsa accesului nesigur la Bukkit API din thread-uri async;
- fallback si anulare pentru operatii lente;
- raport care separa masurarea de estimare.

## Legaturi

- `operations/observability-and-logs.md`
- `operations/debugging-si-testare.md`
- `architecture/simulation-service.md`
