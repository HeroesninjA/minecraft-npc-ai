# Tutorial: GUI Quest Creator & Mapping Creator

## Cuprins

1. [Accesare GUI](#1-accesare-gui)
2. [QuickQuest Wizard — Creare Quest Rapid](#2-quickquest-wizard--creare-quest-rapid)
3. [Quest Creator Avansat](#3-quest-creator-avansat)
4. [Quest Explorer — Submeniu Complet](#4-quest-explorer--submeniu-complet)
5. [Mapping Creator](#5-mapping-creator)
6. [Wand — Creare Manuală Mapping](#6-wand--creare-manuala-mapping)
7. [Comenzi Rapide Utile](#7-comenzi-rapide-utile)

---

## 1. Accesare GUI

| Comandă | Destinație | Permisiune |
|---------|-----------|------------|
| `/ainpc gui` | Main Hub → buton **Creator Tools** | `ainpc.admin` |
| `/ainpc gui creator` | Direct CreatorHub | `ainpc.creator` |
| `/ainpc quest gui` | Quest Log (progresii jucător) | `ainpc.quest` |

**CreatorHub** conține:
- **Quick Quest Wizard** — creare rapidă în pași (recomandat pentru quest-uri simple)
- **Quest Creator (avansat)** — formular complet cu stage-uri, dialoguri, story events
- **Quest Explorer** — submeniu cu definiții, test, editor, ancore, authoring
- **Creator Mapping** — creare regiuni, place-uri, noduri manual sau demo
- **Demo mapping** — creează mapping automat la poziția ta
- **Salveaza mapping** — persistă modificările în config.yml

---

## 2. QuickQuest Wizard — Creare Quest Rapid

**Acces:** `/ainpc gui creator` → buton **Quick Quest Wizard**

### Pas 0: Alege NPC Giver

Alege profesia NPC-ului care dă questul. Click pe o opțiune:

- `profession:blacksmith` (Fierar)
- `profession:guard` (Gardă)
- `profession:innkeeper` (Hangiu)
- `profession:farmer` (Fermier)
- `profession:merchant` (Negustor)
- `profession:healer` (Vindecător)

### Pas 1: Numele Questului

Scrie numele în chat (click pe carte). Exemple: `Ajutor pentru Fierar`, `Blestemul Castelului`

### Pas 2: Tip Obiectiv + Număr Obiective

Alege tipul obiectivului:
- `talk_to_npc` — vorbește cu un NPC
- `collect_item` — colectează iteme
- `visit_place` — vizitează un loc
- `inspect_node` — inspectează un nod
- `kill_mob` — omoară creaturi
- `craft_item` — craftează iteme
- `place_block` — plasează blocuri
- `break_block` — sparge blocuri

Apoi alege câte obiective (1-5). Fiecare obiectiv va avea propriul stage.

### Pas 3: Țintele

Pentru fiecare obiectiv, click pe carte și scrie ținta:

| Tip Obiectiv | Sugestii Țintă |
|-------------|----------------|
| `talk_to_npc` | `profession:garda`, `profession:fermier`, `profession:blacksmith` |
| `collect_item` | `OAK_LOG`, `EMERALD`, `IRON_INGOT` |
| `visit_place` | `demo_sat:piata`, `castel:curte_castel` |
| `inspect_node` | `castel:curte_castel:cufar` |
| `kill_mob` | `ZOMBIE`, `SKELETON`, `SPIDER`, `WITCH` |
| `craft_item` | `IRON_SWORD`, `TORCH`, `BOOK` |
| `place_block` | `OAK_PLANKS`, `STONE` |
| `break_block` | `COBBLESTONE`, `DEEPSLATE` |

### Pas 4: Recompensa

Alege recompensa:
- `EMERALD x5` — 5 smaralde
- `DIAMOND x1` — 1 diamant
- `IRON_SWORD x1` — 1 sabie de fier
- `BOOK x1` — 1 carte
- `XP 50` — 50 XP

### Pas 5: Finalizare

- **[Previzualizare YAML]** — vezi YAML-ul generat în chat
- **[Exporta YAML]** — salvează fișierul `quickquest_XXXX.yml` în folderul pluginului
- **[Renunta]** — anulează și revino la CreatorHub

### Exemplu: Quest "Ajutor pentru Fierar"

| Pas | Acțiune | Selecție |
|-----|---------|----------|
| 0 | Alege giver | `profession:blacksmith` |
| 1 | Nume quest | `Ajutor pentru Fierar` |
| 2 | Tip + count | `collect_item`, 2 obiective |
| 3 | Obiectiv 1 | `IRON_INGOT` |
| 3 | Obiectiv 2 | `OAK_PLANKS` |
| 4 | Recompensă | `IRON_SWORD x1` |
| 5 | Export | Click Exporta YAML |

**YAML generat:**
```yaml
scenarios:
  QQ_1234:
    name: "Ajutor pentru Fierar"
    base_type: QUEST
    mechanic: side_quests
    quest:
      code: "QQ_1234"
      giver_profession: "blacksmith"
      kind: "fetch"
      stages:
        STAGE_1:
          description: "Etapa 1."
          completion_mode: "all_objectives"
          next_stage: "STAGE_2"
          objectives: ["obj_1"]
        STAGE_2:
          description: "Etapa 2."
          completion_mode: "all_objectives"
          next_stage: "RETURN"
          objectives: ["obj_2"]
        RETURN:
          description: "Intoarce-te la giver."
          completion_mode: "manual_turn_in"
          objectives: ["return_to_giver"]
      objectives:
        obj_1:
          type: "collect_item"
          item: "IRON_INGOT"
          amount: 1
          phase: "STAGE_1"
          description: "Etapa 1: IRON_INGOT."
        obj_2:
          type: "collect_item"
          item: "OAK_PLANKS"
          amount: 1
          phase: "STAGE_2"
          description: "Etapa 2: OAK_PLANKS."
        return_to_giver:
          type: "talk_to_npc"
          item: "profession:blacksmith"
          amount: 1
          phase: "RETURN"
          description: "Intoarce-te la giver."
      rewards:
        reward:
          type: "item"
          item: "IRON_SWORD"
          amount: 1
          description: "Primesti IRON_SWORD x1."
```

---

## 3. Quest Creator Avansat

**Acces:** `/ainpc gui creator` → buton **Quest Creator (avansat)**

Formular complet cu 5 secțiuni.

### Secțiunea General (slots 9-17)

| Câmp | Descriere | Exemplu |
|------|----------|---------|
| **ID** | Cod unic | `CST02`, `Q99`, `DGN_CASTEL` |
| **Nume** | Titlu quest | `Blestemul Castelului Parasit` |
| **Mecanica** | Categorie runtime | `side_quests`, `main_quests` |
| **Tip** | Base type | `QUEST`, `BOUNTY`, `DUTY` |
| **NPC Giver** | Capturează NPC apropiat | Click buton lângă NPC |
| **Descriere** | Text descriptiv | Orice text |
| **Locatie NPC** | Capturează place curent | Click buton |

### Secțiunea Obiectiv (slots 19-22)

| Câmp | Descriere | Exemplu |
|------|----------|---------|
| **Tip obiectiv** | Acțiunea cerută | `kill_mob`, `visit_place` |
| **Tinta** | Ce/cine/unde | `ZOMBIE`, `castel:poarta_castel` |
| **Count** | Câte | `1`, `2`, `5`, `10` |
| **Dialog obiectiv** | Text afișat la activare | `Elimina zombi din zona 1.` |

### Secțiunea Stage-uri (slots 27-35) — **NOU**

| Buton | Funcție |
|-------|---------|
| **Stage ID** | Numele stage-ului (ex: `ZONE_1`, `BOSS`, `RETURN`) |
| **Stage nume** | Nume afișat (ex: `Zona 1`, `Vrajitoarea`, `Intoarcere`) |
| **Stage mode** | `all_objectives`, `manual_turn_in` |
| **+ Adauga Stage** | Adaugă un stage nou (max 10) |
| **← Prev Stage** | Navighează la stage-ul anterior |
| **Next Stage →** | Navighează la următorul stage |
| **Sterge Stage** | Șterge stage-ul curent |
| **Stage preset** | Aplică preset RETURN |

**Indicator:** `Stage 2/5` — arată poziția curentă.

### Secțiunea Recompensă (slots 37-41)

| Câmp | Descriere | Exemplu |
|------|----------|---------|
| **Tip recompensa** | `item`, `experience`, `story_event`, `reputation` |
| **Obiect** | Ce recompensă | `DIAMOND_SWORD`, `castle_cleansed` |
| **Cantitate** | Cât | `1`, `100` |

**Dacă tipul e `story_event`, apar câmpuri suplimentare (slots 32-35):**

| Câmp | Descriere | Exemplu |
|------|----------|---------|
| **Event key** | Cheia unică | `cst02_complete` |
| **Scope** | Domeniul | `region`, `place` |
| **Target** | Ținta | `current_region`, `anchor:obj_key` |
| **Titlu** | Titlu event | `Castelul a fost curatat` |

### Secțiunea Dialog (slots 40-44)

| Câmp | Descriere |
|------|----------|
| **Text** | Ce spune NPC-ul |
| **Tip dialog** | `npc_greeting`, `npc_accept`, `npc_progress`, `npc_complete` |
| **Vorbitor** | `npc`, `player`, `narrator` |

### Export

- **Exporta Draft JSON** — Salvează fișier JSON în `debug-dumps/quest-drafts/`
- **Testeaza in joc** — Rulează `/ainpc quest accept nearest`

---

## 4. Quest Explorer — Submeniu Complet

**Acces:** CreatorHub → **Quest Explorer**

| Submeniu | Ce face | Comandă rapidă |
|----------|---------|---------------|
| **Definiții** | Listează toate quest-urile disponibile | `/ainpc quest definitions` |
| **Test** | Comenzi rapide: accept, status, debug, reset, complete | — |
| **Editor** | Selectează definiție → vezi/editează detalii | — |
| **Authoring** | Context story + mapping → sugerează quest-uri | `/ainpc authoring` |
| **Quest Map** | Leagă obiective de locații în lume (ancore) | — |
| **Ancore** | Listează toate ancorele persistate | `/ainpc quest anchors all` |
| **Quest Admin** | Dashboard admin: statistici, duplicate, nerezolvate | `/ainpc gui admin` |

---

## 5. Mapping Creator

### 5a. Demo Mapping (Automat)

```bash
# Creează mapping automat la poziția ta
/ainpc world demo create

# Verifică ce s-a creat
/ainpc overview

# Salvează mapping-ul
/ainpc world save
```

**Ce creează demo-ul:**
- 1 regiune: `demo_sat` (tip Settlement)
- 4 case cu pat, intrare, spawnpoint
- 1 piață cu meeting_point, social, quest_board
- 1 fierărie cu workstation, work_anchor, inspect_node
- 1 fermă cu work_anchor, inspect_node
- 1 taverna cu entrance, social_anchor, dialog_anchor
- 1 altar cu ritual_circle, altar_node
- Total: ~25 noduri semantice

### 5b. Mapping Manual (Wand)

```bash
# Activează wand-ul
/ainpc wand

# Setează modul
/ainpc wand mode region
/ainpc wand mode place
/ainpc wand mode node

# Selectează zona cu click stânga/dreapta în joc
# Clic stânga = pos1, clic dreapta = pos2

# Verifică selecția
/ainpc wand status

# Creează din selecție
/ainpc map <descriere>

# Previzualizează
/ainpc map preview

# Deschide sau redeschide editorul pentru draftul curent
/ainpc map edit

# Confirmă
/ainpc map confirm

# Salvează
/ainpc world save
```

### 5c. Mapping asistat AI / intent

Fluxul AI/intent creeaza tot un draft de mapping si pastreaza aceeasi regula de siguranta: preview inainte de scriere, apoi confirmare explicita.

```bash
# Ajutor si exemple acceptate
/ainpc world create ai help

# Preview-only: creeaza draft, arata sumar si particule, dar nu deschide formularul
/ainpc world create ai preview region name=curte_castel type=castle size=48

# Deschide formularul potrivit pentru draftul curent
/ainpc map edit

# Aliasuri echivalente pentru editare
/ainpc map open
/ainpc map gui

# Confirma sau anuleaza draftul
/ainpc map confirm
/ainpc map cancel
```

Hinturi utile in descriere: `id=`, `name=`, `label=`, `type=`, `region=`, `place=`, `size=`, `radius=`.

Daca `id=` lipseste, `name=` este folosit ca ID local fallback. Foloseste `label=` cand vrei un nume afisat separat de ID.

### 5d. Comenzi Directe

```bash
# Creare regiune
/ainpc world region create <id> <nume> <tip> <minX> <minY> <minZ> <maxX> <maxY> <maxZ>
# Exemplu: /ainpc world region create castel "Castelul Parasit" castle -100 60 -100 100 80 100

# Creare place
/ainpc world place create <regionId> <id> <nume> <tip> <minX> <minY> <minZ> <maxX> <maxY> <maxZ>
# Exemplu: /ainpc world place create castel poarta "Poarta Castelului" castle_gate -10 60 -10 10 70 10

# Creare node
/ainpc world node create <regionId> <placeId> <id> <tip> <x> <y> <z> <radius>
# Exemplu: /ainpc world node create castel poarta intrare entrance 0 62 0 2.5

# Ștergere
/ainpc world region remove <id>
/ainpc world place remove <id>
/ainpc world node remove <id>
```

---

## 6. Wand — Creare Manuală Mapping

**Acces:** `/ainpc wand` sau `/ainpc gui creator` → buton **Creator Mapping**

### Moduri Wand

| Mod | Ce creează | Click Stânga | Click Dreapta |
|-----|-----------|-------------|---------------|
| `region` | Regiune | pos1 colț | pos2 colț opus |
| `place` | Place | pos1 colț | pos2 colț opus |
| `node` | Nod | punct | — |
| `npc_bind` | Legătură NPC | — | — |
| `quest_anchor` | Ancoră quest | — | — |

### Workflow

1. `./ainpc wand` — activează wand în modul PLACE
2. Click stânga = primul colț, click dreapta = colț opus
3. `./ainpc map Taverna din sat` — creează draft
4. `./ainpc map preview` — vezi particulele în joc
5. `./ainpc map edit` — ajustează draftul în editor dacă este nevoie
6. `./ainpc map confirm` — aplică în world mapping
7. `./ainpc world save` — salvează permanent

---

## 7. Comenzi Rapide Utile

### Vizualizare

| Comandă | Descriere |
|---------|-----------|
| `/ainpc overview` | Dashboard complet: NPC-uri + Questuri + Regiuni + Place-uri + Noduri |
| `/ainpc quest definitions` | Toate definițiile de progresie |
| `/ainpc quest import <file>.yml` | Validează și afișează un YAML din `packs/` |
| `/ainpc world regions` | Toate regiunile cu nume, tip, dimensiune |
| `/ainpc world places <regionId>` | Place-urile unei regiuni |

### Export/Import

| Comandă | Descriere |
|---------|-----------|
| `/ainpc world save` | Salvează mapping-ul curent în config.yml |
| `/ainpc quest quick-export <nume> <tip> <target> <reward>` | Export rapid din chat |
| `/ainpc quest import <file>.yml` | Importă din folderul packs/ |

### Testare

| Comandă | Descriere |
|---------|-----------|
| `/ainpc quest accept nearest` | Acceptă quest de la cel mai apropiat NPC |
| `/ainpc quest status nearest` | Status quest curent |
| `/ainpc quest progress tracked` | Progres quest urmărit |
| `/ainpc quest abandon nearest` | Abandonează quest |
| `/ainpc quest complete nearest` | Marchează quest ca finalizat (admin) |

### Verificare

| Comandă | Descriere |
|---------|-----------|
| `/ainpc audit npc` | Verifică NPC-uri pentru duplicate |
| `/ainpc audit world` | Verifică mapping pentru erori |
| `/ainpc debugdump all` | Export complet stare sistem |
| `/ainpc health` | Status rapid: plugin, DB, AI |

---

## Exemple Complete

### Exemplu 1: Quest simplu — Fetch

```
1. /ainpc gui creator → Quick Quest Wizard
2. Pas 0: profession:farmer
3. Pas 1: "Grau pentru Hambar"
4. Pas 2: collect_item, 1 obiectiv
5. Pas 3: WHEAT
6. Pas 4: EMERALD x5
7. Pas 5: Exporta YAML
```

### Exemplu 2: Quest multi-zone — Combat + Boss

```
1. /ainpc gui creator → Quest Creator (avansat)
2. General: ID=CST02, Nume=Blestemul Castelului, NPC=profession:blacksmith
3. Obiectiv: kill_mob, ZOMBIE, count=2
4. Stage-uri:
   - +Adauga x5 → editează ZONE_1..ZONE_5
   - +Adauga → BOSS, mode=all_objectives
   - +Adauga → RETURN, mode=manual_turn_in
   - Folosește ←Prev/Next→ pentru a edita fiecare
5. Recompensa: story_event, event_key=castle_cleansed, scope=region
6. Exporta Draft JSON
```

### Exemplu 3: Mapping complet

```bash
# 1. Creează demo
/ainpc world demo create

# 2. Verifică
/ainpc overview
# Output: 1 regiune, 8 places, 25 noduri

# 3. Adaugă place manual cu wand-ul
/ainpc wand mode place
# Click stânga + dreapta în joc
/ainpc map Zona secreta
/ainpc map confirm

# 4. Salvează
/ainpc world save

# 5. Importă YAML local
/ainpc quest import dagon_quest.yml
# Output: "Scenarii gasite: castel_dagon..."
/ainpc reload
```
