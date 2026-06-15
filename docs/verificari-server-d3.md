# D3: NPC Population & Bindings — Verificari pe Server Paper

## T018: Planificare asezare

```
/ainpc world settlement plan demo_sat 5
```

**Verificati:**
- Se afiseaza casele disponibile
- Nu sunt erori de planificare
- Planul include locatiile de spawn

**Gate:** Planul se genereaza fara erori.

## T019: Spawn asezare

```
/ainpc world settlement spawn demo_sat 5
/ainpc list
```

**Verificati:**
- Se creeaza 3-5 NPC-uri
- Fiecare NPC are entitate (villager)
- NPC-urile apar in `/ainpc list`
- Rollback practic la esec partial

**Gate:** Sat populat, NPC-uri vizibile.

## T020: Verificare NPC-uri

```
/ainpc list
/ainpc info <numeNPC>
```

**Verificati pentru fiecare NPC:**
- Are nume si display name
- Are ocupatie
- Are profil creat in DB
- Este spawnat sau asteapta chunk-ul corect
- Nu exista duplicate in acelasi chunk

**Gate:** NPC-uri valide.

## T021: NPC bindings

```
/ainpc world bindings <numeNPC>
```

**Verificati:**
- homePlaceId setat
- workPlaceId setat
- socialPlaceId setat
- Locurile exista in `demo_sat`

**Gate:** Fiecare NPC are toate cele 3 bindings.

## T022: Audit NPC

```
/ainpc audit npc
```

**Gate:** Audit trece fara erori.

## T023: Audit spawn

```
/ainpc audit spawn
```

**Gate:** Spawn audit trece.

## T024: NPC persist dupa restart

1. Oprire server
2. Pornire server
3. `/ainpc list` — acelasi numar de NPC-uri
4. `/ainpc world bindings nearest` — aceleasi date

**Gate:** NPC-urile supravietuiesc restartului.
