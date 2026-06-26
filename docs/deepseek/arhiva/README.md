# Arhiva DeepSeek

Actualizat: 2026-06-25

Acest folder pastreaza documentele istorice ale seriei DeepSeek deja realizate. Seria activa continua in `./deepseek-taskuri-late-50-7.md`.

Pentru orientare generala in standarde si format, foloseste `../../deepseek-batch-guide.md`.

## Documente arhivate

| Document | Interval | Motiv arhivare |
|---|---:|---|
| `deepseek-taskuri-late-25.md` | L031-L150 | Seria initiala locala a fost mutata in arhiva dupa extinderea backlog-ului activ |
| `deepseek-taskuri-late-50.md` | L151-L200 | Batch istoric, inlocuit de continuarea numerotata si indexul curent |
| `deepseek-taskuri-late-50-2.md` | L201-L250 | Batch istoric, pastrat doar pentru trasabilitate |
| `deepseek-taskuri-late-50-3.md` | L251-L300 | Batch istoric, pastrat doar pentru trasabilitate |
| `deepseek-taskuri-late-50-4.md` | L301-L350 | Batch istoric, pastrat doar pentru trasabilitate |
| `deepseek-taskuri-late-50-5.md` | L351-L400 | Batch istoric, pastrat doar pentru trasabilitate |
| `deepseek-taskuri-late-50-6.md` | L401-L450 | Batch de arhivare si igiena istorica inchis in ledger si mutat in arhiva |

## Conventie de nume

Nota audit: intervalul `L001-L030` este referit istoric in indexul de batch-uri, dar nu exista in arhiva locala ca heading-uri `### L001` pana la `### L030`. Pana la recuperarea sursei, acest interval ramane referinta istorica neverificata, nu task verificat.

Fisierele mutate in arhiva isi pastreaza numele original. Calea se schimba, nu numele fisierului. Aceasta regula pastreaza trasabilitatea intre index, changelog si istoricul local.

## Regula de folosire

Consulta aceste documente doar pentru istoric, audit sau rationale vechi. Pentru lucru curent, porneste din seria activa.

## Politica de mentenanta

- Arhiva este read-only implicit. Editarile sunt acceptate doar pentru navigare, metadate, corecturi de linkuri sau explicatii de trasabilitate.
- Nu se rescriu masiv documentele istorice. Daca un batch trebuie refolosit, se creeaza un task nou in seria activa sau se marcheaza restaurarea temporara.
- Batch-urile pot fi arhivate progresiv, dar nu se muta doar o parte dintr-un batch fara nota explicita de tranzitie.
- Un batch readus temporar din arhiva trebuie mentionat in raportul de stare si trebuie sa pastreze numele canonic al fisierului.
- Citarea unui batch istoric trebuie sa includa calea arhivata, de forma `./arhiva/<fisier>`.

## Template restaurare

```text
Restaurat temporar <fisier> din `./arhiva/` in <destinatie>.
Motiv: <motiv concret>.
Durata/conditie de revenire: <conditie>.
Verificare: index actualizat si checker rulat.
```

## Nota de inchidere

Arhiva DeepSeek este istoric de lucru, nu backlog activ. Foloseste-o pentru audit, rationale si trasabilitate. Pentru executie curenta, porneste din `../../deepseek-execution-cursor.md` si din seria activa listata in `../../deepseek-active-series-summary.md`.

