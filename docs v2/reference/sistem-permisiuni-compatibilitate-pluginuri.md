# Sistem de permisiuni si compatibilitate cu alte pluginuri

Status: referinta canonica pentru autorizarea Bukkit/Paper si integrarile externe verificate.
Actualizat: 2026-07-16.

## Ownership

- `ainpc-core-plugin/src/main/resources/plugin.yml` declara nodurile, valorile implicite si ordinea optionala de incarcare;
- apelurile runtime `CommandSender.hasPermission` si `Player.hasPermission` decid accesul efectiv;
- `GuiService.canOpen` detine matricea pe cheie GUI, iar `GuiService.handleClick` o reaplica pentru cheia sesiunii;
- `hasCreatorAccess` unifica dreptul de authoring al comenzilor la `ainpc.admin OR ainpc.creator`;
- `IntegrationRegistry` detine lifecycle-ul adaptoarelor externe;
- `VaultEconomyHook` si `VaultEconomyInvocationHandler` detin singura integrare third-party concreta confirmata.

AINPC nu detine grupuri, mosteniri de rol, contexte LuckPerms sau stocarea permisiunilor unui player.

## Model de autorizare

- toate verificarile folosesc API-ul standard Bukkit/Paper `hasPermission`;
- permisiunile nu sunt copiate in DB si nu sunt cache-uite de AINPC;
- un feature flag decide daca o capabilitate este pornita, nu daca actorul este autorizat;
- numele `ainpc.admin` nu creeaza singur un wildcard: accesul exista unde codul verifica explicit nodul sau unde `plugin.yml` declara o relatie `children`;
- descriptorul curent nu declara `children` pentru `ainpc.admin` si nu declara `permission:` pe radacinile de comanda;
- de aceea o comanda poate fi vizibila si rutata, apoi respinsa de handlerul subcomenzii;
- granturile si negarile managerului de permisiuni raman autoritatea finala.

Ordinea normala este:

1. intrarea verifica feature flagul relevant, daca exista;
2. handlerul comenzii sau `GuiService.canOpen` verifica permisiunea;
3. la click, cheia sesiunii este verificata din nou;
4. o comanda dispatch-uita ori serviciul mutator trebuie sa-si pastreze propriul gate si validarea de business.

Confirmarea nu inlocuieste autorizarea. `ConfirmActionGui` confirma intentia, iar comanda sau operatia finala trebuie sa valideze din nou actorul si targetul.

## Roluri GUI

`GuiRole` este o proiectie tranzitorie, nu un rol persistent:

1. `ADMIN` daca playerul are `ainpc.admin`;
2. `CREATOR` daca nu este admin si are `ainpc.creator`;
3. `PLAYER` in rest.

Suprafetele player folosesc nodurile publice de quest, info, talk si GUI. Formularele quest creator, quest map, quick quest si hub-urile creator cer `ainpc.admin` sau `ainpc.creator`; `ainpc.gui.quest` nu mai acorda authoring. Ecranele administrative folosesc `ainpc.admin` ori nodul specializat declarat.

## Catalog declarat

| Nod | Default | Responsabilitate curenta |
| --- | --- | --- |
| `ainpc.admin` | `op` | comenzi administrative si fallback explicit in multe ecrane |
| `ainpc.creator` | `op` | quest/mapping authoring si hub creator |
| `ainpc.talk` | `true` | dialog si interactiuni normale cu NPC-uri |
| `ainpc.info` | `true` | informatii NPC si fallback pentru statistici/memorii |
| `ainpc.quest` | `true` | quest log si actiuni normale de progresie |
| `ainpc.gui` | `true` | hub GUI principal/player |
| `ainpc.gui.quest` | `true` | quest log, detail si oferta; nu authoring |
| `ainpc.gui.stats` | `true` | statistici |
| `ainpc.gui.interact` | `true` | interactiuni NPC |
| `ainpc.gui.shop` | `true` | shop NPC |
| `ainpc.gui.story` | `op` | story read-only si, impreuna cu creator, story authoring |
| `ainpc.gui.relationship` | `op` | relatii NPC-NPC |
| `ainpc.gui.npc` | `op` | acces specializat la memorii; `ainpc.info` ramane fallback |
| `ainpc.gui.routine` | `op` | rutina NPC |
| `ainpc.gui.world` | `op` | world, region/place si mapping administrativ |
| `ainpc.gui.manager` | `op` | manager NPC si fallback pentru rutina/relatii |
| `ainpc.gui.audit` | `op` | audit GUI |
| `ainpc.gui.debug` | `op` | debug si authoring read-only |
| `ainpc.gui.mcp` | `op` | MCP admin |
| `ainpc.gui.quest_map` | `op` | grant specializat pentru quest map mutator |

`default: true` inseamna acces implicit Bukkit si poate fi negat de un manager de permisiuni. `default: op` nu trebuie folosit ca substitut pentru o politica explicita pe serverele de productie.

## Comenzi

Radacinile `/ainpc`, `/npc`, `/quest`, `/progression`, `/contract`, `/duty`, `/bounty`, `/event`, `/tutorial` si `/ritual` nu au un singur `permission:` in `plugin.yml`, deoarece combina operatii publice, creator si admin.

- quest log, status, info si interactiunile normale folosesc `ainpc.quest`, `ainpc.info` sau `ainpc.talk`;
- formularele, resetarea draftului, preview/validate de draft, quick quest si `create ai` folosesc `ainpc.admin OR ainpc.creator`;
- exportul AI in `packs/`, importul, reload-ul, backup-ul, reindexarea si mutatiile administrative raman `ainpc.admin`;
- consola nu are rol GUI; fiecare handler isi rezolva separat permisiunea si cerinta de player.

Help-ul si tab completion-ul nu reprezinta o dovada de autorizare. Gate-ul handlerului este contractul executabil.

## LuckPerms

Compatibilitatea LuckPerms este indirecta si intentionata:

- LuckPerms furnizeaza rezultatul Bukkit pentru `hasPermission`;
- AINPC nu importa API-ul LuckPerms si nu cauta nume de grupuri;
- permisiunea este evaluata la deschidere si la actiune, deci un context LuckPerms activ poate influenta rezultatul standard;
- AINPC nu are teste dedicate pentru contexte world/server, meta, track-uri sau recalculare dupa schimbarea contextului.

Nu documenta o integrare LuckPerms nativa pana cand nu exista adaptor sau utilizare directa a API-ului sau.

## IntegrationRegistry si Vault

`IntegrationRegistry` inregistreaza adaptoare dupa numele normalizat al pluginului. Intrarea devine vizibila numai dupa un `onRegister` reusit, reinregistrarea aceleiasi instante este idempotenta, iar inlocuirea dezactiveaza vechiul adaptor si il restaureaza daca activarea celui nou esueaza. O activare partiala este curatata prin `onUnregister`; la shutdown, o exceptie normala dintr-un adaptor este raportata fara a opri curatarea celorlalte intrari. Prezenta intrarii `Vault` in registry inseamna ca adaptorul AINPC a fost instalat, nu ca Vault este activ. Starea concreta este expusa de `VaultEconomyHook.isVaultPresent()` si `isRegistered()`.

Vault este declarat `softdepend` pentru ca API-ul optional sa fie vizibil dupa incarcarea Vault. Bridge-ul:

- este provider Vault Economy pentru economia interna AINPC, nu consumer al altui provider;
- inregistreaza un proxy `Economy` in Bukkit `ServicesManager` cu `ServicePriority.Normal`;
- raporteaza zero zecimale si foloseste solduri intregi AINPC;
- suporta balance, has, deposit si withdraw pentru overload-urile player/nume si world;
- respinge sume negative, fractionare, nefinite si overflow-ul soldului;
- poate citi soldul offline, dar mutatiile necesita player online;
- nu implementeaza operatii bancare si raspunde cu failure pentru ele;
- se dezregistreaza si isi reseteaza starea la shutdown.

Un server cu mai multi provideri Vault trebuie sa verifice providerul selectat de `ServicesManager`; AINPC nu forteaza prioritate peste un provider existent.

## Matrice de compatibilitate

| Sistem | Stare verificata | Limita |
| --- | --- | --- |
| Bukkit/Paper permissions | nativ | politica este distribuita intre handler, GUI si serviciul final |
| LuckPerms | compatibil prin Bukkit | fara API, grupuri sau teste de contexte dedicate |
| Bukkit `ServicesManager` | activ | publica `AINPCPlatformApi` si providerul optional Vault |
| Vault Economy | adaptor activ optional | AINPC este provider; fara bank, fractionar sau mutatii offline |
| WorldEdit | fara adaptor runtime | nicio constructie, schematic paste sau undo |
| WorldGuard/region protection | fara adaptor runtime | mutatiile AINPC nu consulta automat claim-uri sau regiuni protejate |
| PlaceholderAPI | fara hook verificat | nu exista expansion AINPC |
| Citizens/ProtocolLib | fara hook verificat | NPC-urile AINPC nu sunt adaptate prin aceste API-uri |

Prezenta unui nume in `IntegrationType`, in documentatie sau intr-un feature-pack nu dovedeste existenta unui adaptor.

## Checklist operational

1. neaga explicit nodurile publice care nu sunt dorite;
2. acorda `ainpc.creator` numai autorilor de continut;
3. testeaza separat player, creator, admin si consola;
4. verifica atat deschiderea GUI, cat si actiunea finala;
5. pentru Vault, confirma logul de inregistrare si providerul ales de server;
6. nu activa mutatii world intr-o zona protejata presupunand integrare WorldGuard inexistenta;
7. dupa schimbarea nodurilor, ruleaza testele descriptorului si smoke-ul Paper controlat.

## Surse in cod

- `ainpc-core-plugin/src/main/resources/plugin.yml`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/commands/AINPCCommandAccess.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/gui/GuiRole.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/gui/GuiService.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/integration/IntegrationRegistry.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/economy/VaultEconomyHook.kt`
- `ainpc-core-plugin/src/main/kotlin/ro/ainpc/economy/VaultEconomyInvocationHandler.kt`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/security/PluginAuthorizationDescriptorTest.kt`
- `ainpc-core-plugin/src/test/kotlin/ro/ainpc/economy/VaultEconomyInvocationHandlerTest.kt`

## Legaturi

- `reference/gui-stack.md`
- `reference/feature-flags-lifecycle.md`
- `architecture/harta-clase-addons.md`
- `architecture/worldedit-integration-contract.md`
- `planning/authorization-and-plugin-compatibility-hardening.md`
- `planning/gui-ux-hardening.md`
