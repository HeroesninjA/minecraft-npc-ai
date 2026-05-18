package ro.ainpc.utils

import java.util.Locale
import java.util.Random

object NPCNameGenerator {
    private val MALE_NAMES = arrayOf(
        "Aaron", "Abel", "Achim", "Adam", "Adelin", "Adi", "Adrian", "Alec", "Alexandru", "Alin",
        "Ambrozie", "Andrei", "Anghel", "Antim", "Anton", "Antonie", "Apostol", "Aurel", "Aurelian", "Avram",
        "Barbu", "Bartolomeu", "Basarab", "Beniamin", "Bogdan", "Bran", "Brindus", "Calin", "Camil", "Carol",
        "Casian", "Catalin", "Cezar", "Ciprian", "Claudiu", "Codin", "Codrut", "Constantin", "Cornel", "Corneliu",
        "Cosmin", "Costache", "Costel", "Crin", "Cristian", "Dacian", "Dan", "Daniel", "Danut", "Darie",
        "Darius", "David", "Decebal", "Dinu", "Dionisie", "Dorel", "Dorin", "Doru", "Dragomir", "Dragos",
        "Dumitru", "Eduard", "Eftimie", "Emil", "Emanuel", "Eremia", "Ernest", "Eugen", "Eusebiu", "Fabian",
        "Felix", "Filip", "Flaviu", "Florea", "Florian", "Florin", "Gabriel", "Gavril", "Gelasiu", "George",
        "Gheorghe", "Gherasim", "Gicu", "Grigore", "Haralambie", "Horia", "Iacob", "Iancu", "Ieremia", "Ilie",
        "Ioan", "Ion", "Ionel", "Ionut", "Iosif", "Iov", "Irinel", "Isidor", "Iulian", "Iuliu",
        "Laurentiu", "Lazar", "Leontin", "Liviu", "Luca", "Lucian", "Macedon", "Manole", "Marcel", "Marius",
        "Marin", "Matei", "Mircea", "Mihai", "Mihail", "Milian", "Miron", "Mitica", "Nae", "Narcis",
        "Neculai", "Nelu", "Nichita", "Nicolae", "Nicu", "Nistor", "Octav", "Octavian", "Olimpiu", "Oreste",
        "Ovidiu", "Pamfil", "Paul", "Pavel", "Petre", "Petru", "Radu", "Razvan", "Remus", "Robert",
        "Romeo", "Romulus", "Sabin", "Sandu", "Sebastian", "Septimiu", "Serafim", "Sergiu", "Sever", "Silviu",
        "Sorin", "Stan", "Stefan", "Tase", "Teodor", "Teofil", "Tiberiu", "Titus", "Traian", "Tudor",
        "Valentin", "Valeriu", "Vasile", "Vergil", "Victor", "Viorel", "Virgil", "Vlad", "Vladimir", "Zaharia",
        "Zamfir", "Zeno", "Aftene", "Albu", "Arsenie", "Badea", "Balaur", "Banu", "Bastian", "Boris",
        "Bratosin", "Calistrat", "Ciprianus", "Corvin", "Damaschin", "Damian", "Dionis", "Dobrin", "Dorian", "Ene",
        "Eremie", "Fane", "Faur", "Gligor", "Goran", "Horatiu", "Ilarie", "Ilarion", "Isac", "Leonte",
        "Mihnea", "Mocanu", "Neagu", "Nectarie", "Pahomie", "Panait", "Petrache", "Raducu", "Relu", "Samuil"
    )

    private val FEMALE_NAMES = arrayOf(
        "Adela", "Adelina", "Adina", "Adriana", "Agata", "Aglaia", "Aida", "Alberta", "Alexandra", "Alina",
        "Amalia", "Ana", "Anamaria", "Anca", "Anda", "Andrada", "Andreea", "Angela", "Anica", "Antonia",
        "Ariana", "Aurica", "Aurelia", "Beatrice", "Bianca", "Brindusa", "Calina", "Camelia", "Carina", "Carmen",
        "Casandra", "Catinca", "Catrina", "Cecilia", "Celina", "Clara", "Claudia", "Codruta", "Constantina", "Corina",
        "Cornelia", "Cosmina", "Crenguta", "Crina", "Cristina", "Daciana", "Dana", "Daniela", "Daria", "Delia",
        "Denisa", "Diana", "Doina", "Domnica", "Dorina", "Ecaterina", "Elena", "Eleonora", "Elisabeta", "Eliza",
        "Ema", "Emilia", "Erica", "Estera", "Eugenia", "Eva", "Fabia", "Felicia", "Filofteia", "Flavia",
        "Floarea", "Florentina", "Florica", "Florina", "Gabriela", "Geanina", "Genoveva", "Georgeta", "Gina", "Gratiela",
        "Hana", "Hortensia", "Iasmina", "Ileana", "Ilinca", "Ina", "Ioana", "Ionela", "Irina", "Isabela",
        "Iulia", "Julieta", "Larisa", "Laura", "Lavinia", "Leana", "Lenuta", "Letitia", "Lia", "Lidia",
        "Lili", "Liliana", "Livia", "Lorena", "Lucia", "Luiza", "Magda", "Madalina", "Magdalena", "Mara",
        "Marcela", "Margareta", "Maria", "Mariana", "Maricica", "Marina", "Marta", "Matilda", "Melania", "Mihaela",
        "Mioara", "Mirela", "Miruna", "Monica", "Nadia", "Narcisa", "Natalia", "Nicoleta", "Niculina", "Nina",
        "Nora", "Oana", "Octavia", "Ofelia", "Olga", "Olimpia", "Otilia", "Paula", "Paulina", "Petronela",
        "Raluca", "Ramona", "Rebeca", "Renata", "Rodica", "Romina", "Roxana", "Ruxandra", "Sabina", "Sanda",
        "Sanziana", "Saveta", "Selena", "Severina", "Silvia", "Simina", "Simona", "Smaranda", "Sofia", "Sonia",
        "Sorina", "Stana", "Stefania", "Steluta", "Tamara", "Tatiana", "Teodora", "Tereza", "Tincuta", "Valentina",
        "Valeria", "Vasilica", "Veronica", "Veta", "Vica", "Victoria", "Viorica", "Virginia", "Xenia", "Zamfira",
        "Zina", "Zoe", "Anisia", "Arina", "Blandina", "Brandusa", "Calista", "Cezarina", "Cornelina", "Dafina",
        "Dalia", "Damaris", "Eufrosina", "Fira", "Gherghina", "Iliana", "Iordana", "Lacramioara", "Mireasa", "Nectaria",
        "Ozana", "Paraschiva", "Raisa", "Raveca", "Samira", "Sergiana", "Stanca", "Tasia", "Venera", "Zenovia"
    )

    @JvmStatic
    fun randomName(gender: String?, random: Random): String {
        val normalizedGender = gender?.lowercase(Locale.ROOT) ?: "male"
        val pool = if (normalizedGender == "female") FEMALE_NAMES else MALE_NAMES
        return pool[random.nextInt(pool.size)]
    }

    @JvmStatic
    fun predefinedNames(gender: String?): List<String> {
        val normalizedGender = gender?.lowercase(Locale.ROOT) ?: "male"
        val pool = if (normalizedGender == "female") FEMALE_NAMES else MALE_NAMES
        return pool.toList()
    }

    @JvmStatic
    fun predefinedNameCount(): Int = MALE_NAMES.size + FEMALE_NAMES.size
}
