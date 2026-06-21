# D4: Routine & UX â€” Verificari pe Server Paper

Pentru orientare in cod, foloseste [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md) si apoi [harta completa](./harta-pachetelor-cod.md).

## T025: Rutina NPC

```
/ainpc routine status nearest
```

**Verificati:**
- Se afiseaza slotul curent (home/work/social)
- Explicatia e lizibila
- Daca nu exista NPC, mesaj clar (nu eroare)

**Gate:** Rutina inspectabila.

## T026: Tick rutina

```
/ainpc routine tick
```

**Verificati:**
- Admin summary: total/evaluated/moved/skip
- Fara spam in actionbar/chat

**Gate:** Tick raspunde cu sumar.

## T027: Interactiune click dreapta

Click dreapta pe un NPC.

**Verificati:**
- Se deschide interactiunea
- Se vede actiunea urmatoare clara

**Gate:** Interactiunea functioneaza.

## T028: NPC moves

Observati NPC-ul 1-2 minute.

**Verificati:**
- Nu sare haotic intre ancore
- Cooldown-ul functioneaza
- Teleportul e doar fallback

**Gate:** NPC-ul are comportament rezonabil.

## T029: GUI hub

```
/ainpc gui
/ainpc gui quest all
/ainpc gui routine
/ainpc gui world
/ainpc gui story
```

**Gate:** Hub-ul GUI si toate sectiunile se deschid.

## T030: Nearest selector

```
/ainpc info nearest
/ainpc quest nearest
```

**Gate:** Selectorul nearest e explicit, nu nume de NPC.

