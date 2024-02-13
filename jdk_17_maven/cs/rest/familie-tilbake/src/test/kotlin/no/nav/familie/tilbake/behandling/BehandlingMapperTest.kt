package no.nav.familie.tilbake.behandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.familie.kontrakter.felles.Regelverk
import no.nav.familie.kontrakter.felles.klage.FagsystemType
import no.nav.familie.tilbake.behandling.BehandlingMapper.tilVedtakForFagsystem
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultat
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.config.Constants
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class BehandlingMapperTest {

    @Nested
    inner class TilVedtakForFagsystem {

        @Test
        internal fun `mapper avsluttet behandling`() {
            val behandling = behandling()
            val resultat = tilVedtakForFagsystem(listOf(behandling))
            resultat.shouldHaveSize(1)

            resultat[0].resultat shouldBe "Full tilbakebetaling"
            resultat[0].behandlingstype shouldBe "Tilbakekreving"
            resultat[0].eksternBehandlingId shouldBe behandling.eksternBrukId.toString()
            resultat[0].vedtakstidspunkt shouldBe LocalDate.of(2021, 7, 13).atStartOfDay()
            resultat[0].fagsystemType shouldBe FagsystemType.TILBAKEKREVING
            resultat[0].regelverk shouldBe Regelverk.NASJONAL
        }

        @Test
        internal fun `mapper ikke behandlinger er henlagt`() {
            val behandling = behandling(behandlingsresultatstype = Behandlingsresultatstype.HENLAGT_FEILOPPRETTET)
            tilVedtakForFagsystem(listOf(behandling)).shouldBeEmpty()
        }

        @Test
        internal fun `mapper ikke behandlinger har behandlingsresultat ikke_fastsatt`() {
            val behandling = behandling(behandlingsresultatstype = Behandlingsresultatstype.IKKE_FASTSATT)
            tilVedtakForFagsystem(listOf(behandling)).shouldBeEmpty()
        }

        @Test
        internal fun `mapper ikke behandlinger hvis behandlingsresultat mangler`() {
            val behandling = behandling(behandlingsresultatstype = null)
            tilVedtakForFagsystem(listOf(behandling)).shouldBeEmpty()
        }

        @Test
        internal fun `mapper ikke behandlinger som ikke er avsluttet`() {
            val behandling = behandling(status = Behandlingsstatus.FATTER_VEDTAK)
            tilVedtakForFagsystem(listOf(behandling)).shouldBeEmpty()
        }

        @Test
        internal fun `forventer at behandling inneholder avsluttet dato`() {
            val behandling = behandling(avsluttetDato = null)
            val exception = shouldThrow<IllegalStateException> {
                tilVedtakForFagsystem(listOf(behandling))
            }
            exception.message shouldContain "Mangler avsluttet dato på behandling="
        }
    }

    private fun behandling(
        status: Behandlingsstatus = Behandlingsstatus.AVSLUTTET,
        avsluttetDato: LocalDate? = LocalDate.of(2021, 7, 13),
        behandlingsresultatstype: Behandlingsresultatstype? = Behandlingsresultatstype.FULL_TILBAKEBETALING,
    ) = Behandling(
        fagsakId = UUID.randomUUID(),
        type = Behandlingstype.TILBAKEKREVING,
        ansvarligSaksbehandler = Constants.BRUKER_ID_VEDTAKSLØSNINGEN,
        behandlendeEnhet = "8020",
        behandlendeEnhetsNavn = "Oslo",
        manueltOpprettet = false,
        status = status,
        avsluttetDato = avsluttetDato,
        resultater = behandlingsresultatstype?.let { setOf(Behandlingsresultat(type = behandlingsresultatstype)) } ?: emptySet(),
        regelverk = Regelverk.NASJONAL,
    )
}
