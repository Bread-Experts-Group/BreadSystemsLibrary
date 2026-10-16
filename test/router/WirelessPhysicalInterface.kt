package router

sealed interface WirelessPhysicalInterface {
    val name: String?
    val bands: Map<UShort, Band>
    val attributes: Set<NetLinkAttribute>

    data class Construction(
        override var name: String? = null,
        override val bands: MutableMap<UShort, Band.Construction> = mutableMapOf(),
        override val attributes: MutableSet<NetLinkAttribute> = mutableSetOf()
    ) : WirelessPhysicalInterface

    sealed interface Band {
        val rates: Map<UShort, Rate>
        val frequencies: Map<UShort, Frequency>
        val attributes: Set<NetLinkAttribute>

        data class Construction(
            override val rates: MutableMap<UShort, Rate.Construction> = mutableMapOf(),
            override val frequencies: MutableMap<UShort, Frequency.Construction> = mutableMapOf(),
            override val attributes: MutableSet<NetLinkAttribute> = mutableSetOf()
        ) : Band

        sealed interface Frequency {
            val frequencyHz: Long?
            val attributes: Set<NetLinkAttribute>

            data class Construction(
                override var frequencyHz: Long? = null,
                override val attributes: MutableSet<NetLinkAttribute> = mutableSetOf(),
            ) : Frequency
        }

        sealed interface Rate {
            val attributes: Set<NetLinkAttribute>

            data class Construction(
                override val attributes: MutableSet<NetLinkAttribute> = mutableSetOf(),
            ) : Rate
        }
    }
}