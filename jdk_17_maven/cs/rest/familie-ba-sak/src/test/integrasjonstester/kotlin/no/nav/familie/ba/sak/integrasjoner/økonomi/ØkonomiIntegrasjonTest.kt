package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagPersonResultat
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime

class ØkonomiIntegrasjonTest(
    @Autowired
    private val behandlingService: BehandlingService,

    @Autowired
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,

    @Autowired
    private val vilkårsvurderingService: VilkårsvurderingService,

    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,

    @Autowired
    private val økonomiService: ØkonomiService,

    @Autowired
    private val beregningService: BeregningService,

    @Autowired
    private val personidentService: PersonidentService,

    @Autowired
    private val vedtakService: VedtakService,

    @Autowired
    private val simuleringService: SimuleringService,
) : AbstractSpringIntegrationTest() {

    @Test
    @Tag("integration")
    fun `Iverksett vedtak på aktiv behandling`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()
        val stønadFom = LocalDate.now()
        val stønadTom = stønadFom.plusYears(17)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val barnAktørId = personidentService.hentOgLagreAktør(barnFnr, true)

        val vilkårsvurdering =
            lagVilkårsvurdering(behandling, fagsak.aktør, barnAktørId, stønadFom, stønadTom)

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)
        Assertions.assertNotNull(behandling.fagsak.id)

        val barnAktør = personidentService.hentAktørIder(listOf(barnFnr))
        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                fnr,
                listOf(barnFnr),
                søkerAktør = fagsak.aktør,
                barnAktør = barnAktør,
            )
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling = behandling)

        val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)
        Assertions.assertNotNull(vedtak)
        vedtak!!.vedtaksdato = LocalDateTime.now()
        vedtakService.oppdater(vedtak)

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)

        assertDoesNotThrow {
            økonomiService.oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(
                vedtak,
                "ansvarligSaksbehandler",
                AndelTilkjentYtelseForIverksettingFactory(),
            )
        }
    }

    @Test
    @Tag("integration")
    fun `Hent behandlinger for løpende fagsaker til konsistensavstemming mot økonomi`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()
        val stønadFom = LocalDate.now()
        val stønadTom = stønadFom.plusYears(17)

        // Lag fagsak med behandling og personopplysningsgrunnlag og Iverksett.
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val barnAktørId = personidentService.hentAktør(barnFnr)

        val vedtak = Vedtak(
            behandling = behandling,
            vedtaksdato = LocalDateTime.of(2020, 1, 1, 4, 35),
        )

        val barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr), true)
        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                fnr,
                listOf(barnFnr),
                søkerAktør = fagsak.aktør,
                barnAktør = barnAktør,
            )
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)
        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling)

        val vilkårsvurdering =
            lagVilkårsvurdering(behandling, fagsak.aktør, barnAktørId, stønadFom, stønadTom)
        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = vilkårsvurdering)

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)

        økonomiService.oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(
            vedtak,
            "ansvarligSaksbehandler",
            AndelTilkjentYtelseForIverksettingFactory(),
        )
        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.AVSLUTTET)

        fagsak.status = FagsakStatus.LØPENDE
        fagsakService.lagre(fagsak)

        val behandlingerMedAndelerTilAvstemming =
            behandlingHentOgPersisterService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker()

        Assertions.assertTrue(behandlingerMedAndelerTilAvstemming.contains(behandling.id))
    }

    private fun lagVilkårsvurdering(
        behandling: Behandling,
        søkerAktør: Aktør,
        barnAktør: Aktør,
        stønadFom: LocalDate,
        stønadTom: LocalDate,
    ): Vilkårsvurdering {
        val vilkårsvurdering =
            Vilkårsvurdering(behandling = behandling)
        vilkårsvurdering.personResultater = setOf(
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                person = lagPerson(type = PersonType.SØKER, aktør = søkerAktør),
                resultat = Resultat.OPPFYLT,
                periodeFom = stønadFom,
                periodeTom = stønadTom,
                lagFullstendigVilkårResultat = true,
                personType = PersonType.SØKER,
            ),
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                person = lagPerson(type = PersonType.BARN, aktør = barnAktør, fødselsdato = stønadFom),
                resultat = Resultat.OPPFYLT,
                periodeFom = stønadFom,
                periodeTom = stønadTom,
                lagFullstendigVilkårResultat = true,
                personType = PersonType.BARN,
            ),
        )
        return vilkårsvurdering
    }
}
