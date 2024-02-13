package no.nav.familie.ba.sak.kjerne.vedtak.domene

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException

class VedtaksperiodeRepositoryTest(
    @Autowired private val aktørIdRepository: AktørIdRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val vedtakRepository: VedtakRepository,
    @Autowired private val vedtaksperiodeRepository: VedtaksperiodeRepository,
) : AbstractSpringIntegrationTest() {

    @Nested
    inner class FinnBehandlingIdForVedtaksperiode {
        @Test
        fun `skal kunne hente behandlingId til en vedtaksperiode`() {
            val søker = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(Fagsak(aktør = søker))
            val behandling = behandlingRepository.save(lagBehandling(fagsak))
            val vedtak = vedtakRepository.save(lagVedtak(behandling))
            val lagVedtaksperiodeMedBegrunnelser = lagVedtaksperiodeMedBegrunnelser(vedtak = vedtak)
            lagVedtaksperiodeMedBegrunnelser.begrunnelser.clear()
            val vedtaksperiode = vedtaksperiodeRepository.save(lagVedtaksperiodeMedBegrunnelser)

            assertThat(vedtaksperiodeRepository.finnBehandlingIdForVedtaksperiode(vedtaksperiode.id))
                .isEqualTo(behandling.id)
        }

        @Test
        fun `skal kaste feil hvis ikke vedtaksperiode finnes `() {
            assertThatThrownBy { vedtaksperiodeRepository.finnBehandlingIdForVedtaksperiode(1L) }
                .isInstanceOf(EmptyResultDataAccessException::class.java)
        }
    }
}
