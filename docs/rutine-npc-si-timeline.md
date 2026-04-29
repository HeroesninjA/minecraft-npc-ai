# Rutine NPC si Timeline

Actualizat: 2026-04-29

Status curent:

- `RoutineEngine` exista initial in cod
- `RoutineService` ruleaza periodic prin scheduler
- rutina foloseste `homeAnchor`, `workAnchor` si `socialAnchor`
- comenzile disponibile sunt `/ainpc routine tick` si `/ainpc routine status [numeNpc]`
- timeline-ul complet ramane neimplementat si trebuie sa vina dupa rutine, household si quest hooks

## Decizie

Prima componenta care trebuie implementata este sistemul de rutine NPC, nu timeline-ul complet.

Timeline-ul devine util abia dupa ce NPC-urile au:

- casa
- loc de munca
- familie sau household
- program zilnic
- locatie previzibila
- stare persistenta
- interactiuni minime cu questurile

Fara aceste lucruri, timeline-ul ar produce doar evenimente izolate peste o lume care nu are inca viata stabila.

## Ordinea recomandata

```text
TimeService minim
-> RoutineEngine
-> RoutineAssignment
-> Household/Family binding
-> Quest hooks pe rutina
-> TimelineEngine
-> Timeline overrides pentru evenimente speciale
```

## 1. TimeService minim

`TimeService` trebuie sa fie o componenta mica la inceput. Rolul lui este sa traduca timpul Minecraft in faze utile pentru gameplay.

Responsabilitati:

- citeste timpul lumii Minecraft
- calculeaza faza zilei
- emite schimbari de faza
- ofera API simplu pentru alte servicii

Faze recomandate:

```text
DAWN
MORNING_WORK
MIDDAY
AFTERNOON_WORK
EVENING_HOME
NIGHT_SLEEP
```

API conceptual:

```java
interface TimeService {
    DayPhase currentPhase(World world);
    long currentDay(World world);
    boolean isNight(World world);
}
```

Aceasta componenta nu trebuie sa contina logica de NPC, quest sau familie. Ea doar spune ce moment al zilei este.

## 2. RoutineEngine

`RoutineEngine` este primul sistem mare care merita implementat.

Status implementare initiala:

- decide slotul curent `HOME`, `WORK`, `SOCIAL` sau `IDLE`
- foloseste timpul Minecraft al lumii in care se afla NPC-ul
- noaptea trimite NPC-ul la casa
- ziua trimite NPC-ul la munca daca exista `workAnchor`
- foloseste `socialAnchor` cand nevoia sociala este scazuta sau ca fallback
- seteaza `plannedRoutineActivity`, `currentGoal` si `NPCState`
- evita intreruperea starilor ocupate precum conversatie, combat, fuga sau quest

Rol:

- decide ce intentie are NPC-ul in faza curenta a zilei
- muta sau ghideaza NPC-ul spre locatie
- respecta casa, munca, familie si profesie
- evita ca NPC-urile sa stea permanent in acelasi loc

Rutine de baza:

```text
NIGHT_SLEEP     -> merge acasa / pat / interior casa
DAWN            -> se trezeste / iese din casa
MORNING_WORK    -> merge la locul de munca
MIDDAY          -> pauza / piata / fantana / masa
AFTERNOON_WORK  -> revine la munca
EVENING_HOME    -> revine acasa / sta cu familia
```

Rutina trebuie sa fie determinista, dar nu rigida. Este suficient sa existe mici variatii:

- alegere random intre cateva node-uri valide
- intarziere mica la schimbarea fazei
- fallback daca locatia nu exista
- evitarea teleportarii daca NPC-ul poate merge natural

Limitare MVP:

- implementarea actuala foloseste teleport controlat cand NPC-ul este prea departe de tinta
- pathfinding real sau pasi intermediari trebuie adaugati ulterior

## 3. RoutineAssignment

Rutina are nevoie de date clare pentru fiecare NPC.

Model minim recomandat:

```text
npc_id
home_place_id
work_place_id
family_id
household_id
profession
routine_profile
last_routine_state
last_known_place_id
```

`routine_profile` poate fi initial simplu:

```text
farmer
blacksmith
guard
merchant
child
elder
unemployed
```

Fiecare profil are o harta intre faza zilei si tipul de locatie dorita.

Exemplu:

```yaml
routine_profiles:
  blacksmith:
    NIGHT_SLEEP: home
    DAWN: home_exit
    MORNING_WORK: work
    MIDDAY: market_or_well
    AFTERNOON_WORK: work
    EVENING_HOME: home
```

## 4. Household si familie

Rutinele trebuie legate de sistemul de case si familii.

O cladire de locuit nu trebuie sa aiba doar `owner_npc_id`. Trebuie sa poata avea mai multi rezidenti.

Model recomandat:

```text
household_id
home_place_id
resident_npc_ids
family_id optional
capacity
bed_nodes
door_node
shared_storage_node optional
```

Reguli:

- o casa poate avea mai multi NPC
- un NPC are o casa principala
- o familie poate locui intr-un household
- NPC-urile din aceeasi familie pot avea rutine sincronizate seara
- copiii si batranii pot avea rutine diferite fata de adulti

Exemplu de familie:

```text
household: house_12
family: family_03
residents:
  - npc_matei, father, blacksmith
  - npc_ana, mother, baker
  - npc_ion, child, child
```

Comportament dorit:

- dimineata adultii merg la munca
- copilul poate sta langa casa, piata sau scoala daca exista
- seara familia revine in aceeasi casa
- noaptea NPC-urile raman in interiorul casei

## 5. Integrare cu mapping `region/place/node`

Rutinele trebuie sa consume mapping-ul existent.

Locatii utile:

```text
region: village
place: house
place: forge
place: farm
place: market
place: well
place: church
place: tavern
node: door
node: bed
node: workstation
node: counter
node: meeting_point
```

Regula:

- `place` spune unde merge NPC-ul
- `node` spune punctul exact din acel loc
- `region` spune zona logica in care se afla comunitatea

Exemplu:

```text
blacksmith at MORNING_WORK
-> work_place_id = forge_01
-> target node = workstation/anvil
```

## 6. Quest hooks peste rutine

Questurile nu trebuie sa controleze complet NPC-ul. Ele trebuie sa se conecteze peste rutine.

Exemple:

- un NPC poate oferi quest doar cand este la munca
- un NPC poate finaliza quest doar cand este in casa seara
- un obiectiv poate cere sa gasesti NPC-ul intr-un anumit loc
- un quest poate cere sa astepti pana la o faza a zilei

Obiective posibile:

```text
talk_to_npc_at_place
deliver_item_during_phase
meet_family_member
inspect_node_during_phase
wait_until_day_phase
```

Aceasta integrare face questurile mai naturale decat un sistem generic unde NPC-ul sta permanent intr-un punct fix.

## 7. TimelineEngine

`TimelineEngine` trebuie implementat dupa rutine.

Rol:

- programeaza evenimente de poveste
- schimba temporar rutina unui NPC
- porneste questuri sau conditii de lume
- marcheaza evolutia satului
- coordoneaza evenimente pe mai multe zile

Timeline-ul nu inlocuieste rutina. Timeline-ul doar o modifica temporar.

Exemplu:

```text
Rutina normala:
fierarul merge la forja dimineata

Eveniment timeline:
ziua 5, dimineata, fierarul lipseste din forja pentru ca a disparut in mina

Rezultat:
RoutineEngine cere work/forge
TimelineEngine aplica override: target = mine_entrance
QuestEngine poate activa questul de investigatie
```

## 8. Timeline override

Pentru a nu strica sistemul de rutine, timeline-ul trebuie sa lucreze cu override-uri limitate.

Model conceptual:

```text
timeline_event_id
target_type: npc | family | household | region
target_id
start_day
start_phase
end_day
end_phase
override_type
priority
payload
```

Tipuri de override:

```text
force_location
disable_work
force_dialogue_state
start_quest_available
block_routine_profile
set_region_state
```

Reguli:

- override-ul are durata clara
- override-ul are prioritate
- la final, NPC-ul revine la rutina normala
- override-ul trebuie salvat persistent
- override-ul trebuie inspectabil prin comanda admin

## 9. Persistenta minima

Pentru rutine:

```text
npc_routine_assignments
npc_last_routine_state
households
household_residents
```

Pentru timeline:

```text
timeline_events
timeline_event_state
active_timeline_overrides
```

La inceput nu este nevoie de un timeline complex. Este mai important ca rutina sa fie persistenta si usor de inspectat.

## 10. Comenzi de debug

Comenzile sunt obligatorii pentru testare practica pe server.

Comenzi implementate acum:

```text
/ainpc routine tick
/ainpc routine status [numeNpc]
```

Comenzi recomandate ulterior:

```text
/ainpc routine inspect <npc>
/ainpc routine sethome <npc> <place>
/ainpc routine setwork <npc> <place>
/ainpc household inspect <household>
/ainpc household add <household> <npc>
/ainpc time phase
/ainpc timeline list
/ainpc timeline inspect <event>
/ainpc timeline trigger <event>
```

Debug-ul trebuie sa raspunda la intrebari simple:

- unde ar trebui sa fie NPC-ul acum?
- de ce a ales locatia asta?
- are casa?
- are loc de munca?
- are familie?
- este blocat de un timeline override?
- questul i-a schimbat rutina?

## 11. Etape de implementare

### Etapa 1: Timp minim

Implementari:

- `DayPhase`
- `TimeService`
- detectare schimbare faza zi/noapte
- comanda `/ainpc time phase`

Rezultat:

- pluginul poate spune in ce faza a zilei este lumea

### Etapa 2: Rutina simpla per NPC

Implementari:

- `RoutineAssignment` - implementat initial
- `RoutineEngine` - implementat initial
- `RoutineService` - implementat initial
- target place pe baza fazei zilei
- fallback daca lipseste casa sau munca

Rezultat:

- NPC-ul are program zilnic minim

### Etapa 3: Casa, munca si household

Implementari:

- `home_place_id`
- `work_place_id`
- `household_id`
- lista de rezidenti pe casa
- comenzi de inspectie

Rezultat:

- mai multi NPC pot apartine aceleiasi case
- familia poate fi simulata credibil

### Etapa 4: Rutine bazate pe node-uri

Implementari:

- alegere target node in interiorul unui place
- pat/noapte
- workstation/munca
- door/iesire
- meeting_point/social

Rezultat:

- NPC-ul nu merge doar la centrul cladirii, ci la puncte utile

### Etapa 5: Quest hooks

Implementari:

- obiective dependente de faza zilei
- obiective dependente de place/node
- NPC quest giver disponibil doar in anumite rutine

Rezultat:

- questurile folosesc viata zilnica a NPC-urilor

### Etapa 6: Timeline MVP

Implementari:

- `TimelineEvent`
- `TimelineEngine`
- override temporar pe rutina
- trigger manual prin comanda
- persistenta pentru evenimente active

Rezultat:

- pot exista evenimente de poveste fara sa distruga rutinele normale

### Etapa 7: Timeline avansat

Implementari:

- evenimente pe mai multe zile
- evenimente pe familie
- evenimente pe regiune
- conditii de pornire
- integrare cu questuri generate

Rezultat:

- satul poate evolua in timp, nu doar sa ruleze rutine fixe

## 12. Ce trebuie evitat

- Nu implementa timeline complet inainte de rutine.
- Nu pune logica de familie direct in timeline.
- Nu face NPC-ul controlat simultan de quest, rutina si AI fara prioritate clara.
- Nu introduce evenimente narative care nu pot fi inspectate.
- Nu lasa AI-ul sa modifice direct rutina sau timeline-ul fara validare.
- Nu teleporta agresiv NPC-urile la fiecare tick.
- Nu face rutine hardcodate in clase mari fara profil/config.

## 13. Prioritate de control

Cand mai multe sisteme vor sa controleze NPC-ul, ordinea trebuie sa fie clara.

Prioritate recomandata:

```text
1. Safety / despawn / invalid world state
2. Timeline override activ
3. Quest override activ
4. RoutineEngine normal
5. Idle behavior
```

AI-ul nu trebuie sa fie peste toate. AI-ul poate propune comportamente, dar runtime-ul trebuie sa valideze:

- este NPC-ul disponibil?
- exista locatia?
- are voie sa paraseasca rutina?
- evenimentul este activ?
- questul permite interactiunea?

## Concluzie

Ordinea corecta este:

```text
TimeService mic
-> RoutineEngine
-> Household/Family
-> Quest hooks
-> TimelineEngine
```

Rutinele dau viata zilnica NPC-urilor. Timeline-ul adauga poveste si schimbari peste aceasta viata zilnica.
