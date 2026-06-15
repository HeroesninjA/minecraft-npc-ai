# Criterii de Gata pentru Primul Demo AINPC

Actualizat: 2026-06-15

## Ce inseamna "gata"

Primul demo jucabil AINPC este gata cand toate cele 10 faze D0-D9 trec pe un server Paper dedicat.

## Criterii esentiale (BLOCANTE)

Daca oricare din acestea esueaza, demo-ul NU e gata:

| # | Criteriu | Verificare |
|---|----------|------------|
| 1 | Pluginul se incarca fara erori | /plugins, /ainpc, startup log |
| 2 | Harta demo_sat exista cu regiuni, locuri, noduri | /ainpc world places demo_sat |
| 3 | 3-5 NPC-uri exista si au bindings home/work/social | /ainpc list, /ainpc world bindings |
| 4 | Rutina e inspectabila | /ainpc routine status nearest |
| 5 | Un quest poate fi acceptat si inspectat | /ainpc quest nearest |
| 6 | O mecanica non-QUEST e listata | /ainpc progression definitions |
| 7 | NPC-urile si progresul supravietuiesc restartului | restart + verificare |
| 8 | Auditul nu expune secrete | /ainpc debugdump all |
| 9 | Fallback-ul AI functioneaza fara API key | test fara OPENAI_API_KEY |

## Non-Obiective (NU blocheaza demo-ul)

Urmatoarele NU sunt necesare pentru primul demo:

- Generator automat de cladiri / sate (mapping manual e suficient)
- Story avansat cu branching (story context minimal e suficient)
- Economie sau reputatie globala
- Runtime universal pentru toate scenariile
- Sute de NPC-uri (3-5 e suficient)
- GUI final pentru toate fluxurile
- WorldEdit sau alt tool extern
- Publicare pe server real
- OpenAI extern (fallback local e suficient)

## Ce trebuie sa mearga (sumar pe faze)

| Faza | Ce se verifica | Gate |
|------|----------------|------|
| D0 | Variabile, criterii, backup | Documente exista |
| D1 | Build, JAR, config, plugin | clean build + plugins incarcat |
| D2 | Harta demo_sat | places, noduri, audit |
| D3 | NPC-uri + bindings | list, spawn, audit |
| D4 | Rutina, interactiune, GUI | status nearest, click dreapta |
| D5 | Quest + progression | accept, status, definitions |
| D6 | Story context | story context, events |
| D7 | Dialog + AI fallback | conversatie, fallback |
| D8 | Restart + smoke tests | audit, restart, smoke scripts |
| D9 | Script demo final | toti pasii executati |
