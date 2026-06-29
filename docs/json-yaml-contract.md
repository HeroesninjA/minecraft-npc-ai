# Contract JSON/YAML pentru scripturi

Pentru orientare in cod, foloseste [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md) si apoi [harta completa](./harta-pachetelor-cod.md).

Aceasta documentatie defineste cum integram scripturi scrise in JSON si YAML astfel incat sa ramana usor de citit, usor de interpretat si compatibile in timp.

Obiectivul nu este sa inghete formatul, ci sa stabileasca un contract comun: aceleasi intentii, aceeasi structura logica si aceleasi reguli de validare, indiferent daca sursa este JSON sau YAML.

## Scop

- Ofera o cale unica de interpretare pentru ambele formate.
- Reduce diferentele dintre fisierele scrise manual si cele generate automat.
- Pastreaza compatibilitatea fara sa blocheze extensiile viitoare.
- Evita aparitia unor variante locale de format care nu mai pot fi validate.

## Principii

1. **Un singur model logic**
   - JSON si YAML sunt doar forme de exprimare.
   - In interior, totul se normalizeaza la acelasi model de date.

2. **Citire umana inainte de optimizare**
   - Structura trebuie sa fie clara pentru oameni.
   - Generatorul sau parserul nu trebuie sa impuna artificii inutile.

3. **Compatibilitate prin extensie**
   - Se adauga campuri noi, nu se rup campurile vechi.
   - Schimbarile de nume se fac gradual, cu aliasuri.

4. **Validare pe straturi**
   - Sintaxa.
   - Structura.
   - Semnificatie.
   - Compatibilitate intre versiuni.

## Modelul real: Feature Pack YAML

Formatul folosit in proiect este **Feature Pack YAML**, incarcat prin `FeaturePackLoader` din `packs/*.yml`.

### Structura standard

```yaml
id: pack_id
name: "Nume pack"
description: "Descriere."
addon:
  type: "feature"
  version: "1.0.0"
  runtime_modes: ["standalone", "hybrid", "advanced"]
  capabilities: ["quest", "traits", "dialogues"]
  dependencies: ["medieval"]

scenarios:
  quest_id:
    name: "Nume quest"
    description: "Descriere."
    base_type: "QUEST"        # QUEST | BOUNTY | WORLD_EVENT | TUTORIAL | RITUAL | TRADE_DEAL | DUTY
    mechanic: "side_quests"
    quest:
      code: "Q01"
      giver: "npc_profession"
      kind: "side"            # side | main | repeatable
      category: "tutorial"
      repeatable: false
      cooldown_seconds: 0
      prerequisites: ["Q00"]
      next_quest: "Q02"
      dialogues:
        available: ["Text pentru oferta."]
        active: ["Text pentru progres."]
        completed: ["Text pentru finalizare."]
      objectives:
        obj_id:
          type: "visit_place"    # vezi tabelul cu tipuri
          item: "tag:market"
          amount: 1
          description: "Descriere"
      rewards:
        rew_id:
          type: "item"
          item: "EMERALD"
          amount: 3
          description: "Recompensa"
```

Varianta JSON pastreaza aceleasi chei (vezi exemple in `docs/json-yaml-contract-exemple.md`).

### Pattern-uri

### 1. Nucleu stabil, extensii optionale

- `id` si `name` identifica pack-ul.
- `addon` declara capabilitati si dependinte.
- `scenarios` contine questurile si mecanica.
- `quest` pastreaza dialogurile, obiectivele si recompensele.

Acest pattern permite adaugarea de campuri suplimentare (ex: `stages`, `actors`, `conditions`) fara a rupe pack-urile existente.

### 2. Structuri scurte si previzibile

- Prefera liste atunci cand ordinea conteaza.
- Prefera obiecte atunci cand cheia este stabila.
- Evita adancimi inutile de 6-8 niveluri.
- Nu dubla aceeasi informatie in mai multe locuri.

### 3. Chei constante intre formate

- Aceleasi nume de camp in JSON si YAML.
- Fara sinonime locale de tipul `questSteps` intr-un fisier si `steps` in altul.
- Daca apare un alias, el trebuie tratat ca backward-compatible, nu ca nou contract.

### 4. Separarea datelor de comentarii

- YAML poate contine comentarii utile pentru autor.
- Comentariile nu fac parte din contract.
- JSON ramane sursa de referinta pentru consum automat doar daca provine din aceeasi normalizare.

## Reguli de compatibilitate

### Compatibilitate acceptata

- Adaugarea de campuri noi optionale.
- Adaugarea de valori noi intr-o lista de tipuri permise.
- Extinderea unei sectiuni fara a schimba semnificatia campurilor existente.
- Acceptarea unor aliasuri de nume pentru migrare.

### Schimbari care cer migrare controlata

- Redenumirea unui camp obligatoriu.
- Schimbarea tipului unui camp.
- Mutarea unui camp dintr-o sectiune in alta.
- Schimbarea semnificatiei unui status existent.

### Ce trebuie evitat

- Sa tratam fisierele diferit doar pentru ca sunt JSON sau YAML.
- Sa blocam inputul cu reguli estetice care nu influenteaza semantica.
- Sa introducem campuri obligatorii noi fara perioada de tranzitie.

## Validare

Validarea trebuie sa fie predictibila:

1. **Parse**
   - Fisierul se citeste in formatul lui nativ.

2. **Normalize**
   - JSON si YAML se transforma in aceeasi structura interna.

3. **Validate structural**
   - Se verifica existenta campurilor cerute si tipurile de baza.

4. **Validate semantic**
   - Se verifica relatiile dintre campuri, referintele si valorile permise.

5. **Validate compatibility**
   - Se verifica daca scriptul respecta versiunea curenta sau una acceptata prin alias.

## Reguli pentru pattern-uri usor de citit

- Un concept = un nod.
- O intentie = o sectiune.
- O regula = o cheie clar numita.
- O colectie = o lista scurta si ordonata.
- O exceptie = un camp optional explicit, nu o conventie ascunsa.

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

## Integrare in runtime

- Parserul accepta ambele formate.
- Normalizarea produce acelasi model intern.
- Restul sistemului consuma doar modelul intern.
- Exportul poate genera fie JSON, fie YAML, dar nu trebuie sa schimbe semantica.
- Loaderul runtime poate citi quest definitions din `packs/*.yml` fara sa schimbe contractul logic.
- Mapping-ul world admin poate fi incarcat si din `world-admin.yml` ca overlay optional peste config-ul de baza.
- Debug dump-ul expune `world-mapping.json`, `loaded-quest-definitions.json`, `player-progressions.json`, `story-states.json` si `story-events.json` ca imagini read-only ale starii curente.

Exemple concrete sunt in `docs/json-yaml-contract-exemple.md`.

Astfel, compatibilitatea este pastrata, iar oamenii pot lucra in formatul care li se potriveste mai bine.

## Recomandari practice

- Foloseste YAML cand scriptul este editat frecvent de oameni.
- Foloseste JSON cand payload-ul este generat, transmis sau validat mecanic.
- Pastreaza aceleasi chei in ambele formate.
- Marcheaza explicit versiunea in fisier.
- Adauga aliasuri inainte de a renunta la campurile vechi.

## Ce nu trebuie limitat

- Nu bloca ordinea campurilor daca nu afecteaza semantica.
- Nu interzice comentariile YAML la nivel de autor.
- Nu cere structura identica la nivel fizic, doar la nivel logic.
- Nu transforma conventiile de stil in erori de compatibilitate.

## Template standard

Fiecare script nou ar trebui sa porneasca de la aceeasi forma de baza:

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

- `type` ramane stabil si descriptiv.
- `version` creste doar cand contractul logic se schimba.
- `meta.id` este unic in cadrul familiei de scripturi.
- `spec` contine doar date functionale.
- Listele goale sunt valide daca intentia este explicita.

## Migrare controlata

Migrarea trebuie sa fie lizibila si reversibila pe cat posibil:

1. Adauga campul nou ca optional.
2. Accepta vechiul si noul nume in paralel.
3. Marcheaza vechiul nume ca deprecated in documentatie.
4. Actualizeaza generatorul sau editorul.
5. Elimina aliasul doar dupa o perioada clara de compatibilitate.

Exemplu de tranzitie:

```yaml
# vechi
spec:
  questName: "Intro"

# nou
spec:
  title: "Intro"
```

In perioada de tranzitie, parserul trebuie sa accepte ambele forme si sa le normalizeze la aceeasi cheie interna.

## Evenimente, trigger si hook

Fisierele JSON si YAML pot descrie evenimente ca liste ordonate de actiuni sau ca mapari de declansatoare catre hook-uri. Contractul trebuie sa suporte ambele forme fara a impune o singura reprezentare fizica.

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

- `events` este o lista ordonata cand ordinea de procesare conteaza.
- `trigger` identifica momentul sau conditia de activare.
- `hook` indica handlerul logic asociat.
- `name` este optional, dar util pentru debugging si audit.
- Daca un eveniment are mai multe hook-uri, ele raman intr-o lista explicita.

### Reprezentare alternativa

In unele cazuri, un map de trigger-uri este mai compact:

```json
{
  "events": {
    "world.loaded": ["init_story"],
    "quest.finished": ["reward_player"]
  }
}
```

Aceasta forma este acceptabila doar daca normalizarea interna produce aceeasi semnificatie ca lista de evenimente. Parserul trebuie sa converteasca ambele reprezentari in aceeasi structura interna.

### Reguli de interpretare

- Un `trigger` activeaza unul sau mai multe `hook-uri`.
- Un `hook` nu trebuie sa depinda de ordinea specifica a campurilor din fisier.
- O lista de evenimente pastreaza ordinea declarativa.
- O harta de trigger-uri favorizeaza cautarea rapida si poate fi folosita ca forma derivata.
- Daca exista ambiguitate, modelul canonic este lista normalizata de evenimente.

### Compatibilitate pentru hook-uri

- Adaugarea unui hook nou este compatibila.
- Adaugarea unui trigger nou este compatibila.
- Redenumirea unui hook cere alias sau adaptor.
- Schimbarea semnificatiei unui trigger cere versiune noua.
- Eliminarea unui hook existent cere perioada de tranzitie.

## Quest si mapping

Pentru fisierele care descriu questuri si mapping semantic, contractul trebuie sa pastreze separarea dintre intentie si rezolvare.

### Quest

- `quest` descrie intentia de gameplay.
- `objectives` sau `steps` descriu ordinea logica.
- `trigger` marcheaza momentul de activare.
- `hook` trimite executia catre logica runtime.
- Tinta nu trebuie sa depinda doar de coordonate brute.

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

- `mapping` descrie rezolvarea semantica a tintelor.
- `region`, `place` si `node` trebuie sa ramana tipuri distincte.
- `tags` si `metadata` sunt baza pentru potrivire flexibila.
- Rezultatul resolverului trebuie sa fie stabil si auditat.
- Mapping-ul poate fi exportat in JSON sau YAML fara schimbare de semnificatie.

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

### Regula comuna

- Quest-ul consuma mapping-ul prin identificatori semantici, nu prin coordonate.
- Mapping-ul ramane sursa de adevar pentru localizare si ancore.
- JSON si YAML trebuie sa produca aceeasi structura interna dupa normalizare.

### Flux end-to-end

1. Quest-ul declara obiectivul si intentia.
2. Trigger-ul porneste obiectivul la un eveniment runtime.
3. Hook-ul cere rezolvarea semantica a tintei.
4. Mapping-ul intoarce un `region`, `place` sau `node` valid.
5. Runtime-ul executa logica fara sa depinda de formatul fizic al fisierului.

Exemplu complet:

```yaml
# quest.yaml
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

Normalizare asteptata:

- obiectivul `reach_market` devine o cerere semantica de tip `place`;
- `tag:market` se rezolva la `village_market`;
- `resolve_place_target` primeste un `place` stabil, nu o coordonata hardcodata;
- aceeasi intentie poate fi serializata identic in JSON fara sa se schimbe rezultatul.

## Matrice de compatibilitate

| Schimbare | Compatibilitate | Observatie |
| --- | --- | --- |
| Camp optional nou | Da | Nu rupe scripturile existente |
| Valoare noua intr-un enum | Da, cu fallback | Consumatorii vechi trebuie sa trateze valoarea necunoscuta |
| Redenumire de camp | Partial | Necesita alias si migrare |
| Mutare de sectiune | Partial | Necesita normalizare si documentare clara |
| Schimbare de tip | Nu | Necesita versiune noua sau adaptor |
| Eliminare de camp obligatoriu | Nu | Necesita roadmap de migrare |

## Faze de implementare

### Faza I - Contract minim

Scop: stabilim un model comun pentru JSON si YAML.

Livrabile:

- `type`, `version`, `meta`, `spec` ca structura standard;
- normalizare interna unica;
- validare structurala de baza;
- exemple simple pentru `quest` si `mapping`.

Gate:

- un fisier JSON si unul YAML produc aceeasi structura interna;
- campurile obligatorii sunt validate identic in ambele formate.

### Faza II - Eventuri, trigger si hook

Scop: introducem fluxul de executie declarativa.

Livrabile:

- liste de evenimente ordonate;
- mapari de trigger-uri catre hook-uri;
- suport pentru aliasuri si formate alternative;
- normalizare la aceeasi reprezentare logica.

Gate:

- aceeasi semnificatie poate fi exprimata ca lista sau map;
- runtime-ul primeste aceeasi structura interna.

### Faza III - Quest si mapping semantic

Scop: legam intentia quest-ului de rezolvarea semantica a mapping-ului.

Livrabile:

- exemple `quest.yaml` si `mapping.yaml`;
- rezolvare prin `region`, `place`, `node`;
- fallback controlat pentru tinte ambigue;
- contract clar pentru `trigger`, `hook` si `target`.

Gate:

- quest-ul nu mai depinde de coordonate brute;
- mapping-ul ramane sursa de adevar pentru ancore.

### Faza IV - Migrare si compatibilitate

Scop: pastram continuitatea intre versiuni.

Livrabile:

- aliasuri pentru campuri redenumite;
- reguli de deprecated;
- matrice de compatibilitate pentru schimbari frecvente;
- adaptor pentru formatul vechi cand este necesar.

Gate:

- niciun consumator existent nu este rupt de o schimbare de nume;
- eliminarea unui camp are perioada de tranzitie documentata.

### Faza V - Integrare runtime si tooling

Scop: conectam contractul la fluxul real de utilizare.

Livrabile:

- parser comun pentru JSON si YAML;
- integrare cu quest runtime;
- export/read-only debug pentru validare;
- documente exemple pentru authors si admins.

Gate:

- runtime-ul consuma doar modelul normalizat;
- utilizatorii pot edita in formatul preferat fara diferente semantice.

### Faza VI - Hardening si extindere

Scop: pregatim contractul pentru extensii viitoare fara blocaje.

Livrabile:

- extensii optionale pe `spec`;
- reguli pentru enum-uri si fallback;
- suport pentru hook-uri suplimentare;
- testare pe cazuri limita si regresii.

Gate:

- adaugarea de date noi nu rupe compatibilitatea;
- extensiile raman lizibile si validate.

## Legaturi cu documentatia existenta

- `docs/progression-service.md`
- `docs/story-context-service.md`
- `docs/questuri-avansate-v2.md`
- `docs/schema-scenariu-predefinit-testare.md`

