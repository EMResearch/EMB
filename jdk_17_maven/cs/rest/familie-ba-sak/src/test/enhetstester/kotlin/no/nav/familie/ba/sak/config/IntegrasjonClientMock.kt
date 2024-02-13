package no.nav.familie.ba.sak.config

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.isMockKMock
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.familie.ba.sak.config.ClientMocks.Companion.BARN_DET_IKKE_GIS_TILGANG_TIL_FNR
import no.nav.familie.ba.sak.config.ClientMocks.Companion.søkerFnr
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.LogiskVedleggResponse
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.OppdaterJournalpostResponse
import no.nav.familie.ba.sak.integrasjoner.lagTestJournalpost
import no.nav.familie.ba.sak.integrasjoner.lagTestOppgaveDTO
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.kodeverk.BeskrivelseDto
import no.nav.familie.kontrakter.felles.kodeverk.BetydningDto
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkDto
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkSpråk
import no.nav.familie.kontrakter.felles.navkontor.NavKontorEnhet
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource
import java.io.BufferedReader
import java.time.LocalDate
import java.time.Month
import java.util.UUID

@TestConfiguration
@Profile("dev", "postgres")
class IntegrasjonClientMock {

    @Bean
    @Primary
    fun mockIntegrasjonClient(): IntegrasjonClient {
        val mockIntegrasjonClient = mockk<IntegrasjonClient>(relaxed = false)

        clearIntegrasjonMocks(mockIntegrasjonClient)

        return mockIntegrasjonClient
    }

    @Bean
    @Primary
    fun mockFamilieIntegrasjonerTilgangskontrollClient(): FamilieIntegrasjonerTilgangskontrollClient {
        val mockFamilieIntegrasjonerTilgangskontrollClient =
            mockk<FamilieIntegrasjonerTilgangskontrollClient>(relaxed = false)

        clearMockFamilieIntegrasjonerTilgangskontrollClient(mockFamilieIntegrasjonerTilgangskontrollClient)

        return mockFamilieIntegrasjonerTilgangskontrollClient
    }

    companion object {
        fun clearIntegrasjonMocks(mockIntegrasjonClient: IntegrasjonClient) {
            /**
             * Mulig årsak til at appen må bruke dirties i testene.
             * Denne bønna blir initialisert av mockk, men etter noen av testene
             * er det ikke lenger en mockk bønne!
             */
            if (isMockKMock(mockIntegrasjonClient)) {
                clearMocks(mockIntegrasjonClient)
            } else {
                return
            }

            every { mockIntegrasjonClient.hentJournalpost(any()) } returns lagTestJournalpost(
                søkerFnr[0],
                UUID.randomUUID().toString(),
            )

            every { mockIntegrasjonClient.hentJournalposterForBruker(any()) } returns listOf(
                lagTestJournalpost(
                    søkerFnr[0],
                    UUID.randomUUID().toString(),
                ),
                lagTestJournalpost(
                    søkerFnr[0],
                    UUID.randomUUID().toString(),
                ),
            )

            every { mockIntegrasjonClient.finnOppgaveMedId(any()) } returns
                lagTestOppgaveDTO(1L)

            every { mockIntegrasjonClient.hentOppgaver(any()) } returns
                FinnOppgaveResponseDto(
                    2,
                    listOf(lagTestOppgaveDTO(1L), lagTestOppgaveDTO(2L, Oppgavetype.BehandleSak, "Z999999")),
                )

            every { mockIntegrasjonClient.opprettOppgave(any()) } returns
                OppgaveResponse(12345678L)

            every { mockIntegrasjonClient.patchOppgave(any()) } returns
                OppgaveResponse(12345678L)

            every { mockIntegrasjonClient.tilordneEnhetForOppgave(any(), any()) } returns
                OppgaveResponse(12345678L)

            every { mockIntegrasjonClient.fordelOppgave(any(), any()) } returns
                OppgaveResponse(12345678L)

            every { mockIntegrasjonClient.fjernBehandlesAvApplikasjon(any()) } returns
                OppgaveResponse(12345678L)

            every { mockIntegrasjonClient.oppdaterJournalpost(any(), any()) } returns
                OppdaterJournalpostResponse("1234567")

            every {
                mockIntegrasjonClient.journalførDokument(any())
            } returns ArkiverDokumentResponse(ferdigstilt = true, journalpostId = "journalpostId")

            every {
                mockIntegrasjonClient.leggTilLogiskVedlegg(any(), any())
            } returns LogiskVedleggResponse(12345678)

            every {
                mockIntegrasjonClient.slettLogiskVedlegg(any(), any())
            } returns LogiskVedleggResponse(12345678)

            every { mockIntegrasjonClient.distribuerBrev(any()) } returns "bestillingsId"

            every { mockIntegrasjonClient.ferdigstillJournalpost(any(), any()) } just runs

            every { mockIntegrasjonClient.ferdigstillOppgave(any()) } just runs

            every { mockIntegrasjonClient.hentBehandlendeEnhet(any()) } returns
                listOf(Arbeidsfordelingsenhet("4833", "NAV Familie- og pensjonsytelser Oslo 1"))

            every { mockIntegrasjonClient.hentDokument(any(), any()) } returns TEST_PDF

            every { mockIntegrasjonClient.hentArbeidsforhold(any(), any()) } returns emptyList()

            every { mockIntegrasjonClient.hentBehandlendeEnhet(any()) } returns listOf(
                Arbeidsfordelingsenhet(
                    "100",
                    "NAV Familie- og pensjonsytelser Oslo 1",
                ),
            )

            every { mockIntegrasjonClient.hentEnhet(any()) } returns NavKontorEnhet(
                101,
                "NAV Familie- og pensjonsytelser Oslo 1",
                "101",
                "",
            )

            every { mockIntegrasjonClient.opprettSkyggesak(any(), any()) } just runs

            every { mockIntegrasjonClient.hentLand(any()) } returns "Testland"
            every { mockIntegrasjonClient.hentLandkoderISO2() } returns hentLandkoderISO2()

            every { mockIntegrasjonClient.hentAlleEØSLand() } returns hentKodeverkLand()

            every { mockIntegrasjonClient.oppdaterOppgave(any(), any()) } just runs

            every { mockIntegrasjonClient.hentOrganisasjon(any()) } answers {
                Organisasjon(
                    "998765432",
                    "Testinstitusjon",
                )
            }
        }

        fun clearMockFamilieIntegrasjonerTilgangskontrollClient(mockFamilieIntegrasjonerTilgangskontrollClient: FamilieIntegrasjonerTilgangskontrollClient) {
            clearMocks(mockFamilieIntegrasjonerTilgangskontrollClient)

            every {
                mockFamilieIntegrasjonerTilgangskontrollClient.sjekkTilgangTilPersoner(any())
            } answers {
                val identer = firstArg<List<String>>()
                identer.map { Tilgang(personIdent = it, harTilgang = it != BARN_DET_IKKE_GIS_TILGANG_TIL_FNR) }
            }
        }

        fun FamilieIntegrasjonerTilgangskontrollClient.mockSjekkTilgang(
            map: Map<String, Boolean>,
            slot: MutableList<List<String>> = mutableListOf(),
        ) {
            every { sjekkTilgangTilPersoner(capture(slot)) } answers {
                val arg = firstArg<List<String>>()
                map.entries.filter { arg.contains(it.key) }.map { Tilgang(personIdent = it.key, harTilgang = it.value) }
            }
        }

        fun FamilieIntegrasjonerTilgangskontrollClient.mockSjekkTilgang(
            harTilgang: Boolean = false,
            slot: MutableList<List<String>> = mutableListOf(),
        ) {
            every { sjekkTilgangTilPersoner(capture(slot)) } answers {
                firstArg<List<String>>().map { Tilgang(personIdent = it, harTilgang = harTilgang) }
            }
        }

        fun initEuKodeverk(integrasjonClient: IntegrasjonClient) {
            every { integrasjonClient.hentAlleEØSLand() } returns hentKodeverkLand()
        }

        internal fun hentKodeverkLand(): KodeverkDto {
            val beskrivelsePolen = BeskrivelseDto("POL", "")
            val betydningPolen = BetydningDto(FOM_2004, TOM_9999, mapOf(KodeverkSpråk.BOKMÅL.kode to beskrivelsePolen))
            val beskrivelseTyskland = BeskrivelseDto("DEU", "")
            val betydningTyskland =
                BetydningDto(FOM_1900, TOM_9999, mapOf(KodeverkSpråk.BOKMÅL.kode to beskrivelseTyskland))
            val beskrivelseDanmark = BeskrivelseDto("DNK", "")
            val betydningDanmark =
                BetydningDto(FOM_1990, TOM_9999, mapOf(KodeverkSpråk.BOKMÅL.kode to beskrivelseDanmark))
            val beskrivelseUK = BeskrivelseDto("GBR", "")
            val betydningUK = BetydningDto(FOM_1900, TOM_2010, mapOf(KodeverkSpråk.BOKMÅL.kode to beskrivelseUK))

            return KodeverkDto(
                betydninger = mapOf(
                    "POL" to listOf(betydningPolen),
                    "DEU" to listOf(betydningTyskland),
                    "DNK" to listOf(betydningDanmark),
                    "GBR" to listOf(betydningUK),
                ),
            )
        }

        val FOM_1900 = LocalDate.of(1900, Month.JANUARY, 1)
        val FOM_1990 = LocalDate.of(1990, Month.JANUARY, 1)
        val FOM_2004 = LocalDate.of(2004, Month.JANUARY, 1)
        val TOM_2010 = LocalDate.of(2009, Month.DECEMBER, 31)
        val TOM_9999 = LocalDate.of(9999, Month.DECEMBER, 31)

        data class LandkodeISO2(
            val code: String,
            val name: String,
        )

        fun hentLandkoderISO2(): Map<String, String> {
            val landkoder =
                ClassPathResource("landkoder/landkoder.json").inputStream.bufferedReader().use(BufferedReader::readText)

            return objectMapper.readValue<List<LandkodeISO2>>(landkoder).associate { it.code to it.name }
        }
    }
}
