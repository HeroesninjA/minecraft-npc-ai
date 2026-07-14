# Strategie Plugin Modular si Scenarii Programabile

Acesta este planul de arhitectura pentru un plugin modular si scenarii extensibile prin addonuri.

## Scop

- trateaza pluginul ca o platforma, nu ca un singur gamemode;
- separa `ainpc-api`, `ainpc-core-plugin`, addonurile de scenariu si continutul YAML;
- lasa scenariile noi sa existe fara modificari repetate in nucleu.

## Principiu

- continutul uzual este declarativ;
- comportamentul special vine prin extensii de cod;
- integrari complexe raman addonuri dedicate.
