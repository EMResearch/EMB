package no.nav.familie.tilbake.dokumentbestilling.vedtak

import io.kotest.matchers.equality.shouldBeEqualToComparingFieldsExcept
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.vedtak.domain.Vedtaksbrevsperiode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class VedtaksbrevsperiodeRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var vedtaksbrevsperiodeRepository: VedtaksbrevsperiodeRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val vedtaksbrevsperiode = Testdata.vedtaksbrevsperiode

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
    }

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av Vedtaksbrevsperiode til basen`() {
        vedtaksbrevsperiodeRepository.insert(vedtaksbrevsperiode)

        val lagretVedtaksbrevsperiode = vedtaksbrevsperiodeRepository.findByIdOrThrow(vedtaksbrevsperiode.id)

        lagretVedtaksbrevsperiode.shouldBeEqualToComparingFieldsExcept(
            vedtaksbrevsperiode,
            Vedtaksbrevsperiode::sporbar,
            Vedtaksbrevsperiode::versjon,
        )
        lagretVedtaksbrevsperiode.versjon shouldBe 1
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av Vedtaksbrevsperiode i basen`() {
        vedtaksbrevsperiodeRepository.insert(vedtaksbrevsperiode)
        var lagretVedtaksbrevsperiode = vedtaksbrevsperiodeRepository.findByIdOrThrow(vedtaksbrevsperiode.id)
        val oppdatertVedtaksbrevsperiode = lagretVedtaksbrevsperiode.copy(fritekst = "bob")

        vedtaksbrevsperiodeRepository.update(oppdatertVedtaksbrevsperiode)

        lagretVedtaksbrevsperiode = vedtaksbrevsperiodeRepository.findByIdOrThrow(vedtaksbrevsperiode.id)
        lagretVedtaksbrevsperiode.shouldBeEqualToComparingFieldsExcept(
            oppdatertVedtaksbrevsperiode,
            Vedtaksbrevsperiode::sporbar,
            Vedtaksbrevsperiode::versjon,
        )
        lagretVedtaksbrevsperiode.versjon shouldBe 2
    }
}
