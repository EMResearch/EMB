package no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.BehandlingstemaService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingMetrics
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingUtils
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class VilkårsvurderingForNyBehandlingService(
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val behandlingService: BehandlingService,
    private val persongrunnlagService: PersongrunnlagService,
    private val behandlingstemaService: BehandlingstemaService,
    private val endretUtbetalingAndelService: EndretUtbetalingAndelService,
    private val vilkårsvurderingMetrics: VilkårsvurderingMetrics,
    private val andelerTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
) {

    fun opprettVilkårsvurderingUtenomHovedflyt(
        behandling: Behandling,
        forrigeBehandlingSomErVedtatt: Behandling?,
        nyMigreringsdato: LocalDate? = null,
    ) {
        when (behandling.opprettetÅrsak) {
            BehandlingÅrsak.ENDRE_MIGRERINGSDATO -> {
                genererVilkårsvurderingForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
                    behandling = behandling,
                    forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErVedtatt
                        ?: throw Feil("Kan ikke opprette behandling med årsak 'Endre migreringsdato' hvis det ikke finnes en tidligere behandling som er iverksatt"),
                    nyMigreringsdato = nyMigreringsdato
                        ?: throw Feil("Kan ikke opprette behandling med årsak 'Endre migreringsdato' uten en migreringsdato"),
                )
                // Lagre ned migreringsdato
                behandlingService.lagreNedMigreringsdato(nyMigreringsdato, behandling)
            }

            BehandlingÅrsak.HELMANUELL_MIGRERING -> {
                genererVilkårsvurderingForHelmanuellMigrering(
                    behandling = behandling,
                    nyMigreringsdato = nyMigreringsdato
                        ?: throw Feil("Kan ikke opprette behandling med årsak 'Helmanuell migrering' uten en migreringsdato"),
                )
                // Lagre ned migreringsdato
                behandlingService.lagreNedMigreringsdato(nyMigreringsdato, behandling)
            }

            BehandlingÅrsak.SATSENDRING -> {
                genererVilkårsvurderingForSatsendring(
                    forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErVedtatt
                        ?: throw Feil("Kan ikke opprette behandling med årsak 'Satsendring' hvis det ikke finnes en tidligere behandling"),
                    inneværendeBehandling = behandling,
                )
            }

            !in listOf(BehandlingÅrsak.SØKNAD, BehandlingÅrsak.FØDSELSHENDELSE) -> {
                initierVilkårsvurderingForBehandling(
                    behandling = behandling,
                    bekreftEndringerViaFrontend = true,
                    forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErVedtatt,
                )
            }

            else -> logger.info(
                "Perioder i vilkårsvurdering generer ikke automatisk for " +
                    behandling.opprettetÅrsak.visningsnavn,
            )
        }
    }

    fun genererVilkårsvurderingForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
        behandling: Behandling,
        forrigeBehandlingSomErVedtatt: Behandling,
        nyMigreringsdato: LocalDate,
    ): Vilkårsvurdering {
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling).apply {
            personResultater =
                VilkårsvurderingForNyBehandlingUtils(
                    personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(
                        behandling.id,
                    ),
                ).lagPersonResultaterForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
                    vilkårsvurdering = this,
                    nyMigreringsdato = nyMigreringsdato,
                    forrigeBehandlingVilkårsvurdering = hentVilkårsvurderingThrows(
                        forrigeBehandlingSomErVedtatt.id,
                        feilmelding =
                        "Kan ikke kopiere vilkårsvurdering fra forrige behandling ${forrigeBehandlingSomErVedtatt.id}" +
                            "til behandling ${behandling.id}",
                    ),
                )
        }
        return vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)
    }

    fun genererVilkårsvurderingForHelmanuellMigrering(
        behandling: Behandling,
        nyMigreringsdato: LocalDate,
    ): Vilkårsvurdering {
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling).apply {
            personResultater = VilkårsvurderingForNyBehandlingUtils(
                personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(behandling.id),
            ).lagPersonResultaterForHelmanuellMigrering(
                vilkårsvurdering = this,
                nyMigreringsdato = nyMigreringsdato,
            )
        }
        return vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)
    }

    private fun genererVilkårsvurderingForSatsendring(
        forrigeBehandlingSomErVedtatt: Behandling,
        inneværendeBehandling: Behandling,
    ): Vilkårsvurdering {
        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(inneværendeBehandling.id)

        val forrigeBehandlingVilkårsvurdering = hentVilkårsvurderingThrows(forrigeBehandlingSomErVedtatt.id)

        val nyVilkårsvurdering =
            forrigeBehandlingVilkårsvurdering.tilKopiForNyBehandling(
                nyBehandling = inneværendeBehandling,
                personopplysningGrunnlag,
            )

        endretUtbetalingAndelService.kopierEndretUtbetalingAndelFraForrigeBehandling(
            behandling = inneværendeBehandling,
            forrigeBehandling = forrigeBehandlingSomErVedtatt,
        )

        return vilkårsvurderingService.lagreNyOgDeaktiverGammel(nyVilkårsvurdering)
    }

    fun initierVilkårsvurderingForBehandling(
        behandling: Behandling,
        bekreftEndringerViaFrontend: Boolean,
        forrigeBehandlingSomErVedtatt: Behandling?,
    ): Vilkårsvurdering {
        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(behandling.id)

        validerAtFødselshendelseInneholderMinstEttBarn(behandling, personopplysningGrunnlag)

        val aktivVilkårsvurdering = hentVilkårsvurdering(behandling.id)

        val initiellVilkårsvurdering =
            VilkårsvurderingForNyBehandlingUtils(personopplysningGrunnlag = personopplysningGrunnlag).genererInitiellVilkårsvurdering(
                behandling = behandling,
                barnaAktørSomAlleredeErVurdert = aktivVilkårsvurdering?.personResultater?.mapNotNull {
                    personopplysningGrunnlag.barna.firstOrNull { barn -> barn.aktør == it.aktør }
                }?.filter { it.type == PersonType.BARN }?.map { it.aktør } ?: emptyList(),
            )

        tellMetrikkerForFødselshendelse(
            aktivVilkårsvurdering = aktivVilkårsvurdering,
            behandling = behandling,
            initiellVilkårsvurdering = initiellVilkårsvurdering,
        )

        val løpendeUnderkategori = behandlingstemaService.hentLøpendeUnderkategori(behandling.fagsak.id)

        val finnesVilkårsvurderingPåInneværendeBehandling = aktivVilkårsvurdering != null
        val førsteVilkårsvurderingPåBehandlingOgFinnesTidligereVedtattBehandling =
            forrigeBehandlingSomErVedtatt != null && !finnesVilkårsvurderingPåInneværendeBehandling

        return if (førsteVilkårsvurderingPåBehandlingOgFinnesTidligereVedtattBehandling) {
            genererVilkårsvurderingFraForrigeVedtatteBehandling(
                initiellVilkårsvurdering = initiellVilkårsvurdering,
                forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErVedtatt!!,
                behandling = behandling,
                personopplysningGrunnlag = personopplysningGrunnlag,
                løpendeUnderkategori = løpendeUnderkategori,
            )
        } else if (finnesVilkårsvurderingPåInneværendeBehandling) {
            genererNyVilkårsvurderingForBehandling(
                initiellVilkårsvurdering = initiellVilkårsvurdering,
                aktivVilkårsvurdering = aktivVilkårsvurdering!!,
                løpendeUnderkategori = løpendeUnderkategori,
                forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErVedtatt,
                bekreftEndringerViaFrontend = bekreftEndringerViaFrontend,
            )
        } else {
            vilkårsvurderingService.lagreInitielt(initiellVilkårsvurdering)
        }
    }

    private fun genererNyVilkårsvurderingForBehandling(
        initiellVilkårsvurdering: Vilkårsvurdering,
        aktivVilkårsvurdering: Vilkårsvurdering,
        løpendeUnderkategori: BehandlingUnderkategori?,
        forrigeBehandlingSomErVedtatt: Behandling?,
        bekreftEndringerViaFrontend: Boolean,
    ): Vilkårsvurdering {
        val (initieltSomErOppdatert, aktivtSomErRedusert) = VilkårsvurderingUtils.flyttResultaterTilInitielt(
            initiellVilkårsvurdering = initiellVilkårsvurdering,
            aktivVilkårsvurdering = aktivVilkårsvurdering,
            løpendeUnderkategori = løpendeUnderkategori,
            aktørerMedUtvidetAndelerIForrigeBehandling = finnAktørerMedUtvidetBarnetrygdIForrigeBehandling(forrigeBehandlingSomErVedtatt),
        )

        if (aktivtSomErRedusert.personResultater.isNotEmpty() && !bekreftEndringerViaFrontend) {
            throw FunksjonellFeil(
                melding = "Saksbehandler forsøker å fjerne vilkår fra vilkårsvurdering",
                frontendFeilmelding = VilkårsvurderingUtils.lagFjernAdvarsel(aktivtSomErRedusert.personResultater),
            )
        }
        return vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = initieltSomErOppdatert)
    }

    /***
     * Utvidet vilkår kan kun kopieres med fra forrige behandling hvis det finnes utbetaling av utvidet barnetrygd i forrige behandling
     */
    private fun finnAktørerMedUtvidetBarnetrygdIForrigeBehandling(forrigeBehandlingSomErVedtatt: Behandling?): List<Aktør> =
        forrigeBehandlingSomErVedtatt?.let {
            val forrigeAndeler =
                andelerTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandlingSomErVedtatt.id)

            finnAktørerMedUtvidetFraAndeler(andeler = forrigeAndeler)
        } ?: emptyList()

    private fun tellMetrikkerForFødselshendelse(
        aktivVilkårsvurdering: Vilkårsvurdering?,
        behandling: Behandling,
        initiellVilkårsvurdering: Vilkårsvurdering,
    ) {
        if (førstegangskjøringAvVilkårsvurdering(aktivVilkårsvurdering) && behandling.opprettetÅrsak == BehandlingÅrsak.FØDSELSHENDELSE) {
            vilkårsvurderingMetrics.tellMetrikker(initiellVilkårsvurdering)
        }
    }

    private fun validerAtFødselshendelseInneholderMinstEttBarn(
        behandling: Behandling,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
    ) {
        if (behandling.skalBehandlesAutomatisk && personopplysningGrunnlag.barna.isEmpty()) {
            throw IllegalStateException("PersonopplysningGrunnlag for fødselshendelse skal inneholde minst ett barn")
        }
    }

    private fun genererVilkårsvurderingFraForrigeVedtatteBehandling(
        initiellVilkårsvurdering: Vilkårsvurdering,
        forrigeBehandlingSomErVedtatt: Behandling,
        behandling: Behandling,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        løpendeUnderkategori: BehandlingUnderkategori?,
    ): Vilkårsvurdering {
        val vilkårsvurdering = VilkårsvurderingForNyBehandlingUtils(
            personopplysningGrunnlag = personopplysningGrunnlag,
        ).genererVilkårsvurderingFraForrigeVedtattBehandling(
            initiellVilkårsvurdering = initiellVilkårsvurdering,
            forrigeBehandlingVilkårsvurdering = hentVilkårsvurderingThrows(forrigeBehandlingSomErVedtatt.id),
            behandling = behandling,
            løpendeUnderkategori = løpendeUnderkategori,
            aktørerMedUtvidetAndelerIForrigeBehandling = finnAktørerMedUtvidetBarnetrygdIForrigeBehandling(forrigeBehandlingSomErVedtatt),
        )
        endretUtbetalingAndelService.kopierEndretUtbetalingAndelFraForrigeBehandling(
            behandling,
            forrigeBehandlingSomErVedtatt,
        )
        return vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)
    }

    private fun hentVilkårsvurdering(behandlingId: Long): Vilkårsvurdering? = vilkårsvurderingService.hentAktivForBehandling(
        behandlingId = behandlingId,
    )

    fun hentVilkårsvurderingThrows(
        behandlingId: Long,
        feilmelding: String? = null,
    ): Vilkårsvurdering =
        hentVilkårsvurdering(behandlingId) ?: throw Feil(
            message = feilmelding ?: "Fant ikke aktiv vilkårsvurdering for behandling $behandlingId",
            frontendFeilmelding = feilmelding ?: "Fant ikke aktiv vilkårsvurdering for behandling.",
        )

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}
