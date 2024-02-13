package no.nav.familie.ba.sak.kjerne.behandling.behandlingstema

import jakarta.transaction.Transactional
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjeService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import org.springframework.stereotype.Service

@Service
class BehandlingstemaService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val loggService: LoggService,
    private val oppgaveService: OppgaveService,
    private val vilkårsvurderingTidslinjeService: VilkårsvurderingTidslinjeService,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
) {

    @Transactional
    fun oppdaterBehandlingstema(
        behandling: Behandling,
        overstyrtKategori: BehandlingKategori? = null,
        overstyrtUnderkategori: BehandlingUnderkategori? = null,
        manueltOppdatert: Boolean = false,
    ): Behandling {
        if (behandling.skalBehandlesAutomatisk) return behandling
        if (manueltOppdatert && (overstyrtKategori == null || overstyrtUnderkategori == null)) {
            throw FunksjonellFeil("Du må velge behandlingstema.")
        }

        val utledetKategori = bestemKategori(
            overstyrtKategori = overstyrtKategori,
            kategoriFraSisteIverksattBehandling = hentLøpendeKategori(behandling.fagsak.id),
            kategoriFraInneværendeBehandling = hentKategoriFraInneværendeBehandling(behandling.fagsak.id),
        )

        val utledetUnderkategori = bestemUnderkategori(
            overstyrtUnderkategori = overstyrtUnderkategori,
            underkategoriFraLøpendeBehandling = hentLøpendeUnderkategori(fagsakId = behandling.fagsak.id),
            underkategoriFraInneværendeBehandling = hentUnderkategoriFraInneværendeBehandling(fagsakId = behandling.fagsak.id),
        )

        val forrigeUnderkategori = behandling.underkategori
        val forrigeKategori = behandling.kategori
        val skalOppdatereKategori = utledetKategori != forrigeKategori
        val skalOppdatereUnderkategori = utledetUnderkategori != forrigeUnderkategori
        val skalOppdatereKategoriEllerUnderkategori = skalOppdatereKategori || skalOppdatereUnderkategori

        return if (skalOppdatereKategoriEllerUnderkategori) {
            behandling.apply {
                kategori = utledetKategori
                underkategori = utledetUnderkategori
            }

            behandlingHentOgPersisterService.lagreEllerOppdater(behandling).also { lagretBehandling ->
                oppgaveService.patchOppgaverForBehandling(lagretBehandling) {
                    val lagretUnderkategori = lagretBehandling.underkategori
                    if (it.behandlingstema != lagretBehandling.tilOppgaveBehandlingTema().value || it.behandlingstype != lagretBehandling.kategori.tilOppgavebehandlingType().value) {
                        it.copy(
                            behandlingstema = when (lagretUnderkategori) {
                                BehandlingUnderkategori.ORDINÆR, BehandlingUnderkategori.UTVIDET ->
                                    behandling.tilOppgaveBehandlingTema().value
                            },
                            behandlingstype = lagretBehandling.kategori.tilOppgavebehandlingType().value,
                        )
                    } else {
                        null
                    }
                }

                if (manueltOppdatert) {
                    loggService.opprettEndretBehandlingstema(
                        behandling = lagretBehandling,
                        forrigeKategori = forrigeKategori,
                        forrigeUnderkategori = forrigeUnderkategori,
                        nyKategori = utledetKategori,
                        nyUnderkategori = utledetUnderkategori,
                    )
                }
            }
        } else {
            behandling
        }
    }

    fun hentLøpendeKategori(fagsakId: Long): BehandlingKategori {
        val forrigeVedtatteBehandling =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId = fagsakId)
                ?: return BehandlingKategori.NASJONAL

        val barnasTidslinjer =
            vilkårsvurderingTidslinjeService.hentTidslinjer(behandlingId = BehandlingId(forrigeVedtatteBehandling.id))
                ?.barnasTidslinjer()
        return utledLøpendeKategori(barnasTidslinjer)
    }

    fun hentKategoriFraInneværendeBehandling(fagsakId: Long): BehandlingKategori {
        val aktivBehandling =
            behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsakId = fagsakId)
                ?: return BehandlingKategori.NASJONAL
        val vilkårsvurdering =
            vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId = aktivBehandling.id)
                ?: return aktivBehandling.kategori
        val erVilkårMedEØSRegelverkBehandlet = vilkårsvurdering.personResultater
            .flatMap { it.vilkårResultater }
            .filter { it.sistEndretIBehandlingId == aktivBehandling.id }
            .any { it.vurderesEtter == Regelverk.EØS_FORORDNINGEN }

        return if (erVilkårMedEØSRegelverkBehandlet) {
            BehandlingKategori.EØS
        } else {
            BehandlingKategori.NASJONAL
        }
    }

    fun hentLøpendeUnderkategori(fagsakId: Long): BehandlingUnderkategori? {
        val forrigeAndeler = hentForrigeAndeler(fagsakId)
        return if (forrigeAndeler != null) utledLøpendeUnderkategori(forrigeAndeler) else null
    }

    fun hentUnderkategoriFraInneværendeBehandling(fagsakId: Long): BehandlingUnderkategori {
        val aktivBehandling =
            behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsakId = fagsakId)
                ?: return BehandlingUnderkategori.ORDINÆR

        val erUtvidetVilkårBehandlet =
            vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId = aktivBehandling.id)
                ?.personResultater
                ?.flatMap { it.vilkårResultater }
                ?.filter { it.sistEndretIBehandlingId == aktivBehandling.id }
                ?.any { it.vilkårType == Vilkår.UTVIDET_BARNETRYGD }

        return if (erUtvidetVilkårBehandlet == true) {
            BehandlingUnderkategori.UTVIDET
        } else {
            BehandlingUnderkategori.ORDINÆR
        }
    }

    private fun hentForrigeAndeler(fagsakId: Long): List<AndelTilkjentYtelse>? {
        val forrigeVedtatteBehandling =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId = fagsakId) ?: return null
        return andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = forrigeVedtatteBehandling.id)
    }
}
