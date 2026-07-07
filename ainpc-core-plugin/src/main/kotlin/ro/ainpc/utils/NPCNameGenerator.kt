package ro.ainpc.utils

import java.util.Locale
import java.util.Random

object NPCNameGenerator {
    private val MALE_NAMES = arrayOf(
        "Aaron", "Abel", "Achim", "Adam", "Adelin", "Adi", "Adrian", "Alec", "Alexandru", "Alin",
        "Ambrozie", "Andrei", "Anghel", "Antim", "Anton", "Antonie", "Apostol", "Aurel", "Aurelian", "Avram",
        "Barbu", "Bartolomeu", "Basarab", "Beniamin", "Bogdan", "Bran", "Calin", "Camil", "Carol",
        "Casian", "Catalin", "Cezar", "Ciprian", "Claudiu", "Codin", "Codrut", "Constantin", "Cornel", "Corneliu",
        "Cosmin", "Costache", "Costel", "Crin", "Cristian", "Dacian", "Dan", "Daniel", "Danut", "Darie",
        "Darius", "David", "Decebal", "Dinu", "Dionisie", "Dorel", "Dorin", "Doru", "Dragomir", "Dragos",
        "Dumitru", "Eduard", "Eftimie", "Emil", "Emanuel", "Eremia", "Ernest", "Eugen", "Eusebiu", "Fabian",
        "Felix", "Filip", "Flaviu", "Florea", "Florian", "Florin", "Gabriel", "Gavril", "George",
        "Gheorghe", "Gherasim", "Grigore", "Haralambie", "Horia", "Iacob", "Iancu", "Ieremia", "Ilie",
        "Ioan", "Ion", "Ionel", "Ionut", "Iosif", "Iov", "Irinel", "Isidor", "Iulian", "Iuliu",
        "Laurentiu", "Lazar", "Leontin", "Liviu", "Luca", "Lucian", "Manole", "Marcel", "Marius",
        "Marin", "Matei", "Mircea", "Mihai", "Mihail", "Miron", "Nae", "Narcis",
        "Neculai", "Nelu", "Nichita", "Nicolae", "Nicu", "Nistor", "Octav", "Octavian", "Olimpiu", "Oreste",
        "Ovidiu", "Paul", "Pavel", "Petre", "Petru", "Radu", "Razvan", "Remus", "Robert",
        "Romeo", "Romulus", "Sabin", "Sebastian", "Septimiu", "Serafim", "Sergiu", "Sever", "Silviu",
        "Sorin", "Stefan", "Teodor", "Teofil", "Tiberiu", "Titus", "Traian", "Tudor",
        "Valentin", "Valeriu", "Vasile", "Victor", "Viorel", "Virgil", "Vlad", "Vladimir", "Zaharia",
        "Zamfir", "Zeno", "Arsenie", "Boris", "Calistrat", "Corvin", "Damian", "Dorian",
        "Ene", "Gligor", "Horatiu", "Ilarie", "Leonte",
        "Mihnea", "Neagu", "Panait", "Samuil"
    )

    private val FEMALE_NAMES = arrayOf(
        "Adela", "Adelina", "Adina", "Adriana", "Agata", "Aida", "Alexandra", "Alina",
        "Amalia", "Ana", "Anamaria", "Anca", "Anda", "Andrada", "Andreea", "Angela", "Antonia",
        "Ariana", "Aurelia", "Beatrice", "Bianca", "Calina", "Camelia", "Carmen",
        "Catinca", "Cecilia", "Clara", "Claudia", "Codruta", "Corina",
        "Cornelia", "Cosmina", "Crina", "Cristina", "Daciana", "Dana", "Daniela", "Daria", "Delia",
        "Denisa", "Diana", "Doina", "Dorina", "Ecaterina", "Elena", "Elisabeta", "Eliza",
        "Ema", "Emilia", "Eugenia", "Eva", "Felicia", "Flavia",
        "Florentina", "Florina", "Gabriela", "Geanina", "Genoveva", "Georgeta", "Gina",
        "Ileana", "Ilinca", "Ioana", "Ionela", "Irina", "Isabela",
        "Iulia", "Larisa", "Laura", "Lavinia", "Letitia", "Lidia",
        "Liliana", "Livia", "Lucia", "Luiza", "Magda", "Madalina", "Mara",
        "Marcela", "Margareta", "Maria", "Mariana", "Marina", "Marta", "Melania", "Mihaela",
        "Mioara", "Mirela", "Miruna", "Monica", "Nadia", "Narcisa", "Nicoleta", "Nina",
        "Oana", "Octavia", "Otilia", "Paula", "Petronela",
        "Raluca", "Ramona", "Rebeca", "Rodica", "Roxana", "Ruxandra", "Sabina",
        "Silvia", "Simona", "Smaranda", "Sofia", "Sonia",
        "Sorina", "Stefania", "Tamara", "Teodora", "Valentina",
        "Valeria", "Veronica", "Victoria", "Viorica", "Virginia", "Xenia", "Zamfira",
        "Zina", "Zoe", "Arina", "Dafina",
        "Dalia", "Iliana", "Lacramioara", "Paraschiva", "Stanca", "Venera", "Zenovia"
    )

    private val SURNAMES = arrayOf(
        "Popescu", "Ionescu", "Popa", "Dumitrescu", "Stan", "Dima", "Gheorghiu", "Barbu", "Voicu", "Diaconu",
        "Mihăilescu", "Constantinescu", "Cristea", "Marin", "Nistor", "Tudor", "Radu", "Florescu", "Stoica", "Dobrescu",
        "Anghel", "Neacșu", "Munteanu", "Cojocaru", "Rusu", "Costache", "Dobre", "Dragomir", "Vasilescu", "Bălan",
        "Lupu", "Turcu", "Miron", "Adam", "Toma", "Oprea", "Ursu", "Sandu", "Grigorescu", "Botezatu",
        "Moise", "Crețu", "Sava", "Avram", "Badea", "Moga", "Călinescu", "Bucur", "Chiriac", "Darie",
        "Fodor", "Gal", "Horvath", "Kovacs", "Matei", "Negrea", "Olteanu", "Petrescu", "Rădoi", "Șerban",
        "Tănase", "Vlad", "Zaharia", "Bădescu", "Ciobanu", "Dascălu", "Enache", "Farcaș", "Groza", "Hăisan",
        "Iftimie", "Juravle", "Lazăr", "Macovei", "Năstase", "Păduraru", "Rădulescu", "Sasu", "Țîrdea", "Udrea",
        "Văduva", "Zamfir", "Albu", "Bucșă", "Cîrstea", "Danciu", "Eftimie", "Faur", "Gavrilă", "Hulub",
        "Ivașcu", "Jitea", "Lungu", "Mărginean", "Nicoară", "Pavel", "Rebegea", "Stănescu", "Șova", "Tănasă",
        "Uncuță", "Vornicu", "Zăinescu", "Bârlădeanu", "Ciucă", "Drăgan", "Fierăscu", "Gheța", "Hârșu",
        "Iliuță", "Joița", "Lică", "Mogoș", "Nechita", "Pălășan", "Roată", "Săvulescu", "Șchiopu", "Timofte"
    )

    private val NICKNAME_PREFIXES = arrayOf(
        "Bătrânul", "Tânărul", "Cel", "Micul", "Marele", "Viteazul", "Înțeleptul", "Neînfricatul",
        "Blândul", "Puternicul", "Răbdătorul", "Tăcutul", "Vorbărețul", "Sprințarul", "Credinciosul"
    )

    @JvmStatic
    fun randomName(gender: String?, random: Random): String {
        val normalizedGender = gender?.lowercase(Locale.ROOT) ?: "male"
        val pool = if (normalizedGender == "female") FEMALE_NAMES else MALE_NAMES
        return pool[random.nextInt(pool.size)]
    }

    @JvmStatic
    fun randomFullName(gender: String?, random: Random): String {
        val firstName = randomName(gender, random)
        val surname = SURNAMES[random.nextInt(SURNAMES.size)]
        return "$firstName $surname"
    }

    @JvmStatic
    fun randomNickname(gender: String?, random: Random): String {
        val firstName = randomName(gender, random)
        val prefix = NICKNAME_PREFIXES[random.nextInt(NICKNAME_PREFIXES.size)]
        return "$prefix $firstName"
    }

    @JvmStatic
    fun randomNameWithTitle(occupation: String?, gender: String?, random: Random): String {
        val name = randomFullName(gender, random)
        if (occupation.isNullOrBlank()) return name
        return when (occupation.lowercase()) {
            "mayor", "primar" -> "Primarul $name"
            "priest", "preot" -> "Părintele $name"
            "captain", "căpitan" -> "Căpitanul $name"
            "elder", "bătrân" -> "Bătrânul $name"
            "teacher", "dascăl" -> "Dascălul $name"
            "doctor", "vraci" -> "Vraciul $name"
            "merchant", "negustor" -> "Negustorul $name"
            else -> name
        }
    }

    @JvmStatic
    fun predefinedNames(gender: String?): List<String> {
        val normalizedGender = gender?.lowercase(Locale.ROOT) ?: "male"
        return if (normalizedGender == "female") FEMALE_NAMES.toList() else MALE_NAMES.toList()
    }

    @JvmStatic
    fun predefinedSurnames(): List<String> = SURNAMES.toList()

    @JvmStatic
    fun predefinedNameCount(): Int = MALE_NAMES.size + FEMALE_NAMES.size

    @JvmStatic
    fun totalNameCombinations(): Long = MALE_NAMES.size.toLong() * SURNAMES.size.toLong() +
        FEMALE_NAMES.size.toLong() * SURNAMES.size.toLong()
}
