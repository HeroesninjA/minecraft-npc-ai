# Households persistente

Status: contract de persistenta verificat in cod.
Actualizat: 2026-07-15.

Household-ul este rezultatul persistent al alocarii si spawn-ului. Nu este in prezent sursa de decizie pentru rutina sau simulare.

## Tabele

- `households`: ID, familie, home place, owner key, capacitate, numar rezidenti, hash plan, sursa si timestamp-uri;
- `household_residents`: household/resident key, NPC, source key, rol relational, place/node pentru home/work/social, status si timestamp-uri;
- stergerea household-ului elimina rezidentii prin foreign key;
- stergerea NPC-ului elimina randul sau de rezident.

## Scriere

- `NpcSpawnOrchestrator` salveaza household-ul dupa rezultatele reusite ale spawn planului;
- upsert-ul inlocuieste household-ul conflictual pentru acelasi home place;
- un NPC sau source key mutat este eliminat din vechiul household inainte de upsert;
- rezidentii care nu mai apar in plan sunt stersi;
- `resident_count` este recalculabil si poate fi reparat.

## Migrare si operare

- backfill din `npc_world_bindings` cere `home_place_id`;
- backfill-ul din metadata accepta numai NPC-uri existente si un home place nenul;
- comenzile suporta list/get/residents, dry-run/apply pentru migration si repair de duplicate;
- limitarile si mesajele de audit sunt operationale, nu reguli ale motorului de rutina.

## Limite

- `family_id` nu inlocuieste graful `npc_family`;
- `HouseholdPersistenceService` este consumat de spawn si comenzi, nu de `RoutineEngine`;
- household-ul nu coordoneaza orele, interactiunile sau economia rezidentilor;
- `max_residents` si `resident_count` sunt date persistente, nu un scheduler de populatie.

## Legaturi

- `architecture/generare-populatie-narativa.md`
- `architecture/npc-world-bindings.md`
- `architecture/harta-clase-spawn.md`
- `architecture/comportament-natural-npc-rutine-alocari.md`
