# D1: Verificari pe Server Paper

Pentru T008, T009, T010 — rulati pe serverul Paper.

## T008: Verificare incarcare plugin

```powershell
# 1. Copiati JAR-urile in plugins/
Copy-Item ainpc-core-plugin/build/libs/ainpc-core-plugin-1.0.0.jar $ServerDir/plugins/
Copy-Item ainpc-scenario-medieval/build/libs/ainpc-scenario-medieval-1.0.0.jar $ServerDir/plugins/
Copy-Item ainpc-api/build/libs/ainpc-api-1.0.0.jar $ServerDir/plugins/

# 2. Porniti serverul, apoi in consola/joc:
/plugins              # AINPC si addonul medieval apar ca enabled
/ainpc                # Raspunde cu lista de comenzi
/ainpc audit db       # Fara erori critice
```

**Gate:** Plugin incarcat, /ainpc raspunde, audit db fara erori.

## T009: Config quest-uri

Dupa primul start al serverului, verificati:

```
plugins/AINPC/quests.yml
```

Acest fisier ar trebui sa contina definitii din addonul medieval:
- `village_contracts`
- `npc_duties`
- `local_bounties`

**Gate:** quests.yml exista si contine cel putin o definitie.

## T010: Config rutina si simulare

IMPORTANT: In config.yml implicit, `routine: false` si `simulation: false`. Pentru demo, **editati manual** `plugins/AINPC/config.yml`:

```yaml
features:
  routine: true
  simulation: true
```

Apoi:
```
/ainpc reload
/ainpc routine status nearest    # Trebuie sa raspunda (chiar daca nu exista NPC)
```

**Gate:** Configurarea corecta, comanda raspunde fara stacktrace.
