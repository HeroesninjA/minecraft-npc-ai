# Addon Developer Guide

Status: ghid operational derivat din implementarea curenta.
Actualizat: 2026-07-17.

Ghid scurt pentru un addon de cod incarcat ca plugin Paper separat.

## Dependinte de build si runtime

- compileaza cu `ainpc-api` si Paper API;
- nu include si nu importa `ainpc-core-plugin`;
- declara in `plugin.yml` numele exact al core-ului: `depend: [AINPCPlugin]`;
- trateaza API-ul Kotlin ca suprafata JVM, cu limitele Java descrise separat.

## Descriptor minim

```kotlin
class ExampleAddon(version: String) : AINPCAddon {
    private val descriptor = AddonDescriptor(
        AddonDescriptor.ORIGIN_PLUGIN_ADDON,
        "example-addon",
        "Example Addon",
        version,
        "Extensie exemplu pentru AINPC",
        AddonType.FEATURE,
        false,
        EnumSet.allOf(RuntimeMode::class.java),
        listOf("example-capability"),
        listOf("ainpc-core"),
    )

    override fun getDescriptor(): AddonDescriptor = descriptor
}
```

Din Java, constructorul scurt produce un descriptor `FEATURE` valid pentru registrul strict:

```java
private final AddonDescriptor descriptor = new AddonDescriptor(
    AddonDescriptor.ORIGIN_PLUGIN_ADDON,
    "example-addon",
    "Example Addon",
    version
);
```

Pentru `AddonType.SCENARIO`, validarea stricta cere capabilitatea `scenarios`. ID-ul trebuie sa fie stabil si sa coincida cu ID-ul folosit pentru config, pack-uri si unregister.

## Inregistrare Paper

1. rezolva `AINPCPlatformApi` din `ServicesManager`;
2. verifica `platform.addonRegistry.isAddonEnabled(addonId)`;
3. pregateste configuratia si resursele proprii;
4. apeleaza `registerAddon(addon)`;
5. verifica daca descriptorul a fost acceptat;
6. instaleaza sau elimina pack-urile gestionate si apeleaza `reloadContent()`;
7. la `JavaPlugin.onDisable`, elimina resursele gestionate si apeleaza `unregisterAddon(addonId)`.

`ainpc-scenario-medieval` este implementarea de referinta pentru acest flux.

## Config versionat per-addon

- obtine directorul numai prin `platform.getAddonConfigDirectory(addonId)`;
- pastreaza schema curenta intr-un `config-template.yml` detinut de addon;
- declara un `addon.config_version` intreg si refuza downgrade-ul daca fisierul utilizatorului are o versiune viitoare;
- valideaza sintaxa si tipurile inainte de `registerAddon`;
- pentru migrare automata, creeaza backup, completeaza numai cheile lipsa si foloseste inlocuire atomica;
- ofera un mod `validate_only` pentru operatorii care nu doresc scrieri automate;
- nu confunda `addons.strict_validation` din core cu validarea configuratiei tematice.

## Lifecycle real

- la register cu validare stricta: resolverul verifica mai intai candidatul, apoi addonul vechi cu acelasi ID este dezactivat, ruleaza `onLoad`, descriptorul este publicat si ruleaza `onEnable`;
- un candidat a carui inchidere tranzitiva contine dependinte lipsa ori cicluri, sau care introduce un conflict de scenariu primar, este respins inainte de orice callback; instanta si descriptorul anterior raman active;
- reinregistrarea aceleiasi instante este idempotenta;
- daca `onLoad` sau `onEnable` esueaza, noua instanta primeste cleanup prin `onDisable`, starea ei este eliminata, iar inregistrarea anterioara este restaurata;
- la unregister strict: dependentii de cod activi sunt dezactivati tranzitiv, de la frunzele grafului spre dependinta ceruta; fiecare descriptor este eliminat chiar daca `onDisable` esueaza;
- cascada continua dupa erori, propaga prima exceptie la final si le ataseaza pe urmatoarele ca suppressed; operatiile bulk o logheaza best-effort;
- inlocuirea aceleiasi identitati nu declanseaza cascada: dependentii raman activi, iar versiunea anterioara este restaurata daca activarea noua esueaza;
- `removeByOrigin` ruleaza acelasi lifecycle pentru instantele de cod si elimina direct numai descriptorii declarativi;
- la shutdown core: addonurile inregistrate sunt dezactivate in ordine inversa, iar o exceptie normala este raportata fara a opri cleanup-ul celorlalte addonuri;
- `registerAddon` si `unregisterAddon` propaga exceptia lifecycle dupa cleanup; operatiile bulk de reconfigurare, origine si shutdown o izoleaza si o logheaza;
- callback-urile de eveniment sunt livrate pe un snapshot de ID-uri; o dezregistrare din callback nu invalideaza iterarea, iar fiecare exceptie normala este logata cu numele callback-ului si ID-ul addonului fara a opri ceilalti consumatori.

## Validarea descriptorului

Cu `addons.strict_validation: true`, registrul verifica:

- ID si nume nenule;
- compatibilitatea cu runtime mode-ul curent;
- capabilitati goale sau duplicate;
- capabilitatea `scenarios` pentru tipul `SCENARIO`;
- dependinte goale, duplicate, autoreferentiale sau dezactivate.

Pentru `registerAddon`, aceeasi optiune activeaza si gate-ul candidatului din `AddonDependencyResolver`: intreaga lui inchidere tranzitiva trebuie sa aiba dependintele inregistrate si sa nu contina cicluri, iar candidatul nu poate introduce al doilea scenariu primar. Erorile declarative din afara acestei inchideri nu il blocheaza. `registerDescriptor` ramane o cale declarativa fara lifecycle: pastreaza descriptorul, emite warning pentru problemele grafului care il implica si lasa selectia scenariului primar determinista. Validarea dependintelor dintre feature pack-uri este facuta separat de loader.

Pluginurile Paper trebuie sa declare si sa respecte ordinea reala prin `depend`/`softdepend`; registrul nu activeaza automat o ordine topologica. Cascada apeleaza numai lifecycle-ul `AINPCAddon.onDisable`, nu dezactiveaza obiectul Bukkit `JavaPlugin`. Reload-ul feature pack elimina temporar descriptorii fara cascada, ii reincarca si reconciliaza numai graful final, astfel incat un pack revenit in aceeasi operatie nu opreste consumatorii.

Cheia compatibila `addons.load_order` stabileste numai prioritatea descriptorilor returnati si scenariul primar de fallback, nu ordinea de activare Paper. Cu `addons.strict_validation: false`, gate-ul de register, cascada de unregister si reconcilierea dependintelor sunt dezactivate explicit.

## Limite de extensie

- toate cele cinci callback-uri au producatori core conectati;
- `onNpcStateChange` primeste UUID-ul NPC-ului si numele stabile `NPCState.name` numai pentru o tranzitie runtime acceptata si diferita de starea curenta;
- simularea poate forta o tranzitie peste regula de prioritate, dar publica acelasi callback; rehidratarea starii persistate este intentionat silentioasa;
- callback-urile addon nu inlocuiesc evenimentele Bukkit publice;
- un feature pack declarativ nu primeste lifecycle de cod; el inregistreaza doar un descriptor cu originea `feature-pack`.

## Ce nu trebuie sa faca

- sa acceseze `plugin.platform` sau alte internals ale core-ului;
- sa scrie direct in bazele de date AINPC;
- sa considere descriptorul o garantie ca dependintele au fost pornite;
- sa modifice fisiere pe care nu le detine;
- sa lase pack-uri gestionate in urma dupa dezactivare.

## Legaturi

- `reference/documentatie-api.md`
- `reference/addon-config-template.md`
- `reference/scenario-pack-schema.md`
- `reference/kotlin-interop-api-addonuri.md`
- `planning/stabilizare-api-si-addonuri.md`
