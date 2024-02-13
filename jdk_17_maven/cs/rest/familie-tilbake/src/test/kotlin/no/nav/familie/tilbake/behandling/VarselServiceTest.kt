package no.nav.familie.tilbake.behandling

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class VarselServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var varselService: VarselService

    private val behandling = Testdata.behandling
    private val kravgrunnlag = Testdata.kravgrunnlag431

    @BeforeEach
    fun setup() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(behandling)
    }

    @Test
    fun `lagre skal lagre varselbrev med perioder fra feilutbetaling når behandling har mottatt kravgrunnlag`() {
        kravgrunnlagRepository.insert(kravgrunnlag)
        varselService.lagre(behandling.id, "Hello", 1000)

        val oppdatertBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        val varsler = oppdatertBehandling.varsler

        varsler.size shouldBe 2
        varsler.any { !it.aktiv }.shouldBeTrue()
        val aktivVarsel = oppdatertBehandling.aktivtVarsel
        aktivVarsel.shouldNotBeNull()
        aktivVarsel.varselbeløp shouldBe 1000
        aktivVarsel.varseltekst shouldBe "Hello"

        val varselsperioder = aktivVarsel.perioder
        varselsperioder.shouldNotBeEmpty()
        varselsperioder.any {
            it.fom == kravgrunnlag.perioder.first().periode.fomDato &&
                it.tom == kravgrunnlag.perioder.first().periode.tomDato
        }.shouldBeTrue()
    }

    @Test
    fun `lagre skal lagre varselbrev med perioder ved opprettelse når behandling ikke har mottatt kravgrunnlag`() {
        varselService.lagre(behandling.id, "Hello", 1000)

        val oppdatertBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        val varsler = oppdatertBehandling.varsler

        varsler.size shouldBe 2
        varsler.any { !it.aktiv }.shouldBeTrue()
        val aktivVarsel = oppdatertBehandling.aktivtVarsel
        aktivVarsel.shouldNotBeNull()
        aktivVarsel.varselbeløp shouldBe 1000
        aktivVarsel.varseltekst shouldBe "Hello"

        val varselsperioder = aktivVarsel.perioder
        varselsperioder.shouldNotBeEmpty()
        varselsperioder.any {
            it.fom == Testdata.varsel.perioder.first().fom &&
                it.tom == Testdata.varsel.perioder.first().tom
        }.shouldBeTrue()
    }
}
