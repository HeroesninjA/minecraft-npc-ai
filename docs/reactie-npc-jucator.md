# Reactia NPC La Jucator

Actualizat: 2026-05-06

## Scop

Acest document descrie cum ar trebui construit sistemul de reactie dintre NPC si jucator, astfel incat reactia sa fie:

- coerenta
- rapida
- persistenta in timp
- legata de memorie, relatie, emotii si context
- reutilizabila pentru dialog, questuri, combat si simulare

Pentru evolutia dialogului propriu-zis peste quest, story, environment, memorie si reputatie, vezi si `dialog-si-conversatii.md`. Pentru fluxul concret de click, chat privat, sesiuni si intentii, vezi `interactiuni.md`.

Ideea centrala:
- reactia nu trebuie sa fie doar text
- reactia trebuie sa fie un pipeline de stare + decizie + manifestare

## Principiul corect

Reactia unui NPC la jucator trebuie sa aiba 3 niveluri:

1. Reactie interna
   - cum se schimba emotia, increderea, afectiunea, frica, furia
2. Reactie decizionala
   - ce decide NPC-ul sa faca
3. Reactie externa
   - ce spune, cum se misca, daca fuge, daca ajuta, daca ataca, daca ofera un quest

Textul spus de NPC este doar un rezultat final.
Nu trebuie sa fie sursa principala a reactiei.

## Ce exista deja in cod

Ai deja fundatia buna:

- `EmotionManager`
  - aplica si persista emotii
- `DialogManager`
  - proceseaza mesajul, relatia si memoriile
- `MemoryManager`
  - creeaza si recupereaza amintiri
- `DecisionEngine`
  - alege actiuni pe baza de context, emotii, relatie si rutina
- `NPCContext`
  - tine starea curenta a NPC-ului
- `NPCAction`
  - enum cu actiuni reale pe care NPC-ul le poate alege

Asta inseamna ca sistemul nu trebuie reinventat.
Trebuie doar orchestrat mai clar.

## Problema daca reactia ramane imprastiata

Daca reactia ramane raspandita prin:
- listener-e
- `DialogManager`
- `EmotionManager`
- `DecisionEngine`

fara un punct central, apar probleme:

- aceeasi actiune a jucatorului produce efecte diferite in locuri diferite
- unele reactii schimba emotia, dar nu si memoria
- unele schimbari de relatie nu produc comportament imediat
- unele raspunsuri AI nu reflecta starea reala a NPC-ului

De aceea este recomandat un serviciu dedicat.

## Arhitectura recomandata

### Serviciu nou

Pachet recomandat:
- `ainpc-core-plugin/src/main/java/ro/ainpc/reactions/`

Clase recomandate:
- `PlayerReactionService`
- `ReactionEventType`
- `ReactionEvent`
- `ReactionContext`
- `ReactionOutcome`
- `ReactionRule`

### Responsabilitati

`PlayerReactionService` trebuie sa fie punctul unic care:

- primeste evenimentul produs de jucator
- construieste contextul relevant
- calculeaza efectele interne
- decide intentia NPC-ului
- produce reactia externa
- persista schimbarile importante

## Pipeline recomandat

Pipeline-ul bun este:

1. Jucatorul produce un eveniment
2. Sistemul clasifica evenimentul
3. Se construieste contextul complet
4. Se calculeaza impactul pe relatie
5. Se calculeaza impactul emotional
6. Se decide daca se creeaza memorie
7. Se alege intentia de reactie
8. Se executa reactia externa
9. Se persista si se aplica cooldown

## Tipuri de evenimente

### Evenimente de proximitate

- `PLAYER_APPROACH`
- `PLAYER_LEAVE`
- `PLAYER_ENTER_HOME`
- `PLAYER_ENTER_WORKPLACE`
- `PLAYER_ENTER_RESTRICTED_AREA`

### Evenimente de dialog

- `PLAYER_GREET`
- `PLAYER_ASK_QUESTION`
- `PLAYER_COMPLIMENT`
- `PLAYER_INSULT`
- `PLAYER_THREAT`
- `PLAYER_JOKE`
- `PLAYER_REQUEST_HELP`
- `PLAYER_REQUEST_QUEST`

### Evenimente sociale si gameplay

- `PLAYER_GIVE_GIFT`
- `PLAYER_HELP_NPC`
- `PLAYER_ATTACK_NPC`
- `PLAYER_ATTACK_FAMILY_MEMBER`
- `PLAYER_STEAL_NEARBY`
- `PLAYER_COMPLETE_QUEST`
- `PLAYER_DECLINE_QUEST`
- `PLAYER_ABANDON_QUEST`

### Evenimente de mediu

- `PLAYER_SEEN_AT_NIGHT`
- `PLAYER_IN_COMBAT_NEARBY`
- `PLAYER_ENTERED_REGION`
- `PLAYER_ENTERED_PLACE`

## Modelul de eveniment

`ReactionEvent` ar trebui sa contina minim:

- `eventType`
- `npcId`
- `playerId`
- `worldName`
- `location`
- `regionId`
- `placeId`
- `message`
- `questId`
- `itemId`
- `amount`
- `witnessedByOthers`
- `timestamp`
- `metadata`

Observatie:
- nu toate campurile sunt obligatorii pentru toate evenimentele
- `metadata` permite extensie fara refactor agresiv

## Contextul de reactie

Inainte sa reactioneze, NPC-ul trebuie sa vada mai mult decat evenimentul brut.

`ReactionContext` ar trebui sa combine:

- `NPCContext` curent
- relatia curenta cu jucatorul
- memorii relevante
- starea emotionala actuala
- rutina curenta
- siguranta curenta
- tipul locului
- prezenta familiei sau a altor NPC-uri
- quest activ sau nu

Practic:
- evenimentul spune ce s-a intamplat
- contextul spune cum trebuie interpretat

## Regula de aur

Aceeasi actiune a jucatorului nu trebuie sa aiba aceeasi reactie in orice context.

Exemple:

- un `salut` primit ziua in piata:
  - reactie pozitiva sau neutra
- un `salut` primit noaptea in casa NPC-ului:
  - reactie de surpriza sau suspiciune
- acelasi `cadou`:
  - daca NPC-ul este prieten: bucurie mare
  - daca NPC-ul este dusman: surpriza + suspiciune

## Prioritatile de reactie

Ordinea buna a prioritatilor este:

1. pericol si siguranta
2. integritatea fizica
3. familia si locul personal
4. questuri active
5. relatie si memorie
6. rutina si munca
7. tonul social normal

Astfel:
- daca NPC-ul este atacat, nu trebuie sa intre in dialog lung
- daca un jucator intra intr-o zona privata noaptea, reactia trebuie sa fie de alarma, nu de politete

## Separarea intre reactie locala si reactie AI

Foarte important:

- reactia primara trebuie sa fie locala si determinista
- AI-ul trebuie folosit mai ales pentru formularea textului

Nu este bine sa trimiti fiecare micro-reactie la OpenAI.

### Ce trebuie sa fie local

- schimbarea relatiei
- schimbarea emotiilor
- decizia daca NPC-ul fuge, avertizeaza sau saluta
- decizia daca se creeaza memorie
- reguli de siguranta
- reguli de proprietate si teritoriu

### Ce poate fi generat de AI

- formularea exacta a replicii
- nuanta sociala a raspunsului
- variatie de limbaj
- descriere conversationala a starii

Regula buna:
- AI-ul coloreaza reactia
- logica locala decide reactia

## Formula recomandata pentru reactie

Reactia poate fi calculata pe baza unui scor compus.

Exemplu conceptual:

```text
reaction_score =
  event_base
  + relationship_modifier
  + memory_modifier
  + emotion_modifier
  + safety_modifier
  + territory_modifier
  + routine_modifier
  + personality_modifier
```

Rezultatul nu trebuie neaparat sa fie un singur scor final.
Poate produce un set de scoruri pentru:

- `friendly`
- `neutral`
- `suspicious`
- `fearful`
- `hostile`
- `grateful`

Pe baza scorului dominant, NPC-ul alege intentia.

## Intentiile recomandate

Inainte de `NPCAction`, este util un strat intermediar:

- `IGNORE`
- `OBSERVE`
- `GREET`
- `WARN`
- `THANK`
- `QUESTION`
- `TRUST_MORE`
- `DISTRUST`
- `ASK_TO_LEAVE`
- `CALL_HELP`
- `FLEE`
- `THREATEN`
- `ATTACK`
- `OFFER_QUEST`
- `REWARD_PLAYER`

Apoi aceasta intentie se mapeaza spre una sau mai multe `NPCAction`.

Exemplu:
- intentie `WARN`
  - actiune: `WARN`
  - text scurt
  - privire directa catre jucator
- intentie `THANK`
  - actiune: `THANK`
  - emotie de bucurie
  - crestere de trust

## Reactie imediata vs reactie de durata

Reactia buna are doua faze:

### Reactie imediata

Se intampla acum:
- intoarcerea privirii
- mesaj scurt
- pas inapoi
- intrare in combat
- particle feedback

### Reactie de durata

Ramane dupa eveniment:
- relatie modificata
- memorie salvata
- emotie persistata
- schimbare de rutina
- schimbare de disponibilitate la quest/trade

Exemplu:
- jucatorul insulta NPC-ul
  - imediat: raspuns rece sau agresiv
  - pe termen lung: trust scazut, memorie negativa, sansa mai mica de ajutor

## Relatia cu jucatorul

Relatia nu ar trebui sa fie tratata doar ca afectiune.

Ideal ar trebui sa existe componente separate:

- `affection`
- `trust`
- `respect`
- `fear_of_player`
- `debt`
- `familiarity`

In codul actual ai deja:
- `affection`
- `trust`
- `respect`
- `familiarity`

Pentru reactii mai bune, recomand sa adaugi mai tarziu:
- `fear_of_player`
- `social_debt`

Exemple:

- jucator puternic si amenintator:
  - respect mare
  - afectiune mica
  - frica mare
- jucator care ajuta constant:
  - trust mare
  - debt pozitiv

## Memoria

Nu orice eveniment trebuie salvat ca memorie.

Regula buna:
- memorie doar daca evenimentul are importanta emotionala sau narativa

Exemple de memorii importante:
- prima intalnire
- ajutor semnificativ
- cadou
- tradare
- amenintare
- atac
- salvare in lupta
- completare quest

Exemple de lucruri care nu trebuie memorate mereu:
- salut banal
- trecere prin fata NPC-ului
- mesaj neutru fara impact

### Prag recomandat

Memoria se creeaza daca:
- impactul emotional absolut este peste un prag
sau
- evenimentul este marcat ca `always_remember`
sau
- evenimentul schimba quest, familie, proprietate sau reputatie

## Reactia la loc si teritoriu

Reactia NPC-jucator trebuie sa tina cont de unde se afla NPC-ul.

Cand ai sistemul de mapping complet, reactia trebuie sa foloseasca:
- `region`
- `place`
- `node`
- `tags`

Exemple:

- in `piata`:
  - reactii mai deschise si publice
- in `casa_fierarului`:
  - reactii mai protective
- in `fierarie`:
  - reactii legate de munca si schimb
- in `zona_interzisa`:
  - reactii de avertizare sau ostilitate

Astfel:
- un jucator aflat in loc public nu produce aceeasi reactie ca intr-un loc privat

## Reactia la questuri

Questurile trebuie sa schimbe reactia NPC-ului.

Exemple:

- daca jucatorul are quest activ:
  - NPC-ul trebuie sa recunoasca progresul
- daca jucatorul a abandonat questul:
  - NPC-ul poate fi dezamagit sau neutru
- daca jucatorul revine cu obiectivele complete:
  - reactia trebuie sa fie diferita de un dialog normal

Reactia la quest nu trebuie ascunsa in text.
Trebuie sa schimbe si:
- status
- relatie
- memorie
- disponibilitate pentru questuri viitoare

## Reactia sociala indirecta

Un NPC nu trebuie sa reactioneze doar la ce i se face direct.

Pe termen mai avansat, reactia ar trebui sa includa:

- ce a vazut NPC-ul
- ce a auzit despre jucator
- ce au patit rudele lui
- ce reputatie are jucatorul in regiune

Exemple:

- jucatorul loveste un localnic
  - martorii devin suspiciosi
- jucatorul ajuta satul
  - mai multi NPC devin mai prietenosi

Pentru asta, `ReactionEvent` trebuie sa poata marca:
- `directTarget`
- `witness`
- `rumorSource`

## Integrare recomandata in codul actual

### Puncte de intrare

`PlayerReactionService` trebuie apelat din:

- `NPCInteractionListener`
- `NPCChatListener`
- `PlayerJoinListener`
- `QuestObjectiveListener`
- viitoare listener-e de gift, damage, trade, interact block

### Legaturi cu subsistemele existente

- `MemoryManager`
  - pentru creare si citire memorii
- `DialogManager`
  - pentru relatie si istoric dialog
- `EmotionManager`
  - pentru aplicare rapida de emotii
- `DecisionEngine`
  - pentru alegerea actiunii
- `NPCContext`
  - pentru stare curenta si mediu

### Ce nu trebuie facut

Nu pune toata reactia in:
- `NPCInteractionListener`
- `NPCChatListener`
- `DialogManager`

Acestea trebuie doar sa colecteze input si sa cheme serviciul central.

## Exemplu de flux bun

### Caz: jucatorul intra in fierarie si il saluta pe fierar

1. Se emite `PLAYER_GREET`
2. Context:
   - loc = `fierarie`
   - NPC la munca
   - relatie = `ACQUAINTANCE`
3. Efecte interne:
   - `happiness +0.05`
   - `trust +0.02`
4. Memorie:
   - nu se salveaza daca este interactiune banala
5. Intentie:
   - `GREET`
6. Actiuni:
   - `LOOK_AT_PLAYER`
   - `GREET`
   - optional `TALK`
7. Text:
   - scurt, contextual, de tip lucru/comert

### Caz: jucatorul intra noaptea in casa fierarului

1. Se emite `PLAYER_ENTER_HOME`
2. Context:
   - loc privat
   - noapte
   - relatie = `STRANGER`
3. Efecte interne:
   - `surprise +0.25`
   - `fear +0.10`
   - `trust -0.05`
4. Memorie:
   - se poate salva doar daca jucatorul persista sau ameninta
5. Intentie:
   - `WARN`
6. Actiuni:
   - `WARN`
   - `OBSERVE`
   - `CALL_HELP` daca nu pleaca

### Caz: jucatorul termina un quest important

1. Se emite `PLAYER_COMPLETE_QUEST`
2. Context:
   - quest asociat cu NPC-ul
3. Efecte interne:
   - `happiness +0.20`
   - `trust +0.15`
   - `respect +0.10`
4. Memorie:
   - da, importanta mare
5. Intentie:
   - `THANK` sau `REWARD_PLAYER`
6. Actiuni:
   - `THANK`
   - `GIVE_REWARD`
   - optional `OFFER_QUEST` ulterior

## Configuratie recomandata

Pe termen mediu, reactiile nu ar trebui hardcodate complet.

Ar trebui sa existe un fisier de reguli, de exemplu:
- `reactions.yml`

Sectiuni utile:
- multiplicatori per tip de eveniment
- praguri pentru memorii
- praguri pentru ostilitate
- reguli pe loc:
  - `home`
  - `workplace`
  - `public`
- reguli pe relatie:
  - `stranger`
  - `friend`
  - `enemy`

Exemplu:

```yml
reactions:
  memory_threshold: 0.25
  danger_threshold: 0.7

  events:
    player_greet:
      affection: 0.02
      trust: 0.01
      emotion:
        happiness: 0.05

    player_insult:
      affection: -0.10
      trust: -0.05
      emotion:
        anger: 0.20
        sadness: 0.05

  place_rules:
    home:
      stranger:
        suspicion_bonus: 0.25
    workplace:
      greeting_bonus: 0.10
```

## Testing

Pentru sistemul de reactie, trebuie testate separat:

- schimbarile de relatie
- schimbarile emotionale
- crearea de memorii
- mapping eveniment -> intentie
- mapping intentie -> actiune

Teste minime recomandate:

- salut repetat de la strain
- insultat de prieten
- ajutat de jucator cu reputatie buna
- intrare intr-un loc privat
- completare quest
- atac asupra NPC-ului

## Ordine buna de implementare

1. Creeaza `ReactionEventType`
2. Creeaza `ReactionEvent`
3. Creeaza `ReactionOutcome`
4. Creeaza `PlayerReactionService`
5. Muta reactiile simple din listener-e in serviciul nou
6. Leaga serviciul nou la `EmotionManager`, `MemoryManager`, `DialogManager`
7. Mapeaza intentiile la `NPCAction`
8. Adauga reguli de loc si teritoriu
9. Externalizeaza tuning-ul in `reactions.yml`

## MVP recomandat

Un MVP bun pentru reactia NPC-jucator ar trebui sa includa:

- `PLAYER_APPROACH`
- `PLAYER_GREET`
- `PLAYER_INSULT`
- `PLAYER_GIVE_GIFT`
- `PLAYER_ATTACK_NPC`
- `PLAYER_COMPLETE_QUEST`
- relatie + emotie + memorie + reactie externa scurta

Atat este suficient pentru a avea comportament credibil fara sa complici excesiv sistemul.

## Concluzie

Reactia NPC la jucator nu trebuie sa fie doar un raspuns text generat.

Trebuie sa fie un sistem unificat care:
- interpreteaza evenimentul
- tine cont de context
- modifica starea interna a NPC-ului
- decide o intentie
- executa o reactie externa
- pastreaza consecintele in memorie si relatie

Pe arhitectura ta actuala, cel mai bun pas este introducerea unui `PlayerReactionService` central, nu extinderea haotica a listener-elor existente.
