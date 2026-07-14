# Redundanta si Consolidare

Acesta este rezumatul curat al analizei de suprapunere dintre documente.

## Scop

- identifica zonele de documentatie care repeta acelasi contract;
- indica unde merita consolidare fara a rupe lanturile canonice;
- pastreaza doar cand un document aduce o perspectiva operationala distincta.

## Zone candidate

- `architecture/interactiuni.md`, `architecture/dialog-si-conversatii.md` si `architecture/reactie-npc-jucator.md` formeaza un lant comun player -> NPC -> context -> reactie;
- `architecture/mapping.md`, `guides/mapping-harti-manuale.md` si `reference/mapping-pentru-implementari-ulterioare.md` descriu acelasi spatiu semantic cu niveluri diferite de detaliu;
- `planning/questuri-faza-1-stabilizare.md`, `planning/pregatire-questuri-avansate.md` si `planning/questuri-avansate-v2.md` pot fi citite ca un singur lant de evolutie.

## Decizii aplicate

- `canonical/constitutie-proiect.md` este unica sursa pentru regulile proiectului;
- `canonical/constitusional.md` este doar alias de compatibilitate;
- `canonical/implementat-deja.md` este unica sursa pentru starea confirmata;
- `reference/analiza-stare-dezvoltare.md` si `reference/sumar-implementare-demo.md` sunt documente derivate.
- pentru fluxul player-NPC, interactiunea, reactia si dialogul au contracte distincte, iar `architecture/interactiune-dialog-reactie-stack.md` este doar index derivat.
- pentru stack-ul AI-MCP, politica AI, limita sidecar-ului si bridge-ul runtime au contracte distincte, iar `reference/ai-orchestrare-mcp-stack.md` este doar index derivat.
- pentru story si quest AI, story state-ul, proiectia read-only, consumul AI si generarea de drafturi au contracte distincte, iar `reference/story-context-quest-ai-stack.md` este doar index derivat.

## Recomandare

- trateaza acest document ca o lista de consolidare, nu ca sursa primara pentru design;
- cand apar mai multe documente care spun acelasi lucru, alege unul canonic si marcheaza restul ca derivate sau istorice.
