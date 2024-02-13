package no.nav.familie.ba.sak.kjerne.brev.domene.eøs

import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertKompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.BarnetsBostedsland

fun hentKompetanserForEØSBegrunnelse(
    eøsBegrunnelseMedTriggere: EØSBegrunnelseMedTriggere,
    minimerteKompetanser: List<MinimertKompetanse>,
) =
    minimerteKompetanser.filter {
        eøsBegrunnelseMedTriggere.erGyldigForKompetanseMedData(
            annenForeldersAktivitetFraKompetanse = it.annenForeldersAktivitet,
            barnetsBostedslandFraKompetanse = when (it.barnetsBostedslandNavn.navn) {
                "Norge" -> BarnetsBostedsland.NORGE
                else -> BarnetsBostedsland.IKKE_NORGE
            },
            resultatFraKompetanse = it.resultat,
        )
    }

fun EØSBegrunnelseMedTriggere.erGyldigForKompetanseMedData(
    annenForeldersAktivitetFraKompetanse: KompetanseAktivitet,
    barnetsBostedslandFraKompetanse: BarnetsBostedsland,
    resultatFraKompetanse: KompetanseResultat,
): Boolean = sanityEØSBegrunnelse.annenForeldersAktivitet
    .contains(annenForeldersAktivitetFraKompetanse) &&
    sanityEØSBegrunnelse.barnetsBostedsland
        .contains(barnetsBostedslandFraKompetanse) &&
    sanityEØSBegrunnelse.kompetanseResultat.contains(
        resultatFraKompetanse,
    )
