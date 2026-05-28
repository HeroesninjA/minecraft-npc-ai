package ro.ainpc.engine

import org.bukkit.configuration.ConfigurationSection
import ro.ainpc.topology.TopologyCategory
import java.util.function.BiConsumer

object FeaturePackDefaults {
    @JvmStatic
    fun loadNeutralFallbackPack(
        loadedPacks: MutableMap<String, FeaturePackLoader.FeaturePack>,
        allTraits: MutableMap<String, FeaturePackLoader.TraitDefinition>,
        allProfessions: MutableMap<String, FeaturePackLoader.ProfessionDefinition>,
        registerTopology: BiConsumer<FeaturePackLoader.FeaturePack, FeaturePackLoader.TopologyDefinition>,
        registerPackDescriptor: BiConsumer<FeaturePackLoader.FeaturePack, ConfigurationSection?>,
    ) {
        val fallback = FeaturePackLoader.FeaturePack("core_minimal", "Core Minimal", "Fallback neutru minimal pentru servere fara pack-uri")

        val practical = FeaturePackLoader.TraitDefinition("practical", "Practic", "Preferinta pentru actiuni utile si clare")
        practical.addActionModifier("HELP", 15)
        practical.addActionModifier("START_WORK", 10)
        practical.addActionModifier("OBSERVE", 10)
        practical.addEmotionModifier("happiness", 0.1)
        allTraits["practical"] = practical
        fallback.addTrait(practical)

        val cautious = FeaturePackLoader.TraitDefinition("cautious", "Prudent", "Verifica situatia inainte sa actioneze")
        cautious.addActionModifier("OBSERVE", 20)
        cautious.addActionModifier("WARN", 15)
        cautious.addActionModifier("FLEE", 5)
        cautious.addEmotionModifier("fear", 0.1)
        allTraits["cautious"] = cautious
        fallback.addTrait(cautious)

        val friendly = FeaturePackLoader.TraitDefinition("friendly", "Prietenos", "Interactioneaza usor cu playerii si NPC-urile")
        friendly.addActionModifier("GREET", 20)
        friendly.addActionModifier("SOCIALIZE", 25)
        friendly.addActionModifier("HELP", 15)
        friendly.addEmotionModifier("happiness", 0.3)
        allTraits["friendly"] = friendly
        fallback.addTrait(friendly)

        val hardworking = FeaturePackLoader.TraitDefinition("hardworking", "Harnic", "Isi urmeaza rutina si sarcinile")
        hardworking.addActionModifier("START_WORK", 25)
        hardworking.addActionModifier("CONTINUE_WORK", 20)
        hardworking.addActionModifier("REST", -15)
        allTraits["hardworking"] = hardworking
        fallback.addTrait(hardworking)

        val worker = FeaturePackLoader.ProfessionDefinition("worker", "Lucrator", "Executa sarcini generale ale comunitatii")
        worker.addScheduleEntry("MORNING", "Isi pregateste sarcinile")
        worker.addScheduleEntry("AFTERNOON", "Lucreaza la obiective locale")
        worker.addScheduleEntry("EVENING", "Incheie activitatea si se retrage")
        worker.addScheduleEntry("NIGHT", "Se odihneste")
        worker.tools = listOf("unealta", "materiale", "lista de sarcini")
        allProfessions["worker"] = worker
        fallback.addProfession(worker)

        val caretaker = FeaturePackLoader.ProfessionDefinition("caretaker", "Ingrijitor", "Mentine locurile importante functionale")
        caretaker.addScheduleEntry("MORNING", "Verifica locatiile importante")
        caretaker.addScheduleEntry("AFTERNOON", "Repara si organizeaza resurse")
        caretaker.addScheduleEntry("EVENING", "Raporteaza problemele observate")
        caretaker.addScheduleEntry("NIGHT", "Se odihneste")
        caretaker.tools = listOf("registru", "resurse", "trusa")
        allProfessions["caretaker"] = caretaker
        fallback.addProfession(caretaker)

        val guide = FeaturePackLoader.ProfessionDefinition("guide", "Ghid", "Ajuta playerii sa inteleaga zona")
        guide.addScheduleEntry("MORNING", "Observa traficul prin asezare")
        guide.addScheduleEntry("AFTERNOON", "Ajuta vizitatorii")
        guide.addScheduleEntry("EVENING", "Strange informatii locale")
        guide.addScheduleEntry("NIGHT", "Se retrage")
        guide.tools = listOf("harta", "notite", "semne")
        allProfessions["guide"] = guide
        fallback.addProfession(guide)

        val village = FeaturePackLoader.TopologyDefinition(
            fallback.id,
            "settlement_center",
            "Centru de asezare",
            TopologyCategory.PLAINS,
            "Zona centrala pentru activitati, orientare si interactiuni locale.",
        )
        village.biomes = listOf("PLAINS", "MEADOW", "SUNFLOWER_PLAINS")
        village.dialogueHints = listOf(
            "vorbeste despre locuri, sarcini si oameni din zona",
            "pastreaza un ton practic si usor de inteles",
        )
        village.suggestedTraits = listOf("friendly", "hardworking")
        registerTopology.accept(fallback, village)

        val forestEdge = FeaturePackLoader.TopologyDefinition(
            fallback.id,
            "wild_edge",
            "Margine de zona salbatica",
            TopologyCategory.FOREST,
            "Zona de tranzitie intre asezare si spatii mai putin controlate.",
        )
        forestEdge.biomes = listOf("FOREST", "BIRCH_FOREST", "DARK_FOREST")
        forestEdge.dialogueHints = listOf(
            "accent pe prudenta, orientare si resurse locale",
            "discuta despre trasee, limite si riscuri",
        )
        forestEdge.suggestedTraits = listOf("cautious", "practical")
        registerTopology.accept(fallback, forestEdge)

        registerPackDescriptor.accept(fallback, null)
        loadedPacks[fallback.id] = fallback
    }

    @JvmStatic
    fun loadDefaultTopologies(
        loadedPacks: MutableMap<String, FeaturePackLoader.FeaturePack>,
        registerTopology: BiConsumer<FeaturePackLoader.FeaturePack, FeaturePackLoader.TopologyDefinition>,
        registerPackDescriptor: BiConsumer<FeaturePackLoader.FeaturePack, ConfigurationSection?>,
    ) {
        val pack = loadedPacks.computeIfAbsent("core_topology") {
            FeaturePackLoader.FeaturePack("core_topology", "Core Topology", "Fallback intern pentru topologii")
        }

        val interior = FeaturePackLoader.TopologyDefinition(
            pack.id,
            "interior_default",
            "Interior de baza",
            TopologyCategory.INTERIOR,
            "Spatiu inchis, sigur si mai controlat decat exteriorul.",
        )
        interior.dialogueHints = listOf(
            "accent pe siguranta, adapost si organizare",
            "replici mai calme si apropiate",
        )
        registerTopology.accept(pack, interior)

        val plains = FeaturePackLoader.TopologyDefinition(
            pack.id,
            "plains_default",
            "Camp de baza",
            TopologyCategory.PLAINS,
            "Zona deschisa, buna pentru asezari, agricultura si intalniri sociale.",
        )
        plains.dialogueHints = listOf(
            "discuta despre drumuri, campuri si comunitate",
            "ton practic, deschis si local",
        )
        registerTopology.accept(pack, plains)

        val forest = FeaturePackLoader.TopologyDefinition(
            pack.id,
            "forest_default",
            "Padure de baza",
            TopologyCategory.FOREST,
            "Zona impadurita care sugereaza prudenta, resurse si mister.",
        )
        forest.dialogueHints = listOf("discuta despre lemn, creaturi si poteci ascunse")
        registerTopology.accept(pack, forest)
        registerPackDescriptor.accept(pack, null)
    }
}
