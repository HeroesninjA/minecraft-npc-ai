# AINPC Plugin

Plugin Minecraft Paper pentru NPC-uri AI cu poveste, emotii, amintiri si dialog realist.

## Descriere

AINPC este un plugin Paper 1.21 care adauga NPC-uri inteligente in lumea Minecraft. NPC-urile au:
- **Personalitate** si **emotii** care influenteaza interactiunile
- **Rutine zilnice** bazate pe ancore (casa, munca, social)
- **Quest-uri** si **progression** cu mecanici variate (contracte, bounty-uri, ritualuri)
- **Memorie** - NPC-urile isi amintesc interactiunile cu jucatorii
- **Poveste** generata dinamic

## Comenzi principale

| Comanda | Descriere |
|---------|-----------|
| `/ainpc` | Comanda principala pentru gestionarea NPC-urilor AI |
| `/quest` | Quest log si comenzi rapide pentru quest-uri |
| `/progression` | Log si comenzi pentru mecanici de progres |
| `/npcquest` | Comanda rapida pentru quest-urile NPC |

## Structura proiectului

```
ainpc-core-plugin/        # Plugin principal
ainpc-api/                # API public pentru addon-uri
ainpc-scenario-medieval/  # Scenariu medieval
```

## Build

```powershell
./gradlew.bat clean build
```

## Tehnologii

- **Kotlin** 2.3.21
- **Paper API** 1.21
- **SQLite** / **MySQL** (prin HikariCP)
- **Gson** pentru serializare JSON

## Statistici

- 0 fisiere Java
- 458 teste unitare
- 100% Kotlin
