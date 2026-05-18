# Dialog si Conversatii

Actualizat: 2026-05-11

## Scop

Acest document descrie cum trebuie imbunatatit dialogul NPC pe masura ce avanseaza celelalte mecanici: questuri, story state, environment context, memorie, reputatie, rutine, familie, economie si campanii.

Pentru fluxul concret de click, chat privat, sesiuni, ascultare pasiva si intentii de quest, vezi `interactiuni.md`. Documentul de fata ramane despre continutul si contextul dialogului.

Ideea principala:

```text
Dialogul nu trebuie sa fie sursa de adevar.
Dialogul trebuie sa fie prezentarea inteligenta a starii reale din runtime.
```

NPC-ul nu trebuie sa inventeze progres, reward-uri, questuri completate sau story state. El trebuie sa vorbeasca pe baza unui context validat si sa propuna intentii care sunt executate doar daca trec prin servicii deterministe.

## Decizia de arhitectura

Dialogul trebuie impartit in trei straturi:

| Strat | Rol |
|---|---|
| Context | ce stie NPC-ul despre jucator, loc, quest, story, memorie si rutina |
| Intentie | ce vrea NPC-ul sa faca: raspunde, intreaba, avertizeaza, ofera quest, cere item |
| Formulare | textul final, generat local sau cu AI |

Regula: AI-ul poate ajuta la formulare si nuanta. Runtime-ul decide progres, reward, relatii si actiuni.

## Ce exista deja

Fundatia existenta:

- `DialogManager`;
- `DialogueEngine`;
- `ConversationSessionManager`;
- `OpenAIService`;
- `NPCContext`;
- `MemoryManager`;
- `EmotionManager`;
- `DecisionEngine`;
- `StoryContextService`;
- `QuestEngine` ca directie pentru runtime matur;
- `EnvironmentContextService` ca directie pentru context read-only de lume.

Aceste componente trebuie conectate printr-un contract clar de dialog, nu inlocuite complet.

## Ce trebuie sa devina dialogul

Dialogul trebuie sa poata raspunde diferit in functie de:

- questul activ;
- progresul obiectivelor;
- stage-ul curent;
- locul in care se afla jucatorul;
- timpul si vremea;
- story state-ul regiunii/place-ului;
- reputatia jucatorului;
- relatia cu NPC-ul;
- memoria interactiunilor anterioare;
- rutina NPC-ului;
- familia sau rolul social al NPC-ului;
- evenimentele recente din lume.

Exemplu:

```text
Fierarul nu spune acelasi lucru daca:
- jucatorul nu a acceptat questul;
- jucatorul a adunat itemele, dar nu a ajuns la forja;
- jucatorul este deja in forja;
- forja are story state supplied;
- jucatorul a abandonat questul anterior;
- este noapte si fierarul nu este la lucru.
```

## Principiu de crestere

Dialogul trebuie sa creasca in acelasi ritm cu mecanicile.

Nu adauga dialog ramificat mare inainte sa existe progres real pe questuri.
Nu adauga AI cu context mare inainte sa existe snapshot compact si auditat.
Nu adauga alegeri morale in dialog inainte sa existe `quest.stages` si `branch_rules`.

Ordinea buna:

1. dialog simplu cu context NPC;
2. dialog quest-aware;
3. dialog story-aware;
4. dialog environment-aware;
5. dialog bazat pe memorie si relatie;
6. dialog cu alegeri validate;
7. dialog cu campanii si reputatie;
8. authoring, localizare si editor.

## DialogueContextSnapshot

Dialogul trebuie sa primeasca un snapshot controlat, nu obiecte brute.

Model recomandat:

```text
DialogueContextSnapshot
- player_id
- npc_id
- npc_name
- npc_profession
- npc_role
- relationship_summary
- memory_summary
- emotion_summary
- routine_summary
- environment_summary
- story_summary
- active_quest_summary
- available_quest_summary
- campaign_summary
- recent_dialog_summary
- safety_warnings
```

Reguli:

- snapshot-ul este read-only;
- snapshot-ul este limitat ca marime;
- fiecare camp are sursa clara;
- lipsa contextului produce warning, nu inventie;
- AI-ul primeste rezumat, nu DB brut.

## Prioritatea contextului in prompt

Cand promptul devine prea mare, prioritatea trebuie sa fie:

1. siguranta si reguli de runtime;
2. identitatea NPC-ului;
3. intentia curenta a jucatorului;
4. quest activ si progres;
5. story state relevant;
6. locul curent si environment context;
7. relatie si reputatie;
8. memorii relevante;
9. rutina NPC;
10. stil si ton.

Separarea surselor este obligatorie:

```text
WORLD_CONTEXT:
STORY_CONTEXT:
MEMORY_CONTEXT:
PROGRESSION_CONTEXT:
```

Regula de conflict:

```text
Progression > Story > Memory > AI text
```

Memoria NPC-ului coloreaza reactia, dar nu poate contrazice progresul curent sau story state-ul real.

Ce nu trebuie trimis in prompt:

- toata harta;
- tot istoricul de dialog;
- toate questurile din pack;
- coordonate brute cand exista place/node;
- chei secrete;
- date DB inutile;
- reguli interne care pot fi abuzate de jucator.

## Dialog pe masura ce avanseaza questurile

Questurile trebuie sa schimbe dialogul in mod explicit.

Stari minime:

| Stare quest | Dialog recomandat |
|---|---|
| `NOT_STARTED` | NPC poate introduce problema |
| `OFFERED` | NPC explica riscul si recompensa |
| `ACTIVE` | NPC comenteaza progresul |
| `READY_TO_TURN_IN` | NPC cere finalizarea |
| `COMPLETED` | NPC multumeste si schimba tonul |
| `FAILED` | NPC reactioneaza la esec |
| `ABANDONED` | NPC poate fi dezamagit sau neutru |

Exemplu:

```text
ACTIVE + collect_planks=8/8 + visit_forge=0/1
-> "Ai materialele. Acum adu-le la forja, nu le lasa in drum."
```

Reguli:

- dialogul nu seteaza progres direct;
- dialogul poate explica urmatorul obiectiv;
- dialogul poate oferi hint daca jucatorul este blocat;
- dialogul poate porni acceptarea questului doar prin `QuestEngine`;
- dialogul poate finaliza questul doar daca `QuestEngine` confirma obiectivele.

## Dialog pe masura ce avanseaza story state-ul

Story state-ul trebuie sa schimbe felul in care NPC-urile vorbesc despre lume.

Exemple:

| Story state | Efect in dialog |
|---|---|
| `forge_supplied` | fierarul este mai optimist |
| `road_danger=active` | garda avertizeaza despre drum |
| `crypt_disturbed` | preotul vorbeste mai grav |
| `festival_active` | hangiul mentioneaza festivalul |

Reguli:

- story state-ul este citit prin `StoryContextService`;
- dialogul nu inventeaza story state;
- dialogul poate referi evenimente recente;
- contradictiile de story trebuie detectate in audit;
- AI-ul trebuie sa foloseasca doar story state-ul din snapshot.

## Dialog pe masura ce avanseaza environment context

Environment context face dialogul mai situational.

Exemple:

| Context | Dialog |
|---|---|
| noapte | NPC este mai suspicios sau grabit |
| ploaie | fermierul mentioneaza campul sau noroiul |
| in forja | fierarul vorbeste despre munca |
| in casa privata | NPC avertizeaza jucatorul |
| world event activ | NPC mentioneaza evenimentul |

Reguli:

- foloseste `EnvironmentContextService` read-only;
- nu implementa `EnvironmentEngine` doar pentru replici;
- dialogul poate mentiona contextul, dar nu modifica mediul;
- contextul instabil trebuie folosit ca nuanta, nu blocaj critic.

## Dialog pe masura ce avanseaza relatia si memoria

Relatia si memoria trebuie sa schimbe tonul, increderea si disponibilitatea NPC-ului.

Dimensiuni utile:

- `trust`;
- `respect`;
- `affection`;
- `familiarity`;
- `fear_of_player`;
- `social_debt`;
- `reputation`.

Exemple:

| Relatie/memorie | Dialog |
|---|---|
| trust mare | NPC vorbeste direct si cere ajutor real |
| trust mic | NPC ofera doar informatii limitate |
| memorie de ajutor | NPC multumeste contextual |
| memorie de tradare | NPC este rece sau suspicios |
| reputatie buna in sat | NPC recunoaste contributiile |

Reguli:

- nu salva fiecare salut ca memorie;
- sumarizeaza memorii relevante;
- relatia decide ton si disponibilitate;
- memoria poate fi folosita ca referinta in dialog;
- AI-ul nu modifica relatia direct.

## Dialog pe masura ce avanseaza rutina NPC

Rutina trebuie sa faca NPC-ul sa para ocupat si coerent.

Exemple:

| Rutina | Dialog |
|---|---|
| la munca | raspuns scurt, legat de profesie |
| acasa | ton personal, defensiv daca e strain |
| in piata | ton social |
| in drum | raspuns scurt sau grabit |
| in timpul unui event | dialog legat de eveniment |

Reguli:

- rutina nu trebuie sa blocheze tot dialogul;
- NPC-ul poate raspunde diferit cand este ocupat;
- questurile importante pot intrerupe rutina;
- rutina este context, nu sursa unica de comportament.

## Dialog cu alegeri validate

Alegerile de dialog trebuie introduse dupa ce exista:

- `quest.stages`;
- `branch_rules`;
- `quest_variables`;
- audit pentru branch destinations;
- event log.

Model:

```yml
dialogue:
  nodes:
    report_choice:
      text_key: "quest.q10.choice"
      choices:
        tell_truth:
          text_key: "quest.q10.choice.truth"
          action:
            type: "select_branch"
            branch: "report_priest"
        sell_secret:
          text_key: "quest.q10.choice.sell"
          action:
            type: "select_branch"
            branch: "report_merchant"
```

Reguli:

- alegerile sunt optiuni validate, nu text liber;
- fiecare alegere are efect explicit;
- branch-ul este executat de `QuestEngine`;
- AI-ul poate formula textul, dar nu inventa alegeri runtime;
- alegerile sunt salvate in `quest_variables`.

## Dialog AI vs dialog determinist

Nu fiecare replica trebuie generata de AI.

Foloseste dialog determinist pentru:

- acceptare quest;
- completare quest;
- erori si blocaje;
- status clar;
- comenzi de tutorial;
- reward-uri;
- mesaje de siguranta.

Foloseste AI pentru:

- variatie de ton;
- reactii sociale;
- explicatii scurte;
- dialog contextual bazat pe memorie;
- conversatii libere fara efect direct;
- roleplay controlat.

Regula:

```text
Deterministic runtime decides.
AI phrases.
```

## Intentii de dialog

Inainte de text, sistemul trebuie sa identifice intentia.

Intentii de input:

- `GREET`;
- `ASK_STATUS`;
- `ASK_QUEST`;
- `ACCEPT_QUEST`;
- `DECLINE_QUEST`;
- `TURN_IN_QUEST`;
- `ASK_HINT`;
- `ASK_ABOUT_PLACE`;
- `ASK_ABOUT_NPC`;
- `THANK`;
- `INSULT`;
- `THREATEN`;
- `SMALL_TALK`.

Intentii de output:

- `ANSWER`;
- `ASK_CLARIFICATION`;
- `OFFER_QUEST`;
- `EXPLAIN_OBJECTIVE`;
- `GIVE_HINT`;
- `WARN`;
- `THANK`;
- `REFUSE`;
- `CALL_HELP`;
- `END_CONVERSATION`.

Reguli:

- intentia poate fi detectata local pentru cazuri simple;
- AI poate ajuta la clasificare, dar rezultatul trebuie validat;
- intentiile care modifica runtime cer confirmare prin servicii;
- intentia si textul final trebuie logate in debug sumar.

## Contract YAML pentru dialog

Pe termen mediu, dialogul trebuie sa poata fi definit in pack-uri.

Schema simpla:

```yml
dialogue:
  offer:
    text_key: "quest.q11.offer"
    ai_tone: "practical"
  accepted:
    text_key: "quest.q11.accepted"
  active:
    conditions:
      - type: "objective_complete"
        objective_id: "collect_planks"
    text_key: "quest.q11.active.go_to_forge"
  ready:
    text_key: "quest.q11.ready"
  completed:
    text_key: "quest.q11.completed"
```

Reguli:

- `text_key` este preferat fata de text inline;
- textul inline ramane fallback;
- conditiile de dialog sunt read-only;
- dialogul nu executa actiuni arbitrare;
- fiecare quest important are macar `offer`, `accepted`, `active`, `ready`, `completed`.

## Prompt contract

Promptul pentru AI trebuie sa fie structurat.

Model:

```text
SYSTEM_RULES:
- You are an NPC in AINPC.
- Do not grant rewards.
- Do not mark objectives complete.
- Use only provided context.

NPC_PROFILE:
- name
- profession
- personality
- current_emotion

PLAYER_CONTEXT:
- relationship
- reputation
- relevant_memories

QUEST_CONTEXT:
- active quest
- stage
- next objective
- progress summary

STORY_CONTEXT:
- region state
- place state
- recent events

ENVIRONMENT_CONTEXT:
- current place
- time
- weather

PLAYER_MESSAGE:
- sanitized message
```

Reguli:

- mesajul jucatorului este delimitat clar;
- contextul este scurt;
- instructiunile interzic modificari runtime;
- output-ul AI trebuie post-procesat;
- raspunsul final trebuie limitat ca lungime.

## Post-procesare raspuns AI

Raspunsul AI nu trebuie trimis orbeste.

Verificari:

- lungime maxima;
- fara promisiuni de reward nevalidat;
- fara afirmatii ca questul este complet daca runtime-ul nu confirma;
- fara coordonate inventate;
- fara comenzi server;
- fara continut care contrazice story state-ul;
- fara instructiuni externe jocului.

Fallback:

```text
Daca raspunsul AI este invalid, foloseste replica determinista din dialogue.yml.
```

## Debug si audit pentru dialog

Comenzi recomandate:

```text
/ainpc dialog context <player> <npc>
/ainpc dialog inspect <npc>
/ainpc dialog history <player> <npc>
/ainpc dialog audit
/ainpc dialog test <quest_code> <state>
```

Auditul trebuie sa verifice:

- dialog lipsa pentru stari importante;
- `text_key` lipsa;
- conditii de dialog necunoscute;
- dialog care refera objective_id inexistent;
- prompt prea mare;
- AI enabled fara fallback determinist;
- text prea lung pentru UI.

## Testare

Teste minime:

- NPC fara quest raspunde la salut;
- NPC cu quest oferit explica oferta;
- NPC cu quest activ explica urmatorul obiectiv;
- NPC cu obiectiv complet cere turn-in;
- NPC dupa completare multumeste diferit;
- NPC cu reputatie mica refuza ajutor avansat;
- NPC in loc privat noaptea avertizeaza;
- AI invalid produce fallback.

Fixture recomandat:

```json
{
  "npc": "blacksmith",
  "player_message": "ce fac acum?",
  "quest_status": "ACTIVE",
  "objective_progress": {
    "collect_planks": 8,
    "visit_forge": 0
  },
  "expected_intent": "EXPLAIN_OBJECTIVE"
}
```

## Faze recomandate

### D1 - Dialog simplu stabil

- raspunsuri scurte;
- context NPC;
- fallback determinist;
- istoric de dialog limitat.

### D2 - Dialog quest-aware

- quest activ;
- progres obiective;
- hint pentru urmatorul obiectiv;
- stari `offer`, `active`, `ready`, `completed`.

### D3 - Dialog story-aware

- story state regional/place;
- story events recente;
- reactii dupa completari importante.

### D4 - Dialog environment-aware

- locatie curenta;
- timp;
- vreme;
- world events;
- loc privat/public.

### D5 - Dialog social

- relatie;
- reputatie;
- memorii relevante;
- familie si martori.

### D6 - Dialog cu alegeri validate

- dialogue nodes;
- choices;
- branch rules;
- quest variables;
- audit strict.

### D7 - Dialog ca platforma de continut

- text keys;
- localizare;
- editor;
- semantic diff;
- narrative QA;
- replay fixtures.

## Ordine recomandata de implementare

1. defineste `DialogueContextSnapshot`;
2. separa intentia de text;
3. adauga fallback determinist pentru stari de quest;
4. conecteaza `QuestEngine` pentru `active/ready/completed`;
5. conecteaza `StoryContextService`;
6. conecteaza `EnvironmentContextService`;
7. adauga sumar de memorie si relatie;
8. adauga audit pentru dialog;
9. adauga `dialogue.yml` sau sectiune `dialogue` in quest pack;
10. adauga choices validate;
11. adauga text keys si localizare;
12. adauga editor si narrative QA.

## Ce trebuie evitat

- AI care decide reward-uri;
- AI care marcheaza quest complet;
- dialog care scrie DB direct;
- prompt cu toata harta;
- istoric de dialog nelimitat;
- dialog ramificat fara audit;
- text lung in chat;
- lipsa fallback-ului cand AI esueaza;
- coordonate brute in raspuns cand exista place/node.

## Concluzie

Dialogul trebuie sa devina mai bogat pe masura ce celelalte mecanici avanseaza, dar trebuie sa ramana sub controlul runtime-ului.

Formula buna:

```text
QuestEngine spune ce este adevarat despre progres.
StoryStateService spune ce este adevarat despre lume.
EnvironmentContextService spune unde si cand se intampla scena.
Memory/Reputation spun cine este jucatorul pentru NPC.
DialogueEngine formuleaza raspunsul.
```

Asa dialogul devine mai viu fara sa devina sursa de buguri sau progres inventat.
