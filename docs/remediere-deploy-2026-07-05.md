# Raport de Remediere - Deploy AINPC v1.0.0

**Data:** 2026-07-05
**VPS:** 141.147.48.170
**Server UUID:** cd8c1a26-8639-40fd-9e52-aa95bfc11f3c

---

## Rezumat

| Severitate | Gasite | Remediate | Ramase |
|------------|--------|-----------|--------|
| Critice | 1 | 1 | 0 |
| Inalte | 2 | 1 | 1 |
| Medii | 2 | 0 | 2 |
| Joase | 2 | 0 | 2 |

---

## 1. Erori Critice (blocante)

### 1.1 ~~SyntaxError in deploy/deploy-ainpc.ps1~~ ✅ REMEDIAT

- **Fisier:** `deploy/deploy-ainpc.ps1:62`
- **Eroare:** `Unexpected token 'action\":\"restart\"}'"` — PowerShell nu recunoaste `\"` drept escape in siruri double-quoted.
- **Cauza:** JSON-ul din comanda curl era scris cu `{\"action\":\"restart\"}` — valabil in bash, invalid in PowerShell.
- **Remediere:** Inlocuit cu here-string `@"..."@` unde ghilimelele duble sunt literale.
- **Comite:** Da, deja aplicat in working tree.

---

## 2. Erori Inalte (deploy esuat partial)

### 2.1 ~~SCP Permission Denied~~ ✅ REMEDIAT

- **Eroare:** `scp: dest open "/var/lib/pterodactyl/.../plugins/ainpc-core-plugin-1.0.0.jar": Permission denied`
- **Cauza:** Utilizatorul `ubuntu` nu are drept de scriere directa in `/var/lib/pterodactyl/` (proprietar `pterodactyl:pterodactyl`).
- **Remediere:** SCP in `/tmp/ainpc-deploy/`, apoi `sudo mv` si `sudo chown pterodactyl:pterodactyl`.
- **Fisier:** `deploy/deploy-ainpc.ps1` (liniile 40-53)

### 2.2 Wings API Restart - 401/Internal Error ✅ REMEDIAT (docker restart)

- **Eroare:** Wings API returneaza 401 chiar si cu token-ul corect din config.
- **Cauza:** Wings API necesita un Panel API key pentru autentificare, nu token-ul din `config.yml`.
- **Remediere:** Inlocuit Wings API call cu `sudo docker restart <uuid>` in script.
- **Fisier:** `deploy/deploy-ainpc.ps1` (linia 62-68)

---

## 3. Erori Medii (plugin functional, dar cu warning-uri)

### 3.1 ClassCastException in FeaturePackYamlSupport.kt:692 ✅ REZOLVATA

- **Eroare:** `java.lang.ClassCastException: class java.lang.String cannot be cast to class java.util.Map`
- **Fisier cod:** `ainpc-core-plugin/src/main/kotlin/ro/ainpc/engine/FeaturePackYamlSupport.kt:692`
- **Cauza:** Functia `loadRuntimeTriggers` presupunea ca elementele listei `actions` sunt `Map<String, Any>`, dar YAML foloseste string-uri simple.
- **Remediere:** `loadRuntimeTriggers` accepta acum ambele formate (`String` → referinta, `Map` → definitie inline).
- **Fisiere YAML afectate (3 locatii):** `medieval_quest.yml`, `sample_objectives_quest.yml`
- **Verificare post-remediere:** 18 scenarii medievale incarcate, 0 ClassCastException. ✅

### 3.2 WorldEdit - Java Version Mismatch ❌ RAMANE (infrastructura)

- **Eroare in log:** `java.lang.IllegalArgumentException: Unsupported class file major version 69`
- **Cauza:** Serverul ruleaza `ghcr.io/pterodactyl/yolks:java_25` (Java 25). Paper 1.21.4 nu suporta Java 25 pentru remapping-ul plugin-urilor third-party.
- **Impact:** WorldEdit e nefunctional (jucatorii nu pot folosi //wand, //set, etc). AINPC nu e afectat.
- **Remediere:** Schimbat Docker image in Panel Pterodactyl de la `java_25` la `java_21`. Imaginea `java_21` exista deja pe VPS.
  - Panel → Admin → Servers → cd8c1a26 → Startup → Docker Image → `ghcr.io/pterodactyl/yolks:java_21`
  - Daca nu ai acces Panel, se poate face si prin API cu un API key generat din `Panel → Account → API Credentials`

---

## 4. Erori Joase (nefunctionale, necritice)

### 4.1 AuthMe - GeoLite2 Database Missing

- **Eroare:** `No MaxMind credentials found in the configuration file! GeoIp protections will be disabled.`
- **Impact:** Protectia GeoIP dezactivata; restul functionalitatii AuthMe functioneaza.
- **Remediere:** Optional — configurat cheie MaxMind in `plugins/AuthMe/config.yml`.

### 4.2 ViaVersion - Versiune Veche

- **Warning:** `There is a newer plugin version available: 5.10.0, you're on: 5.2.1`
- **Impact:** Minim. Functionalitatea de baza pentru conectare clienti 1.21.x functioneaza.
- **Remediere:** Optional — facut update la ultima versiune.

---

## 5. Alte Probleme Infrastructure

### 5.1 Backup JAR-uri Stangi in plugins/

- In directorul `plugins/` exista fisiere JAR multiple pentru acelasi plugin (ex: `WorldEdit.jar`, `WorldEdit-new.jar`, `ViaRewind-new.jar`).
- Paper incearca sa le incarce pe toate, ceea ce genereaza erori.
- **Actiune:** Curatat manual fisierele JAR nefolosite din `plugins/`.

---

## Actiuni Final

| # | Actiune | Prioritate | Responsabil | Status |
|---|---------|------------|-------------|--------|
| 1 | Fix sintaxa PowerShell in `deploy-ainpc.ps1` | Critic | Dev | ✅ |
| 2 | Fix SCP permission denied in deploy script | Inalta | Dev | ✅ |
| 3 | Fix Wings API → `docker restart` in script | Inalta | Dev | ✅ |
| 4 | Fix ClassCastException in `FeaturePackYamlSupport.kt` | Inalta | Dev | ✅ |
| 5 | **Schimbat Docker image la `java_21` (WorldEdit)** | Medie | **Admin** | ❌ |
| 6 | Cleanup JAR-uri vechi in plugins/ | Joasa | Dev | ✅ |
| 7 | Configurat GeoIP AuthMe (optional) | Joasa | Admin | ❌ |
| 8 | Update ViaVersion (optional) | Joasa | Admin | ❌ |

---

*Generat de OpenCode dupa deploy pe 2026-07-05.*
