package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.ForlengetSvartidsbrev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.VarselbrevÅrlegKontrollEøs
import no.nav.familie.kontrakter.felles.arbeidsfordeling.Enhet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ManueltBrevRequestTest {
    private val årsaker = listOf("1", "2", "3")
    private val baseRequest = ManueltBrevRequest(
        brevmal = Brevmal.INNHENTE_OPPLYSNINGER,
        multiselectVerdier = årsaker,
        mottakerIdent = "testident",
        mottakerNavn = "testnavn",
        enhet = Enhet("testenhetId", "testenhet"),
        antallUkerSvarfrist = 3,
    )

    @Test
    fun `Forlenget svartidsbrev request skal gi forlenget svartid brevmal med riktig data`() {
        val brev =
            baseRequest.copy(brevmal = Brevmal.FORLENGET_SVARTIDSBREV).tilBrev("saksbehandlerNavn") { emptyMap() }

        assertThat(brev::class).isEqualTo(ForlengetSvartidsbrev::class)
        brev as ForlengetSvartidsbrev

        assertThat(brev.mal).isEqualTo(Brevmal.FORLENGET_SVARTIDSBREV)

        assertThat(brev.data.flettefelter.antallUkerSvarfrist!!.single()).isEqualTo("3")
        assertThat(brev.data.flettefelter.aarsakerSvartidsbrev!!).isEqualTo(årsaker)
    }

    @Test
    fun `Forlenget svartidsbrev institusjon request skal gi forlenget svartid brevmal med riktig data`() {
        val brev = baseRequest.copy(
            brevmal = Brevmal.FORLENGET_SVARTIDSBREV_INSTITUSJON,
            mottakerIdent = "998765432",
            mottakerNavn = "Testorganisasjon",
            vedrørende = PersonITest(
                fødselsnummer = "testident",
                navn = "testnavn",
            ),
        )
            .tilBrev("saksbehandlerNavn") { emptyMap() }

        assertThat(brev::class).isEqualTo(ForlengetSvartidsbrev::class)
        brev as ForlengetSvartidsbrev

        assertThat(brev.mal).isEqualTo(Brevmal.FORLENGET_SVARTIDSBREV_INSTITUSJON)

        assertThat(brev.data.flettefelter.antallUkerSvarfrist!!.single()).isEqualTo("3")
        assertThat(brev.data.flettefelter.aarsakerSvartidsbrev!!).isEqualTo(årsaker)
        assertThat(brev.data.flettefelter.organisasjonsnummer).containsExactly("998765432")
        assertThat(brev.data.flettefelter.navn).containsExactly("Testorganisasjon")
        assertThat(brev.data.flettefelter.fodselsnummer).containsExactly("testident")
        assertThat(brev.data.flettefelter.gjelder).containsExactly("testnavn")
    }

    @Test
    fun `Innhente opplysninger brev til person og institusjon`() {
        val fnr = "12345678910"
        val orgnr = "123456789"
        val brevRequestTilPerson = baseRequest.copy(
            mottakerIdent = fnr,
        )
        val brevRequestTilInstitusjon = baseRequest.copy(
            brevmal = Brevmal.INNHENTE_OPPLYSNINGER_INSTITUSJON,
            mottakerIdent = orgnr,
            vedrørende = PersonITest(
                fødselsnummer = fnr,
                navn = "navn tilhørende $fnr",
            ),
        )
        brevRequestTilPerson.tilBrev("saksbehandlerNavn") { emptyMap() }.data.apply {
            assertThat(flettefelter.fodselsnummer).containsExactly(brevRequestTilPerson.mottakerIdent)
            assertThat(flettefelter.navn).containsExactly(brevRequestTilPerson.mottakerNavn)
            assertThat(flettefelter.organisasjonsnummer).isNull()
            assertThat(flettefelter.gjelder).isNull()
        }
        brevRequestTilInstitusjon.tilBrev("saksbehandlerNavn") { emptyMap() }.data.apply {
            assertThat(flettefelter.organisasjonsnummer).containsExactly(brevRequestTilInstitusjon.mottakerIdent)
            assertThat(flettefelter.fodselsnummer).containsExactly(brevRequestTilInstitusjon.vedrørende?.fødselsnummer)
            assertThat(flettefelter.navn).containsExactly(brevRequestTilPerson.mottakerNavn)
            assertThat(flettefelter.gjelder).containsExactly(brevRequestTilInstitusjon.vedrørende?.navn)
        }
    }

    @Test
    fun `Varsel årleg kontroll eøs request skal gi varsel årleg kontroll eøs brevmal med riktig data`() {
        val brev = baseRequest.copy(brevmal = Brevmal.VARSEL_OM_ÅRLIG_REVURDERING_EØS, mottakerlandSed = listOf("SE"))
            .tilBrev("saksbehandlerNavn") { mapOf(Pair("SE", "Sverige")) }

        assertThat(brev::class).isEqualTo(VarselbrevÅrlegKontrollEøs::class)
        brev as VarselbrevÅrlegKontrollEøs

        assertThat(brev.mal).isEqualTo(Brevmal.VARSEL_OM_ÅRLIG_REVURDERING_EØS)

        assertThat(brev.data.flettefelter.mottakerlandSed!!.single()).isEqualTo("Sverige")
        assertThat(brev.data.flettefelter.dokumentliste!!.isEmpty()).isTrue
    }

    @Test
    fun `Varsel årleg kontroll eøs med innhenting av opplysninger request skal gi varsel årleg kontroll eøs brevmal med riktig data`() {
        val dokumentliste = listOf("Dokument 1", "Dokument 2")
        val brev = baseRequest.copy(
            brevmal = Brevmal.VARSEL_OM_ÅRLIG_REVURDERING_EØS_MED_INNHENTING_AV_OPPLYSNINGER,
            mottakerlandSed = listOf("SE"),
            multiselectVerdier = dokumentliste,
        )
            .tilBrev("saksbehandlerNavn") { mapOf(Pair("SE", "Sverige")) }

        assertThat(brev::class).isEqualTo(VarselbrevÅrlegKontrollEøs::class)
        brev as VarselbrevÅrlegKontrollEøs

        assertThat(brev.mal).isEqualTo(Brevmal.VARSEL_OM_ÅRLIG_REVURDERING_EØS_MED_INNHENTING_AV_OPPLYSNINGER)

        assertThat(brev.data.flettefelter.mottakerlandSed!!.single()).isEqualTo("Sverige")
        assertThat(brev.data.flettefelter.dokumentliste!!.isEmpty()).isFalse
        assertThat(brev.data.flettefelter.dokumentliste).containsAll(dokumentliste)
    }

    @Test
    fun `Varsel årleg kontroll EØS request med flere mottakerland skal gi riktig brevdata`() {
        val brev =
            baseRequest.copy(brevmal = Brevmal.VARSEL_OM_ÅRLIG_REVURDERING_EØS, mottakerlandSed = listOf("SE", "DK"))
                .tilBrev("saksbehandlerNavn") { mapOf(Pair("SE", "Sverige"), Pair("DK", "Danmark")) }

        assertThat(brev::class).isEqualTo(VarselbrevÅrlegKontrollEøs::class)
        brev as VarselbrevÅrlegKontrollEøs

        assertThat(brev.mal).isEqualTo(Brevmal.VARSEL_OM_ÅRLIG_REVURDERING_EØS)

        assertThat(brev.data.flettefelter.mottakerlandSed!!.single()).isEqualTo("Sverige og Danmark")
    }

    @Test
    fun `Varsel årleg kontroll EØS request skal validere mottakerland`() {
        val brevRequest =
            baseRequest.copy(
                brevmal = Brevmal.VARSEL_OM_ÅRLIG_REVURDERING_EØS,
                mottakerlandSed = listOf("SE", "NO"),
            )

        assertThrows<FunksjonellFeil> {
            brevRequest.tilBrev("saksbehandlerNavn") { mapOf(Pair("SE", "Sverige"), Pair("NO", "Norge")) }
        }
    }

    class PersonITest(override val fødselsnummer: String, override val navn: String) : Person
}
