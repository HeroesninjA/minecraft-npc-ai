# Kotlin Interop, API si Addonuri

Status: referinta verificata pentru ABI-ul JVM curent.
Actualizat: 2026-07-17.

`ainpc-api` este implementat in Kotlin, nu in Java. Compatibilitatea Java exista partial si trebuie verificata pe semnaturile JVM, nu presupusa din sursa Kotlin.

## Stare curenta

- modulul `ainpc-api` contine surse Kotlin si produce clase JVM consumabile din Java;
- proprietatile Kotlin sunt expuse ca gettere Java;
- `AINPCAddon` expune callback-urile cu corp ca metode `default` in artefactul curent;
- `AddonDescriptor` foloseste `@JvmOverloads` si expune constructori Java de la 4 la 10 argumente;
- constructorul cu 4 argumente foloseste tipul neutru `FEATURE`, toate runtime modes si colectii goale pentru capabilitati si dependinte;
- argumentele implicite Kotlin nu genereaza automat overload-uri Java in lipsa `@JvmOverloads` sau a unor constructori expliciti;
- `DependencyResolver.resolve` cere din Java si parametrul `enabledIds`, dar contractul este rezervat si nu are provider in `AINPCPlatformApi`;
- `AINPCPlatformApi.registerObjectiveHandler` pastreaza tipul functie Kotlin si ofera overload-ul SAM `ObjectiveProgressHandler` pentru Java.

## Acoperire de test existenta

- exista teste Java de consum pentru evenimentele publice;
- exista test Java pentru implementarea implicita `getAddonConfigDirectory`;
- exista test Java pentru inregistrarea unui objective handler prin `ObjectiveProgressHandler`;
- exista fixture Java pentru constructorul scurt `AddonDescriptor`, `AINPCAddon`, `registerAddon`, ordinea `onLoad`/`onEnable` si `unregisterAddon`/`onDisable`;
- existenta unui test pentru un subset nu inseamna compatibilitate Java garantata pentru intregul modul.

## Reguli pentru API nou

- prefera tipuri JVM standard in semnaturile publice;
- pentru callback-uri consumate din Java, foloseste o interfata functionala publica, nu un tip functie Kotlin;
- foloseste overload-uri explicite sau `@JvmOverloads` unde declaratia permite;
- nu schimba constructori, gettere, enum IDs sau semnaturi fara test de compatibilitate;
- adauga un test Java de compilare/consum pentru fiecare contract nou destinat addonurilor Java;
- ruleaza `:ainpc-api:verifyApiAbi`; orice diferenta necesita clasificare SemVer, bump `apiVersion` si baseline revizuit.

## Limitare activa

API-ul este Kotlin-first si Java-callable pe multe trasee. ABI-ul JVM public este verificat automat, dar ergonomia Java si compatibilitatea comportamentala nu sunt garantate numai de snapshot; remedierea golurilor ramane roadmap activ in `planning/stabilizare-api-si-addonuri.md`.

## Legaturi

- `reference/documentatie-api.md`
- `reference/api-versioning-and-abi.md`
- `reference/addon-developer-guide.md`
- `reference/kotlin-code-review-checklist.md`
- `reference/kotlin-paper-packaging-si-smoke.md`
- `planning/stabilizare-api-si-addonuri.md`
