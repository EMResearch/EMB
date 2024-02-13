package no.nav.familie.tilbake.dokumentbestilling.vedtak

import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.tilbake.api.dto.PeriodeMedTekstDto
import no.nav.familie.tilbake.dokumentbestilling.vedtak.domain.Friteksttype
import no.nav.familie.tilbake.dokumentbestilling.vedtak.domain.Vedtaksbrevsoppsummering
import no.nav.familie.tilbake.dokumentbestilling.vedtak.domain.Vedtaksbrevsperiode
import java.util.UUID

object VedtaksbrevFritekstMapper {

    fun tilDomene(behandlingId: UUID, oppsummeringstekst: String?): Vedtaksbrevsoppsummering {
        return Vedtaksbrevsoppsummering(
            behandlingId = behandlingId,
            oppsummeringFritekst = oppsummeringstekst,
        )
    }

    fun tilDomeneVedtaksbrevsperiode(
        behandlingId: UUID,
        perioderMedFritekst: List<PeriodeMedTekstDto>,
    ): List<Vedtaksbrevsperiode> {
        val vedtaksbrevsperioder = mutableListOf<Vedtaksbrevsperiode>()
        perioderMedFritekst.forEach {
            lagFaktaAvsnitt(behandlingId, it)?.let { faktaAvsnitt -> vedtaksbrevsperioder.add(faktaAvsnitt) }
            lagForeldelseAvsnitt(behandlingId, it)?.let { foreldelseAvsnitt -> vedtaksbrevsperioder.add(foreldelseAvsnitt) }
            lagVilkårsvurderingAvsnitt(behandlingId, it)?.let { vilkårAvsnitt -> vedtaksbrevsperioder.add(vilkårAvsnitt) }
            lagSærligGrunnerAvsnitt(
                behandlingId,
                it,
            )?.let { særligGrunnerAvsnitt -> vedtaksbrevsperioder.add(særligGrunnerAvsnitt) }
            lagSærligGrunnerAnnetAvsnitt(behandlingId, it)?.let { annetAvsnitt -> vedtaksbrevsperioder.add(annetAvsnitt) }
        }
        return vedtaksbrevsperioder
    }

    fun mapFritekstFraDb(fritekstPerioder: Set<Vedtaksbrevsperiode>): List<PeriodeMedTekstDto> {
        val perioderTilMap = HashMap<Månedsperiode, MutableMap<Friteksttype, String>>()

        fritekstPerioder.forEach {
            val avsnittTilTekst = perioderTilMap.getOrDefault(it.periode, mutableMapOf())
            avsnittTilTekst[it.fritekststype] = it.fritekst
            perioderTilMap[it.periode] = avsnittTilTekst
        }

        return perioderTilMap.entries.map { (periode, avsnittTilTekst) ->
            PeriodeMedTekstDto(
                periode = periode.toDatoperiode(),
                faktaAvsnitt = avsnittTilTekst[Friteksttype.FAKTA],
                foreldelseAvsnitt = avsnittTilTekst[Friteksttype.FORELDELSE],
                vilkårAvsnitt = avsnittTilTekst[Friteksttype.VILKÅR],
                særligeGrunnerAvsnitt = avsnittTilTekst[Friteksttype.SÆRLIGE_GRUNNER],
                særligeGrunnerAnnetAvsnitt = avsnittTilTekst[Friteksttype.SÆRLIGE_GRUNNER_ANNET],
            )
        }
    }

    private fun lagFaktaAvsnitt(behandlingId: UUID, faktaAvsnittMedPeriode: PeriodeMedTekstDto): Vedtaksbrevsperiode? {
        return faktaAvsnittMedPeriode.faktaAvsnitt?.let {
            Vedtaksbrevsperiode(
                behandlingId = behandlingId,
                periode = faktaAvsnittMedPeriode.periode.toMånedsperiode(),
                fritekst = faktaAvsnittMedPeriode.faktaAvsnitt,
                fritekststype = Friteksttype.FAKTA,
            )
        }
    }

    private fun lagForeldelseAvsnitt(behandlingId: UUID, foreldelsesAvsnittMedPeriode: PeriodeMedTekstDto): Vedtaksbrevsperiode? {
        return foreldelsesAvsnittMedPeriode.foreldelseAvsnitt?.let {
            Vedtaksbrevsperiode(
                behandlingId = behandlingId,
                periode = foreldelsesAvsnittMedPeriode.periode.toMånedsperiode(),
                fritekst = foreldelsesAvsnittMedPeriode.foreldelseAvsnitt,
                fritekststype = Friteksttype.FORELDELSE,
            )
        }
    }

    private fun lagVilkårsvurderingAvsnitt(
        behandlingId: UUID,
        vilkårAvsnittMedPeriode: PeriodeMedTekstDto,
    ): Vedtaksbrevsperiode? {
        return vilkårAvsnittMedPeriode.vilkårAvsnitt?.let {
            Vedtaksbrevsperiode(
                behandlingId = behandlingId,
                periode = vilkårAvsnittMedPeriode.periode.toMånedsperiode(),
                fritekst = vilkårAvsnittMedPeriode.vilkårAvsnitt,
                fritekststype = Friteksttype.VILKÅR,
            )
        }
    }

    private fun lagSærligGrunnerAvsnitt(
        behandlingId: UUID,
        særligGrunnerAvsnittMedPeriode: PeriodeMedTekstDto,
    ): Vedtaksbrevsperiode? {
        return særligGrunnerAvsnittMedPeriode.særligeGrunnerAvsnitt?.let {
            Vedtaksbrevsperiode(
                behandlingId = behandlingId,
                periode = særligGrunnerAvsnittMedPeriode.periode.toMånedsperiode(),
                fritekst = særligGrunnerAvsnittMedPeriode.særligeGrunnerAvsnitt,
                fritekststype = Friteksttype.SÆRLIGE_GRUNNER,
            )
        }
    }

    private fun lagSærligGrunnerAnnetAvsnitt(
        behandlingId: UUID,
        særligGrunnerAnnetAvsnittMedPeriode: PeriodeMedTekstDto,
    ): Vedtaksbrevsperiode? {
        return særligGrunnerAnnetAvsnittMedPeriode.særligeGrunnerAnnetAvsnitt?.let {
            Vedtaksbrevsperiode(
                behandlingId = behandlingId,
                periode = særligGrunnerAnnetAvsnittMedPeriode.periode.toMånedsperiode(),
                fritekst = særligGrunnerAnnetAvsnittMedPeriode.særligeGrunnerAnnetAvsnitt,
                fritekststype = Friteksttype.SÆRLIGE_GRUNNER_ANNET,
            )
        }
    }
}
