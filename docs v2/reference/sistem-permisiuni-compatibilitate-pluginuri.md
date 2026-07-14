# Sistem de permisiuni si compatibilitate cu alte pluginuri

Acesta este ghidul pentru folosirea permisiunilor Bukkit/Paper fara dependinte pe un manager intern de grupuri.

## Principiu

- AINPC foloseste API-ul standard `hasPermission`;
- nu implementeaza propriul sistem de grupuri sau stocare de permisiuni;
- compatibilitatea cu LuckPerms vine din design, nu din cod special.

## Reguli

- defineste noduri clare si stabile;
- evita hardcode pe nume de grupuri;
- reevalueaza permisiunea la momentul actiunii;
- nu salva local permisiuni de player.
