# Kotlin Testing Strategy

Acesta este rezumatul strategiei de testare pentru conversia Java -> Kotlin.

## Principii

- testele trebuie sa dovedeasca faptul ca semantica Java a ramas aceeasi;
- fiecare conversie mica are test specific;
- fiecare conversie de productie are build sau smoke relevant;
- orice schimbare care poate afecta Paper are smoke test.

## Niveluri

- test specific;
- test de pachet;
- test de modul;
- build complet;
- smoke Paper.
