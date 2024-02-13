package no.nav.familie.ba.sak.kjerne.brev.domene.maler

data class HenleggeTrukketSøknadBrev(
    override val mal: Brevmal = Brevmal.HENLEGGE_TRUKKET_SØKNAD,
    override val data: HenleggeTrukketSøknadData,
) : Brev

data class HenleggeTrukketSøknadData(
    override val delmalData: DelmalData,
    override val flettefelter: FlettefelterForDokumentImpl,
) : BrevData {

    data class DelmalData(
        val signatur: SignaturDelmal,
    )
}
