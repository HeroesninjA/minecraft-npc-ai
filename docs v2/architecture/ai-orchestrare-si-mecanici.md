# AI orchestration si mecanici runtime

Status: contract canonic pentru folosirea AI-ului in core.
Actualizat: 2026-07-14.

AI-ul propune, formuleaza si explica. Runtime-ul valideaza, executa si persista.

## Responsabilitate

- construieste contextul necesar capabilitatii solicitate;
- routeaza intentia catre modelul sau serviciul potrivit;
- valideaza raspunsul inainte de folosire;
- transmite efectele aprobate serviciilor deterministe.

## Utilizari permise

- dialog contextual;
- drafturi de quest si story;
- explicatii pentru admin;
- rezumate si propuneri inspectabile;
- generare asistata si tool calls validate.

## Limite

- nu acorda reward-uri si nu decide progresul;
- nu modifica direct DB sau world state;
- nu trateaza promptul sau raspunsul drept sursa de adevar;
- nu defineste transportul MCP sau persistenta snapshot-urilor.

## Legaturi

- `architecture/spring-ai-mcp-serviciu-intern.md`
- `architecture/mcp-runtime-bridge-design.md`
- `canonical/implementat-deja.md`
