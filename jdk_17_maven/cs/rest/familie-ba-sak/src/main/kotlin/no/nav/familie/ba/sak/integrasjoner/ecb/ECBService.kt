package no.nav.familie.ba.sak.integrasjoner.ecb

import no.nav.familie.ba.sak.common.del
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.valutakurs.Frequency
import no.nav.familie.valutakurs.ValutakursRestClient
import no.nav.familie.valutakurs.domene.ExchangeRate
import no.nav.familie.valutakurs.domene.exchangeRateForCurrency
import no.nav.familie.valutakurs.exception.ValutakursClientException
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

@Service
@Import(ValutakursRestClient::class)
class ECBService(private val ecbClient: ValutakursRestClient) {

    /**
     * @param utenlandskValuta valutaen vi skal konvertere til NOK
     * @param kursDato datoen vi skal hente valutakurser for
     * @return Henter valutakurs for *utenlandskValuta* -> EUR og NOK -> EUR på *kursDato*, og returnerer en beregnet kurs for *utenlandskValuta* -> NOK.
     */
    @Throws(ECBServiceException::class)
    fun hentValutakurs(utenlandskValuta: String, kursDato: LocalDate): BigDecimal {
        try {
            val exchangeRates =
                ecbClient.hentValutakurs(Frequency.Daily, listOf(ECBConstants.NOK, utenlandskValuta), kursDato)
            validateExchangeRates(utenlandskValuta, kursDato, exchangeRates)
            val valutakursNOK = exchangeRates.exchangeRateForCurrency(ECBConstants.NOK)!!
            if (utenlandskValuta == ECBConstants.EUR) {
                return valutakursNOK.exchangeRate
            }
            val valutakursUtenlandskValuta = exchangeRates.exchangeRateForCurrency(utenlandskValuta)!!
            return beregnValutakurs(valutakursUtenlandskValuta.exchangeRate, valutakursNOK.exchangeRate)
        } catch (e: ValutakursClientException) {
            throw ECBServiceException(e.message, e)
        }
    }

    private fun beregnValutakurs(valutakursUtenlandskValuta: BigDecimal, valutakursNOK: BigDecimal) =
        valutakursNOK.del(valutakursUtenlandskValuta, 10)

    private fun validateExchangeRates(
        currency: String,
        exchangeRateDate: LocalDate,
        exchangeRates: List<ExchangeRate>,
    ) {
        val expectedSize = if (currency != ECBConstants.EUR) 2 else 1
        val currencies =
            if (currency != ECBConstants.EUR) listOf(currency, ECBConstants.NOK) else listOf(ECBConstants.NOK)

        if (!isValid(exchangeRates, currencies, exchangeRateDate, expectedSize)) {
            throwValidationException(currency, exchangeRateDate)
        }
    }

    private fun isValid(
        exchangeRates: List<ExchangeRate>,
        currencies: List<String>,
        exchangeRateDate: LocalDate,
        expectedSize: Int,
    ) =
        exchangeRates.size == expectedSize &&
            exchangeRates.all { it.date == exchangeRateDate } &&
            exchangeRates.map { it.currency }.containsAll(currencies)

    private fun throwValidationException(currency: String, exchangeRateDate: LocalDate) {
        throw ECBServiceException("Fant ikke nødvendige valutakurser for valutakursdato ${exchangeRateDate.tilKortString()} for å bestemme valutakursen $currency - NOK")
    }
}

object ECBConstants {
    const val NOK = "NOK"
    const val EUR = "EUR"
}
