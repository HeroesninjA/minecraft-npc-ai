# Kotlin Coroutines si Paper Policy

Actualizat: 2026-05-16

## Scop

Acest document stabileste politica pentru coroutine in proiectul AINPC.

Rezumat:

- coroutine nu se introduc ca parte a conversiei Java -> Kotlin
- Paper scheduler ramane mecanismul principal de scheduling
- coroutine necesita design separat, testare separata si smoke test Paper

## Decizie curenta

Status:

- coroutine sunt interzise in prima faza a conversiei Kotlin

Motiv:

- Paper are reguli stricte pentru main thread si async work
- pluginul are deja scheduler si servicii lifecycle
- coroutine pot ascunde thread switching
- cancellation si shutdown trebuie proiectate explicit

## Ce este permis

Permis in conversia initiala:

- Kotlin classes
- Kotlin tests
- `data class`
- `object`
- null-safety
- sealed intern
- servicii normale cu constructor injection

Interzis in conversia initiala:

- `kotlinx.coroutines`
- `GlobalScope`
- `runBlocking` in runtime Paper
- dispatchere custom fara document
- suspend functions in servicii Paper
- flow/channel pentru gameplay runtime

## De ce `GlobalScope` este interzis

`GlobalScope` rupe lifecycle-ul pluginului.

Riscuri:

- task-uri care continua dupa disable
- acces la Bukkit API de pe thread gresit
- memory leaks
- erori greu de reprodus la reload/restart

Regula:

- niciun cod runtime nu foloseste `GlobalScope`.

## De ce `runBlocking` este interzis in Paper runtime

Riscuri:

- blocheaza main thread
- poate ingheta serverul
- poate provoca timeout-uri
- ascunde probleme de design async

Permis doar:

- in teste izolate, daca este documentat
- niciodata in listener, command handler sau scheduler Paper live

## Cand ar putea fi acceptate coroutine

Coroutine pot fi evaluate doar dupa:

- Kotlin este stabil in core
- exista smoke test Paper stabil
- exista document separat de design async
- exista lifecycle owner pentru plugin
- exista strategie de cancellation in `onDisable`
- exista reguli de thread pentru Bukkit API
- exista teste pentru shutdown

## Design necesar inainte de coroutine

Un document viitor trebuie sa defineasca:

- cine detine scope-ul
- cand se creeaza scope-ul
- cand se anuleaza scope-ul
- ce dispatcher foloseste pentru IO
- cum revine pe main thread Paper
- ce operatii Bukkit sunt permise async
- cum se logheaza erorile
- cum se face backpressure
- cum se testeaza restart/reload

## Model acceptabil, doar dupa aprobare

Exemplu conceptual, nu de implementat acum:

```kotlin
class PluginCoroutineRuntime(
    private val logger: Logger,
) {
    fun start() {
        // creeaza scope controlat de plugin
    }

    fun stop() {
        // anuleaza toate joburile
    }
}
```

Regula:

- scope-ul este detinut de plugin, nu global
- shutdown-ul este explicit
- toate erorile sunt logate
- orice acces Bukkit pe main thread este explicit

## Zone unde coroutine ar fi tentante, dar trebuie amanate

AI/OpenAI:

- tentant pentru request-uri async
- se amana pana exista politica de timeout, cancellation si fallback

Database:

- tentant pentru query async
- se amana pana exista executor/lifecycle clar

Simulation:

- tentant pentru tick-uri si flows
- se amana pentru ca Paper scheduler este deja modelul natural

GUI:

- tentant pentru update-uri async
- se amana pentru ca inventory API este main-thread sensitive

## Reguli pentru review

Respinge schimbarea daca apare:

- import `kotlinx.coroutines`
- `GlobalScope`
- `runBlocking` in productie
- `suspend` pe API public
- dispatcher introdus fara document
- async Bukkit API fara dovada ca este permis

## Comenzi de audit

Cauta coroutine:

```powershell
rg -n "kotlinx\.coroutines|GlobalScope|runBlocking|suspend fun|CoroutineScope|Dispatchers" .
```

Gate:

- inainte de aprobarea coroutine, comanda trebuie sa nu gaseasca folosiri in productie
- folosirile in teste trebuie justificate

## Definitia de gata

Conversia Kotlin initiala este corecta daca:

- nu foloseste coroutine
- nu schimba modelul de scheduling
- nu blocheaza main thread
- nu introduce lifecycle async nou

Coroutine pot avea faza lor doar dupa ce conversia Kotlin de baza este stabila.
