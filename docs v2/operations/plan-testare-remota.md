# Plan de Testare Remota AINPC

Acesta este planul operational pentru testare asistata remote cu RCON si verificare in joc.

## Model de lucru

- operatorul remote pregateste serverul, ruleaza comenzi si analizeaza loguri;
- jucatorul local executa pasii in joc si confirma vizual;
- testele sunt organizate pe faze: smoke, NPC, quest.

## Fazele

- smoke tests pentru sanatatea serverului si a pluginului;
- NPC tests pentru creare, dialog, stergere si binding;
- quest tests pentru obiective si progres;
- fiecare test are confirmare clara PASS/FAIL.
