# Contract JSON/YAML pentru scripturi

Pentru orientare in cod, foloseste [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md) si apoi [harta completa](./harta-pachetelor-cod.md).

AceastÄƒ documentaÈ›ie defineÈ™te cum integrÄƒm scripturi scrise Ã®n `JSON` È™i `YAML` astfel Ã®ncÃ¢t sÄƒ rÄƒmÃ¢nÄƒ uÈ™or de citit, uÈ™or de interpretat È™i compatibile Ã®n timp.

Obiectivul nu este sÄƒ Ã®ngheÈ›e formatul, ci sÄƒ stabileascÄƒ un contract comun: aceleaÈ™i intenÈ›ii, aceeaÈ™i structurÄƒ logicÄƒ È™i aceleaÈ™i reguli de validare, indiferent dacÄƒ sursa este JSON sau YAML.

## Scop

- OferÄƒ o cale unicÄƒ de interpretare pentru ambele formate.
- Reduce diferenÈ›ele dintre fiÈ™ierele scrise manual È™i cele generate automat.
- PÄƒstreazÄƒ compatibilitatea fÄƒrÄƒ sÄƒ blocheze extensiile viitoare.
- EvitÄƒ apariÈ›ia unor variante locale de format care nu mai pot fi validate.

## Principii

1. **Un singur model logic**
   - JSON È™i YAML sunt doar forme de exprimare.
   - ÃŽn interior, totul se normalizeazÄƒ la acelaÈ™i model de date.

2. **Citire umanÄƒ Ã®nainte de optimizare**
   - Structura trebuie sÄƒ fie clarÄƒ pentru oameni.
   - Generatorul sau parserul nu trebuie sÄƒ impunÄƒ artificii inutile.

3. **Compatibilitate prin extensie**
   - Se adaugÄƒ cÃ¢mpuri noi, nu se rup cÃ¢mpurile vechi.
   - SchimbÄƒrile de nume se fac gradual, cu aliasuri.

4. **Validare pe straturi**
   - SintaxÄƒ.
   - StructurÄƒ.
   - SemnificaÈ›ie.
   - Compatibilitate Ã®ntre versiuni.

## Model recomandat

Formatul recomandat este:

```yaml
type: quest-pack
version: 1
meta:
  id: quest_pack_001
  title: "Intro dungeon"
  tags: [demo, readable]
spec:
  inputs:
    - name: hero_level
      required: true
  steps:
    - id: start
      kind: spawn
      target: village_gate
```

Varianta echivalentÄƒ Ã®n JSON pÄƒstreazÄƒ aceleaÈ™i chei:

```json
{
  "type": "quest-pack",
  "version": 1,
  "meta": {
    "id": "quest_pack_001",
    "title": "Intro dungeon",
    "tags": ["demo", "readable"]
  },
  "spec": {
    "inputs": [
      { "name": "hero_level", "required": true }
    ],
    "steps": [
      { "id": "start", "kind": "spawn", "target": "village_gate" }
    ]
  }
}
```

## Pattern-uri recomandate

### 1. Nucleu stabil, extensii opÈ›ionale

- `type` identificÄƒ familia de script.
- `version` indicÄƒ schema logicÄƒ.
- `meta` conÈ›ine identificatori È™i etichete.
- `spec` conÈ›ine comportamentul real.

Acest pattern lasÄƒ loc pentru cÃ¢mpuri suplimentare fÄƒrÄƒ sÄƒ amestece metadatele cu execuÈ›ia.

### 2. Structuri scurte È™i previzibile

- PreferÄƒ liste atunci cÃ¢nd ordinea conteazÄƒ.
- PreferÄƒ obiecte atunci cÃ¢nd cheia este stabilÄƒ.
- EvitÄƒ adÃ¢ncimi inutile de 6-8 niveluri.
- Nu dubla aceeaÈ™i informaÈ›ie Ã®n mai multe locuri.

### 3. Chei constante Ã®ntre formate

- AceleaÈ™i nume de cÃ¢mp Ã®n JSON È™i YAML.
- FÄƒrÄƒ sinonime locale de tipul `questSteps` Ã®ntr-un fiÈ™ier È™i `steps` Ã®n altul.
- DacÄƒ apare un alias, el trebuie tratat ca backward-compatible, nu ca nou contract.

### 4. Separarea datelor de comentarii

- YAML poate conÈ›ine comentarii utile pentru autor.
- Comentariile nu fac parte din contract.
- JSON rÄƒmÃ¢ne sursa de referinÈ›Äƒ pentru consum automat doar dacÄƒ provine din aceeaÈ™i normalizare.

## Reguli de compatibilitate

### Compatibilitate acceptatÄƒ

- AdÄƒugarea de cÃ¢mpuri noi opÈ›ionale.
- AdÄƒugarea de valori noi Ã®ntr-o listÄƒ de tipuri permise.
- Extinderea unei secÈ›iuni fÄƒrÄƒ a schimba semnificaÈ›ia cÃ¢mpurilor existente.
- Acceptarea unor aliasuri de nume pentru migrare.

### SchimbÄƒri care cer migrare controlatÄƒ

- Redenumirea unui cÃ¢mp obligatoriu.
- Schimbarea tipului unui cÃ¢mp.
- Mutarea unui cÃ¢mp dintr-o secÈ›iune Ã®n alta.
- Schimbarea semnificaÈ›iei unui status existent.

### Ce trebuie evitat

- SÄƒ tratÄƒm fiÈ™ierele diferit doar pentru cÄƒ sunt JSON sau YAML.
- SÄƒ blocÄƒm inputul cu reguli estetice care nu influenÈ›eazÄƒ semantica.
- SÄƒ introducem cÃ¢mpuri obligatorii noi fÄƒrÄƒ perioadÄƒ de tranziÈ›ie.

## Validare

Validarea trebuie sÄƒ fie predictibilÄƒ:

1. **Parse**
   - FiÈ™ierul se citeÈ™te Ã®n formatul lui nativ.

2. **Normalize**
   - JSON È™i YAML se transformÄƒ Ã®n aceeaÈ™i structurÄƒ internÄƒ.

3. **Validate structural**
   - Se verificÄƒ existenÈ›a cÃ¢mpurilor cerute È™i tipurile de bazÄƒ.

4. **Validate semantic**
   - Se verificÄƒ relaÈ›iile dintre cÃ¢mpuri, referinÈ›ele È™i valorile permise.

5. **Validate compatibility**
   - Se verificÄƒ dacÄƒ scriptul respectÄƒ versiunea curentÄƒ sau una acceptatÄƒ prin alias.

## Reguli pentru pattern-uri uÈ™or de citit

- Un concept = un nod.
- O intenÈ›ie = o secÈ›iune.
- O regulÄƒ = o cheie clar numitÄƒ.
- O colecÈ›ie = o listÄƒ scurtÄƒ È™i ordonatÄƒ.
- O excepÈ›ie = un cÃ¢mp opÈ›ional explicit, nu o convenÈ›ie ascunsÄƒ.

Exemplu bun:

```yaml
spec:
  requirements:
    - level: 3
    - item: key_of_dawn
```

Exemplu slab:

```yaml
a:
  b:
    c:
      d: 3
```

## Integrare Ã®n runtime

- Parserul acceptÄƒ ambele formate.
- Normalizarea produce acelaÈ™i model intern.
- Restul sistemului consumÄƒ doar modelul intern.
- Exportul poate genera fie JSON, fie YAML, dar nu trebuie sÄƒ schimbe semantica.
- Loaderul runtime poate citi `quests.json` sau `quests.yml` fÄƒrÄƒ sÄƒ schimbe contractul logic.
- Mapping-ul world admin poate fi Ã®ncÄƒrcat È™i din `world-admin.json` sau `world-admin.yml` ca overlay opÈ›ional peste config-ul de bazÄƒ.
- Debug dump-ul expune È™i `quests-snapshot.json` ca imagine read-only a config-ului Ã®ncÄƒrcat.
- Debug dump-ul expune È™i `mapping-snapshot.json` ca imagine normalizatÄƒ a world mapping-ului È™i a surselor overlay.

Exemple concrete sunt Ã®n `docs/json-yaml-contract-exemple.md`.

Astfel, compatibilitatea este pÄƒstratÄƒ, iar oamenii pot lucra Ã®n formatul care li se potriveÈ™te mai bine.

## RecomandÄƒri practice

- FoloseÈ™te YAML cÃ¢nd scriptul este editat frecvent de oameni.
- FoloseÈ™te JSON cÃ¢nd payload-ul este generat, transmis sau validat mecanic.
- PÄƒstreazÄƒ aceleaÈ™i chei Ã®n ambele formate.
- MarcheazÄƒ explicit versiunea Ã®n fiÈ™ier.
- AdaugÄƒ aliasuri Ã®nainte de a renunÈ›a la cÃ¢mpurile vechi.

## Ce nu trebuie limitat

- Nu bloca ordinea cÃ¢mpurilor dacÄƒ nu afecteazÄƒ semantica.
- Nu interzice comentariile YAML la nivel de autor.
- Nu cere structurÄƒ identicÄƒ la nivel fizic, doar la nivel logic.
- Nu transforma convenÈ›iile de stil Ã®n erori de compatibilitate.

## Template standard

Fiecare script nou ar trebui sÄƒ porneascÄƒ de la aceeaÈ™i formÄƒ de bazÄƒ:

```yaml
type: example-script
version: 1
meta:
  id: example_001
  title: "Example"
  author: "team"
spec:
  enabled: true
  inputs: []
  outputs: []
```

Reguli pentru acest template:

- `type` rÄƒmÃ¢ne stabil È™i descriptiv.
- `version` creÈ™te doar cÃ¢nd contractul logic se schimbÄƒ.
- `meta.id` este unic Ã®n cadrul familiei de scripturi.
- `spec` conÈ›ine doar date funcÈ›ionale.
- Listele goale sunt valide dacÄƒ intenÈ›ia este explicitÄƒ.

## Migrare controlatÄƒ

Migrarea trebuie sÄƒ fie lizibilÄƒ È™i reversibilÄƒ pe cÃ¢t posibil:

1. AdaugÄƒ cÃ¢mpul nou ca opÈ›ional.
2. AcceptÄƒ vechiul È™i noul nume Ã®n paralel.
3. MarcheazÄƒ vechiul nume ca deprecated Ã®n documentaÈ›ie.
4. ActualizeazÄƒ generatorul sau editorul.
5. EliminÄƒ aliasul doar dupÄƒ o perioadÄƒ clarÄƒ de compatibilitate.

Exemplu de tranziÈ›ie:

```yaml
# vechi

Pentru orientare in cod, foloseste [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md) si apoi [harta completa](./harta-pachetelor-cod.md).
spec:
  questName: "Intro"

# nou

Pentru orientare in cod, foloseste [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md) si apoi [harta completa](./harta-pachetelor-cod.md).
spec:
  title: "Intro"
```

ÃŽn perioada de tranziÈ›ie, parserul trebuie sÄƒ accepte ambele forme È™i sÄƒ le normalizeze la aceeaÈ™i cheie internÄƒ.

## Event lists, trigger È™i hook

FiÈ™ierele JSON È™i YAML pot descrie evenimente ca liste ordonate de acÈ›iuni sau ca mapÄƒri de declanÈ™atoare cÄƒtre hook-uri. Contractul trebuie sÄƒ suporte ambele forme fÄƒrÄƒ a impune o singurÄƒ reprezentare fizicÄƒ.

### Model recomandat pentru evenimente

```yaml
events:
  - name: on_start
    trigger: world.loaded
    hook: init_story
  - name: on_complete
    trigger: quest.finished
    hook: reward_player
```

Reguli:

- `events` este o listÄƒ ordonatÄƒ cÃ¢nd ordinea de procesare conteazÄƒ.
- `trigger` identificÄƒ momentul sau condiÈ›ia de activare.
- `hook` indicÄƒ handlerul logic asociat.
- `name` este opÈ›ional, dar util pentru debugging È™i audit.
- DacÄƒ un eveniment are mai multe hook-uri, ele rÄƒmÃ¢n Ã®ntr-o listÄƒ explicitÄƒ.

### Reprezentare alternativÄƒ

ÃŽn unele cazuri, un map de trigger-uri este mai compact:

```json
{
  "events": {
    "world.loaded": ["init_story"],
    "quest.finished": ["reward_player"]
  }
}
```

AceastÄƒ formÄƒ este acceptabilÄƒ doar dacÄƒ normalizarea internÄƒ produce aceeaÈ™i semnificaÈ›ie ca lista de evenimente. Parserul trebuie sÄƒ converteascÄƒ ambele reprezentÄƒri Ã®n aceeaÈ™i structurÄƒ internÄƒ.

### Reguli de interpretare

- Un `trigger` activeazÄƒ unul sau mai multe `hook-uri`.
- Un `hook` nu trebuie sÄƒ depindÄƒ de ordinea specificÄƒ a cÃ¢mpurilor din fiÈ™ier.
- O listÄƒ de evenimente pÄƒstreazÄƒ ordinea declarativÄƒ.
- O hartÄƒ de trigger-uri favorizeazÄƒ cÄƒutarea rapidÄƒ È™i poate fi folositÄƒ ca formÄƒ derivatÄƒ.
- DacÄƒ existÄƒ ambiguitate, modelul canonic este lista normalizatÄƒ de evenimente.

### Compatibilitate pentru hook-uri

- AdÄƒugarea unui hook nou este compatibilÄƒ.
- AdÄƒugarea unui trigger nou este compatibilÄƒ.
- Redenumirea unui hook cere alias sau adaptor.
- Schimbarea semnificaÈ›iei unui trigger cere versiune nouÄƒ.
- Eliminarea unui hook existent cere perioadÄƒ de tranziÈ›ie.

## Quest È™i mapping

Pentru fiÈ™ierele care descriu questuri È™i mapping semantic, contractul trebuie sÄƒ pÄƒstreze separarea dintre intenÈ›ie È™i rezolvare.

### Quest

- `quest` descrie intenÈ›ia de gameplay.
- `objectives` sau `steps` descriu ordinea logicÄƒ.
- `trigger` marcheazÄƒ momentul de activare.
- `hook` trimite execuÈ›ia cÄƒtre logica runtime.
- Tinta nu trebuie sÄƒ depindÄƒ doar de coordonate brute.

Exemplu:

```yaml
type: quest
version: 1
meta:
  id: quest_intro_market
spec:
  objectives:
    - id: visit_market
      trigger: quest.started
      hook: bind_place_target
      target:
        kind: place
        ref: tag:market
```

### Mapping

- `mapping` descrie rezolvarea semanticÄƒ a È›intelor.
- `region`, `place` È™i `node` trebuie sÄƒ rÄƒmÃ¢nÄƒ tipuri distincte.
- `tags` È™i `metadata` sunt baza pentru potrivire flexibilÄƒ.
- Rezultatul resolverului trebuie sÄƒ fie stabil È™i auditat.
- Mapping-ul poate fi exportat Ã®n JSON sau YAML fÄƒrÄƒ schimbare de semnificaÈ›ie.

Exemplu:

```json
{
  "type": "mapping",
  "version": 1,
  "meta": {
    "id": "market_center"
  },
  "spec": {
    "places": [
      {
        "id": "village_market",
        "tags": ["market", "town_center"]
      }
    ]
  }
}
```

### RegulÄƒ comunÄƒ

- Quest-ul consumÄƒ mapping-ul prin identificatori semantici, nu prin coordonate.
- Mapping-ul rÄƒmÃ¢ne sursa de adevÄƒr pentru localizare È™i ancore.
- JSON È™i YAML trebuie sÄƒ producÄƒ aceeaÈ™i structurÄƒ internÄƒ dupÄƒ normalizare.

### Flux end-to-end

1. Quest-ul declarÄƒ obiectivul È™i intenÈ›ia.
2. Trigger-ul porneÈ™te obiectivul la un eveniment runtime.
3. Hook-ul cere rezolvarea semanticÄƒ a È›intei.
4. Mapping-ul Ã®ntoarce un `region`, `place` sau `node` valid.
5. Runtime-ul executÄƒ logica fÄƒrÄƒ sÄƒ depindÄƒ de formatul fizic al fiÈ™ierului.

Exemplu complet:

```yaml
# quest.yaml

Pentru orientare in cod, foloseste [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md) si apoi [harta completa](./harta-pachetelor-cod.md).
type: quest
version: 1
meta:
  id: quest_intro_market
spec:
  objectives:
    - id: reach_market
      trigger: quest.started
      hook: resolve_place_target
      target:
        kind: place
        ref: tag:market
```

```yaml
# mapping.yaml

Pentru orientare in cod, foloseste [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md) si apoi [harta completa](./harta-pachetelor-cod.md).
type: mapping
version: 1
meta:
  id: market_center
spec:
  places:
    - id: village_market
      tags: [market, town_center]
      metadata:
        npc_service: merchant
```

Normalizare aÈ™teptatÄƒ:

- obiectivul `reach_market` devine o cerere semanticÄƒ de tip `place`;
- `tag:market` se rezolvÄƒ la `village_market`;
- `resolve_place_target` primeÈ™te un `place` stabil, nu o coordonatÄƒ hardcodatÄƒ;
- aceeaÈ™i intenÈ›ie poate fi serializatÄƒ identic Ã®n JSON fÄƒrÄƒ sÄƒ se schimbe rezultatul.

## Matrice de compatibilitate

| Schimbare | Compatibilitate | ObservaÈ›ie |
| --- | --- | --- |
| CÃ¢mp opÈ›ional nou | Da | Nu rupe scripturile existente |
| Valoare nouÄƒ Ã®ntr-un enum | Da, cu fallback | Consumatorii vechi trebuie sÄƒ trateze valoarea necunoscutÄƒ |
| Redenumire de cÃ¢mp | ParÈ›ial | NecesitÄƒ alias È™i migrare |
| Mutare de secÈ›iune | ParÈ›ial | NecesitÄƒ normalizare È™i documentare clarÄƒ |
| Schimbare de tip | Nu | NecesitÄƒ versiune nouÄƒ sau adaptor |
| Eliminare de cÃ¢mp obligatoriu | Nu | NecesitÄƒ roadmap de migrare |

## Faze de implementare

### Faza I - Contract minim

Scop: stabilim un model comun pentru JSON È™i YAML.

Livrabile:

- `type`, `version`, `meta`, `spec` ca structurÄƒ standard;
- normalizare internÄƒ unicÄƒ;
- validare structuralÄƒ de bazÄƒ;
- exemple simple pentru `quest` È™i `mapping`.

Gate:

- un fiÈ™ier JSON È™i unul YAML produc aceeaÈ™i structurÄƒ internÄƒ;
- cÃ¢mpurile obligatorii sunt validate identic Ã®n ambele formate.

### Faza II - Eventuri, trigger È™i hook

Scop: introducem fluxul de execuÈ›ie declarativÄƒ.

Livrabile:

- liste de evenimente ordonate;
- mapÄƒri de trigger-uri cÄƒtre hook-uri;
- suport pentru aliasuri È™i formate alternative;
- normalizare la aceeaÈ™i reprezentare logicÄƒ.

Gate:

- aceeaÈ™i semnificaÈ›ie poate fi exprimatÄƒ ca listÄƒ sau map;
- runtime-ul primeÈ™te aceeaÈ™i structurÄƒ internÄƒ.

### Faza III - Quest È™i mapping semantic

Scop: legÄƒm intenÈ›ia quest-ului de rezolvarea semanticÄƒ a mapping-ului.

Livrabile:

- exemple `quest.yaml` È™i `mapping.yaml`;
- rezolvare prin `region`, `place`, `node`;
- fallback controlat pentru È›inte ambigue;
- contract clar pentru `trigger`, `hook` È™i `target`.

Gate:

- quest-ul nu mai depinde de coordonate brute;
- mapping-ul rÄƒmÃ¢ne sursa de adevÄƒr pentru ancore.

### Faza IV - Migrare È™i compatibilitate

Scop: pÄƒstrÄƒm continuitatea Ã®ntre versiuni.

Livrabile:

- aliasuri pentru cÃ¢mpuri redenumite;
- reguli de deprecated;
- matrice de compatibilitate pentru schimbÄƒri frecvente;
- adaptor pentru formatul vechi cÃ¢nd este necesar.

Gate:

- niciun consumator existent nu este rupt de o schimbare de nume;
- eliminarea unui cÃ¢mp are perioada de tranziÈ›ie documentatÄƒ.

### Faza V - Integrare runtime È™i tooling

Scop: conectÄƒm contractul la fluxul real de utilizare.

Livrabile:

- parser comun pentru JSON È™i YAML;
- integrare cu quest runtime;
- export/read-only debug pentru validare;
- documente exemple pentru authors È™i admins.

Gate:

- runtime-ul consumÄƒ doar modelul normalizat;
- utilizatorii pot edita Ã®n formatul preferat fÄƒrÄƒ diferenÈ›e semantice.

### Faza VI - Hardening È™i extindere

Scop: pregÄƒtim contractul pentru extensii viitoare fÄƒrÄƒ blocaje.

Livrabile:

- extensii opÈ›ionale pe `spec`;
- reguli pentru enum-uri È™i fallback;
- suport pentru hook-uri suplimentare;
- testare pe cazuri limitÄƒ È™i regresii.

Gate:

- adÄƒugarea de date noi nu rupe compatibilitatea;
- extensiile rÄƒmÃ¢n lizibile È™i validate.

## LegÄƒturi cu documentaÈ›ia existentÄƒ

- `docs/progression-service.md`
- `docs/story-context-service.md`
- `docs/questuri-avansate-v2.md`
- `docs/schema-scenariu-predefinit-testare.md`

