package ro.ainpc.ai

import ro.ainpc.economy.NpcShopDefinition
import java.util.Locale

internal object NpcShopFactResolver {
    fun hasListedGoods(shops: List<NpcShopDefinition>): Boolean = listedGoods(shops).isNotEmpty()

    fun isLookupQuestion(message: String): Boolean {
        val normalized = DialogueMessageText.directIntentText(message)
        return isCatalogQuestion(normalized) || isLocationQuestion(normalized)
    }

    fun requiresWorkPlace(message: String): Boolean =
        isLocationQuestion(DialogueMessageText.directIntentText(message))

    fun resolve(
        message: String,
        shops: List<NpcShopDefinition>,
        workPlaceName: String?,
        accessDenied: Boolean = false,
    ): String? {
        val normalized = DialogueMessageText.directIntentText(message)
        if (isCatalogQuestion(normalized)) {
            return tradeSummary(shops, accessDenied)
        }
        if (accessDenied && !hasListedGoods(shops) && isLocationQuestion(normalized)) {
            return "Magazinul acesta cere acces special; nu iti pot prezenta ofertele acum."
        }
        if (shops.isEmpty()) return null
        if (isLocationQuestion(normalized) && !workPlaceName.isNullOrBlank()) {
            return "Lucrez la $workPlaceName."
        }
        return null
    }

    fun tradeSummary(shops: List<NpcShopDefinition>, accessDenied: Boolean = false): String =
        catalogSummary(shops) ?: if (accessDenied) {
            "Magazinul acesta cere acces special; nu iti pot prezenta ofertele acum."
        } else {
            "Nu am oferte pregatite acum."
        }

    fun catalogSummary(shops: List<NpcShopDefinition>): String? {
        val allGoods = listedGoods(shops)
        val goods = allGoods.take(4)
        if (goods.isEmpty()) return null
        val catalog = goods.joinToString(", ") { (name, amount) -> "$amount x $name" }
        val moreGoods = if (allGoods.size > goods.size) " si alte produse" else ""
        return "In catalog am $catalog$moreGoods. Preturile si conditiile le gasesti in magazin."
    }

    private fun listedGoods(shops: List<NpcShopDefinition>): List<Pair<String, Int>> = shops.asSequence()
        .flatMap { shop -> shop.offers.asSequence() }
        .filter { offer -> offer.configurationIssue() == null }
        .flatMap { offer -> offer.resultItems.asSequence() }
        .distinctBy { (material, _) -> material }
        .map { (material, amount) -> material.name.lowercase(Locale.ROOT).replace('_', ' ') to amount }
        .toList()

    private fun isCatalogQuestion(message: String): Boolean = DialogueMessageText.matches(
        message,
        "ce vinzi", "ce vindeti", "ce ai de vanzare", "ce aveti de vanzare",
        "ce pot cumpara", "ce oferte ai", "ce oferte aveti", "ce marfa ai", "ce marfa aveti",
        "arata-mi marfa", "arata marfa",
    )

    private fun isLocationQuestion(message: String): Boolean = DialogueMessageText.matches(
        message,
        "unde este magazinul", "unde e magazinul", "unde este pravalia", "unde e pravalia",
        "unde vinzi", "unde lucrezi",
    )
}
