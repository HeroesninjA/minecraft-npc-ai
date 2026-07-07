# Plan de Testare Remota AINPC

## Cum functioneaza colaborarea

| Ce fac EU (remote) | Ce faci TU (in joc) |
|--------------------|---------------------|
| Pregatesc serverul (config, resets) | Te conectezi pe server |
| Rulez comenzi RCON pentru setup/stare | Executi actiunile din joc |
| Verific log-uri si output comenzi | Confirmi vizual (GUI, NPC, chat) |
| Analizez rezultate si documentez pas/fail | Raportezi ce vezi pe ecran |
| Resetez starea intre teste | Te misti la coordonate |

### Ciclu tipic de test

```
EU: RCON -> /ainpc quest accept Q01
EU: RCON -> /quest track Q01
TU: Mergi la coordonatele obiectivului (it spun eu unde)
EU: RCON -> /quest log (verific progres)
TU: Interactionezi cu NPC-ul / block-ul
EU: RCON -> /quest log (confirm completare)
TU: Confirmi ca ai primit recompensa
EU: Marchez testul ca PASS/FAIL
```

---

## Faza 1: Smoke Tests (A01-A05) — Prima sesiune

**Obiectiv:** Verificam ca serverul si pluginul sunt functionali

| Test | EU (RCON) | TU (in joc) | Confirmare |
|------|-----------|-------------|------------|
| A01 | `grep "Done ("` in log | — | Timp < 30s |
| A02 | `/ainpc health` | — | Feature Packs >= 1 |
| A03 | `/ainpc debug` | — | OpenAI "disponibil" |
| A04 | `/ainpc list` | — | NPC-uri listate |
| A05 | `/ainpc help` | — | Comenzi afisate fara erori |

**Durata estimata:** ~5 minute. Nu necesita jucator in joc.

---

## Faza 2: NPC Tests (B01-B08) — O sesiune

**Obiectiv:** Verificam ca NPC-urile functioneaza corect

| Test | Pregatire EU | Actiune TU | Verificare EU |
|------|-------------|------------|---------------|
| B01 | RCON: setez permisiuni | `/ainpc create` | RCON: NPC listat in DB |
| B02 | RCON: verific NPC existent | Click dreapta pe NPC | RCON: dialog session in log |
| B03 | — | Scrii mesaj langa NPC | RCON: ChatGPT raspuns in log |
| B04 | — | `/ainpc info` (target NPC) | RCON: output comanda |
| B05 | — | `/ainpc delete` | RCON: NPC sters din DB |
| B06 | RCON: resetez memoria NPC | Dialog -> reconectare -> dialog | RCON: NPC recunoaste jucatorul |
| B07 | — | `/ainpc mood <npc>` | RCON: emotia curenta |
| B08 | RCON: setez wand | Folosesti wand pe blocuri | RCON: binding creat |

---

## Faza 3: Quest Tests (C01-C12) — 2-3 sesiuni

**Obiectiv:** Verificam toate tipurile de obiective quest

**Pregatire:** Satul trebuie sa fie mapat cu regiuni, places si nodes. Daca nu e, facem mai intai un setup.

| Test | Tip obiectiv | Actiunea ta |
|------|-------------|-------------|
| C01 | talk_to_npc | `/quest accept <id>`, apoi vorbesti cu NPC-ul |
| C02 | collect_item | Colectezi itemele cerute |
| C03 | visit_region | Te duci in coordonatele regiunii |
| C04 | visit_place | Intri in place |
| C05 | inspect_node | Click pe nod |
| C06 | kill_mob | Kill X mobi |
| C07 | deliver_to_npc | Dai item-ul la NPC |
| C08 | craft_item | Craftuiesti itemul |
| C09 | break/place block | Spargi / pui un block |
| C10 | Multi-stagiu | Parcurgi tot questul pas cu pas |
| C11 | Actor NPC | Observi NPC-ul care apare |
| C12 | Abandon | `/quest abandon <id>` |

**La fiecare test:** Eu verific cu `/quest log` progresul si confirm completarea.

---

## Faza 4: GUI Tests (D01-D06) — O sesiune

| Test | Ce faci | Ce verific |
|------|---------|------------|
| D01 | `/npc gui` — cronometrezi | Timpul de deschidere (< 1s) |
| D02 | `/npc gui quest` | Questurile apar in lista |
| D03 | `/npc gui admin` | Ecranul admin e accesibil |
| D04 | `/npc gui world` | Regiunile apar |
| D05 | `/npc gui shop` | Ofertele magazinului apar |
| D06 | `/npc gui debug` | Status OpenAI, versiuni |

**Nota:** GUI-urile NU pot fi testate de bot. Doar tu poti confirma vizual.

---

## Faza 5: Restul testelor (E-M)

| Faza | Teste | Metoda |
|------|-------|--------|
| 5a | E01-E08 (Mapping) | Wand tools + comenzi |
| 5b | F01-F06 (Economy) | Comenzi economice |
| 5c | G01-G04 (AI) | Dialog cu NPC + verificare log |
| 5d | H01-H05 (Progression) | Comenzi progresie |
| 5e | I01-I04 (Performance) | Spark profile |
| 5f | J01-J06 (Edge Cases) | Scenarii limita |
| 5g | K01-K04 (Story) | Comenzi story |
| 5h | L01-L03 (MCP) | Status MCP |
| 5i | M01-M04 (Config) | Schimb config + test |

---

## Structura raport de test

```
## C01 - talk_to_npc
Status: PASS
Durata: 45s
Note: Quest acceptat, NPC dialog functioneaza, recompensa primita
Bug: None
```

---

## Lista Completa de Scenarii

### A. Smoke Tests
| ID | Scenariu |
|----|----------|
| A01 | Serverul porneste fara erori (Done < 30s) |
| A02 | Pluginul se incarca (Feature Packs >= 1) |
| A03 | OpenAI conectat si model disponibil |
| A04 | NPC-urile din baza de date sunt incarcate |
| A05 | Comenzile de baza nu dau erori |

### B. NPC System
| ID | Scenariu |
|----|----------|
| B01 | Creaza NPC cu `/ainpc create` si apare in lume |
| B02 | Click dreapta pe NPC deschide dialogul |
| B03 | NPC raspunde la mesaj in chat |
| B04 | NPC are nume, trasaturi si profesie |
| B05 | NPC dispare cand e sters cu `/ainpc delete` |
| B06 | NPC are memorie (isi aminteste dialogul anterior) |
| B07 | NPC are emotii (schimba emotia la interactiune) |
| B08 | NPC poate fi binduit la o locatie (world binding) |

### C. Quest System
| ID | Scenariu |
|----|----------|
| C01 | talk_to_npc — accepta quest, vorbeste cu NPC, completeaza |
| C02 | collect_item — accepta, colecteaza iteme, preda |
| C03 | visit_region — accepta, intra in regiune, completeaza |
| C04 | visit_place — accepta, intra in place, completeaza |
| C05 | inspect_node — accepta, merge la node, inspecteaza |
| C06 | kill_mob — accepta, kill mobi, completeaza |
| C07 | deliver_to_npc — accepta, duce item la NPC |
| C08 | craft_item — accepta, crafting, completeaza |
| C09 | break_block / place_block — accepta, modifica lumea |
| C10 | Quest cu mai multe stagii — parcurge toate etapele |
| C11 | Quest cu actor NPC — NPC-ul apare si interactioneaza |
| C12 | Abandon quest / fail quest |

### D. GUI
| ID | Scenariu |
|----|----------|
| D01 | `/ainpc gui` se deschide in < 1s |
| D02 | `/ainpc gui quest` afiseaza questurile active |
| D03 | `/ainpc gui admin` — ecran admin accesibil |
| D04 | `/ainpc gui world` — afiseaza regiunile/lumea |
| D05 | `/ainpc gui shop` — magazinul NPC-ului |
| D06 | `/ainpc gui debug` — status OpenAI, versiuni |

### E. World Mapping
| ID | Scenariu |
|----|----------|
| E01 | Creaza regiune cu wand (pos1/pos2) |
| E02 | Creaza place in interiorul regiunii |
| E03 | Creaza node in interiorul place-ului |
| E04 | Editeaza regiune existenta (modifica coordonate) |
| E05 | Sterge regiune si verifica ca places/nodes se sterg |
| E06 | Auto-indexare: findRegion la coordonate cunoscute |
| E07 | Scanare sat vanilla si import semantic |
| E08 | Plan patch pentru sat si aplicare |

### F. Economy & Shops
| ID | Scenariu |
|----|----------|
| F01 | `/ainpc economy balance` — balanta corecta (0) |
| F02 | `/ainpc economy set` + `balance` — setare balanta |
| F03 | `/ainpc economy pay` — transfer intre jucatori |
| F04 | `/ainpc economy top` — clasament |
| F05 | Deschide shop NPC si verifica ofertele |
| F06 | Cumpara din shop — itemele ajung in inventar |

### G. AI / OpenAI
| ID | Scenariu |
|----|----------|
| G01 | NPC raspunde coerent la salut (chat) |
| G02 | NPC include contextul lumii in raspuns |
| G03 | OpenAI offline fallback — dezactiveaza, NPC da fallback |
| G04 | Diagnostic OpenAI — `/ainpc debug` arata statusul |

### H. Progression
| ID | Scenariu |
|----|----------|
| H01 | `/ainpc progression` — afiseaza progresul jucatorului |
| H02 | Completeaza quest si verifica XP castigat |
| H03 | `/ainpc progression definitions` — listeaza definitiile |
| H04 | Skill-uri — verifica nivel dupa completare questuri |
| H05 | `/ainpc progression top` — leaderboard |

### I. Performance & Stress
| ID | Scenariu |
|----|----------|
| I01 | `/ainpc gui` se deschide in < 1s (masurat cu spark) |
| I02 | `/spark tps` — TPS peste 19.0 |
| I03 | 10 boti conectati simultan — TPS nu scade sub 18 |
| I04 | Memoria nu creste necontrolat (heap dupa 1h) |

### J. Edge Cases
| ID | Scenariu |
|----|----------|
| J01 | Jucatorul se deconecteaza in timpul dialogului |
| J02 | Plugin reload in timpul unui quest activ |
| J03 | Creare NPC cu nume gol / invalid |
| J04 | Acceptare quest deja activ |
| J05 | Comanda inexistenta — mesaj de eroare prietenos |
| J06 | Jucator fara permisiuni — comanda respinsa |

### K. Story System
| ID | Scenariu |
|----|----------|
| K01 | `/ainpc story context` — afiseaza contextul curent |
| K02 | Completeaza quest si verifica story event creat |
| K03 | Story state al regiunii — se modifica la completare |
| K04 | Story mode — verifica modul curent al regiunii |

### L. MCP Integration
| ID | Scenariu |
|----|----------|
| L01 | `/ainpc gui mcp` — status MCP |
| L02 | MCP health check — serverul MCP raspunde |
| L03 | Runtime snapshot — snapshot disponibil |

### M. Configuration & Feature Flags
| ID | Scenariu |
|----|----------|
| M01 | `features.gui: false` — `/ainpc gui` e respins |
| M02 | `features.quest: false` — `/ainpc quest` e respins |
| M03 | `features.story: false` — `/ainpc story` e respins |
| M04 | `demo.enabled: false` — continutul demo dispare |

---

## Statistici

| Categorie | Nr. teste | Automatizabil |
|-----------|-----------|---------------|
| A. Smoke | 5 | 100% (RCON + bot) |
| B. NPC | 8 | 100% (bot) |
| C. Quest | 12 | 100% (bot) |
| D. GUI | 6 | 50% (bot comenzi, manual cronometru) |
| E. Mapping | 8 | 100% (bot) |
| F. Economy | 6 | 100% (bot) |
| G. AI | 4 | 100% (bot chat) |
| H. Progression | 5 | 100% (bot) |
| I. Performance | 4 | 100% (bot + spark) |
| J. Edge Cases | 6 | 100% (bot) |
| K. Story | 4 | 100% (bot) |
| L. MCP | 3 | 100% (bot + RCON) |
| M. Config | 4 | 100% (RCON + bot) |
| **Total** | **75** | **~95%** |
