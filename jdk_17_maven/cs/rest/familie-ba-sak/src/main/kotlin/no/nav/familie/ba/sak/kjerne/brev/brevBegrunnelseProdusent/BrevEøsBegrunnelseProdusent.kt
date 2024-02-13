import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.Utils.storForbokstav
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.kjerne.brev.brevBegrunnelseProdusent.GrunnlagForBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.brevBegrunnelseProdusent.hentSanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityPeriodeResultat
import no.nav.familie.ba.sak.kjerne.brev.tilSammenslåttKortString
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.EØSBegrunnelseData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.EØSBegrunnelseDataMedKompetanse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.EØSBegrunnelseDataUtenKompetanse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.IBegrunnelseGrunnlagForPeriode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.hentGyldigeBegrunnelserPerPerson
import java.time.LocalDate

fun EØSStandardbegrunnelse.lagBrevBegrunnelse(
    vedtaksperiode: VedtaksperiodeMedBegrunnelser,
    grunnlag: GrunnlagForBegrunnelse,
    begrunnelsesGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>,
    landkoder: Map<String, String>,
): List<EØSBegrunnelseData> {
    val sanityBegrunnelse = hentSanityBegrunnelse(grunnlag)
    val personerGjeldeneForBegrunnelse = vedtaksperiode.hentGyldigeBegrunnelserPerPerson(
        grunnlag,
    ).mapNotNull { (person, begrunnelserPåPerson) -> person.takeIf { this in begrunnelserPåPerson } }
    val periodegrunnlagForPersonerIBegrunnelse =
        begrunnelsesGrunnlagPerPerson.filter { (person, _) -> person in personerGjeldeneForBegrunnelse }

    val kompetanser = when (sanityBegrunnelse.periodeResultat) {
        SanityPeriodeResultat.INNVILGET_ELLER_ØKNING,
        SanityPeriodeResultat.INGEN_ENDRING,
        -> periodegrunnlagForPersonerIBegrunnelse.values.mapNotNull { it.dennePerioden.kompetanse }

        SanityPeriodeResultat.IKKE_INNVILGET,
        SanityPeriodeResultat.REDUKSJON,
        -> periodegrunnlagForPersonerIBegrunnelse.values.mapNotNull { it.forrigePeriode?.kompetanse }

        null -> error("Feltet 'periodeResultat' er ikke satt for begrunnelse fra sanity '${sanityBegrunnelse.apiNavn}'.")
    }

    return if (kompetanser.isEmpty() && sanityBegrunnelse.periodeResultat == SanityPeriodeResultat.IKKE_INNVILGET) {
        val personerIBegrunnelse = personerGjeldeneForBegrunnelse
        val barnPåBehandling = grunnlag.behandlingsGrunnlagForVedtaksperioder.persongrunnlag.barna
        val barnIBegrunnelse = personerGjeldeneForBegrunnelse.filter { it.type == PersonType.BARN }
        val gjelderSøker = personerIBegrunnelse.any { it.type == PersonType.SØKER }

        val barnasFødselsdatoer = hentBarnasFødselsdatoerForAvslagsbegrunnelse(
            barnIBegrunnelse = barnIBegrunnelse,
            barnPåBehandling = barnPåBehandling,
            uregistrerteBarn = grunnlag.behandlingsGrunnlagForVedtaksperioder.uregistrerteBarn,
            gjelderSøker = gjelderSøker,
        )

        listOf(
            EØSBegrunnelseDataUtenKompetanse(
                vedtakBegrunnelseType = this.vedtakBegrunnelseType,
                apiNavn = sanityBegrunnelse.apiNavn,
                barnasFodselsdatoer = barnasFødselsdatoer.tilSammenslåttKortString(),
                antallBarn = barnasFødselsdatoer.size,
                maalform = grunnlag.behandlingsGrunnlagForVedtaksperioder.persongrunnlag.søker.målform.tilSanityFormat(),
                gjelderSoker = gjelderSøker,
            ),
        )
    } else {
        kompetanser.map { kompetanse ->
            EØSBegrunnelseDataMedKompetanse(
                vedtakBegrunnelseType = this.vedtakBegrunnelseType,
                apiNavn = sanityBegrunnelse.apiNavn,
                annenForeldersAktivitet = kompetanse.annenForeldersAktivitet,
                annenForeldersAktivitetsland = kompetanse.annenForeldersAktivitetsland?.tilLandNavn(landkoder)?.navn,
                barnetsBostedsland = kompetanse.barnetsBostedsland.tilLandNavn(landkoder).navn,
                barnasFodselsdatoer = Utils.slåSammen(
                    kompetanse.barnAktører.map { aktør ->
                        grunnlag.hent(aktør).fødselsdato.tilKortString()
                    },
                ),
                antallBarn = kompetanse.barnAktører.size,
                maalform = grunnlag.behandlingsGrunnlagForVedtaksperioder.persongrunnlag.søker.målform.tilSanityFormat(),
                sokersAktivitet = kompetanse.søkersAktivitet,
                sokersAktivitetsland = kompetanse.søkersAktivitetsland.tilLandNavn(landkoder).navn,
            )
        }
    }
}

private fun GrunnlagForBegrunnelse.hent(
    aktør: Aktør,
) = behandlingsGrunnlagForVedtaksperioder.persongrunnlag.personer.single { it.aktør == aktør }

fun hentBarnasFødselsdatoerForAvslagsbegrunnelse(
    barnIBegrunnelse: List<Person>,
    barnPåBehandling: List<Person>,
    uregistrerteBarn: List<BarnMedOpplysninger>,
    gjelderSøker: Boolean,
): List<LocalDate> {
    val registrerteBarnFødselsdatoer =
        if (gjelderSøker) barnPåBehandling.map { it.fødselsdato } else barnIBegrunnelse.map { it.fødselsdato }
    val uregistrerteBarnFødselsdatoer =
        uregistrerteBarn.mapNotNull { it.fødselsdato }
    val alleBarnaFødselsdatoer = registrerteBarnFødselsdatoer + uregistrerteBarnFødselsdatoer
    return alleBarnaFødselsdatoer
}

data class Landkode(val kode: String, val navn: String) {
    init {
        if (this.kode.length != 2) {
            throw Feil("Forventer landkode på 'ISO 3166-1 alpha-2'-format")
        }
    }
}

fun String.tilLandNavn(landkoderISO2: Map<String, String>): Landkode {
    val kode = landkoderISO2.entries.find { it.key == this } ?: throw Feil("Fant ikke navn for landkode $this.")

    return Landkode(kode.key, kode.value.storForbokstav())
}
