# Analiză Îmbunătățiri Viitoare AINPC

**Data:** 2026-07-07
**Stadiu Curent:** ~96%

Acest document analizează ce îmbunătățiri mai pot fi aduse proiectului, grupate pe categorii: feature-uri noi, optimizări arhitecturale, performanță, testare, DevOps și conținut.

---

## 1. Feature-uri Noi cu Impact Mediu-Mare

### 1.1 NPC Relationship GUI (🟢, ~1 zi)
**Stare:** RelationshipService există, dar nu există GUI dedicat.
**Implementare:** Ecran GUI nou care afișează:
- Lista NPC-urilor cu numărul de relații
- Click pe NPC → detalii relații (top parteneri, tip relație, scor)
- Filtrare după tip (friend/rival/acquaintance)
- Grafic simplu ASCII în chat cu `/ainpc relationship graph`

### 1.2 Story Authoring GUI (🟢, ~2 zile)
**Stare:** StoryAuthoringService + comenzi există, dar nu există GUI.
**Implementare:** 
- Ecran cu listă regiuni/place-uri
- Selectare template eveniment + preview descriere
- Buton "Aplică" + câmpuri pentru customizare titlu/descriere
- Istoric evenimente recente în același ecran

### 1.3 Banking/Investment Mechanics (🟢, ~2 zile)
**Stare:** EconomyService (jucători) + NpcEconomyService (NPC) există.
**Implementare:**
- Dobândă la solduri jucători (configurabil %)
- Investiții în proiecte ale satului (costă, produc venit pasiv)
- Împrumuturi de la NPC (cu dobândă)
- Comenzi: `/ainpc economy interest`, `/ainpc economy invest`

### 1.4 Building Template Auto-Placement (🟢, ~2 zile)
**Stare:** BuildingTemplateRegistry + SettlementConfigLoader există.
**Implementare:**
- Scanare regiune pentru spațiu liber
- Plasare automată clădiri template (casă, fierărie, tavernă)
- Update mapping cu noile place-uri/noduri
- Comandă: `/ainpc world building auto-place <templateId> <regionId>`

### 1.5 NPC Group synchronized movement (🟡, ~2 zile)
**Stare:** Group activities detection există, dar NPC-urile nu se mișcă sincron.
**Implementare:**
- Override `stableRoutineOffset` pentru NPC-uri din aceeași adunare
- NPC-urile pleacă și ajung la locația socială în același timp (±5 secunde)
- Efect de "grup" vizibil în lume

### 1.6 NPC Memory Visualization (🟢, ~1 zi)
**Stare:** MemoryManager cu decay per tip există.
**Implementare:**
- Comandă `/ainpc info <npc> memories [player]` — vezi amintirile unui NPC
- GUI cu listă amintiri + impact emoțional + importanță + expirare
- Ștergere manuală amintiri (admin)

### 1.7 Dynamic Shop Prices (🟡, ~2 zile)
**Stare:** ShopService + NpcEconomyService există.
**Implementare:**
- Prețurile magazinelor se ajustează în funcție de balanța NPC-ului
- NPC bogat → prețuri mai mari
- NPC sărac → prețuri mai mici
- Ciclu economic săptămânal în joc

---

## 2. Optimizări Arhitecturale

### 2.1 Service Layer Abstraction (🟡, ~3 zile)
**Problemă:** Majoritatea serviciilor sunt clase concrete, fără interfețe.
**Impact:** Dificil de testat, greu de înlocuit implementări.
**Soluție:**
- Definire interfețe pentru: RelationshipService, NpcEconomyService, StoryAuthoringService, RoutineCoordinator
- Implementări curente → implementează interfețele
- Teste cu mock-uri

### 2.2 Dependency Injection (🟡, ~4 zile)
**Problemă:** Toate serviciile sunt create manual în `AINPCPlugin.onEnable()`.
**Impact:** ~40 de linii de inițializare manuală, ordinea contează, greu de întreținut.
**Soluție:**
- Container DI simplu (sau Koin) 
- Serviciile se declară singleton + dependențe automate
- Inițializare într-o singură linie

### 2.3 Event Bus pentru deschidere arhitecturală (🟡, ~3 zile)
**Problemă:** Addon-urile pot fi notificate doar prin hook-urile AINPCAddon.
**Impact:** Limitativ pentru addon-uri care vor să asculte evenimente specifice.
**Soluție:**
- Event bus intern cu publish/subscribe
- Evenimente: StoryEventCreated, RelationshipChanged, NpcStateChanged, SalaryPaid, SeasonChanged, RoutineTick
- Addon-urile se abonează la evenimente specifice
- Runtime-ul publică evenimente fără să știe cine ascultă

### 2.4 Separare API public de implementare (🟢, ~2 zile)
**Problemă:** Clasele de implementare sunt accesibile direct din API.
**Impact:** Addon-urile pot depinde de implementări, nu doar de API.
**Soluție:**
- Mutat interfețele expuse în `ainpc-api`
- Implementările rămân în `ainpc-core-plugin`
- Acces prin `AINPCPlatformApi` sau serviciu central

---

## 3. Performanță

### 3.1 NPC Tick Profiling (🟢, ~1 zi)
**Problemă:** Nu există metrici de performanță pentru tick-urile NPC.
**Soluție:**
- Adăugat timer în jurul `RoutineCoordinator.tick()`
- Raportat în `/ainpc health` și debugdump
- Prag de warning la >50ms per tick

### 3.2 Database Connection Pool Tuning (🟢, ~0.5 zile)
**Problemă:** SQLite folosit cu un singur `connection` + lock.
**Soluție:**
- Măsurat timpi de execuție query-uri
- Adăugat caching pentru query-uri frecvente (ex: NPC count, relationship count)
- Configurabil: `database.query_cache_ttl`

### 3.3 Lazy Loading pentru servicii grele (🟢, ~1 zi)
**Problemă:** Toate serviciile sunt inițializate la startup.
**Soluție:**
- Serviciile grele (AI, MCP) → lazy loading
- Se inițializează doar când sunt folosite prima dată
- Startup mai rapid

### 3.4 NPC Batch Processing (🟢, ~1 zi)
**Problemă:** `RoutineCoordinator.tick()` iterează toate NPC-urile secvențial.
**Soluție:**
- NPC-urile procesate în loturi de 10
- Între loturi, `yield()` pentru a nu bloca server thread
- Configurabil: `routine.batch_size: 10`

---

## 4. Testare

### 4.1 Integration Tests pentru RelationshipService (🟡, ~1 zi)
**Ce:** Test cu SQLite în-memory care verifică:
- Creare relație între 2 NPC-uri
- Decay după timestamp
- Persistare și citire din DB
- Interacțiuni multiple

### 4.2 Integration Tests pentru NpcEconomyService (🟡, ~1 zi)
**Ce:** Test care verifică:
- Depunere/retragere balanță
- Plată salarii
- Persistare DB
- Limită MAX_BALANCE

### 4.3 Integration Tests pentru StoryAuthoringService (🟡, ~1 zi)
**Ce:** Test care verifică:
- Creare eveniment story
- Aplicare template
- Listare evenimente recente
- Reacția NPC (mock)

### 4.4 Integration Tests pentru McpCommandQueue (🟡, ~1 zi)
**Ce:** Test care verifică:
- Scriere fișier comandă
- Citire și execuție
- Scriere fișier rezultat
- Timeout handling

### 4.5 Smoke Tests pentru servicii noi (🟡, ~2 zile)
**Ce:** Test care verifică integrarea completă:
- RelationshipService + RoutineCoordinator (adunări → relații)
- StoryAuthoringService + StoryReactionService (eveniment → reacție)
- NpcEconomyService + RoutineCoordinator (muncă → salariu)

---

## 5. DevOps și Tooling

### 5.1 Script de benchmark (🟢, ~1 zi)
**Ce:** Script care măsoară:
- Timp de tick rutină pentru X NPC-uri
- Timp de query DB
- Memory usage
- Salvare rezultate în fișier

### 5.2 GitHub Actions CI (🟡, ~1 zi)
**Ce:** Pipeline CI care:
- Rulează `gradlew build` la fiecare push
- Rulează toate testele
- Verifică 0 warnings
- Publich artifact JAR

### 5.3 Docker Compose pentru development (🟢, ~1 zi)
**Ce:** Docker Compose care pornește:
- Server Paper cu pluginul pre-instalat
- MCP sidecar
- Volum pentru date persistente
- Port mapping pentru RCON

### 5.4 Script de generare release notes (🟢, ~0.5 zile)
**Ce:** Script care:
- Citește CHANGELOG.md
- Extrage entry-urile nerelease-uite
- Generează release notes format markdown
- Update version în gradle.properties

---

## 6. Conținut și Gameplay

### 6.1 Verificare pachete festival/wilderness (🟡, ~1 zi)
**Stare:** Pachetele există dar nu au fost testate complet.
**Acțiuni:**
- Încărcare pe server Paper
- Testare flow-uri quest
- Verificare dialoguri și recompense
- Raport bug-uri

### 6.2 NPC quest offering GUI (🟢, ~1 zi)
**Stare:** Quest-urile se oferă prin dialog text.
**Îmbunătățire:**
- GUI care arată quest-urile disponibile de la NPC
- Nume, descriere, recompense
- Buton "Acceptă" / "Refuză"

### 6.3 Tutorial in-game pentru admini (🟢, ~2 zile)
**Ce:** 
- Tutorial interactiv pentru setup inițial
- `/ainpc tutorial setup` — ghid pas-cu-pas
- Creare lume demo, spawn NPC, verificare rutine

### 6.4 Random World Events (🟡, ~3 zile)
**Ce:** Evenimente aleatoare care se declanșează în lume:
- Festivaluri automate (pe baza anotimpului)
- Atacuri ale jefuitorilor (noaptea)
- Vizite ale negustorilor ambulanți
- Folosește StoryAuthoringService + StoryReactionService

---

## 7. Refactorizări Tehnice

### 7.1 Reducere dimensiune AINPCCommand.kt (🟡, ~2 zile)
**Problemă:** 234KB, ~4,700 linii. Cel mai mare fișier.
**Soluție:**
- Extragere handler-e în fișiere separate
- Deja parțial făcut (AINPCCommandMisc, Quest, Story, Economy, World, etc.)
- Mai sunt de extras: `handleCreate`, `handleDelete`, `handleInfo`, audit

### 7.2 Unificare constant strings (🟢, ~1 zi)
**Problemă:** String-uri de configurare duplicate prin cod (ex: `"routine.enabled"`).
**Soluție:**
- Obiect central `ConfigKeys` cu constante
- Toate referințele la config → `ConfigKeys.ROUTINE_ENABLED`

### 7.3 Standardizare logging (🟢, ~0.5 zile)
**Problemă:** Unele servicii folosesc `plugin.logger`, altele `Logger.getLogger()`.
**Soluție:**
- Toate serviciile → același pattern de logging
- Niveluri consistente: INFO la startup, WARNING la erori, FINE la debug

### 7.4 Cod冗余: NPCManager duplicate methods (🟢, ~0.5 zile)
**Problemă:** `getNPCByUuid` și `getNPCByUUID` (același).
**Soluție:** Păstrare unul, deprecate celălalt.

---

## 8. Roadmap Estimat

| Fază | Priorități | Efort total |
|------|-----------|-------------|
| **Faza 1** (quick wins) | Relationship GUI, Story authoring GUI, NPC Memory vizualization, Tutorial in-game | ~5 zile |
| **Faza 2** (feature) | Banking/investment, Building auto-placement, Dynamic shop prices, NPC synchronized movement | ~8 zile |
| **Faza 3** (arhitectură) | Service layer abstraction, Dependency injection, Event bus, API separation | ~12 zile |
| **Faza 4** (performanță) | NPC profiling, Batch processing, DB caching, Lazy loading | ~3 zile |
| **Faza 5** (testare) | Integration tests (5 servicii), Smoke tests, CI pipeline | ~7 zile |
| **Faza 6** (conținut) | Random world events, Pack verification, Quest offering GUI | ~5 zile |

**Total efort estimat rămas: ~40 de zile**

---

## 9. Matrice Impact/Efort

| Features | Efort | Impact |
|----------|-------|--------|
| Relationship GUI | 1 zi | Vizibilitate imediată |
| Story authoring GUI | 2 zile | UX authoring |
| Banking/investment | 2 zile | Economie profundă |
| Building auto-placement | 2 zile | Generare lume |
| NPC synchronized movement | 2 zile | NPC-uri realiste |
| Dynamic shop prices | 2 zile | Economie dinamică |
| Integration tests | 5 zile | Stabilitate |
| CI pipeline | 1 zi | Calitate cod |
| Event bus | 3 zile | Arhitectură extensibilă |
| Random world events | 3 zile | Lume vie |
| Tutorial admin | 2 zile | UX admin |
| GitHub Actions | 1 zi | DevOps |
