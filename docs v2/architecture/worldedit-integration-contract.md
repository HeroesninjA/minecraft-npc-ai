# WorldEdit Integration Contract

Acesta este contractul pentru integrarea optionala WorldEdit ca executant de constructii.

## Principiu

- WorldEdit este optional;
- core-ul defineste contractele si capabilitatile;
- adapterul executa, nu planifica gameplay;
- dry-run si rollback explicit sunt obligatorii pentru patch-uri automate.

## Module

- `ainpc-core-plugin` defineste interfetele si ordinea de aplicare;
- `ainpc-api` expune DTO-uri stabile fara tipuri WorldEdit;
- un modul optional de integrare executa paste-ul, rotirea si rollback-ul.
