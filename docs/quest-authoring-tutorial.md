# Quest Authoring / Quick Quest Tutorial

Acest ghid explica fluxul recomandat pentru autoring-ul de quest-uri din GUI.

## 1. Alege fluxul potrivit

Foloseste `Quick Quest` cand vrei un quest simplu, rapid, cu putine campuri.

Foloseste `Quest Authoring` cand vrei:

- selector mai clar
- mecanica de progresie
- preview explicit
- corectie asistata

## 2. Porneste cu AI preset

Comanda recomandata este:

```text
/ainpc quest create ai
```

Aceasta poate precompleta:

- numele quest-ului
- giver-ul
- tipul
- recompensa

## 3. Corecteaza in GUI

In ecranul de authoring ai de obicei:

- `Back`
- `Next`
- `Suggest`
- `Edit`
- `Confirm`
- `Cancel`

Cand AI-ul greseste, corectezi manual campul curent sau reaplici sugestia.

## 4. Verifica structura

Inainte de confirmare, verifica:

- numele quest-ului
- selectorul sau ancorele
- recompensa
- dependintele pe `Region / Place / Node`
- mecanica de progresie

## 5. Foloseste progresia cand este necesar

Cand quest-ul trebuie legat de progresie, foloseste:

```text
/ainpc progression create ai
```

Acest flux este util cand quest-ul trebuie conectat la:

- tutoriale
- contracte
- evenimente
- ritualuri
- colectare

## 6. Inspecteaza dupa creare

Comenzi utile:

```text
/ainpc quest create ai help
/ainpc progression create ai help
/ainpc debugdump quest summary
/ainpc debugdump progression summary
```

## 7. Ordinea buna

1. pornesti din `create ai`
2. lasi GUI-ul sa precompleteze
3. corectezi manual ce lipseste
4. verifici preview-ul
5. confirmi
6. inspectezi rezultatul cu `help` sau `debugdump`

