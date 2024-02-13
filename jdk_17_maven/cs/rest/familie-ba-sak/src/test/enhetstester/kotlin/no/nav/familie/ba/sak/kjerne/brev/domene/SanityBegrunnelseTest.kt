package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.common.lagRestSanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SanityBegrunnelseTest {
    @Test
    fun `skal fjerne ugyldige enumverdier`() {
        val restSanityBegrunnelse = lagRestSanityBegrunnelse(
            apiNavn = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET.sanityApiNavn,
            ovrigeTriggere = listOf(
                ØvrigTrigger.BARN_MED_6_ÅRS_DAG.name,
                "IKKE_GYLDIG_ØVRIG_TRIGGER",
            ),
        )
        Assertions.assertEquals(
            listOf(
                ØvrigTrigger.BARN_MED_6_ÅRS_DAG,
            ),
            restSanityBegrunnelse.tilSanityBegrunnelse()!!.ovrigeTriggere?.toList(),
        )
    }

    @Test
    fun `skal konverdere string til enumverdi dersom det finnes og null ellers`() {
        Assertions.assertEquals(null, "IKKE_GYLDIG_VERDI".finnEnumverdi<ØvrigTrigger>(""))
        Assertions.assertEquals(
            ØvrigTrigger.BARN_MED_6_ÅRS_DAG,
            "BARN_MED_6_ÅRS_DAG".finnEnumverdi<ØvrigTrigger>(""),
        )
    }
}
