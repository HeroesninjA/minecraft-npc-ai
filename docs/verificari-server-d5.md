# D5: Quest + Progression â€” Verificari pe Server Paper

Pentru orientare in cod, foloseste [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md) si apoi [harta completa](./harta-pachetelor-cod.md).

## T031: Quest clasic

```
/ainpc quest nearest
/ainpc quest accept nearest
```

**Verificati:**
- Quest disponibil
- Se poate accepta
- Obiectivele folosesc locuri/noduri din demo_sat

**Gate:** Quest acceptat cu succes.

## T032: Quest status

```
/ainpc quest status nearest
```

**Gate:** Statusul e inspectabil (stage, obiective, tracking).

## T033: Progression non-quest

```
/ainpc progression definitions
/ainpc contract definitions
/ainpc bounty definitions
/ainpc tutorial definitions
```

**Gate:** Cel putin o mecanica non-QUEST e listata.

## T034: Progression stored

```
/ainpc progression stored <player>
```

**Gate:** Progresia persistata e vizibila.

## T035: Quest tracking

```
/ainpc quest track start
```

**Verificati:** Busola indica tinta. Tracking-ul e persistent dupa restart.

**Gate:** Tracking functioneaza.

## T036: Questuri nu depind de AI

Testati cu OpenAI dezactivat (fara OPENAI_API_KEY):
```
/ainpc quest nearest
/ainpc quest accept nearest
/ainpc quest status nearest
```

**Gate:** Quest-urile functioneaza complet si fara AI extern.

