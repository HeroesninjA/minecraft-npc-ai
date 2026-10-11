# Simulare de conștiință pentru NPC-uri: momentul potrivit și etape

Data: 2026-10-07

Statut: recomandare de planificare, nu funcționalitate implementată sau validată în runtime.

## Concluzie

O variantă simplă poate fi abordată gradual. Un sistem amplu de „conștiință simulată” ar fi prematur înainte de validarea circuitului dintre percepție, acțiuni, consecințe și memorie.

Prin acest termen înțelegem simularea unor funcții cognitive: urmărirea stării proprii, memorarea experiențelor, autoevaluare și adaptarea deciziilor. Aceste comportamente nu demonstrează existența unei experiențe subiective.

## Mediul necesar

Prioritatea este un mediu coerent, nu unul foarte complex. NPC-ul trebuie să poată observa o situație, să acționeze și să perceapă rezultatul acțiunii. Un scenariu restrâns este suficient pentru un prim prototip.

Exemplu propus: NPC-ului îi este foame → caută hrană → un jucător îl ajută → reține experiența → într-o situație ulterioară comparabilă, preferă să ceară ajutor acelui jucător.

Exemplul este un obiectiv de verificare, nu o descriere a unui comportament confirmat în proiect.

## Puncte de plecare în proiect

Contextul proiectului identifică `MemoryManager` și `EnvironmentEngine` drept componente relevante pentru memorie și percepția mediului. Existența lor nu confirmă integrarea într-un circuit cognitiv complet. Înainte de implementare trebuie verificate conexiunile reale dintre observații, decizii, execuția acțiunilor și actualizarea memoriei.

Documentație conexă:

- [Contextul mediului](../architecture/environment-context-si-engine.md)
- [Comportament natural, rutine și alocări](../architecture/comportament-natural-npc-rutine-alocari.md)
- [Simularea satului și lumii](../architecture/simulare-sat-si-lume.md)

## Etape recomandate

1. **Circuit comportamental minim:** percepție → nevoie → decizie → acțiune → rezultat → memorie. Alege un singur scenariu și verifică efectul experienței asupra unei decizii ulterioare.
2. **Autoevaluare limitată:** urmărește ce încerca NPC-ul, dacă a reușit și ce trebuie schimbat. Autoevaluarea trebuie să influențeze o alegere observabilă.
3. **Extindere graduală:** adaugă obiective concurente, relații și reflecție asupra experiențelor după ce circuitul simplu funcționează.

Monologul interior permanent și reflecțiile LLM frecvente se amână până când pot modifica efectiv acțiunile. Dialogul convingător, singur, nu validează adaptarea comportamentală.

## Criteriul de trecere la etapa următoare

Întrebarea de verificare este: „Poate NPC-ul să aleagă diferit data viitoare din cauza unei experiențe anterioare?”

Pentru prototip, urmărește observația inițială, acțiunea executată, rezultatul, memoria creată și decizia ulterioară. Compară situații echivalente cu și fără experiența memorată, pentru a distinge efectul memoriei de variația aleatorie a răspunsului.

Extinderea este justificată când schimbarea deciziei poate fi explicată prin experiența anterioară și produce un efect în lume. Această notă nu stabilește un calendar de implementare și nu înlocuiește verificarea runtime.
