package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.ekstern.restDomene.RestVedtakBegrunnelseTilknyttetVilkår
import no.nav.familie.ba.sak.integrasjoner.sanity.SanityService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth

@Service
class VilkårsvurderingService(
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val sanityService: SanityService,
) {

    fun hentAktivForBehandling(behandlingId: Long): Vilkårsvurdering? {
        return vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId)
    }

    fun hentAktivForBehandlingThrows(behandlingId: Long): Vilkårsvurdering = hentAktivForBehandling(behandlingId)
        ?: throw Feil("Fant ikke vilkårsvurdering knyttet til behandling=$behandlingId")

    fun oppdater(vilkårsvurdering: Vilkårsvurdering): Vilkårsvurdering {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppdaterer vilkårsvurdering $vilkårsvurdering")
        return vilkårsvurderingRepository.saveAndFlush(vilkårsvurdering)
    }

    fun lagreNyOgDeaktiverGammel(vilkårsvurdering: Vilkårsvurdering): Vilkårsvurdering {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter vilkårsvurdering $vilkårsvurdering")

        val aktivVilkårsvurdering = hentAktivForBehandling(vilkårsvurdering.behandling.id)

        if (aktivVilkårsvurdering != null) {
            vilkårsvurderingRepository.saveAndFlush(aktivVilkårsvurdering.also { it.aktiv = false })
        }

        return vilkårsvurderingRepository.save(vilkårsvurdering)
    }

    fun lagreInitielt(vilkårsvurdering: Vilkårsvurdering): Vilkårsvurdering {
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter vilkårsvurdering $vilkårsvurdering")

        val aktivVilkårsvurdering = hentAktivForBehandling(vilkårsvurdering.behandling.id)
        if (aktivVilkårsvurdering != null) {
            error("Det finnes allerede et aktivt vilkårsvurdering for behandling ${vilkårsvurdering.behandling.id}")
        }

        return vilkårsvurderingRepository.save(vilkårsvurdering)
    }

    fun hentVilkårsbegrunnelser(): Map<VedtakBegrunnelseType, List<RestVedtakBegrunnelseTilknyttetVilkår>> =
        standardbegrunnelserTilNedtrekksmenytekster(sanityService.hentSanityBegrunnelser()) +
            eøsStandardbegrunnelserTilNedtrekksmenytekster(sanityService.hentSanityEØSBegrunnelser())

    fun hentTidligsteVilkårsvurderingKnyttetTilMigrering(behandlingId: Long): YearMonth? {
        val vilkårsvurdering = hentAktivForBehandling(
            behandlingId = behandlingId,
        )

        return vilkårsvurdering?.personResultater
            ?.flatMap { it.vilkårResultater }
            ?.filter { it.periodeFom != null }
            ?.filter { it.vilkårType != Vilkår.UNDER_18_ÅR && it.vilkårType != Vilkår.GIFT_PARTNERSKAP }
            ?.minByOrNull { it.periodeFom!! }
            ?.periodeFom
            ?.toYearMonth()
    }

    @Transactional
    fun oppdaterVilkårVedDødsfall(behandlingId: BehandlingId, dødsfallsDato: LocalDate, aktør: Aktør) {
        val vilkårsvurdering = hentAktivForBehandlingThrows(behandlingId.id)

        val personResultat = vilkårsvurdering.personResultater.find { it.aktør == aktør }
            ?: throw Feil(message = "Fant ikke vilkårsvurdering for person under manuell registrering av dødsfall dato")

        personResultat.vilkårResultater.filter { it.periodeTom != null && it.periodeTom!! > dødsfallsDato }.forEach {
            it.periodeTom = dødsfallsDato
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(VilkårsvurderingService::class.java)

        fun matchVilkårResultater(
            vilkårsvurdering1: Vilkårsvurdering,
            vilkårsvurdering2: Vilkårsvurdering,
        ): List<Pair<VilkårResultat?, VilkårResultat?>> {
            val vilkårResultater =
                (vilkårsvurdering1.personResultater.map { it.vilkårResultater } + vilkårsvurdering2.personResultater.map { it.vilkårResultater }).flatten()

            data class Match(
                val aktør: Aktør,
                val vilkårType: Vilkår,
                val resultat: Resultat,
                val periodeFom: LocalDate?,
                val periodeTom: LocalDate?,
                val begrunnelse: String,
                val erEksplisittAvslagPåSøknad: Boolean?,
            )

            val gruppert = vilkårResultater.groupBy {
                Match(
                    aktør = it.personResultat?.aktør ?: error("VilkårResultat mangler aktør"),
                    vilkårType = it.vilkårType,
                    resultat = it.resultat,
                    periodeFom = it.periodeFom,
                    periodeTom = it.periodeTom,
                    begrunnelse = it.begrunnelse,
                    erEksplisittAvslagPåSøknad = it.erEksplisittAvslagPåSøknad,
                )
            }
            return gruppert.map { (_, gruppe) ->
                if (gruppe.size > 2) throw Feil("Finnes flere like vilkår i én vilkårsvurdering")
                val vilkår1 = gruppe.find { it.personResultat!!.vilkårsvurdering == vilkårsvurdering1 }
                val vilkår2 = gruppe.find { it.personResultat!!.vilkårsvurdering == vilkårsvurdering2 }
                if (vilkår1 == null && vilkår2 == null) error("Vilkårresultater mangler tilknytning til vilkårsvurdering")
                Pair(vilkår1, vilkår2)
            }
        }
    }
}
