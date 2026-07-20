# Prompt Safety Guide

Status: politica operationala; protectiile nu sunt toate conectate automat.
Actualizat: 2026-07-15.

## Reguli obligatorii

1. Construieste contextul minim pentru use case.
2. Redacteaza textul la ultimul boundary inainte de provider sau log.
3. Separa instructiunile de datele provenite din player, DB, YAML, MCP si model.
4. Valideaza raspunsul generic si apoi contractul domeniului.
5. Pastreaza AI-ul read-only pana exista aprobare si executor determinist.
6. Foloseste fallback care nu modifica stare.

## Realitatea implementarii curente

- `StoryContextSnapshot.toPromptBlock()` nu redacteaza;
- `OpenAIPromptBuilder` include nume, mesaje, istoric, memorii si context story ca text;
- `ContextRedactor` exista, dar nu este apelat de fluxul de prompt din `src/main`;
- `AIResponseValidator` exista, dar `AIOrchestrationService.orchestrate(...)` nu il apeleaza;
- `AISafetyLabel` este derivat in principal din confidence/status si nu inlocuieste validarea continutului;
- filtrul MCP redacteaza numai campuri selectate din snapshot-ul cunoscut sidecar-ului.

## Gate inainte de activarea unui flux AI

- test cu API key, UUID, email si IP in fiecare sursa de context;
- test de prompt injection prin mesaj, memorie, metadata si eveniment story;
- test de output gol, trunchiat, prea lung si cu tip gresit;
- test ca niciun draft nu executa comanda, reward, world edit sau persistenta;
- audit cu provenance real al providerului si rezultat de validator;
- fallback verificat cand providerul si MCP sunt indisponibile.

## Interzis

- tratarea unui `toPromptBlock()` drept text sigur implicit;
- folosirea marcajului `[EXECUTED]` ca dovada de executie;
- publicarea tool-urilor de scriere fara autentificare, configurare si gate runtime;
- copierea secreta a configuratiei in debugdump, prompt sau audit.

## Legaturi

- `architecture/story-context-service.md`
- `architecture/ai-orchestrare-si-mecanici.md`
- `architecture/mcp-runtime-bridge-design.md`
- `planning/questuri-avansate-v2.md`
