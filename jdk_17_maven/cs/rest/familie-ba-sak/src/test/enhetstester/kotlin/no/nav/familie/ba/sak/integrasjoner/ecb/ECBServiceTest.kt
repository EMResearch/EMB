package no.nav.familie.ba.sak.integrasjoner.ecb

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.unmockkAll
import no.nav.familie.valutakurs.Frequency
import no.nav.familie.valutakurs.ValutakursRestClient
import no.nav.familie.valutakurs.domene.ECBExchangeRate
import no.nav.familie.valutakurs.domene.ECBExchangeRateDate
import no.nav.familie.valutakurs.domene.ECBExchangeRateKey
import no.nav.familie.valutakurs.domene.ECBExchangeRateValue
import no.nav.familie.valutakurs.domene.ECBExchangeRatesData
import no.nav.familie.valutakurs.domene.ECBExchangeRatesDataSet
import no.nav.familie.valutakurs.domene.ECBExchangeRatesForCurrency
import no.nav.familie.valutakurs.domene.toExchangeRates
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ECBServiceTest {

    @MockK
    private lateinit var ecbClient: ValutakursRestClient

    @InjectMockKs
    private lateinit var ecbService: ECBService

    @AfterAll
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `Hent valutakurs for utenlandsk valuta til NOK og sjekk at beregning av kurs er riktig`() {
        val valutakursDato = LocalDate.of(2022, 6, 28)
        val ecbExchangeRatesData = createECBResponse(
            Frequency.Daily,
            listOf(Pair("NOK", BigDecimal.valueOf(10.337)), Pair("SEK", BigDecimal.valueOf(10.6543))),
            valutakursDato.toString(),
        )
        every {
            ecbClient.hentValutakurs(
                Frequency.Daily,
                listOf("NOK", "SEK"),
                valutakursDato,
            )
        } returns ecbExchangeRatesData.toExchangeRates()
        val SEKtilNOKValutakurs = ecbService.hentValutakurs("SEK", valutakursDato)
        assertEquals(BigDecimal.valueOf(0.9702185972), SEKtilNOKValutakurs)
    }

    @Test
    fun `Test at ECBService kaster ESBServiceException dersom de returnerte kursene ikke inneholder kurs for forespurt valuta`() {
        val valutakursDato = LocalDate.of(2022, 7, 22)
        val ecbExchangeRatesData = createECBResponse(
            Frequency.Daily,
            listOf(Pair("NOK", BigDecimal.valueOf(10.337))),
            valutakursDato.toString(),
        )
        every {
            ecbClient.hentValutakurs(
                Frequency.Daily,
                listOf("NOK", "SEK"),
                valutakursDato,
            )
        } returns ecbExchangeRatesData.toExchangeRates()
        assertThrows<ECBServiceException> { ecbService.hentValutakurs("SEK", valutakursDato) }
    }

    @Test
    fun `Test at ECBService kaster ESBServiceException dersom de returnerte kursene ikke inneholder kurser med forespurt dato`() {
        val valutakursDato = LocalDate.of(2022, 7, 20)
        val ecbExchangeRatesData = createECBResponse(
            Frequency.Daily,
            listOf(Pair("NOK", BigDecimal.valueOf(10.337)), Pair("SEK", BigDecimal.valueOf(10.6543))),
            valutakursDato.minusDays(1).toString(),
        )
        every {
            ecbClient.hentValutakurs(
                Frequency.Daily,
                listOf("NOK", "SEK"),
                valutakursDato,
            )
        } returns ecbExchangeRatesData.toExchangeRates()
        assertThrows<ECBServiceException> { ecbService.hentValutakurs("SEK", valutakursDato) }
    }

    @Test
    fun `Test at ECBService returnerer NOK til EUR dersom den forespurte valutaen er EUR`() {
        val nokTilEur = BigDecimal.valueOf(9.4567)
        val valutakursDato = LocalDate.of(2022, 7, 20)
        val ecbExchangeRatesData = createECBResponse(
            Frequency.Daily,
            listOf(Pair("NOK", BigDecimal.valueOf(9.4567))),
            valutakursDato.toString(),
        )
        every {
            ecbClient.hentValutakurs(
                Frequency.Daily,
                listOf("NOK", "EUR"),
                valutakursDato,
            )
        } returns ecbExchangeRatesData.toExchangeRates()
        assertEquals(nokTilEur, ecbService.hentValutakurs("EUR", valutakursDato))
    }

    private fun createECBResponse(
        frequency: Frequency,
        exchangeRates: List<Pair<String, BigDecimal>>,
        exchangeRateDate: String,
    ): ECBExchangeRatesData {
        return ECBExchangeRatesData(
            ECBExchangeRatesDataSet(
                exchangeRates.map {
                    ECBExchangeRatesForCurrency(
                        listOf(
                            ECBExchangeRateKey("CURRENCY", it.first),
                            ECBExchangeRateKey("FREQ", frequency.toFrequencyParam()),
                        ),
                        listOf(
                            ECBExchangeRate(
                                ECBExchangeRateDate(exchangeRateDate),
                                ECBExchangeRateValue((it.second)),
                            ),
                        ),
                    )
                },
            ),
        )
    }
}
