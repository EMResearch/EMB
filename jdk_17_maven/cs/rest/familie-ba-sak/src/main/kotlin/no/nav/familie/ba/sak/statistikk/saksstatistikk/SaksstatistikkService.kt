package no.nav.familie.ba.sak.statistikk.saksstatistikk

import no.nav.familie.ba.sak.common.Utils.hentPropertyFraMaven
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfoService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.HENLAGT_FEILAKTIG_OPPRETTET
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat.HENLAGT_SØKNAD_TRUKKET
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType.BARN_ENSLIG_MINDREÅRIG
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType.INSTITUSJON
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType.NORMAL
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.eksterne.kontrakter.saksstatistikk.AktørDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.BehandlingDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.ResultatBegrunnelseDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.SakDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.SettPåVent
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

@Service
class SaksstatistikkService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val behandlingSøknadsinfoService: BehandlingSøknadsinfoService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val vedtakService: VedtakService,
    private val fagsakService: FagsakService,
    private val personopplysningerService: PersonopplysningerService,
    private val persongrunnlagService: PersongrunnlagService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val settPåVentService: SettPåVentService,
) {

    fun mapTilBehandlingDVH(behandlingId: Long): BehandlingDVH? {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        val forrigeBehandlingId = behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling)
            .takeIf { erRevurderingEllerTekniskBehandling(behandling) }?.id

        val datoMottatt = when (behandling.opprettetÅrsak) {
            BehandlingÅrsak.SØKNAD -> {
                behandlingSøknadsinfoService.hentSøknadMottattDato(behandlingId) ?: behandling.opprettetTidspunkt
            }

            else -> behandling.opprettetTidspunkt
        }

        val behandlendeEnhetsKode =
            arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId).behandlendeEnhetId
        val ansvarligEnhetKode = arbeidsfordelingService.hentArbeidsfordelingsenhet(behandling).enhetId

        val aktivtVedtak = vedtakService.hentAktivForBehandling(behandlingId)
        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandlingId)

        val now = ZonedDateTime.now()

        return BehandlingDVH(
            funksjonellTid = now,
            tekniskTid = now,
            mottattDato = datoMottatt.atZone(TIMEZONE),
            registrertDato = behandling.opprettetTidspunkt.atZone(TIMEZONE),
            behandlingId = behandling.id.toString(),
            funksjonellId = UUID.randomUUID().toString(),
            sakId = behandling.fagsak.id.toString(),
            behandlingType = behandling.type.name,
            behandlingStatus = behandling.status.name,
            behandlingKategori = when (behandling.underkategori) { // Gjøres pga. tilpasning til DVH-modell
                BehandlingUnderkategori.ORDINÆR, BehandlingUnderkategori.UTVIDET ->
                    behandling.underkategori.name
            },
            behandlingUnderkategori = when (behandling.fagsak.type) { // <-'
                NORMAL -> null
                BARN_ENSLIG_MINDREÅRIG -> ENSLIG_MINDREÅRIG_KODE
                INSTITUSJON -> INSTITUSJON.name
            },
            behandlingAarsak = behandling.opprettetÅrsak.name,
            automatiskBehandlet = behandling.skalBehandlesAutomatisk,
            utenlandstilsnitt = behandling.kategori.name, // Gjøres pga. tilpasning til DVH-modell
            ansvarligEnhetKode = ansvarligEnhetKode,
            behandlendeEnhetKode = behandlendeEnhetsKode,
            ansvarligEnhetType = "NORG",
            behandlendeEnhetType = "NORG",
            totrinnsbehandling = !behandling.skalBehandlesAutomatisk,
            avsender = "familie-ba-sak",
            versjon = hentPropertyFraMaven("familie.kontrakter.saksstatistikk") ?: "2",
            // Ikke påkrevde felt
            vedtaksDato = aktivtVedtak?.vedtaksdato?.toLocalDate(),
            relatertBehandlingId = forrigeBehandlingId?.toString(),
            vedtakId = aktivtVedtak?.id?.toString(),
            resultat = behandling.resultat.name,
            behandlingTypeBeskrivelse = behandling.type.visningsnavn,
            resultatBegrunnelser = behandling.resultatBegrunnelser(aktivtVedtak),
            behandlingOpprettetAv = behandling.opprettetAv,
            behandlingOpprettetType = "saksbehandlerId",
            behandlingOpprettetTypeBeskrivelse = "saksbehandlerId. VL ved automatisk behandling",
            beslutter = totrinnskontroll?.beslutterId,
            saksbehandler = totrinnskontroll?.saksbehandlerId,
            settPaaVent = hentSettPåVentDVH(behandlingId),
        )
    }

    private fun hentSettPåVentDVH(behandlingId: Long): SettPåVent? {
        val settPåVent = settPåVentService.finnAktivSettPåVentPåBehandling(behandlingId) ?: return null
        return SettPåVent(
            frist = settPåVent.frist.atStartOfDay(TIMEZONE),
            tidSattPaaVent = settPåVent.tidSattPåVent.atStartOfDay(TIMEZONE),
            aarsak = settPåVent.årsak.name,
        )
    }

    fun mapTilSakDvh(sakId: Long): SakDVH? {
        val aktivBehandling = behandlingHentOgPersisterService.finnAktivForFagsak(fagsakId = sakId)
        val fagsak = aktivBehandling?.fagsak ?: fagsakService.hentPåFagsakId(sakId)

        var landkodeSøker: String = PersonopplysningerService.UKJENT_LANDKODE

        val deltagere = if (aktivBehandling != null) {
            val personer = persongrunnlagService.hentAktiv(behandlingId = aktivBehandling.id)?.søkerOgBarn ?: emptySet()
            personer.map {
                if (it.type == PersonType.SØKER) {
                    landkodeSøker = hentLandkode(it)
                }
                AktørDVH(
                    it.aktør.aktørId.toLong(),
                    it.type.name,
                )
            }
        } else {
            landkodeSøker = hentLandkode(fagsak.aktør)
            listOf(AktørDVH(fagsak.aktør.aktørId.toLong(), PersonType.SØKER.name))
        }

        return SakDVH(
            funksjonellTid = ZonedDateTime.now(),
            tekniskTid = ZonedDateTime.now(),
            opprettetDato = LocalDate.now(),
            funksjonellId = UUID.randomUUID().toString(),
            sakId = sakId.toString(),
            aktorId = fagsak.aktør.aktørId.toLong(),
            aktorer = deltagere,
            sakStatus = fagsak.status.name,
            avsender = "familie-ba-sak",
            versjon = hentPropertyFraMaven("familie.kontrakter.saksstatistikk") ?: "2",
            bostedsland = landkodeSøker,
        )
    }

    private fun hentLandkode(person: Person): String {
        return if (person.bostedsadresser.isNotEmpty()) {
            "NO"
        } else {
            personopplysningerService.hentLandkodeAlpha2UtenlandskBostedsadresse(
                person.aktør,
            )
        }
    }

    private fun hentLandkode(aktør: Aktør): String {
        val personInfo = personopplysningerService.hentPersoninfoEnkel(aktør)

        return if (personInfo.bostedsadresser.isNotEmpty()) {
            "NO"
        } else {
            personopplysningerService.hentLandkodeAlpha2UtenlandskBostedsadresse(
                aktør,
            )
        }
    }

    private fun erRevurderingEllerTekniskBehandling(behandling: Behandling) =
        behandling.type == BehandlingType.REVURDERING || behandling.type == BehandlingType.TEKNISK_OPPHØR || behandling.type == BehandlingType.TEKNISK_ENDRING

    private fun Behandling.resultatBegrunnelser(vedtak: Vedtak?): List<ResultatBegrunnelseDVH> {
        return when (resultat) {
            HENLAGT_SØKNAD_TRUKKET, HENLAGT_FEILAKTIG_OPPRETTET -> emptyList()
            else ->
                vedtak
                    ?.hentResultatBegrunnelserFraVedtaksbegrunnelser()
                    ?: emptyList()
        }
    }

    private fun Vedtak.hentResultatBegrunnelserFraVedtaksbegrunnelser(): List<ResultatBegrunnelseDVH> {
        return vedtaksperiodeService.hentPersisterteVedtaksperioder(this)
            .flatMap { vedtaksperiode ->
                vedtaksperiode.begrunnelser
                    .map {
                        ResultatBegrunnelseDVH(
                            fom = vedtaksperiode.fom,
                            tom = vedtaksperiode.tom,
                            type = it.standardbegrunnelse.vedtakBegrunnelseType.name,
                            vedtakBegrunnelse = it.standardbegrunnelse.name,
                        )
                    }
            }
    }

    companion object {

        val TIMEZONE: ZoneId = ZoneId.systemDefault()
        val ENSLIG_MINDREÅRIG_KODE = "ENSLIG_MINDREÅRIG"
    }
}
