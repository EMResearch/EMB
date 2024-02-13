package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.DagTidspunkt.Companion.tilTidspunktEllerUendeligSent
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.DagTidspunkt.Companion.tilTidspunktEllerUendeligTidlig
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilInneværendeMåned
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import org.apache.kafka.common.Uuid

// Ønsker ikke å slå sammen like perioder, så legger ved en unikId for å unngå det.
class VedtaksperioderMedUnikIdTidslinje(
    private val vedtaksperioderMedBegrunnelser: List<VedtaksperiodeMedBegrunnelser>,
) : Tidslinje<VedtaksperiodeOgUnikId, Måned>() {

    override fun lagPerioder(): List<Periode<VedtaksperiodeOgUnikId, Måned>> =
        vedtaksperioderMedBegrunnelser.map {
            Periode(
                fraOgMed = it.fom.tilTidspunktEllerUendeligTidlig(it.tom).tilInneværendeMåned(),
                tilOgMed = it.tom.tilTidspunktEllerUendeligSent(it.fom).tilInneværendeMåned(),
                innhold = VedtaksperiodeOgUnikId(vedtaksperiode = it, uuid = Uuid.randomUuid()),
            )
        }
}

data class VedtaksperiodeOgUnikId(
    val vedtaksperiode: VedtaksperiodeMedBegrunnelser,
    val uuid: Uuid,
)
