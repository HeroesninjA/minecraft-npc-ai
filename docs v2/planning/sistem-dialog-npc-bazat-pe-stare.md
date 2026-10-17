# Sistem de dialog NPC bazat pe stare persistentă

Data: 2026-10-15

Statut: propunere de arhitectură compatibilă cu proiectul. Componentele descrise nu trebuie considerate implementate decât unde secțiunea „Puncte de plecare existente” spune explicit acest lucru.

Document canonic pentru comportamentul curent: [`../architecture/dialog-si-conversatii.md`](../architecture/dialog-si-conversatii.md).

## Verdict

Direcția este potrivită pentru AINPC. Un dialog credibil nu trebuie construit ca un chatbot separat pentru fiecare NPC, ci ca expresia unei stări persistente care include identitate, memorie, cunoștințe, relații, emoții, obiective și contextul lumii.

Modelul generativ rămâne opțional. El poate interpreta contextul și formula o replică naturală, dar nu devine sursa adevărului și nu modifică direct datele persistente sau lumea Minecraft.

Arhitectura trebuie să păstreze trei proprietăți:

1. același NPC rămâne coerent între conversații și restarturi;
2. dialogul continuă într-o formă redusă când providerul AI lipsește sau eșuează;
3. orice acțiune ori schimbare persistentă este validată de cod determinist.

## Principii obligatorii

- **Starea NPC-ului este sursa adevărului.** LLM-ul primește un instantaneu selectat și poate propune o replică sau o intenție, fără acces direct la stocare.
- **Dialogul este o capacitate, nu centrul NPC-ului.** Aceleași date trebuie să poată influența rutine, questuri, comerț, relații și acțiuni autonome.
- **Faptele și afirmațiile sunt distincte.** O informație spusă de un jucător nu devine automat adevăr verificat.
- **Mutațiile sunt explicite și auditate.** Memoria, relația, emoția, credința sau obiectivul se actualizează prin comenzi validate, nu prin text liber generat.
- **Providerul este înlocuibil.** Reguli, șabloane, OpenAI, Ollama sau un provider viitor trebuie să implementeze același contract de generare.
- **Contextul este selectat dinamic.** Promptul primește numai informația relevantă, în limite configurabile de latență și tokenuri.
- **Execuția respectă modelul Paper.** Citirea lumii și acțiunile Bukkit/Paper care cer threadul principal sunt capturate sau executate acolo; inferența și accesul potrivit la DB rămân asincrone.
- **Rezultatul conversației nu este automat permanent.** O glumă, o promisiune, o observație și o faptă confirmată au reguli diferite de retenție și impact.

## Componentele modelului

| Componentă | Responsabilitate | Exemple de stare |
|---|---|---|
| Identity & Personality | Definește cine este NPC-ul și cum preferă să vorbească sau să decidă. | nume, vârstă, ocupație, statut, valori, trăsături, vocabular |
| Memory System | Păstrează experiențe și informații cu proveniență, timp și importanță. | episoade, cunoștințe, amintiri sociale, proceduri învățate |
| Dialogue Context | Construiește instantaneul minim relevant pentru schimbul curent. | istoric, interlocutor, loc, vreme, activitate, memorii relevante |
| Intent & Dialogue State | Urmărește scopul participanților și starea conversației. | întrebare, negociere, refuz, clarificare, închidere, subiect activ |
| Emotion & Social Relations | Modelează starea afectivă temporară și relațiile persistente. | încredere, respect, teamă, afecțiune, resentiment, furie curentă |
| World Knowledge & Perception | Separă realitatea lumii de ceea ce NPC-ul a observat sau a aflat. | observație, zvon, informație profesională, secret, fapt expirat |
| Goal System | Prioritizează obiective pe termen scurt și lung. | protejarea reputației, finalizarea muncii, ajutorarea familiei |
| Belief System | Reprezintă ce crede NPC-ul, sursa și gradul de încredere. | afirmație, autor, dovezi, contradicții, certitudine |
| Dialogue Policy | Decide dacă NPC-ul răspunde, întreabă, evită, refuză sau încheie. | act conversațional, ton permis, informații divulgabile |
| Conversation Lifecycle | Controlează inițierea, continuitatea, întreruperea și expirarea. | stare sesiune, timeout, participant activ, motiv de închidere |
| Action Integration | Transformă o decizie aprobată într-o acțiune observabilă. | magazin, deplasare, quest, chemare ajutor |
| Consistency Validator | Respinge contradicții cu starea, regulile lumii și permisiunile. | fapt inexistent, identitate schimbată, acțiune imposibilă |

### Identitate și personalitate

Profilul stabil trebuie să includă mai mult decât un paragraf pentru prompt:

- identitate, profesie, familie, statut și rol social;
- trăsături stabile, valori, limite și obiective personale;
- stil de vorbire, vocabular și nivel aproximativ de educație;
- preferințe, opinii și convingeri care pot fi incomplete sau greșite;
- reguli deterministe prin care trăsăturile influențează acțiunile și actele conversaționale.

Personalitatea nu trebuie confundată cu emoția curentă. Un NPC calm poate deveni furios într-o situație concretă fără să-și schimbe profilul de bază.

### Memorie

Modelul propus separă cel puțin:

- **memoria episodică:** un eveniment petrecut la un moment și într-un loc;
- **memoria semantică:** informații despre lume, obiecte și persoane;
- **memoria socială:** experiențe și așteptări legate de un interlocutor;
- **memoria procedurală:** pași sau strategii învățate pentru o sarcină.

Fiecare înregistrare trebuie să poată păstra tipul, sursa, momentul, locul, participanții, importanța, impactul emoțional, nivelul de încredere și politica de expirare. Rechemarea trebuie să combine relevanța pentru subiect cu importanța și recența; nu este suficientă încărcarea celor mai noi rânduri.

Consolidarea și uitarea trebuie să fie explicite. Evenimentele importante pot deveni amintiri de durată, în timp ce detaliile banale expiră sau sunt rezumate.

### Credințe și cunoaștere

Realitatea lumii și perspectiva NPC-ului sunt două lucruri diferite. O credință ar trebui să poată indica:

- propoziția sau faptul normalizat;
- sursa: observație, jucător, alt NPC, document sau inferență;
- gradul de încredere și motivele acestuia;
- momentul observării și momentul ultimei confirmări;
- contradicțiile cunoscute și starea `susținută`, `contestată` sau `respinsă`.

Un fierar dintr-un sat îndepărtat nu cunoaște automat un eveniment din altă regiune. Zvonurile se transmit ca informații atribuite, nu ca fapte globale.

### Relații și emoții

Relația trebuie păstrată separat pentru fiecare pereche și nu redusă la o singură valoare de „prietenie”. Dimensiunile minime utile sunt încredere, respect, afecțiune, familiaritate și teamă sau resentiment.

Emoția curentă este temporară, poate avea degradare în timp și poate modifica tonul ori prioritățile. Actualizarea relației trebuie să depindă de evenimentul validat și de istoricul existent, nu doar de clasificarea lexicală a unei singure replici.

### Obiective și politică de dialog

NPC-ul poate urmări simultan obiective concurente. Politica de dialog selectează un act conversațional folosind prioritățile, relația, emoția, permisiunile și riscul social. Poate răspunde direct, cere dovezi, refuza divulgarea unui secret, schimba subiectul, negocia, încheia conversația sau propune o acțiune în joc.

LLM-ul poate ajuta la clasificare ori formulare, dar politica finală trebuie să aplice reguli deterministe pentru permisiuni, questuri, economie și acțiuni privilegiate.

## Fluxul țintă al unei conversații

1. **Captură și percepție:** mesajul, participanții, locația, timpul, vremea, activitatea și evenimentele apropiate sunt capturate într-un instantaneu sigur.
2. **Interpretare:** sunt estimate intenția, subiectul, ambiguitatea, sentimentul și actul conversațional cerut.
3. **Context Builder:** selectează identitatea, istoricul, amintirile, credințele, relația, emoția, obiectivele și faptele relevante.
4. **Dialogue Policy:** decide dacă NPC-ul poate și vrea să răspundă, ce urmărește și ce informații poate folosi.
5. **Realizare:** un generator bazat pe reguli, șabloane sau LLM formulează replica și eventual o propunere structurată de acțiune.
6. **Validare:** verifică formatul, faptele, consistența, permisiunile, resursele și versiunea stării folosite.
7. **Livrare și execuție:** replica este livrată, iar acțiunile aprobate sunt executate prin servicii dedicate.
8. **Actualizare:** numai rezultatele confirmate produc mutații în memorie, credințe, emoții, relații, obiective și istoric.

Contextul trebuie să poarte o revizie. Înainte de aplicarea mutațiilor, sistemul verifică dacă starea relevantă nu s-a schimbat între timp. Comenzile persistente trebuie să fie idempotente pentru a preveni dublarea efectelor după retry.

## Politica de actualizare a stării

Nu orice replică devine memorie sau schimbă relația. Interpretarea produce evenimente tipizate, iar codul determinist decide retenția:

| Tip | Persistență recomandată | Condiție |
|---|---|---|
| glumă sau conversație banală | doar istoric scurt | nu schimbă automat relația |
| afirmație a jucătorului | memorie atribuită | este păstrată ca „jucătorul a spus”, nu ca fapt |
| promisiune | memorie socială | are autor, beneficiar, termen și stare |
| observație directă | episod și posibilă credință | evenimentul a fost capturat de sistem |
| insultă sau ajutor | episod și impact social | severitatea este validată și limitată |
| acțiune executată | fapt și rezultat | executorul confirmă succesul |
| zvon | credință cu certitudine redusă | păstrează lanțul de proveniență |

## Puncte de plecare existente

Proiectul nu pornește de la zero. Starea verificată la data documentului oferă următoarele baze:

| Domeniu | Bază existentă | Limită față de țintă |
|---|---|---|
| personalitate | `NPCPersonality` păstrează trăsături Big Five, descrieri și afinități conversaționale; `DialoguePersonalityPolicy` folosește extraversia pentru alegerea replicilor locale generale de salut și continuare | nu reprezintă încă valori, principii, opinii și un stil social complet; intențiile speciale și acțiunile autonome cer reguli separate |
| emoții | `NPCEmotions` păstrează emoții distincte, aplică efecte și degradare; `DialogueInteractionPolicy` filtrează efectele dialogului | legătura cauzală cu evenimente validate din lume trebuie încă întărită |
| relații player-NPC | `DialogManager` și `NPCRelationship` folosesc afecțiune, încredere, respect și familiaritate; politica limitează schimbările sociale la mesaje adresate NPC-ului, cu formulare directă; `addressClarified` confirmă destinatarul fără să elimine filtrele pentru citate, negații și relatări | actualizarea rămâne legată de post-procesarea dialogului și de euristici lexicale pentru actul social |
| relații NPC-NPC | `RelationshipService` gestionează separat perechi de NPC-uri | contractul trebuie diferențiat clar de relația player-NPC într-o viitoare API |
| memorie | `MemoryManager` persistă memorii, importanță, impact, expirare și afirmații explicite; citirile active exclud rândurile expirate; `getRelevantMemories(...)` grupează în context înregistrările cu text, sursă și zi identice, păstrând numărul lor, apoi selectează lexical amintiri întregi în bugetul `npc.memory_prompt_max_chars` | căutarea semantică, data reală a evenimentului, proveniența completă și tipurile cognitive separate rămân de implementat |
| context | `DialogManager`, `DialogueEngine` și resolverele folosesc istoric, relații, mediu, story, muncă și comerț; căutarea memoriei și reperele promptului reutilizează același subiect pentru continuări, cu prioritate pentru întrebările noi explicite; promptul folosește interlocutorul cererii fără să modifice interacțiunea live, iar decizia AI citește mesajul cererii, nu ultimul mesaj global | lipsește un agregat unic, versionat, imuabil și bugetat pentru toate sursele; izolarea interlocutorului nu îngheață întreaga stare NPC |
| intenții | `DialogueIntentSelector` oferă clasificare locală, iar orchestrarea AI clasifică un set restrâns; clarificarea interlocutorului are un flux determinist separat, fără apel LLM | lipsește o stare conversațională generală cu scopuri și acte de dialog; meniul clarifică destinatarul, nu sensul mesajului |
| sesiuni | adresarea explicită poate schimba interlocutorul apropiat; ambiguitatea oferă 2..8 opțiuni stabile prin UUID, selectabile prin număr sau nume complet normalizat; mesajul original poate fi reluat cel mult o dată în 60 de secunde, după reverificarea sesiunii și a proximității; livrarea replicilor întârziate cere UUID-ul sesiunii neexpirate (5 minute de inactivitate), aceeași instanță NPC în registru, spawn valid și aceeași lume; activitatea nu reactivează sesiuni expirate; un interlocutor indisponibil oprește mesajul fără redirecționare; formulele complete de rămas-bun închid local numai sesiunea destinatarului | clarificările sunt temporare, limitate la 512 și invalidate la schimbarea contextului; rutarea și încheierea rămân lexicale, timeout-ul nu emite singur notificări, deplasarea nu este monitorizată continuu, iar operațiile persistente deja pornite nu sunt anulate |
| percepție | `NPCContext`, `EnvironmentFactResolver` și world/story mapping oferă fapte capturate | nu există încă un model general de observații, proveniență și certitudine |
| decizie | `DecisionEngine` folosește personalitatea, emoțiile, nevoile și contextul | obiectivele persistente și feedback-ul experiență → decizie nu formează încă un circuit complet |
| siguranță | `AIResponseValidator`, resolverele și verificările de profil limitează răspunsurile | lipsește un validator unificat pentru credințe, mutații și acțiuni |

Generarea obișnuită este acum limitată atomic la o cerere activă per pereche jucător-NPC prin `DialogueRequestGate`, până la terminarea post-procesării. Cererile suplimentare primesc `BUSY`, fără coadă sau alt apel AI; perechile diferite sunt independente. Listenerul refuză mesajul ocupat înainte de modificarea contextului, sesiunii sau questului. Comenzile personale explicite rămân disponibile cu regulile și protecțiile de revizie existente; acest guard nu serializează toate mutațiile NPC-ului între jucători diferiți și nu introduce timeout pentru operațiuni blocate.

Această propunere extinde componentele existente și nu justifică înlocuirea lor printr-un al doilea stack paralel.

## Contracte conceptuale

Următoarele interfețe descriu separarea responsabilităților. Ele nu sunt clase confirmate ca implementate, iar numele finale trebuie alese după auditul API-urilor existente.

```kotlin
interface DialogueContextBuilder {
    fun build(request: DialogueRequest): DialogueContext
}

interface MemoryService {
    fun recall(query: MemoryQuery): List<MemoryEntry>
    fun propose(event: ObservedEvent): List<MemoryMutation>
}

interface BeliefService {
    fun relevantBeliefs(npcId: NpcId, topic: Topic): List<Belief>
    fun proposeUpdates(event: ObservedEvent): List<BeliefMutation>
}

interface GoalService {
    fun activeGoals(npcId: NpcId): List<NpcGoal>
}

interface DialoguePolicy {
    fun decide(context: DialogueContext): DialogueDecision
}

interface DialogueGenerator {
    fun generate(decision: DialogueDecision, context: DialogueContext): DialogueDraft
}

interface DialogueValidator {
    fun validate(draft: DialogueDraft, context: DialogueContext): ValidatedDialogue
}

interface NpcActionExecutor {
    fun execute(action: ValidatedNpcAction): ActionResult
}

interface NpcStateMutationService {
    fun apply(command: ValidatedStateMutation): MutationResult
}
```

Implementările generatorului pot fi bazate pe reguli, șabloane sau un adaptor pentru providerul LLM. Toate trebuie să primească aceeași decizie și același context verificat.

Este recomandată separarea explicită dintre relațiile player-NPC și NPC-NPC, chiar dacă unele structuri interne sunt reutilizate. Un nume generic precum `RelationshipService` riscă să ascundă două reguli de persistență și două cicluri de viață diferite.

## Exemplu de comportament

Jucătorul îi spune fierarului:

> Am auzit că ai vândut sabia familiei mele bandiților.

Contextul selectat poate conține:

- un episod în care fierarul a vândut o sabie unui comerciant;
- lipsa unei observații care să confirme că acel comerciant era bandit;
- o personalitate mândră și defensivă;
- încredere redusă în jucător;
- obiectivul de a-și proteja reputația;
- locația curentă în atelier, în timpul programului.

Politica poate alege actul `contestă acuzația și cere dovezi`. Generatorul poate formula:

> Am vândut o sabie unui negustor acum câteva zile. Nu știam să lucreze cu bandiții. Dacă ai dovezi, arată-mi-le înainte să mă acuzi.

Replica nu confirmă vinovăția comerciantului. Dacă jucătorul prezintă ulterior o dovadă validată de joc, sistemul poate adăuga un eveniment, actualiza credința fierarului și modifica o decizie viitoare. Textul jucătorului, singur, nu produce aceeași schimbare.

## Ordinea recomandată de implementare

### Etapa 0 — Contract și măsurare

- definește tipurile de evenimente, sursele adevărului și granița dintre replică, acțiune și mutație;
- inventariază tabelele și API-urile existente pentru a evita stocare duplicată;
- stabilește măsurători pentru latență, tokenuri, contradicții, fallback și dimensiunea memoriei.

Criteriu de ieșire: același scenariu poate fi urmărit printr-un ID de conversație de la input până la mutațiile aprobate.

### Etapa 1 — Identitate și personalitate stabilă

- extinde profilul existent numai cu câmpurile care influențează reguli observabile;
- conectează trăsăturile la politica de dialog și la selecția acțiunilor, nu doar la prompt.

Criteriu de ieșire: două profiluri diferite aleg predictibil acte conversaționale diferite în același scenariu controlat.

### Etapa 2 — Context și stare conversațională

- introduce un `DialogueContext` versionat și bugetat;
- separă subiectul, intenția, actul conversațional și starea ciclului de viață;
- păstrează resolverele factuale existente ca surse deterministe.

Criteriu de ieșire: continuările, clarificările, refuzurile și încheierea pot fi testate fără provider AI.

### Etapa 3 — Memorie tipizată și rechemare relevantă

- clasifică episoadele și afirmațiile existente fără migrare distructivă;
- adaugă proveniență, timp și scor de relevanță;
- implementează consolidare, expirare și limite per NPC/interlocutor.

Criteriu de ieșire: o experiență relevantă schimbă o decizie ulterioară, iar una nerelevantă nu intră inutil în context.

### Etapa 4 — Relații și emoții

- definește reguli de actualizare bazate pe evenimente validate;
- separă efectul temporar emoțional de schimbarea relației persistente;
- limitează acumularea și aplică degradare unde este potrivit.

Criteriu de ieșire: ajutorul, insulta și trădarea produc efecte diferite, explicabile și reproductibile.

### Etapa 5 — Credințe, zvonuri și secrete

- adaugă sursă, certitudine, contradicții și expirare;
- controlează transmiterea informației în funcție de relație, rol și permisiuni;
- nu transformă automat zvonurile în fapte globale.

Criteriu de ieșire: NPC-ul își poate schimba justificat o credință după dovezi și poate explica de unde știe informația.

### Etapa 6 — Obiective autonome

- modelează priorități pe termen scurt și lung;
- leagă politica de dialog și `DecisionEngine` de aceleași obiective;
- permite NPC-ului să inițieze sau să evite o conversație dintr-un motiv verificabil.

Criteriu de ieșire: dialogul și acțiunea aleasă urmăresc același obiectiv, fără două surse divergente de stare.

### Etapa 7 — Integrarea acțiunilor în lume

- introduce propuneri structurate, validare, permisiuni și execuție dedicată;
- confirmă rezultatul înainte de actualizarea memoriei sau a obiectivului;
- păstrează fallback fără acțiune când validarea ori execuția eșuează.

Criteriu de ieșire: o conversație poate produce în siguranță o acțiune observabilă, iar memoria reflectă rezultatul real.

### Etapa 8 — Consistență și evaluare

- testează continuitatea identității, izolarea cunoștințelor și actualizarea credințelor;
- compară generatorul local și cel LLM pe aceleași scenarii;
- măsoară latența, tokenurile, rata de fallback, contradicțiile și creșterea stocării;
- adaugă scenarii de restart, concurență și răspuns construit pe revizie veche.

Criteriu de ieșire: comportamentul critic rămâne corect cu AI activ, dezactivat, indisponibil și după restart.

## Primul vertical slice recomandat

Primul prototip nu trebuie să implementeze toate componentele. Scenariul fierarului este suficient dacă demonstrează complet:

1. o identitate și o trăsătură stabilă;
2. o vânzare observată și persistată ca episod;
3. o acuzație păstrată ca afirmație atribuită jucătorului;
4. o credință cu certitudine redusă despre cumpărător;
5. o decizie de a cere dovezi;
6. prezentarea unei dovezi validate;
7. actualizarea credinței și o reacție diferită la următoarea conversație;
8. același rezultat logic cu generator local și cu generator LLM.

Acest vertical slice verifică arhitectura mai bine decât multe replici spectaculoase fără efect persistent.

## Riscuri de controlat

- **Explozia stocării:** aplică limite, expirare, consolidare și rezumate auditate.
- **Context prea mare:** bugetează fiecare categorie și selectează după relevanță.
- **Contradicții:** păstrează proveniență, versiuni și validare factuală.
- **Latență pe server:** capturează rapid starea Paper și mută lucrul permis în afara threadului principal.
- **Cost AI:** folosește răspunsuri deterministe pentru fapte simple și nu apela modelul pentru fiecare tick sau reflecție internă.
- **Mutații duble:** folosește ID-uri de eveniment, revizii și operații idempotente.
- **Manipulare prin prompt:** tratează mesajul, memoria și textul MCP ca date neîncrezătoare.
- **Cuplare excesivă:** serviciile comunică prin contracte, nu prin acces reciproc la implementări.

## Criterii de succes

- NPC-ul nu afirmă că știe evenimente pe care nu le-a observat sau aflat;
- distinge un fapt verificat de o afirmație ori un zvon;
- o experiență relevantă îi schimbă justificat o decizie viitoare;
- personalitatea influențează politica, nu doar formularea;
- relația și emoția evoluează prin reguli explicabile;
- acțiunile sunt executate numai după validare și permisiuni;
- fallback-ul local păstrează funcționalitatea de bază;
- restartul nu șterge identitatea, relațiile și amintirile persistente;
- costul și latența rămân în limite configurate.

## Legături

- [`../architecture/dialog-si-conversatii.md`](../architecture/dialog-si-conversatii.md)
- [`../architecture/interactiune-dialog-reactie-stack.md`](../architecture/interactiune-dialog-reactie-stack.md)
- [`../architecture/reactie-npc-jucator.md`](../architecture/reactie-npc-jucator.md)
- [`../architecture/environment-context-si-engine.md`](../architecture/environment-context-si-engine.md)
- [`../architecture/comportament-natural-npc-rutine-alocari.md`](../architecture/comportament-natural-npc-rutine-alocari.md)
- [`simulare-constiinta-npc.md`](simulare-constiinta-npc.md)
