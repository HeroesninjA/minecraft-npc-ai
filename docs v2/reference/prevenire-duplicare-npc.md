# Prevenire si Remediere Duplicare NPC

Acesta este ghidul pentru prevenirea si remedierea duplicatelor de NPC.

## Scop

- identifica tipurile de duplicate;
- defineste invariantul de identitate pentru NPC-urile permanente;
- descrie mecanismele de audit, cleanup si reconciliere.

## Reguli

- un NPC permanent trebuie sa aiba un singur rand canonic si o singura entitate activa;
- source key si marker-ele persistente sunt sursa de reconciliere;
- duplicatele se rezolva prin inspectie si cleanup controlat, nu prin spawnuire noua.
