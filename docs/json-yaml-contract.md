# Contract JSON/YAML pentru scripturi

Această documentație definește cum integrăm scripturi scrise în `JSON` și `YAML` astfel încât să rămână ușor de citit, ușor de interpretat și compatibile în timp.

Obiectivul nu este să înghețe formatul, ci să stabilească un contract comun: aceleași intenții, aceeași structură logică și aceleași reguli de validare, indiferent dacă sursa este JSON sau YAML.

## Scop

- Oferă o cale unică de interpretare pentru ambele formate.
- Reduce diferențele dintre fișierele scrise manual și cele generate automat.
- Păstrează compatibilitatea fără să blocheze extensiile viitoare.
- Evită apariția unor variante locale de format care nu mai pot fi validate.

## Principii

1. **Un singur model logic**
   - JSON și YAML sunt doar forme de exprimare.
   - În interior, totul se normalizează la același model de date.

2. **Citire umană înainte de optimizare**
   - Structura trebuie să fie clară pentru oameni.
   - Generatorul sau parserul nu trebuie să impună artificii inutile.

3. **Compatibilitate prin extensie**
   - Se adaugă câmpuri noi, nu se rup câmpurile vechi.
   - Schimbările de nume se fac gradual, cu aliasuri.

4. **Validare pe straturi**
   - Sintaxă.
   - Structură.
   - Semnificație.
   - Compatibilitate între versiuni.

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

Varianta echivalentă în JSON păstrează aceleași chei:

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

### 1. Nucleu stabil, extensii opționale

- `type` identifică familia de script.
- `version` indică schema logică.
- `meta` conține identificatori și etichete.
- `spec` conține comportamentul real.

Acest pattern lasă loc pentru câmpuri suplimentare fără să amestece metadatele cu execuția.

### 2. Structuri scurte și previzibile

- Preferă liste atunci când ordinea contează.
- Preferă obiecte atunci când cheia este stabilă.
- Evită adâncimi inutile de 6-8 niveluri.
- Nu dubla aceeași informație în mai multe locuri.

### 3. Chei constante între formate

- Aceleași nume de câmp în JSON și YAML.
- Fără sinonime locale de tipul `questSteps` într-un fișier și `steps` în altul.
- Dacă apare un alias, el trebuie tratat ca backward-compatible, nu ca nou contract.

### 4. Separarea datelor de comentarii

- YAML poate conține comentarii utile pentru autor.
- Comentariile nu fac parte din contract.
- JSON rămâne sursa de referință pentru consum automat doar dacă provine din aceeași normalizare.

## Reguli de compatibilitate

### Compatibilitate acceptată

- Adăugarea de câmpuri noi opționale.
- Adăugarea de valori noi într-o listă de tipuri permise.
- Extinderea unei secțiuni fără a schimba semnificația câmpurilor existente.
- Acceptarea unor aliasuri de nume pentru migrare.

### Schimbări care cer migrare controlată

- Redenumirea unui câmp obligatoriu.
- Schimbarea tipului unui câmp.
- Mutarea unui câmp dintr-o secțiune în alta.
- Schimbarea semnificației unui status existent.

### Ce trebuie evitat

- Să tratăm fișierele diferit doar pentru că sunt JSON sau YAML.
- Să blocăm inputul cu reguli estetice care nu influențează semantica.
- Să introducem câmpuri obligatorii noi fără perioadă de tranziție.

## Validare

Validarea trebuie să fie predictibilă:

1. **Parse**
   - Fișierul se citește în formatul lui nativ.

2. **Normalize**
   - JSON și YAML se transformă în aceeași structură internă.

3. **Validate structural**
   - Se verifică existența câmpurilor cerute și tipurile de bază.

4. **Validate semantic**
   - Se verifică relațiile dintre câmpuri, referințele și valorile permise.

5. **Validate compatibility**
   - Se verifică dacă scriptul respectă versiunea curentă sau una acceptată prin alias.

## Reguli pentru pattern-uri ușor de citit

- Un concept = un nod.
- O intenție = o secțiune.
- O regulă = o cheie clar numită.
- O colecție = o listă scurtă și ordonată.
- O excepție = un câmp opțional explicit, nu o convenție ascunsă.

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

## Integrare în runtime

- Parserul acceptă ambele formate.
- Normalizarea produce același model intern.
- Restul sistemului consumă doar modelul intern.
- Exportul poate genera fie JSON, fie YAML, dar nu trebuie să schimbe semantica.

Astfel, compatibilitatea este păstrată, iar oamenii pot lucra în formatul care li se potrivește mai bine.

## Recomandări practice

- Folosește YAML când scriptul este editat frecvent de oameni.
- Folosește JSON când payload-ul este generat, transmis sau validat mecanic.
- Păstrează aceleași chei în ambele formate.
- Marchează explicit versiunea în fișier.
- Adaugă aliasuri înainte de a renunța la câmpurile vechi.

## Ce nu trebuie limitat

- Nu bloca ordinea câmpurilor dacă nu afectează semantica.
- Nu interzice comentariile YAML la nivel de autor.
- Nu cere structură identică la nivel fizic, doar la nivel logic.
- Nu transforma convențiile de stil în erori de compatibilitate.

## Template standard

Fiecare script nou ar trebui să pornească de la aceeași formă de bază:

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

- `type` rămâne stabil și descriptiv.
- `version` crește doar când contractul logic se schimbă.
- `meta.id` este unic în cadrul familiei de scripturi.
- `spec` conține doar date funcționale.
- Listele goale sunt valide dacă intenția este explicită.

## Migrare controlată

Migrarea trebuie să fie lizibilă și reversibilă pe cât posibil:

1. Adaugă câmpul nou ca opțional.
2. Acceptă vechiul și noul nume în paralel.
3. Marchează vechiul nume ca deprecated în documentație.
4. Actualizează generatorul sau editorul.
5. Elimină aliasul doar după o perioadă clară de compatibilitate.

Exemplu de tranziție:

```yaml
# vechi
spec:
  questName: "Intro"

# nou
spec:
  title: "Intro"
```

În perioada de tranziție, parserul trebuie să accepte ambele forme și să le normalizeze la aceeași cheie internă.

## Event lists, trigger și hook

Fișierele JSON și YAML pot descrie evenimente ca liste ordonate de acțiuni sau ca mapări de declanșatoare către hook-uri. Contractul trebuie să suporte ambele forme fără a impune o singură reprezentare fizică.

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

- `events` este o listă ordonată când ordinea de procesare contează.
- `trigger` identifică momentul sau condiția de activare.
- `hook` indică handlerul logic asociat.
- `name` este opțional, dar util pentru debugging și audit.
- Dacă un eveniment are mai multe hook-uri, ele rămân într-o listă explicită.

### Reprezentare alternativă

În unele cazuri, un map de trigger-uri este mai compact:

```json
{
  "events": {
    "world.loaded": ["init_story"],
    "quest.finished": ["reward_player"]
  }
}
```

Această formă este acceptabilă doar dacă normalizarea internă produce aceeași semnificație ca lista de evenimente. Parserul trebuie să convertească ambele reprezentări în aceeași structură internă.

### Reguli de interpretare

- Un `trigger` activează unul sau mai multe `hook-uri`.
- Un `hook` nu trebuie să depindă de ordinea specifică a câmpurilor din fișier.
- O listă de evenimente păstrează ordinea declarativă.
- O hartă de trigger-uri favorizează căutarea rapidă și poate fi folosită ca formă derivată.
- Dacă există ambiguitate, modelul canonic este lista normalizată de evenimente.

### Compatibilitate pentru hook-uri

- Adăugarea unui hook nou este compatibilă.
- Adăugarea unui trigger nou este compatibilă.
- Redenumirea unui hook cere alias sau adaptor.
- Schimbarea semnificației unui trigger cere versiune nouă.
- Eliminarea unui hook existent cere perioadă de tranziție.

## Quest și mapping

Pentru fișierele care descriu questuri și mapping semantic, contractul trebuie să păstreze separarea dintre intenție și rezolvare.

### Quest

- `quest` descrie intenția de gameplay.
- `objectives` sau `steps` descriu ordinea logică.
- `trigger` marchează momentul de activare.
- `hook` trimite execuția către logica runtime.
- Tinta nu trebuie să depindă doar de coordonate brute.

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

- `mapping` descrie rezolvarea semantică a țintelor.
- `region`, `place` și `node` trebuie să rămână tipuri distincte.
- `tags` și `metadata` sunt baza pentru potrivire flexibilă.
- Rezultatul resolverului trebuie să fie stabil și auditat.
- Mapping-ul poate fi exportat în JSON sau YAML fără schimbare de semnificație.

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

### Regulă comună

- Quest-ul consumă mapping-ul prin identificatori semantici, nu prin coordonate.
- Mapping-ul rămâne sursa de adevăr pentru localizare și ancore.
- JSON și YAML trebuie să producă aceeași structură internă după normalizare.

### Flux end-to-end

1. Quest-ul declară obiectivul și intenția.
2. Trigger-ul pornește obiectivul la un eveniment runtime.
3. Hook-ul cere rezolvarea semantică a țintei.
4. Mapping-ul întoarce un `region`, `place` sau `node` valid.
5. Runtime-ul execută logica fără să depindă de formatul fizic al fișierului.

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

Normalizare așteptată:

- obiectivul `reach_market` devine o cerere semantică de tip `place`;
- `tag:market` se rezolvă la `village_market`;
- `resolve_place_target` primește un `place` stabil, nu o coordonată hardcodată;
- aceeași intenție poate fi serializată identic în JSON fără să se schimbe rezultatul.

## Matrice de compatibilitate

| Schimbare | Compatibilitate | Observație |
| --- | --- | --- |
| Câmp opțional nou | Da | Nu rupe scripturile existente |
| Valoare nouă într-un enum | Da, cu fallback | Consumatorii vechi trebuie să trateze valoarea necunoscută |
| Redenumire de câmp | Parțial | Necesită alias și migrare |
| Mutare de secțiune | Parțial | Necesită normalizare și documentare clară |
| Schimbare de tip | Nu | Necesită versiune nouă sau adaptor |
| Eliminare de câmp obligatoriu | Nu | Necesită roadmap de migrare |

## Faze de implementare

### Faza I - Contract minim

Scop: stabilim un model comun pentru JSON și YAML.

Livrabile:

- `type`, `version`, `meta`, `spec` ca structură standard;
- normalizare internă unică;
- validare structurală de bază;
- exemple simple pentru `quest` și `mapping`.

Gate:

- un fișier JSON și unul YAML produc aceeași structură internă;
- câmpurile obligatorii sunt validate identic în ambele formate.

### Faza II - Eventuri, trigger și hook

Scop: introducem fluxul de execuție declarativă.

Livrabile:

- liste de evenimente ordonate;
- mapări de trigger-uri către hook-uri;
- suport pentru aliasuri și formate alternative;
- normalizare la aceeași reprezentare logică.

Gate:

- aceeași semnificație poate fi exprimată ca listă sau map;
- runtime-ul primește aceeași structură internă.

### Faza III - Quest și mapping semantic

Scop: legăm intenția quest-ului de rezolvarea semantică a mapping-ului.

Livrabile:

- exemple `quest.yaml` și `mapping.yaml`;
- rezolvare prin `region`, `place`, `node`;
- fallback controlat pentru ținte ambigue;
- contract clar pentru `trigger`, `hook` și `target`.

Gate:

- quest-ul nu mai depinde de coordonate brute;
- mapping-ul rămâne sursa de adevăr pentru ancore.

### Faza IV - Migrare și compatibilitate

Scop: păstrăm continuitatea între versiuni.

Livrabile:

- aliasuri pentru câmpuri redenumite;
- reguli de deprecated;
- matrice de compatibilitate pentru schimbări frecvente;
- adaptor pentru formatul vechi când este necesar.

Gate:

- niciun consumator existent nu este rupt de o schimbare de nume;
- eliminarea unui câmp are perioada de tranziție documentată.

### Faza V - Integrare runtime și tooling

Scop: conectăm contractul la fluxul real de utilizare.

Livrabile:

- parser comun pentru JSON și YAML;
- integrare cu quest runtime;
- export/read-only debug pentru validare;
- documente exemple pentru authors și admins.

Gate:

- runtime-ul consumă doar modelul normalizat;
- utilizatorii pot edita în formatul preferat fără diferențe semantice.

### Faza VI - Hardening și extindere

Scop: pregătim contractul pentru extensii viitoare fără blocaje.

Livrabile:

- extensii opționale pe `spec`;
- reguli pentru enum-uri și fallback;
- suport pentru hook-uri suplimentare;
- testare pe cazuri limită și regresii.

Gate:

- adăugarea de date noi nu rupe compatibilitatea;
- extensiile rămân lizibile și validate.

## Legături cu documentația existentă

- `docs/progression-service.md`
- `docs/story-context-service.md`
- `docs/questuri-avansate-v2.md`
- `docs/schema-scenariu-predefinit-testare.md`
