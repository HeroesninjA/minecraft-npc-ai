# Audit de conformitate cu constitutia proiectului

Status: checklist operational derivat.
Actualizat: 2026-07-15.

Constitutia este `canonical/constitutie-proiect.md`. Nu exista un singur test care demonstreaza conformitatea completa.

## Automatizare existenta

`ainpc-core-plugin/src/test/kotlin/ro/ainpc/CoreNeutralityStaticAuditTest.kt` scaneaza fisierele Java, Kotlin si YAML din `ainpc-core-plugin/src/main` pentru o lista explicita de termeni tematici de profesie.

```powershell
./gradlew.bat :ainpc-core-plugin:test --tests "ro.ainpc.CoreNeutralityStaticAuditTest"
```

Testul are excluderi explicite pentru mai multe fisiere si verifica numai aceasta felie de neutralitate. Un rezultat verde nu confirma automat:

- separarea completa core/addon;
- lifecycle-ul feature flags;
- siguranta storage sau AI;
- compatibilitatea API/ABI;
- lipsa continutului tematic in toate formele;
- respectarea regulilor operationale si de release.

## Checklist manual

- [ ] schimbarea respecta autoritatea documentelor canonice;
- [ ] continutul de scenariu ramane in addon/config sau are o exceptie justificata;
- [ ] o functie optionala are flag, stare runtime si degradare sigura;
- [ ] API-ul public nu expune accidental implementarea interna;
- [ ] AI propune sau explica, iar mutatiile raman validate determinist;
- [ ] persistenta si comenzile mutatoare au teste si procedura de rollback;
- [ ] documentatia separa implementat, partial si roadmap;
- [ ] ideile compatibile neimplementate nu sunt etichetate abandonate.

## Dovezi

Pastreaza rezultatul testelor relevante, diff-ul modulelor afectate, decizia de owner si orice exceptie asumata. Auditul runtime `/ainpc audit` verifica stare operationala, nu constitutia proiectului.

## Legaturi

- `canonical/constitutie-proiect.md`
- `canonical/implementat-deja.md`
- `reference/feature-flags-lifecycle.md`
- `operations/release-checklist.md`
