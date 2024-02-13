package no.nav.familie.ba.sak.ekstern.bisys

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.statistikk.producer.DefaultKafkaProducer
import no.nav.familie.ba.sak.statistikk.producer.DefaultKafkaProducer.Companion.OPPHOER_BARNETRYGD_BISYS_TOPIC
import no.nav.familie.ba.sak.task.SendMeldingTilBisysTask
import no.nav.familie.eksterne.kontrakter.bisys.BarnetrygdBisysMelding
import no.nav.familie.eksterne.kontrakter.bisys.BarnetrygdEndretType
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.kafka.support.SendResult
import java.math.BigDecimal
import java.time.YearMonth
import java.util.concurrent.CompletableFuture

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SendMeldingTilBisysTaskTest {

    data class Mocks(
        val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
        val kafkaProducer: DefaultKafkaProducer,
        val tilkjentYtelseRepository: TilkjentYtelseRepository,
        val kafkaResult: CompletableFuture<SendResult<String, String>>,
        val behandling: List<Behandling>,
    )

    fun setupMocks(): Mocks {
        val tilkjentYtelseRepositoryMock = mockk<TilkjentYtelseRepository>()
        val kafkaProducer = DefaultKafkaProducer(mockk())
        val listenableFutureMock = mockk<CompletableFuture<SendResult<String, String>>>()
        val behandlingHentOgPersisterServiceMock = mockk<BehandlingHentOgPersisterService>()

        val forrigeBehandling = lagBehandling(defaultFagsak(), førsteSteg = StegType.BEHANDLING_AVSLUTTET)

        val nyBehandling = lagBehandling(
            forrigeBehandling.fagsak,
            resultat = Behandlingsresultat.OPPHØRT,
            førsteSteg = StegType.IVERKSETT_MOT_OPPDRAG,
        )

        every { behandlingHentOgPersisterServiceMock.hent(forrigeBehandling.id) } returns forrigeBehandling
        every { behandlingHentOgPersisterServiceMock.hent(nyBehandling.id) } returns nyBehandling

        every { behandlingHentOgPersisterServiceMock.hentForrigeBehandlingSomErVedtatt(nyBehandling) } returns forrigeBehandling

        every { listenableFutureMock.thenAccept(any()) } returns CompletableFuture()

        kafkaProducer.kafkaAivenTemplate = mockk()
        return Mocks(
            behandlingHentOgPersisterServiceMock,
            kafkaProducer,
            tilkjentYtelseRepositoryMock,
            listenableFutureMock,
            listOf(forrigeBehandling, nyBehandling),
        )
    }

    @Test
    fun `Skal send riktig melding til Bisys hvis barnetrygd er opphørt`() {
        val (behandlingRepository, kafkaProducer, tilkjentYtelseRepository, kafkaResult, behandling) = setupMocks()
        val sendMeldingTilBisysTask =
            SendMeldingTilBisysTask(kafkaProducer, tilkjentYtelseRepository, behandlingRepository)

        val barn1 = lagPerson(type = PersonType.BARN)

        every { tilkjentYtelseRepository.findByBehandling(behandling[0].id) } returns lagInitiellTilkjentYtelse().also {
            it.andelerTilkjentYtelse.add(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2020, 1),
                    tom = YearMonth.of(2037, 12),
                    prosent = BigDecimal(100),
                    person = barn1,
                ),
            )
        }
        every { tilkjentYtelseRepository.findByBehandling(behandling[1].id) } returns lagInitiellTilkjentYtelse().also {
            // Barn1 opphør fra 04/2022
            it.andelerTilkjentYtelse.add(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2020, 1),
                    tom = YearMonth.of(2022, 3),
                    prosent = BigDecimal(100),
                    person = barn1,
                ),
            )
        }

        val meldingSlot = slot<String>()
        every {
            kafkaProducer.kafkaAivenTemplate.send(
                OPPHOER_BARNETRYGD_BISYS_TOPIC,
                behandling[1].id.toString(),
                capture(meldingSlot),
            )
        } returns kafkaResult

        sendMeldingTilBisysTask.doTask(SendMeldingTilBisysTask.opprettTask(behandling[1].id))

        verify(exactly = 1) { kafkaProducer.kafkaAivenTemplate.send(any(), any(), any()) }
        val jsonMelding = objectMapper.readValue(meldingSlot.captured, BarnetrygdBisysMelding::class.java)
        assertThat(jsonMelding.søker).isEqualTo(behandling[1].fagsak.aktør.aktivFødselsnummer())
        assertThat(jsonMelding.barn).hasSize(1)
        assertThat(jsonMelding.barn[0].ident).isEqualTo(barn1.aktør.aktivFødselsnummer())
        assertThat(jsonMelding.barn[0].årsakskode.toString()).isEqualTo("RO")
        assertThat(jsonMelding.barn[0].fom).isEqualTo(YearMonth.of(2022, 4))
    }

    @Test
    fun `Skal send riktig melding til Bisys hvis barnetrygd er redusert`() {
        val (behandlingRepository, kafkaProducer, tilkjentYtelseRepository, kafkaResult, behandling) = setupMocks()
        val sendMeldingTilBisysTask =
            SendMeldingTilBisysTask(kafkaProducer, tilkjentYtelseRepository, behandlingRepository)

        val barn1 = lagPerson(type = PersonType.BARN)

        every { tilkjentYtelseRepository.findByBehandling(behandling[0].id) } returns lagInitiellTilkjentYtelse().also {
            it.andelerTilkjentYtelse.add(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2020, 1),
                    tom = YearMonth.of(2037, 12),
                    prosent = BigDecimal(100),
                    person = barn1,
                ),
            )
        }
        every { tilkjentYtelseRepository.findByBehandling(behandling[1].id) } returns lagInitiellTilkjentYtelse().also {
            // Barn1 reduser fra 04/2022
            it.andelerTilkjentYtelse.add(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2020, 1),
                    tom = YearMonth.of(2022, 3),
                    prosent = BigDecimal(100),
                    person = barn1,
                ),
            )
            it.andelerTilkjentYtelse.add(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2022, 4),
                    tom = YearMonth.of(2037, 12),
                    prosent = BigDecimal(50),
                    person = barn1,
                ),
            )
        }

        val meldingSlot = slot<String>()
        every {
            kafkaProducer.kafkaAivenTemplate.send(
                OPPHOER_BARNETRYGD_BISYS_TOPIC,
                behandling[1].id.toString(),
                capture(meldingSlot),
            )
        } returns kafkaResult

        sendMeldingTilBisysTask.doTask(SendMeldingTilBisysTask.opprettTask(behandling[1].id))

        verify(exactly = 1) { kafkaProducer.kafkaAivenTemplate.send(any(), any(), any()) }
        val jsonMelding = objectMapper.readValue(meldingSlot.captured, BarnetrygdBisysMelding::class.java)
        assertThat(jsonMelding.søker).isEqualTo(behandling[1].fagsak.aktør.aktivFødselsnummer())
        assertThat(jsonMelding.barn).hasSize(1)
        assertThat(jsonMelding.barn[0].ident).isEqualTo(barn1.aktør.aktivFødselsnummer())
        assertThat(jsonMelding.barn[0].årsakskode.toString()).isEqualTo("RR")
        assertThat(jsonMelding.barn[0].fom).isEqualTo(YearMonth.of(2022, 4))
    }

    @Test
    fun `finnBarnEndretOpplysning() skal return riktig endret opplysning for barn`() {
        val (behandlingRepository, kafkaProducer, tilkjentYtelseRepository, _, behandling) = setupMocks()

        val sendMeldingTilBisysTask =
            SendMeldingTilBisysTask(kafkaProducer, tilkjentYtelseRepository, behandlingRepository)

        val barn1 = lagPerson(type = PersonType.BARN)
        val barn2 = lagPerson(type = PersonType.BARN)
        val barn3 = lagPerson(type = PersonType.BARN)

        every { tilkjentYtelseRepository.findByBehandling(behandling[0].id) } returns lagInitiellTilkjentYtelse().also {
            it.andelerTilkjentYtelse.add(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2020, 1),
                    tom = YearMonth.of(2037, 12),
                    prosent = BigDecimal(100),
                    person = barn1,
                ),
            )
            it.andelerTilkjentYtelse.add(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2020, 1),
                    tom = YearMonth.of(2037, 12),
                    prosent = BigDecimal(100),
                    person = barn2,
                ),
            )
            it.andelerTilkjentYtelse.add(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2019, 1),
                    tom = YearMonth.of(2036, 12),
                    prosent = BigDecimal(100),
                    person = barn3,
                ),
            )
        }
        every { tilkjentYtelseRepository.findByBehandling(behandling[1].id) } returns lagInitiellTilkjentYtelse().also {
            // Barn1 opphør fra 04/2022
            it.andelerTilkjentYtelse.add(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2020, 1),
                    tom = YearMonth.of(2022, 3),
                    prosent = BigDecimal(100),
                    person = barn1,
                ),
            )

            // Barn2 redusert fra 02/2026
            it.andelerTilkjentYtelse.add(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2020, 1),
                    tom = YearMonth.of(2026, 1),
                    prosent = BigDecimal(100),
                    person = barn2,
                ),
            )
            it.andelerTilkjentYtelse.add(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2026, 2),
                    tom = YearMonth.of(2037, 12),
                    prosent = BigDecimal(50),
                    person = barn2,
                ),
            )

            // Barn3 redusert fra 04/2019 og opphørt fra 10/2019
            it.andelerTilkjentYtelse.add(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2019, 1),
                    tom = YearMonth.of(2019, 4),
                    prosent = BigDecimal(100),
                    person = barn3,
                ),
            )
            it.andelerTilkjentYtelse.add(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2019, 5),
                    tom = YearMonth.of(2019, 9),
                    prosent = BigDecimal(50),
                    person = barn3,
                ),
            )
        }

        val endretPerioder = sendMeldingTilBisysTask.finnBarnEndretOpplysning(behandling[1])
        val barn1Perioder = endretPerioder[barn1.aktør.aktivFødselsnummer()]
        val barn2Perioder = endretPerioder[barn2.aktør.aktivFødselsnummer()]
        val barn3Perioder = endretPerioder[barn3.aktør.aktivFødselsnummer()]

        assertThat(barn1Perioder).hasSize(1)
        assertThat(barn1Perioder!![0].årsakskode).isEqualTo(BarnetrygdEndretType.RO)
        assertThat(barn1Perioder[0].fom).isEqualTo(YearMonth.of(2022, 4))

        assertThat(barn2Perioder).hasSize(1)
        assertThat(barn2Perioder!![0].årsakskode).isEqualTo(BarnetrygdEndretType.RR)
        assertThat(barn2Perioder[0].fom).isEqualTo(YearMonth.of(2026, 2))

        assertThat(barn3Perioder).hasSize(2)

        val barn3PeriodeOpphør = barn3Perioder!!.first { it.årsakskode == BarnetrygdEndretType.RO }
        assertThat(barn3PeriodeOpphør.årsakskode).isEqualTo(BarnetrygdEndretType.RO)
        assertThat(barn3PeriodeOpphør.fom).isEqualTo(YearMonth.of(2019, 10))

        val barn3PeriodeReduser = barn3Perioder.first { it.årsakskode == BarnetrygdEndretType.RR }
        assertThat(barn3PeriodeReduser.årsakskode).isEqualTo(BarnetrygdEndretType.RR)
        assertThat(barn3PeriodeReduser.fom).isEqualTo(YearMonth.of(2019, 5))
    }

    @Test
    fun `Skal ikke sende melding til bisys hvis endring ikke er reduksjon eller opphøring`() {
        val (behandlingRepository, kafkaProducer, tilkjentYtelseRepository, _, behandling) = setupMocks()
        val sendMeldingTilBisysTask =
            SendMeldingTilBisysTask(kafkaProducer, tilkjentYtelseRepository, behandlingRepository)

        val barn1 = lagPerson(type = PersonType.BARN)

        every { tilkjentYtelseRepository.findByBehandling(behandling[0].id) } returns lagInitiellTilkjentYtelse().also {
            it.andelerTilkjentYtelse.add(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2020, 1),
                    tom = YearMonth.of(2021, 1),
                    prosent = BigDecimal(100),
                    person = barn1,
                ),
            )
        }
        every { tilkjentYtelseRepository.findByBehandling(behandling[1].id) } returns lagInitiellTilkjentYtelse().also {
            // Barn1 legger til period fra 04/2022
            it.andelerTilkjentYtelse.add(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2020, 1),
                    tom = YearMonth.of(2022, 3),
                    prosent = BigDecimal(100),
                    person = barn1,
                ),
            )
            it.andelerTilkjentYtelse.add(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2022, 4),
                    tom = YearMonth.of(2037, 12),
                    prosent = BigDecimal(100),
                    person = barn1,
                ),
            )
        }

        sendMeldingTilBisysTask.doTask(SendMeldingTilBisysTask.opprettTask(behandling[1].id))

        verify(exactly = 0) { kafkaProducer.kafkaAivenTemplate.send(any(), any(), any()) }
    }
}
