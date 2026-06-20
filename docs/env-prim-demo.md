# Environment Variables pentru Primul Demo AINPC

Actualizat: 2026-06-15

## Scop

Acest document defineste variabilele de mediu si parametrii necesari pentru a rula primul demo jucabil AINPC pe un server Paper local. Toate celelalte taskuri din D0-D9 refera acest document.

## Variabile

| Variabila | Valoare implicita | Descriere |
|-----------|-------------------|-----------|
| ServerDir | Calea catre serverul Paper local | Directorul unde ruleaza serverul Paper |
| RegionId | demo_sat | ID-ul regiunii satului demo |
| PlayerName | (numele tau) | Numele jucatorului care testeaza demo-ul |
| JAVA_HOME | JDK 21+ | Calea catre JDK-ul folosit pentru build |
| OpenAI | enabled / disabled | Daca OpenAI e activat sau nu |
| CoreJAR | ainpc-core-plugin/build/libs/ainpc-core-plugin-1.0.0.jar | Calea catre JAR-ul core |
| AddonJAR | ainpc-scenario-medieval/build/libs/ainpc-scenario-medieval-1.0.0.jar | Calea catre JAR-ul addon |
| ApiJAR | ainpc-api/build/libs/ainpc-api-1.0.0.jar | Calea catre JAR-ul API |

## Reguli

- ServerDir trebuie sa existe si sa aiba Paper instalat
- JAR-urile se copiaza in ServerDir/plugins/
- OPENAI_API_KEY se citeste din env var, NU din fisiere
- JAVA_HOME trebuie sa pointeze la JDK 21 sau mai nou
- Demo-ul trebuie verificat si fara OpenAI (fallback local)

## Comenzi utile

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.2"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

# Build
./gradlew.bat clean build

# Verifica JAR-uri
Get-ChildItem ainpc-core-plugin/build/libs/*.jar
```
