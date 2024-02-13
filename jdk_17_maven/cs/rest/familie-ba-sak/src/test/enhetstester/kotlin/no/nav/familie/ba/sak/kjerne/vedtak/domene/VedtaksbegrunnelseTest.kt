package no.nav.familie.ba.sak.kjerne.vedtak.domene

import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.datagenerator.brev.lagBrevBegrunnelseGrunnlagMedPersoner
import no.nav.familie.ba.sak.datagenerator.vedtak.lagVedtaksbegrunnelse
import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertUregistrertBarn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.tilBrevTekst
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtaksbegrunnelseTest {
    val søker = lagPerson(type = PersonType.SØKER)
    val barn1 = lagPerson(type = PersonType.BARN)
    val barn2 = lagPerson(type = PersonType.BARN)
    val barn3 = lagPerson(type = PersonType.BARN)

    val restVedtaksbegrunnelse = lagVedtaksbegrunnelse(
        standardbegrunnelse = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET,
    )

    val vedtaksperiode = NullablePeriode(LocalDate.now().minusMonths(1), LocalDate.now())

    val personerIPersongrunnlag = listOf(søker, barn1, barn2, barn3).map { it.tilMinimertPerson() }

    val målform = Målform.NB

    val beløp = "1234"

    @Test
    fun `skal ta med alle barnas fødselsdatoer ved avslag på søker, men ikke inkludere dem i antall barn`() {
        val brevBegrunnelseGrunnlagMedPersoner = lagBrevBegrunnelseGrunnlagMedPersoner(
            standardbegrunnelse = Standardbegrunnelse.AVSLAG_BOR_HOS_SØKER,
            personIdenter = listOf(søker).map { it.aktør.aktivFødselsnummer() },
            vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG,
        )

        val brevbegrunnelse = brevBegrunnelseGrunnlagMedPersoner.tilBrevBegrunnelse(
            vedtaksperiode = vedtaksperiode,
            personerIPersongrunnlag = personerIPersongrunnlag,
            brevMålform = målform,
            uregistrerteBarn = emptyList(),
            minimerteUtbetalingsperiodeDetaljer = emptyList(),
            minimerteRestEndredeAndeler = emptyList(),
        ) as BegrunnelseData

        Assertions.assertEquals(true, brevbegrunnelse.gjelderSoker)
        Assertions.assertEquals(
            listOf(barn1, barn2, barn3).map { it.fødselsdato }.tilBrevTekst(),
            brevbegrunnelse.barnasFodselsdatoer,
        )
        Assertions.assertEquals(0, brevbegrunnelse.antallBarn)
        Assertions.assertEquals(målform.tilSanityFormat(), brevbegrunnelse.maalform)
        Assertions.assertEquals(Utils.formaterBeløp(0), brevbegrunnelse.belop)
    }

    @Test
    fun `skal ta med uregistrerte barn`() {
        val uregistrerteBarn = listOf(
            lagPerson(type = PersonType.BARN),
            lagPerson(type = PersonType.BARN),
        ).map {
            BarnMedOpplysninger(
                ident = it.aktør.aktivFødselsnummer(),
                fødselsdato = it.fødselsdato,
            ).tilMinimertUregistrertBarn()
        }

        val brevBegrunnelseGrunnlagMedPersoner = lagBrevBegrunnelseGrunnlagMedPersoner(
            standardbegrunnelse = Standardbegrunnelse.AVSLAG_UREGISTRERT_BARN,
            personIdenter = emptyList(),
            vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG,
        )

        val brevbegrunnelse = brevBegrunnelseGrunnlagMedPersoner.tilBrevBegrunnelse(
            vedtaksperiode = vedtaksperiode,
            personerIPersongrunnlag = personerIPersongrunnlag,
            brevMålform = målform,
            uregistrerteBarn = uregistrerteBarn,
            minimerteUtbetalingsperiodeDetaljer = emptyList(),
            minimerteRestEndredeAndeler = emptyList(),
        ) as BegrunnelseData

        Assertions.assertEquals(false, brevbegrunnelse.gjelderSoker)
        Assertions.assertEquals(
            uregistrerteBarn.map { it.fødselsdato!! }.tilBrevTekst(),
            brevbegrunnelse.barnasFodselsdatoer,
        )
        Assertions.assertEquals(2, brevbegrunnelse.antallBarn)
        Assertions.assertEquals(målform.tilSanityFormat(), brevbegrunnelse.maalform)
        Assertions.assertEquals(Utils.formaterBeløp(0), brevbegrunnelse.belop)
    }
}
