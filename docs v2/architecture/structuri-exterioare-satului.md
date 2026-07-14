# Structuri exterioare satului

Acesta este contractul pentru structurile care exista in afara satului principal.

## Scop

- standardizeaza castelul, padurea, fantana izolata, casa izolata, mini-satul, satul de barbari si alte zone de explorare;
- leaga structurile de `Region`, `Place`, `Node`, `SettlementPlan` si quest anchors;
- pastreaza schema extensibila semantic.

## Reguli

- tipurile noi trebuie sa fie indexabile semantic prin `type`, `tags`, `aliases`, `family` si `questAnchors`;
- structurile exterioare nu sunt decor fara semantica;
- fluxul corect merge de la structura fizica sau planificata spre mapping si apoi spre quest/story/runtime.
