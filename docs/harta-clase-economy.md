# Harta claselor pentru economy

Actualizat: 2026-06-25

Acest document este doar documentatie. Nu schimba runtime-ul si nu modifica ordinea de executie.

Pentru orientare rapida, citeste mai intai [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md), apoi [harta claselor de cod](./harta-clase-cod.md).

## Scop

Aceasta harta urmareste subsistemul `economy`: serviciul de economie simpla, magazinele NPC, ofertele de cumparare/vanzare si comanda de top.

## Noduri principale

- `EconomyService` -> serviciul de baza: get/set balance, deposit, withdraw, transfer (fara Vault)
- `ShopService` -> gestioneaza registry-ul de magazine NPC, verificari de affordabilitate, cumparare/vanzare
- `NpcShopDefinition` -> definitia unui magazin NPC (currency, restock policy, permisiuni, oferte)
- `ShopOffer` -> o oferta individuala (item afisat, cost, rezultat, utilizari maxime)
- `ShopCurrency` -> enum: ITEM, VAULT_OPTIONAL
- `RestockPolicy` -> enum pentru politica de reumplere
- `EconomyTopCommand` -> comanda `/ainpc economy top` pentru clasament balante

## Flux principal

`EconomyService` -> gestionare balanta per player in config

`ShopService` -> `NpcShopDefinition` -> `ShopOffer` -> verificare `ShopCurrency` -> tranzactie -> `EconomyService` (daca VAULT)

`EconomyTopCommand` -> citeste balantele -> sorteaza -> afiseaza top

## Relatii utile

- `EconomyService` este independent de Vault; foloseste configuratia pluginului pentru stocare
- `ShopService` depinde de `EconomyService` pentru tranzactii cu monede
- `NpcShopDefinition` si `ShopOffer` sunt modele de date pure
- `EconomyTopCommand` este o comanda admin read-only

## Cum se citeste

1. Incepe cu `EconomyService` (serviciul de baza)
2. Continua cu `ShopService` (magazinele)
3. Treci la `NpcShopDefinition` si `ShopOffer` (modelele de date)
4. Consulta `EconomyTopCommand` pentru comanda de inspectie
