# Decizii editoriale pentru documentația de audit

Data reviziei: 2026-09-28

Acest document transformă răspunsurile primite în reguli pentru reviziile
următoare ale documentației. Nu modifică încă implementarea aplicației.

## Contextul proiectului

- Mediul țintă este Paper.
- Proiectul este în dezvoltare pe termen lung și nu are încă o versiune lansată.
- Feature-pack-urile sunt administrate de persoane de încredere. Riscul de
  execuție a comenzilor din pack-uri este, prin urmare, documentat ca risc de
  hardening pentru etapa apropiată de lansare, nu ca vulnerabilitate publică
  exploatabilă în configurația actuală.
- SQLite este folosit în dezvoltare. Suportul pentru mai multe baze de date va
  fi stabilit ulterior, înainte de lansare.
- Ollama este o metodă planificată pentru un API AI universal; nu este încă
  criteriu pentru dialogul funcțional din versiunea curentă.

## Reguli pentru raport

- Nu se mai folosește P1/P2 ca politică fixă de produs. Raportul va folosi
  identificatori stabili și va separa explicit impactul de stadiul verificării.
- Fiecare constatare va avea câmpurile: `Tip`, `Dovadă`, `Impact`, `Condiții de
  reproducere`, `Remediere` și `Status de verificare`.
- `Confirmat` înseamnă că există test sau reproducere directă. `Static` înseamnă
  că observația rezultă din citirea codului și nu a fost confirmată end-to-end.
- Afirmațiile condiționale vor fi mutate într-o secțiune distinctă de riscuri
  sau vor fi marcate explicit ca ipoteze.
- Problemele questurilor rămân distincte: cleanup la offline și reload cu pack
  invalid au mecanisme și condiții diferite.
- Problemele shop-ului vor fi descrise ca feature incomplet/implementat prea
  devreme, nu ca funcționalitate universală obligatorie.
- Problemele deploy smoke și log smoke se reunesc într-o singură constatare:
  „smoke-testul nu oferă un verdict de încredere”.
- Problemele providerului AI și validării AI se reunesc în constatarea despre
  fluxul AI incomplet.
- Progresul questului trebuie să fie recuperabil din baza de date la reconnect.
- Un reload eșuat trebuie să păstreze ultimul snapshot valid.
- Investițiile cu durată zero sau negativă nu sunt comportament intenționat.
- Undo-ul WorldEdit trebuie analizat pentru persistență permanentă atunci când
  lumea depinde de WorldEdit.
- Numerele de linie nu sunt considerate identificatori permanenți; revizia
  următoare va prefera simboluri și fișiere, folosind linii doar ca orientare.
- Documentația finală va fi împărțită pe documente: defecte, securitate,
  arhitectură și plan de remediere. Rapoartele istorice vor fi referențiate și
  reconciliate printr-un index, nu copiate necontrolat.
- Data reviziei se actualizează la fiecare modificare editorială semnificativă.

## Încă neclar

- „Butoanele shop conform scenariului de utilizare” trebuie definit printr-un
  scenariu concret înainte de a clasifica lipsa handlerului ca defect.
- Disponibilitatea efectivă a unui server Paper, VPS, Ollama și a unei baze
  MySQL/MariaDB trebuie confirmată separat când încep testele end-to-end.
