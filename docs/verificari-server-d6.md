# D6: Story Context â€” Verificari pe Server Paper

Pentru orientare in cod, foloseste [harta scurta a pachetelor](./harta-pachetelor-cod-scurta.md) si apoi [harta completa](./harta-pachetelor-cod.md).

## T037: Story context

```
/ainpc story context
```

**Gate:** Context narativ care foloseste mapping si ancore quest.

## T038: Story region

```
/ainpc story region demo_sat
```

**Gate:** Starea povestii pe regiune e vizibila. Zero stacktrace.

## T039: Story events

Dupa completarea unui quest:
```
/ainpc story events
```

**Gate:** Cel putin un story event vizibil.

## T040: Debugdump story

```
/ainpc debugdump story
```

**Verificati:** Fisierul nu contine API keys, token-uri, parole.

**Gate:** Export story fara secrete.

