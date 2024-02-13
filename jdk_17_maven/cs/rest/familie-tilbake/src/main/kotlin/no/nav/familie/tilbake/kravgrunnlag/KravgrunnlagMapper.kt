package no.nav.familie.tilbake.kravgrunnlag

import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.tilbake.kravgrunnlag.domain.Fagområdekode
import no.nav.familie.tilbake.kravgrunnlag.domain.GjelderType
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassekode
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsbeløp433
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsperiode432
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagBelopDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagPeriodeDto
import java.util.UUID

object KravgrunnlagMapper {

    fun tilKravgrunnlag431(kravgrunnlag: DetaljertKravgrunnlagDto, behandlingId: UUID): Kravgrunnlag431 {
        return Kravgrunnlag431(
            behandlingId = behandlingId,
            vedtakId = kravgrunnlag.vedtakId,
            omgjortVedtakId = kravgrunnlag.vedtakIdOmgjort,
            kravstatuskode = Kravstatuskode.fraKode(kravgrunnlag.kodeStatusKrav),
            fagområdekode = Fagområdekode.fraKode(kravgrunnlag.kodeFagomraade),
            fagsystemId = kravgrunnlag.fagsystemId,
            fagsystemVedtaksdato = kravgrunnlag.datoVedtakFagsystem,
            gjelderVedtakId = kravgrunnlag.vedtakGjelderId,
            gjelderType = GjelderType.fraKode(kravgrunnlag.typeGjelderId.value()),
            utbetalesTilId = kravgrunnlag.utbetalesTilId,
            utbetIdType = GjelderType.fraKode(kravgrunnlag.typeUtbetId.value()),
            hjemmelkode = kravgrunnlag.kodeHjemmel,
            beregnesRenter = "J" == kravgrunnlag.renterBeregnes?.value(),
            ansvarligEnhet = kravgrunnlag.enhetAnsvarlig,
            behandlingsenhet = kravgrunnlag.enhetBehandl,
            bostedsenhet = kravgrunnlag.enhetBosted,
            kontrollfelt = kravgrunnlag.kontrollfelt,
            saksbehandlerId = kravgrunnlag.saksbehId,
            referanse = kravgrunnlag.referanse,
            eksternKravgrunnlagId = kravgrunnlag.kravgrunnlagId,
            perioder = tilKravgrunnlagsperiode(kravgrunnlag.tilbakekrevingsPeriode),
        )
    }

    private fun tilKravgrunnlagsperiode(perioder: List<DetaljertKravgrunnlagPeriodeDto>): Set<Kravgrunnlagsperiode432> {
        return perioder.map {
            Kravgrunnlagsperiode432(
                periode = Månedsperiode(it.periode.fom, it.periode.tom),
                månedligSkattebeløp = it.belopSkattMnd,
                beløp = tilKravgrunnlagsbeløp(it.tilbakekrevingsBelop),
            )
        }.toSet()
    }

    private fun tilKravgrunnlagsbeløp(beløpPosteringer: List<DetaljertKravgrunnlagBelopDto>): Set<Kravgrunnlagsbeløp433> {
        return beløpPosteringer.map {
            val klassetype = Klassetype.fraKode(it.typeKlasse.value())
            Kravgrunnlagsbeløp433(
                klassetype = klassetype,
                klassekode = Klassekode.fraKode(it.kodeKlasse, klassetype),
                opprinneligUtbetalingsbeløp = it.belopOpprUtbet,
                nyttBeløp = it.belopNy,
                tilbakekrevesBeløp = it.belopTilbakekreves,
                uinnkrevdBeløp = it.belopUinnkrevd,
                skatteprosent = it.skattProsent,
                resultatkode = it.kodeResultat,
                årsakskode = it.kodeAArsak,
                skyldkode = it.kodeSkyld,
            )
        }.toSet()
    }
}
