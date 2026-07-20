# Propunere: AI pentru drafturi de constructie

Status: propunere compatibila, neimplementata.
Actualizat: 2026-07-15.

AI poate ramane o unealta optionala de authoring pentru sate, cladiri, ruine, pesteri, castele sau tabere. Directia nu este abandonata; trebuie insa limitata la drafturi validate.

## Baseline curent

- `AIUseCase.BUILD_PLAN_DRAFT` exista in scaffold-ul `AIOrchestrationService`;
- orchestratorul nu are apelant de productie pentru acest use case;
- nu exista schema de draft conectata la un validator si executor;
- planner-ele active sunt deterministe si opereaza in principal pe mapping semantic;
- nu exista constructor fizic de blocuri AINPC.

## Rol propus pentru AI

AI poate:

- sugera structuri si relatii semantice;
- completa un draft conform unei scheme versionate;
- explica alegeri si riscuri;
- propune variante pentru review administrativ.

AI nu poate:

- scrie direct mapping, blocuri, NPC-uri sau quest progress;
- inventa IDs ori tipuri in afara registrului validat;
- ocoli preview-ul, aprobarea sau limitele de capabilitate;
- decide singur rollback-ul si persistenta.

## Pipeline propus

`context minim -> draft AI -> schema validation -> plan determinist -> preview -> aprobare -> executor -> audit`

Fallback-ul trebuie sa fie un draft determinist sau refuz explicit, nu executie partiala mascata drept succes.

## Dependente

- schema de authoring pentru structuri;
- validator strict si redactor de context;
- executor semantic sau fizic separat;
- politica de aprobare si audit;
- teste pentru prompt injection si output invalid.

## Gate-uri pentru conectare

- consumer explicit pentru `BUILD_PLAN_DRAFT`;
- schema si validator testate;
- zero mutatii inainte de aprobare;
- rezultat trasabil la prompt, model, versiune si plan final;
- comportament sigur cand providerul lipseste.

## Legaturi

- `architecture/ai-orchestrare-si-mecanici.md`
- `planning/schema-authoring-structuri-world.md`
- `planning/generare-sate-worldedit-si-npc.md`
- `reference/prompt-safety-guide.md`

