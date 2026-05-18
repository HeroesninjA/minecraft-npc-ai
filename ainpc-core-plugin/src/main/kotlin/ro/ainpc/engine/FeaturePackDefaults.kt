package ro.ainpc.engine

import org.bukkit.configuration.ConfigurationSection
import ro.ainpc.topology.TopologyCategory
import java.util.function.BiConsumer

object FeaturePackDefaults {
    @JvmStatic
    fun loadDefaultMedievalPack(
        loadedPacks: MutableMap<String, FeaturePackLoader.FeaturePack>,
        allTraits: MutableMap<String, FeaturePackLoader.TraitDefinition>,
        allProfessions: MutableMap<String, FeaturePackLoader.ProfessionDefinition>,
        registerTopology: BiConsumer<FeaturePackLoader.FeaturePack, FeaturePackLoader.TopologyDefinition>,
        registerPackDescriptor: BiConsumer<FeaturePackLoader.FeaturePack, ConfigurationSection?>,
    ) {
        val medieval = FeaturePackLoader.FeaturePack("medieval", "Medieval", "Pachet pentru setari medievale")

        val greedy = FeaturePackLoader.TraitDefinition("greedy", "Lacom", "Doreste sa acumuleze bogatie")
        greedy.addActionModifier("TRADE", 15)
        greedy.addActionModifier("GIVE_ITEM", -20)
        greedy.addEmotionModifier("happiness", 0.2)
        allTraits["greedy"] = greedy
        medieval.addTrait(greedy)

        val brave = FeaturePackLoader.TraitDefinition("brave", "Curajos", "Nu se teme de pericole")
        brave.addActionModifier("FLEE", -30)
        brave.addActionModifier("ATTACK", 20)
        brave.addActionModifier("DEFEND", 15)
        brave.addEmotionModifier("fear", -0.3)
        allTraits["brave"] = brave
        medieval.addTrait(brave)

        val shy = FeaturePackLoader.TraitDefinition("shy", "Timid", "Evita interactiunile sociale")
        shy.addActionModifier("GREET", -15)
        shy.addActionModifier("SOCIALIZE", -20)
        shy.addActionModifier("TALK", -10)
        shy.addEmotionModifier("fear", 0.2)
        allTraits["shy"] = shy
        medieval.addTrait(shy)

        val friendly = FeaturePackLoader.TraitDefinition("friendly", "Prietenos", "Iubeste sa interactioneze cu altii")
        friendly.addActionModifier("GREET", 20)
        friendly.addActionModifier("SOCIALIZE", 25)
        friendly.addActionModifier("HELP", 15)
        friendly.addEmotionModifier("happiness", 0.3)
        allTraits["friendly"] = friendly
        medieval.addTrait(friendly)

        val lazy = FeaturePackLoader.TraitDefinition("lazy", "Lenes", "Evita munca grea")
        lazy.addActionModifier("START_WORK", -25)
        lazy.addActionModifier("CONTINUE_WORK", -20)
        lazy.addActionModifier("REST", 30)
        allTraits["lazy"] = lazy
        medieval.addTrait(lazy)

        val hardworking = FeaturePackLoader.TraitDefinition("hardworking", "Harnic", "Iubeste sa munceasca")
        hardworking.addActionModifier("START_WORK", 25)
        hardworking.addActionModifier("CONTINUE_WORK", 20)
        hardworking.addActionModifier("REST", -15)
        allTraits["hardworking"] = hardworking
        medieval.addTrait(hardworking)

        val blacksmith = FeaturePackLoader.ProfessionDefinition("blacksmith", "Fierar", "Lucreaza metalul")
        blacksmith.addScheduleEntry("MORNING", "Deschide atelierul")
        blacksmith.addScheduleEntry("AFTERNOON", "Forjeaza unelte")
        blacksmith.addScheduleEntry("EVENING", "Curata atelierul")
        blacksmith.addScheduleEntry("NIGHT", "Doarme")
        blacksmith.tools = listOf("ciocan", "nicovala", "clesti")
        allProfessions["blacksmith"] = blacksmith
        medieval.addProfession(blacksmith)

        val farmer = FeaturePackLoader.ProfessionDefinition("farmer", "Fermier", "Cultiva pamantul")
        farmer.addScheduleEntry("MORNING", "Se trezeste devreme, uda plantele")
        farmer.addScheduleEntry("AFTERNOON", "Lucreaza campul")
        farmer.addScheduleEntry("EVENING", "Hraneste animalele")
        farmer.addScheduleEntry("NIGHT", "Doarme")
        farmer.tools = listOf("sapa", "coasa", "galeata")
        allProfessions["farmer"] = farmer
        medieval.addProfession(farmer)

        val guard = FeaturePackLoader.ProfessionDefinition("guard", "Garda", "Pazeste si protejeaza")
        guard.addScheduleEntry("MORNING", "Patruleaza")
        guard.addScheduleEntry("AFTERNOON", "Pazeste poarta")
        guard.addScheduleEntry("EVENING", "Patruleaza")
        guard.addScheduleEntry("NIGHT", "Pazeste sau doarme in ture")
        guard.tools = listOf("sabie", "scut", "lancie")
        allProfessions["guard"] = guard
        medieval.addProfession(guard)

        val village = FeaturePackLoader.TopologyDefinition(
            medieval.id,
            "village_center",
            "Sat deschis",
            TopologyCategory.PLAINS,
            "Asezare rurala activa, cu munca, schimb social si comunitate.",
        )
        village.biomes = listOf("PLAINS", "MEADOW", "SUNFLOWER_PLAINS")
        village.dialogueHints = listOf(
            "vorbeste despre recolta, vecini si targ",
            "pastreaza un ton practic si comunitar",
        )
        village.suggestedTraits = listOf("friendly", "hardworking")
        registerTopology.accept(medieval, village)

        val forestEdge = FeaturePackLoader.TopologyDefinition(
            medieval.id,
            "forest_edge",
            "Margine de padure",
            TopologyCategory.FOREST,
            "Zona de tranzitie intre sat si salbaticie, buna pentru avertismente si zvonuri.",
        )
        forestEdge.biomes = listOf("FOREST", "BIRCH_FOREST", "DARK_FOREST")
        forestEdge.dialogueHints = listOf(
            "accent pe prudenta, vanatoare si drumuri periculoase",
            "discuta despre creaturi si lemnari",
        )
        forestEdge.suggestedTraits = listOf("brave", "suspicious")
        registerTopology.accept(medieval, forestEdge)

        registerPackDescriptor.accept(medieval, null)
        loadedPacks["medieval"] = medieval
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
