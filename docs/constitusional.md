# ConstituÈ›ia Proiectului â€” DocumentaÈ›ie FundamentalÄƒ

Pentru orientare in cod, foloseste [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md) si apoi [harta completa](./harta-pachetelor-cod.md).

Acest document defineÈ™te regulile structurale, arhitecturale È™i de convenÈ›ie ale proiectului AINPC.
Orice modificare a acestui document trebuie aprobatÄƒ È™i Ã®nregistratÄƒ Ã®n changelog.

## Principii Arhitecturale

1. **Separarea responsabilitÄƒÈ›ilor**: Codul Java sursÄƒ se extrage Ã®n fiÈ™iere Kotlin sidecar, pÄƒstrÃ¢nd fiÈ™ierul Java original cÃ¢t mai subÈ›ire (delegate one-line).
2. **Plugin-ul nu se propagÄƒ** (prin constructori/parametri) â€” se foloseÈ™te `lateinit var` + funcÈ›ie `init*()` pentru metodele care necesitÄƒ `plugin`.
3. **Build-ul trebuie sÄƒ rÄƒmÃ¢nÄƒ verde** dupÄƒ fiecare extracÈ›ie â€” zero erori, zero avertismente noi.
4. **Nicio rescriere completÄƒ** a fiÈ™ierelor Java mari â€” doar extracÈ›ie sigurÄƒ (safe slice).

## ConvenÈ›ii de Cod

- FiÈ™ierele Kotlin sidecar poartÄƒ adnotarea `@file:JvmName("...")` pentru compatibilitate Java.
- Metodele Java originale devin delegate de o linie cÄƒtre sidecar.
- Metodele pure (fÄƒrÄƒ `plugin` sau cÃ¢mpuri NPCManager) se extrag Ã®n `NPCManagerText.kt`.
- Metodele care folosesc `plugin` (dar nu alte cÃ¢mpuri private) se extrag Ã®n `NPCManagerVillagerLookup.kt` (cu `lateinit var`).
- Pentru `AINPCCommand.java`, metodele pure se extrag Ã®n `AINPCCommandText.kt`.

## Starea ExtracÈ›iei

| FiÈ™ier | Linii iniÈ›iale | Linii curente | Extrageri |
|--------|---------------|---------------|-----------|
| NPCManager.java | 2985 | 2135 | 57 metode |
| NPCManagerText.kt | 740 | 986 | 14 funcÈ›ii |
| NPCManagerVillagerLookup.kt | 0 | 199 | 16 funcÈ›ii |
| NPCManagerDB.kt | 0 | 283 | 16 funcÈ›ii |
| NPCManagerAnchors.kt | 0 | 181 | 11 funcÈ›ii |
| AINPCCommand.java | 6248 | 6942 | 2 metode |
| AINPCCommandText.kt | 2817 | 3175 | 2 metode |

## Reguli de Stabilitate

- Nu se extrag metode care acceseazÄƒ cÃ¢mpuri private ale clasei gazdÄƒ (ex: `npcsByUuid`, `npcsById`).
- Se prioritizeazÄƒ clusterele logice (toate metodele Ã®nrudite se extrag Ã®mpreunÄƒ).
- Avertismentele pre-existante Kotlin nu se repara â€” doar cele nou introduse de extracÈ›ie.

