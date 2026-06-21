# D2: Mapping demo_sat â€” Verificari pe Server Paper

Pentru orientare in cod, foloseste [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md) si apoi [harta completa](./harta-pachetelor-cod.md).

Pentru T011-T017 â€” rulati pe serverul Paper.

## T011: Creare harta demo

```powershell
# Intrati in joc, mergeti intr-o zona relativ plata, rulati:

Pentru orientare in cod, foloseste [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md) si apoi [harta completa](./harta-pachetelor-cod.md).
/ainpc world demo create demo_sat
```

**Verificati:**
- Comanda se executa fara erori
- Se creeaza regiunea `demo_sat`
- Exista cel putin un loc de tip `house`, un `workplace`, un `social`

**Gate:** Harta creata cu succes.

## T012: Verificare regiuni si locuri

```
/ainpc world places demo_sat
/ainpc world region info demo_sat
/ainpc world whereami
```

**Verificati:**
- Regiunea `demo_sat` exista cu tipul corect
- Minim 3 locuri (case, piata, work)
- Fiecare loc are tip si coordonate valide
- `/ainpc world whereami` arata aceeasi regiune

**Gate:** Structura hartii e corecta.

## T013: Verificare noduri

```
/ainpc world region info demo_sat
```

**Verificati:**
- Exista minim un nod de tip `quest_board` sau `interaction`
- Nodurile sunt in interiorul locurilor corecte

**Gate:** Noduri exista si sunt bine plasate.

## T014: Salvare harta

```
/ainpc world save
```

Apoi restartati serverul si verificati:
```
/ainpc world places demo_sat    # Aceleasi date ca inainte
```

**Gate:** Harta se salveaza si se restauraza corect.

## T015: Auto-index

1. Cu `world_admin.auto_index.enabled: true` in config.yml, rulati:
   ```
   /ainpc world whereami
   ```
2. Dezactivati auto-indexul in config.yml (`auto_index.enabled: false`), reload:
   ```
   /ainpc reload
   /ainpc world whereami
   ```
3. Rezultatul trebuie sa fie acelasi.

**Gate:** Auto-indexul nu schimba rezultatul functional.

## T016: World audit

```
/ainpc audit world
```

**Verificati:**
- Nu exista gap-uri critice
- Toate locurile sunt complete
- Regiunea e corect formatata
- Orice WARN sau FAIL trebuie documentat

**Gate:** Audit fara erori critice.

## T017: Persistenta mapping

1. Dupa salvare (`/ainpc world save`), opriti serverul
2. Porniti serverul din nou
3. Verificati:
   ```
   /ainpc world places demo_sat    # Aceleasi date
   /ainpc world region info demo_sat  # Aceleasi date
   ```

**Gate:** Toate datele de mapping supravietuiesc restartului.

