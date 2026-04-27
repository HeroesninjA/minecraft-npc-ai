# TODO Caracteristici

Actualizat: 2026-04-26

## Exista deja

- [x] NPC-uri AI bazate pe `Villager`
- [x] Dialog contextual cu memorie, emotii si relatie cu jucatorul
- [x] Comenzi administrative prin `/ainpc`, `/npc` si `/ai`
- [x] Pachete tematice configurabile prin YAML
- [x] Modularizare initiala in `ainpc-api`, `ainpc-core-plugin` si `ainpc-scenario-medieval`
- [x] Scenariu medieval separat ca addon de continut

## Prioritate curenta

- [ ] Questuri complete pe NPC, cu obiective clare si progres persistent
- [ ] Povesti distribuite pe sate, regiuni si puncte de interes
- [ ] Incarcare curata a scenariilor ca module separate
- [ ] World admin pentru regiuni, noduri si control de zona

## Componente lipsa confirmate in cod

- [ ] Runtime extensibil pentru scenarii:
- [ ] `ScenarioActionRegistry`
- [ ] `ScenarioConditionRegistry`
- [ ] `ScenarioTriggerRegistry`
- [ ] `ScenarioExecutionContext`
- [ ] `ScenarioVariableProvider`
- [ ] `ScenarioValidationReport`
- [ ] Validator pentru addonuri si feature pack-uri:
- [ ] verificare `dependencies`
- [ ] verificare `capabilities`
- [ ] verificare compatibilitate cu `RuntimeMode`
- [ ] mesaje clare la load pentru incompatibilitati
- [ ] Multi-quest runtime matur pe jucator, nu doar un singur quest activ
- [ ] Sistem semantic de `places` peste world admin:
- [ ] locuri de tip `fierarie`, `taverna`, `casa_fierarului`
- [ ] API public pentru interogarea acestor locuri
- [ ] Sistem extins de reward:
- [ ] reputatie
- [ ] economie / monede
- [ ] progresie jucator
- [ ] factiuni sau afiliere regionala
- [ ] Comenzi de debug si inspectie pentru:
- [ ] prompt AI
- [ ] scenarii active
- [ ] quest progress
- [ ] validare pack-uri si addonuri
- [ ] Suita de teste automate pentru questuri, world admin si addon registry
- [ ] Mutarea finala a surselor core din folderul legacy `src/src` in `ainpc-core-plugin/src`

## NPC-uri

- [ ] Rutine zilnice mai clare pentru fiecare NPC
- [ ] Reactii mai bune la reputatie, familie, emotii si istoric
- [ ] Roluri mai bine definite: negustor, gardian, fermier, quest giver
- [ ] Coordonare intre NPC-uri din acelasi sat sau aceeasi regiune
- [ ] Dialog care se schimba in functie de povestea locala

## Questuri si povesti

- [ ] Questuri in lant, nu doar interactiuni izolate
- [ ] Obiective cu stari: inceput, progres, completat, esuat
- [ ] Recompense configurabile pe scenariu
- [ ] Questuri legate de locatie, anotimp, eveniment sau reputatie
- [ ] Story state pe regiune, pentru lumi care evolueaza in timp

## Lume si gameplay

- [ ] Regiuni cu identitate proprie: sat, castel, pestera, dungeon
- [ ] Spawn si comportament diferit pe tipuri de zona
- [ ] Economie de baza: monede, tranzactii, roluri comerciale
- [ ] Reputatie pe sat, regiune sau factiune
- [ ] Sistem de progres pentru jucator: nivel, skill-uri sau experienta

## Scenarii si addonuri

- [ ] API public stabil pentru scenarii si addonuri
- [ ] Addonuri de integrare cu pluginuri externe
- [ ] Feature packs noi pe langa medieval: social, modern, fantasy
- [ ] Posibilitatea de a combina mai multe addonuri fara configuratie fragila

## Admin si debug

- [ ] Comenzi mai bune pentru inspectarea starii unui NPC
- [ ] Debug pentru prompt, model AI si raspuns fallback
- [ ] Reload sigur pentru config, pack-uri si scenarii
- [ ] Mesaje de eroare mai clare pentru configuratii invalide

## Pentru primul release bun

- [ ] Un scenariu medieval complet jucabil
- [ ] Un set mic de NPC-uri cu roluri distincte
- [ ] Cateva questuri cap-coada care pot fi testate usor
- [ ] Instalare simpla si documentatie scurta pentru server admins
