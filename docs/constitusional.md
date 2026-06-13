# Constituția Proiectului — Documentație Fundamentală

Acest document definește regulile structurale, arhitecturale și de convenție ale proiectului AINPC.
Orice modificare a acestui document trebuie aprobată și înregistrată în changelog.

## Principii Arhitecturale

1. **Separarea responsabilităților**: Codul Java sursă se extrage în fișiere Kotlin sidecar, păstrând fișierul Java original cât mai subțire (delegate one-line).
2. **Plugin-ul nu se propagă** (prin constructori/parametri) — se folosește `lateinit var` + funcție `init*()` pentru metodele care necesită `plugin`.
3. **Build-ul trebuie să rămână verde** după fiecare extracție — zero erori, zero avertismente noi.
4. **Nicio rescriere completă** a fișierelor Java mari — doar extracție sigură (safe slice).

## Convenții de Cod

- Fișierele Kotlin sidecar poartă adnotarea `@file:JvmName("...")` pentru compatibilitate Java.
- Metodele Java originale devin delegate de o linie către sidecar.
- Metodele pure (fără `plugin` sau câmpuri NPCManager) se extrag în `NPCManagerText.kt`.
- Metodele care folosesc `plugin` (dar nu alte câmpuri private) se extrag în `NPCManagerVillagerLookup.kt` (cu `lateinit var`).
- Pentru `AINPCCommand.java`, metodele pure se extrag în `AINPCCommandText.kt`.

## Starea Extracției

| Fișier | Linii inițiale | Linii curente | Extrageri |
|--------|---------------|---------------|-----------|
| NPCManager.java | 2985 | 2135 | 57 metode |
| NPCManagerText.kt | 740 | 986 | 14 funcții |
| NPCManagerVillagerLookup.kt | 0 | 199 | 16 funcții |
| NPCManagerDB.kt | 0 | 283 | 16 funcții |
| NPCManagerAnchors.kt | 0 | 181 | 11 funcții |
| AINPCCommand.java | 6248 | 6942 | 2 metode |
| AINPCCommandText.kt | 2817 | 3175 | 2 metode |

## Reguli de Stabilitate

- Nu se extrag metode care accesează câmpuri private ale clasei gazdă (ex: `npcsByUuid`, `npcsById`).
- Se prioritizează clusterele logice (toate metodele înrudite se extrag împreună).
- Avertismentele pre-existante Kotlin nu se repara — doar cele nou introduse de extracție.
