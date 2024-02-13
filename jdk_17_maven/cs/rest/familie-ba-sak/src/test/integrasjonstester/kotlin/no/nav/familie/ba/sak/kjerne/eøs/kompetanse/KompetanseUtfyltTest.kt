package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import no.nav.familie.ba.sak.ekstern.restDomene.UtfyltStatus
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestKompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.lagKompetanse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.BarnetsBostedsland
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KompetanseUtfyltTest {

    @Test
    fun `Skal sette UtfyltStatus til OK når alle felter i skjema er fylt ut`() {
        val kompetanse = lagKompetanse(
            annenForeldersAktivitet = KompetanseAktivitet.I_ARBEID,
            barnetsBostedsland = BarnetsBostedsland.NORGE.name,
            annenForeldersAktivitetsland = "NORGE",
            kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
            søkersAktivitet = KompetanseAktivitet.ARBEIDER,
            søkersAktivitetsland = "NO",
        )

        val restKompetanse = kompetanse.tilRestKompetanse()

        assertEquals(UtfyltStatus.OK, restKompetanse.status)
    }

    @Test
    fun `Skal sette UtfyltStatus til OK dersom alle felter unntatt annenForeldersAktivitetsland er fylt ut og annenForeldersAktivitet er IKKE_AKTUELT eller INAKTIV`() {
        var kompetanse = lagKompetanse(
            annenForeldersAktivitet = KompetanseAktivitet.IKKE_AKTUELT,
            barnetsBostedsland = BarnetsBostedsland.NORGE.name,
            kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
            søkersAktivitet = KompetanseAktivitet.ARBEIDER,
            søkersAktivitetsland = "NO",
        )

        var restKompetanse = kompetanse.tilRestKompetanse()

        assertEquals(UtfyltStatus.OK, restKompetanse.status)

        kompetanse = lagKompetanse(
            annenForeldersAktivitet = KompetanseAktivitet.INAKTIV,
            barnetsBostedsland = BarnetsBostedsland.NORGE.name,
            kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
            søkersAktivitet = KompetanseAktivitet.ARBEIDER,
            søkersAktivitetsland = "NO",
        )

        restKompetanse = kompetanse.tilRestKompetanse()

        assertEquals(UtfyltStatus.OK, restKompetanse.status)
    }

    @Test
    fun `Skal sette UtfyltStatus til UFULLSTENDIG dersom alle felter unntatt annenForeldersAktivitetsland er fylt ut og annenForeldersAktivitet ikke er IKKE_AKTUELT eller INAKTIV`() {
        var kompetanse = lagKompetanse(
            annenForeldersAktivitet = KompetanseAktivitet.I_ARBEID,
            barnetsBostedsland = BarnetsBostedsland.NORGE.name,
            kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
            søkersAktivitet = KompetanseAktivitet.ARBEIDER,
        )

        var restKompetanse = kompetanse.tilRestKompetanse()

        assertEquals(UtfyltStatus.UFULLSTENDIG, restKompetanse.status)

        kompetanse = lagKompetanse(
            annenForeldersAktivitet = KompetanseAktivitet.MOTTAR_PENSJON,
            barnetsBostedsland = BarnetsBostedsland.NORGE.name,
            kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
            søkersAktivitet = KompetanseAktivitet.ARBEIDER,
        )

        restKompetanse = kompetanse.tilRestKompetanse()

        assertEquals(UtfyltStatus.UFULLSTENDIG, restKompetanse.status)

        kompetanse = lagKompetanse(
            annenForeldersAktivitet = KompetanseAktivitet.FORSIKRET_I_BOSTEDSLAND,
            barnetsBostedsland = BarnetsBostedsland.NORGE.name,
            kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
            søkersAktivitet = KompetanseAktivitet.ARBEIDER,
        )

        restKompetanse = kompetanse.tilRestKompetanse()

        assertEquals(UtfyltStatus.UFULLSTENDIG, restKompetanse.status)

        kompetanse = lagKompetanse(
            annenForeldersAktivitet = KompetanseAktivitet.MOTTAR_UTBETALING_SOM_ERSTATTER_LØNN,
            barnetsBostedsland = BarnetsBostedsland.NORGE.name,
            kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
            søkersAktivitet = KompetanseAktivitet.ARBEIDER,
        )

        restKompetanse = kompetanse.tilRestKompetanse()

        assertEquals(UtfyltStatus.UFULLSTENDIG, restKompetanse.status)
    }

    @Test
    fun `Skal sette UtfyltStatus til UFULLSTENDIG dersom 1 til 4 felter er satt med unntak av regel om annenForeldersAktivitet`() {
        var kompetanse = lagKompetanse(
            annenForeldersAktivitet = KompetanseAktivitet.IKKE_AKTUELT,
        )

        var restKompetanse = kompetanse.tilRestKompetanse()

        assertEquals(UtfyltStatus.UFULLSTENDIG, restKompetanse.status)

        kompetanse = lagKompetanse(
            annenForeldersAktivitet = KompetanseAktivitet.INAKTIV,
            barnetsBostedsland = BarnetsBostedsland.NORGE.name,
        )

        restKompetanse = kompetanse.tilRestKompetanse()

        assertEquals(UtfyltStatus.UFULLSTENDIG, restKompetanse.status)

        kompetanse = lagKompetanse(
            annenForeldersAktivitet = KompetanseAktivitet.FORSIKRET_I_BOSTEDSLAND,
            barnetsBostedsland = BarnetsBostedsland.NORGE.name,
            kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
        )

        restKompetanse = kompetanse.tilRestKompetanse()

        assertEquals(UtfyltStatus.UFULLSTENDIG, restKompetanse.status)

        kompetanse = lagKompetanse(
            annenForeldersAktivitet = KompetanseAktivitet.MOTTAR_UTBETALING_SOM_ERSTATTER_LØNN,
            barnetsBostedsland = BarnetsBostedsland.NORGE.name,
            kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
            søkersAktivitet = KompetanseAktivitet.ARBEIDER,
        )

        restKompetanse = kompetanse.tilRestKompetanse()

        assertEquals(UtfyltStatus.UFULLSTENDIG, restKompetanse.status)
    }

    @Test
    fun `Skal sette UtfyltStatus til IKKE_UTFYLT dersom ingen av feltene er utfylt`() {
        val kompetanse = lagKompetanse()

        val restKompetanse = kompetanse.tilRestKompetanse()

        assertEquals(UtfyltStatus.IKKE_UTFYLT, restKompetanse.status)
    }

    @Test
    fun `Skal sette UtfyltStatus til UFULLSTENDIG dersom alle felter unntatt søkersAktivitetsland er fylt ut`() {
        val kompetanse = lagKompetanse(
            annenForeldersAktivitet = KompetanseAktivitet.I_ARBEID,
            barnetsBostedsland = BarnetsBostedsland.NORGE.name,
            annenForeldersAktivitetsland = "NORGE",
            kompetanseResultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
            søkersAktivitet = KompetanseAktivitet.ARBEIDER,
        )

        val restKompetanse = kompetanse.tilRestKompetanse()

        assertEquals(UtfyltStatus.UFULLSTENDIG, restKompetanse.status)
    }
}
