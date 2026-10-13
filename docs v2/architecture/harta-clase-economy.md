# Harta claselor pentru economy

Status: canonical in `docs v2`.
Actualizat: 2026-10-12.

Harta scurta pentru subsistemul de economie simpla si magazine NPC.

## Noduri principale

- `EconomyService`;
- `ShopService`;
- `ShopConfigLoader`;
- `NpcShopDefinition`;
- `ShopOffer`;
- `ShopCurrency`;
- `RestockPolicy`;
- `EconomyTopCommand`.

## Flux

- economy store;
- shop offer evaluation;
- transaction;
- top command read-only.

## Legaturi

- `ShopService` asociaza magazinele cu NPC-uri prin `npcRole`, comparat cu ocupatia NPC-ului. Daca magazinul defineste `place_types` sau `place_ids`, `NpcShopAssociation` cere si ca `workAnchor` sa fie intr-o cladire mapata cu tipul si/sau ID-ul permis; `SHOP_ONLY` cere o oferta valida intr-un magazin pentru care jucatorul are permisiune.
- `ShopOffer.configurationIssue()` este verificarea comuna pentru tranzactie, catalogul relatat de NPC si butonul din `ShopGui`; ofertele invalide ori inaccesibile raman vizibile in GUI cu motivul blocarii, dar nu pot fi cumparate prin click.
- `NpcShopFactResolver` poate raspunde despre produse si locul magazinului din ofertele configurate valid si din locul de munca mapat. Calea activa a dialogului ii transmite numai magazinele accesibile jucatorului; nu garanteaza stocul sau plata.
- `workAnchor` indica pozitia locului de munca. Dialogul cauta cladirea mapata prin lume si coordonate, apoi poate mentiona numele ei in raspunsurile despre meserii.
- Un magazin fara `place_types` si `place_ids` ramane asociat doar prin rol pentru compatibilitate; niciunul dintre mecanisme nu dovedeste stoc disponibil.

## Configurarea magazinelor

La prima pornire, pluginul copiaza `shops.yml` in directorul sau de date. Fisierul include magazine pentru ocupatiile generate la piata, fierarie, ferma, taverna si atelier: `negustor`, `fierar`, `fermier`, `hangiu`, `mestesugar`. `ShopConfigLoader` incarca ofertele la pornire si la reincarcarea continutului; `/ainpc reload` reincarca si el continutul. O eroare de sintaxa, un material necunoscut sau o cantitate invalida pastreaza catalogul incarcat anterior si este raportata in jurnal. Nu se suprascrie fisierul editat de administrator.

Fiecare intrare `shops.<id>` are `npc_role`, `place_types` si `place_ids` optionale, `currency` (`ITEM` implicit sau `VAULT_OPTIONAL`), `restock_policy` (`NEVER` implicit), `permission` optional si `offers.<id>`. O oferta are `display_item`, `cost_items` si `result_items` cu perechi material-cantitate; `display_amount` este implicit 1. Exemplu:

```yaml
shops:
  piata_satului:
    npc_role: negustor
    place_types: [MARKET, SHOP]
    currency: ITEM
    offers:
      paine:
        display_item: BREAD
        cost_items:
          EMERALD: 1
        result_items:
          BREAD: 4
```

Ocupatia trebuie sa corespunda rolului NPC-ului, fara diferenta intre litere mari si mici. `place_types` accepta tipuri precum `MARKET`, `SHOP`, `FORGE`, `FARM`, `TAVERN`; `place_ids` accepta ID-urile exacte ale locurilor mapate, de exemplu `place_ids: ["demo_sat:piata"]`. Daca ambele liste sunt definite, cladirea de la `workAnchor` trebuie sa corespunda ambelor restrictii. Comparatia ID-urilor ignora diferenta dintre litere mari si mici. Magazinele implicite folosesc doar `place_types`, deci functioneaza in orice asezare cu tipul potrivit; un magazin fara ambele liste continua sa functioneze doar pe rol. `max_uses` pozitiv si `requirements` nevida pot fi citite din configuratie, dar nu sunt aplicate: oferta apare dezactivata in GUI si nu intra in catalogul relatat de NPC. `restock_policy` este doar metadada in fluxul actual, fara stoc/reaprovizionare. Pentru moneda `VAULT_OPTIONAL`, serviciul foloseste bani numai cand este disponibil serviciul de economie; in caz contrar foloseste itemele configurate la cost.

## Referinte

- `reference/documentatie-api.md`
- `architecture/harta-clase-addons.md`
