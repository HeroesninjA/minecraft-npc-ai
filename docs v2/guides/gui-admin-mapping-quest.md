# GUI Admin pentru mapping si questuri

Status: matrice operationala derivata.
Verificat in cod: 2026-07-18.

Aceasta pagina compara efectele butoanelor GUI. Contractele mapping si quest raman in documentele lor canonice.

## Matrice de efecte

| Suprafata | Efect imediat | Persistenta/publicare |
| --- | --- | --- |
| mapping creator region/place/node | colecteaza draft si confirma mutatia runtime | necesita `/ainpc world save` |
| world/admin mapping | inspecteaza ori executa comenzi world | depinde de comanda; save este explicit |
| Quick Quest | pastreaza draft local si poate afisa YAML simplificat in chat | nu publica pack |
| Quest Creator avansat | pastreaza formularul si exporta JSON | draft in `debug-dumps/quest-drafts`, neincarcat automat |
| Quest Authoring | proiectie read-only | nicio mutatie |
| Quest Definitions/Editor/Test | lucreaza cu definitii deja incarcate | nu importa automat drafturi |
| Quest Map | inspecteaza si muta binding-uri de ancora | repository-ul de progresie persista binding-ul |
| actiuni numite AI | precompleteaza/reface draftul curent | nu demonstreaza apel extern si nu publica |

## Confirmare

- dialogurile explicite continua sa foloseasca `openConfirmCommand` si adauga `--confirm` numai comenzilor clasificate drept mutatii;
- orice buton care trimite direct o mutatie de mapping prin `runCommand` este interceptat si deschide un dialog generic, deci acoperirea nu mai depinde de fiecare buton;
- dupa confirmare, comanda tinta ramane responsabila de permisiune, protectia `read_only`, validare si compensare;
- `gui.skip_confirmations=true` adauga confirmarea tehnica si executa direct; ramane optiune numai pentru medii de test aprobate.

## Etichete obligatorii

- foloseste `preview` pentru inspectie;
- foloseste `draft` pentru stare in memorie;
- foloseste `export draft` pentru fisier ori text generat;
- foloseste `persist mapping` numai dupa `/ainpc world save` reusit;
- foloseste `publish/load pack` numai dupa instalarea si reincarcarea explicita a continutului.

## Dovezi dupa actiune

- mapping: verifica region/place/node, ruleaza save si reporneste controlat;
- quest draft: verifica fisierul ori YAML-ul, schema si ID-urile;
- quest map: relisteaza binding-ul pentru acel player/template/obiectiv;
- comanda dispatch-uita: citeste mesajul chat/consola dupa inchiderea inventarului;
- mutatie riscanta: pastreaza auditul si un debugdump revizuit.

## Legaturi

- `reference/gui-stack.md`
- `architecture/mapping.md`
- `guides/mapping-harti-manuale.md`
- `guides/quest-authoring-tutorial.md`
- `reference/quest-anchor-bindings.md`
- `planning/questuri-avansate-v2.md`
