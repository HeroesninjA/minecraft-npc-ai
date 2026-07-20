# Credentiale si secrete operationale

Status: politica operationala fara valori secrete.
Actualizat: 2026-07-15.

Acest fisier nu stocheaza IP-uri, utilizatori, cai private, parole, tokenuri sau comenzi de conectare reale.

## Reguli

- valorile de mediu raman in secret manager, credential store sau configuratie locala ignorata de Git;
- documentatia urmarita descrie numele parametrului si procedura, nu valoarea;
- parolele si tokenurile nu se transmit ca argumente daca pot ajunge in history, log sau process list;
- debugdump-urile, rapoartele si capturile se sanitizeaza inainte de partajare;
- o valoare eliminata din HEAD poate ramane in istoricul Git, loguri sau backup-uri;
- rotatia/revocarea este obligatorie dupa expunere, chiar daca fisierul a fost corectat.

## Blocaj curent

`scripts/deploy-ainpc-vps.sh` contine un credential bearer literal. Valoarea nu trebuie copiata sau publicata.

Inainte de reutilizarea scriptului:

1. roteste sau revoca credentialul la sursa;
2. verifica accesul si logurile asociate;
3. elimina valoarea urmarita si orice fallback secret;
4. injecteaza credentialul printr-un canal controlat;
5. adauga o verificare care esueaza cand parametrul lipseste;
6. ruleaza secret scanning inclusiv pe istoricul relevant.

Pana atunci, scriptul nu este aprobat pentru deploy.

## Alte valori specifice mediului

Scripturile PowerShell din `scripts/` si `deploy/` contin valori implicite pentru host, utilizator, cheie, cale, RCON sau versiunea artefactului. Acestea:

- nu sunt sursa curenta de adevar;
- nu trebuie tratate drept credentiale valide;
- trebuie furnizate explicit sau mutate intr-o configuratie locala ignorata;
- blocheaza promovarea scriptului drept pipeline portabil pana la parametrizare.

## Backup-uri

Arhiva produsa de `scripts/release-backup-restore-check.ps1` poate include `plugins/AINPC/config.yml`. Scriptul calculeaza hash-uri, dar nu cripteaza arhiva. Backup-urile necesita acces restrictionat, retentie si transport securizat.

## Legaturi

- `planning/testare-si-deploy-remote.md`
- `operations/release-checklist.md`
- `operations/migration-si-backup.md`
