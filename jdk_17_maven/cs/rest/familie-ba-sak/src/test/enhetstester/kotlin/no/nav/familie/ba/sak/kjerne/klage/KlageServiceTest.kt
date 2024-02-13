package no.nav.familie.ba.sak.kjerne.klage

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.klage.KanIkkeOppretteRevurderingÅrsak
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class KlageServiceTest {
    val fagsakService = mockk<FagsakService>()
    val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    val stegService = mockk<StegService>()
    val klageService = KlageService(
        fagsakService = fagsakService,
        klageClient = mockk(),
        integrasjonClient = mockk(),
        behandlingHentOgPersisterService = behandlingHentOgPersisterService,
        stegService = stegService,
        vedtakService = mockk(),
        tilbakekrevingKlient = mockk(),

    )

    @Nested
    inner class KanOppretteRevurdering {

        @Test
        internal fun `kan opprette revurdering hvis det finnes en ferdigstilt behandling`() {
            every { fagsakService.hentPåFagsakId(any()) } returns Fagsak(aktør = mockk())
            every { behandlingHentOgPersisterService.erÅpenBehandlingPåFagsak(any()) } returns false
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns lagBehandling(
                status = BehandlingStatus.AVSLUTTET,
            )

            val result = klageService.kanOppretteRevurdering(0L)

            Assertions.assertTrue(result.kanOpprettes)
            Assertions.assertEquals(result.årsak, null)
        }

        @Test
        internal fun `kan ikke opprette revurdering hvis det finnes åpen behandling`() {
            every { fagsakService.hentPåFagsakId(any()) } returns Fagsak(aktør = mockk())
            every { behandlingHentOgPersisterService.erÅpenBehandlingPåFagsak(any()) } returns true
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns lagBehandling(
                status = BehandlingStatus.UTREDES,
            )

            val result = klageService.kanOppretteRevurdering(0L)

            Assertions.assertFalse(result.kanOpprettes)
            Assertions.assertEquals(result.årsak, KanIkkeOppretteRevurderingÅrsak.ÅPEN_BEHANDLING)
        }

        @Test
        internal fun `kan ikke opprette revurdering hvis det ikke finnes noen behandlinger`() {
            every { fagsakService.hentPåFagsakId(any()) } returns Fagsak(aktør = mockk())
            every { behandlingHentOgPersisterService.erÅpenBehandlingPåFagsak(any()) } returns false
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns null

            val result = klageService.kanOppretteRevurdering(0L)

            Assertions.assertFalse(result.kanOpprettes)
            Assertions.assertEquals(result.årsak, KanIkkeOppretteRevurderingÅrsak.INGEN_BEHANDLING)
        }
    }

    @Nested
    inner class OpprettRevurderingKlage {

        @Test
        internal fun `kan opprette revurdering hvis det finnes en ferdigstilt behandling`() {
            val aktør = randomAktør()
            val fagsak = Fagsak(aktør = aktør)
            val forrigeBehandling = lagBehandling(
                behandlingKategori = BehandlingKategori.EØS,
                underkategori = BehandlingUnderkategori.UTVIDET,
                fagsak = fagsak,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                årsak = BehandlingÅrsak.OMREGNING_SMÅBARNSTILLEGG,
                status = BehandlingStatus.AVSLUTTET,
            )

            every { fagsakService.hentPåFagsakId(any()) } returns fagsak
            every { behandlingHentOgPersisterService.erÅpenBehandlingPåFagsak(any()) } returns false
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns forrigeBehandling

            val nyBehandling = NyBehandling(
                kategori = forrigeBehandling.kategori,
                underkategori = forrigeBehandling.underkategori,
                søkersIdent = forrigeBehandling.fagsak.aktør.aktivFødselsnummer(),
                behandlingType = BehandlingType.REVURDERING,
                behandlingÅrsak = BehandlingÅrsak.KLAGE,
                navIdent = SikkerhetContext.hentSaksbehandler(),
                barnasIdenter = emptyList(),
                fagsakId = forrigeBehandling.fagsak.id,
            )

            klageService.validerOgOpprettRevurderingKlage(fagsak.id)

            verify { stegService.håndterNyBehandling(nyBehandling) }
        }

        @Test
        internal fun `kan ikke opprette revurdering hvis det finnes åpen behandling`() {
            every { fagsakService.hentPåFagsakId(any()) } returns Fagsak(aktør = mockk())
            every { behandlingHentOgPersisterService.erÅpenBehandlingPåFagsak(any()) } returns true
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns lagBehandling(
                status = BehandlingStatus.UTREDES,
            )

            val result = klageService.validerOgOpprettRevurderingKlage(0L)

            Assertions.assertFalse(result.opprettetBehandling)
        }

        @Test
        internal fun `kan ikke opprette revurdering hvis det ikke finnes noen behandlinger`() {
            every { fagsakService.hentPåFagsakId(any()) } returns Fagsak(aktør = mockk())
            every { behandlingHentOgPersisterService.erÅpenBehandlingPåFagsak(any()) } returns false
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(any()) } returns null

            val result = klageService.validerOgOpprettRevurderingKlage(0L)

            Assertions.assertFalse(result.opprettetBehandling)
        }
    }
}
