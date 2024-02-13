package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentÅrsak
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BrevTypeTest {

    private val førerTilAvventerDokumentasjon = listOf(
        Brevmal.INNHENTE_OPPLYSNINGER,
        Brevmal.INNHENTE_OPPLYSNINGER_INSTITUSJON,
        Brevmal.VARSEL_OM_REVURDERING,
        Brevmal.VARSEL_OM_REVURDERING_INSTITUSJON,
        Brevmal.VARSEL_OM_REVURDERING_DELT_BOSTED_PARAGRAF_14,
        Brevmal.INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED,
        Brevmal.VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS,
        Brevmal.VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED,
        Brevmal.SVARTIDSBREV,
        Brevmal.FORLENGET_SVARTIDSBREV,
        Brevmal.SVARTIDSBREV_INSTITUSJON,
        Brevmal.FORLENGET_SVARTIDSBREV_INSTITUSJON,
    )

    private val eøsDokumentMedAvventerDokumentasjon = listOf(
        Brevmal.VARSEL_OM_ÅRLIG_REVURDERING_EØS,
        Brevmal.VARSEL_OM_ÅRLIG_REVURDERING_EØS_MED_INNHENTING_AV_OPPLYSNINGER,
    )

    private val førerIkkeTilAvventingAvDokumentasjon = Brevmal.values()
        .filter {
            it !in førerTilAvventerDokumentasjon && it !in eøsDokumentMedAvventerDokumentasjon
        }

    @Test
    fun `Skal si om behandling settes på vent`() {
        val setterIkkeBehandlingPåVent = Brevmal.values()
            .filter { !førerTilAvventerDokumentasjon.contains(it) && it !in eøsDokumentMedAvventerDokumentasjon }

        setterIkkeBehandlingPåVent.forEach {
            Assertions.assertFalse(it.setterBehandlingPåVent())
        }

        førerTilAvventerDokumentasjon.forEach {
            Assertions.assertTrue(it.setterBehandlingPåVent())
        }

        eøsDokumentMedAvventerDokumentasjon.forEach {
            Assertions.assertTrue(it.setterBehandlingPåVent())
        }

        førerIkkeTilAvventingAvDokumentasjon.forEach {
            Assertions.assertFalse(it.setterBehandlingPåVent())
        }
    }

    @Test
    fun `Skal gi riktig ventefrist nasjonal`() {
        førerTilAvventerDokumentasjon.forEach {
            Assertions.assertEquals(
                21L,
                it.ventefristDager(manuellFrist = 21L, behandlingKategori = BehandlingKategori.NASJONAL),
            )
        }

        førerIkkeTilAvventingAvDokumentasjon.forEach {
            assertThrows<Feil> { it.ventefristDager(behandlingKategori = BehandlingKategori.NASJONAL) }
        }
    }

    @Test
    fun `Skal gi riktig ventefrist eøs`() {
        Assertions.assertEquals(90L, Brevmal.SVARTIDSBREV.ventefristDager(behandlingKategori = BehandlingKategori.EØS))
        Assertions.assertEquals(
            60L,
            Brevmal.VARSEL_OM_ÅRLIG_REVURDERING_EØS.ventefristDager(behandlingKategori = BehandlingKategori.EØS),
        )
        Assertions.assertEquals(
            60L,
            Brevmal.VARSEL_OM_ÅRLIG_REVURDERING_EØS_MED_INNHENTING_AV_OPPLYSNINGER.ventefristDager(behandlingKategori = BehandlingKategori.EØS),
        )
    }

    @Test
    fun `Skal gi riktig venteårsak`() {
        førerTilAvventerDokumentasjon.forEach {
            Assertions.assertEquals(SettPåVentÅrsak.AVVENTER_DOKUMENTASJON, it.venteårsak())
        }

        førerIkkeTilAvventingAvDokumentasjon.forEach {
            Assertions.assertFalse(it.setterBehandlingPåVent())
        }
    }
}
