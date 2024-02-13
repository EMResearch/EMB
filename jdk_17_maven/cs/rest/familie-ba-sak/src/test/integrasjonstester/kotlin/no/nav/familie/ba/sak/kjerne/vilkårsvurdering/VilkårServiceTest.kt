package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.kjørStegprosessForFGB
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.common.nyOrdinærBehandling
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.til18ÅrsVilkårsdato
import no.nav.familie.ba.sak.common.vurderVilkårsvurderingTilInnvilget
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.datagenerator.vilkårsvurdering.lagBarnVilkårResultat
import no.nav.familie.ba.sak.datagenerator.vilkårsvurdering.lagSøkerVilkårResultat
import no.nav.familie.ba.sak.ekstern.restDomene.RestNyttVilkår
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.RestSlettVilkår
import no.nav.familie.ba.sak.ekstern.restDomene.RestVilkårResultat
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPersonResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.BehandlingstemaService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.BehandlingStegStatus
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.VilkårsvurderingForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.ResultatBegrunnelse
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime

class VilkårServiceTest(
    @Autowired
    private val behandlingService: BehandlingService,

    @Autowired
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,

    @Autowired
    private val vilkårsvurderingService: VilkårsvurderingService,

    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val persongrunnlagService: PersongrunnlagService,

    @Autowired
    private val vilkårService: VilkårService,

    @Autowired
    private val databaseCleanupService: DatabaseCleanupService,

    @Autowired
    private val personidentService: PersonidentService,

    @Autowired
    private val behandlingstemaService: BehandlingstemaService,

    @Autowired
    private val vedtakService: VedtakService,

    @Autowired
    private val vedtaksperiodeService: VedtaksperiodeService,

    @Autowired
    private val stegService: StegService,

    @Autowired
    private val vilkårsvurderingForNyBehandlingService: VilkårsvurderingForNyBehandlingService,

    @Autowired
    private val brevmalService: BrevmalService,

) : AbstractSpringIntegrationTest() {

    @BeforeAll
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `Manuell vilkårsvurdering skal få erAutomatiskVurdert på enkelte vilkår`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val forrigeBehandlingSomErIverksatt =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsakId = behandling.fagsak.id)

        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                fnr,
                listOf(barnFnr),
                søkerAktør = personidentService.hentOgLagreAktør(fnr, true),
                barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr), true),
            )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vilkårsvurdering = vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(
            behandling = behandling,
            bekreftEndringerViaFrontend = true,
            forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErIverksatt,
        )
        vilkårsvurdering.personResultater.forEach { personResultat ->
            personResultat.vilkårResultater.forEach { vilkårResultat ->
                when (vilkårResultat.vilkårType) {
                    Vilkår.UNDER_18_ÅR, Vilkår.GIFT_PARTNERSKAP -> assertTrue(vilkårResultat.erAutomatiskVurdert)
                    else -> assertFalse(vilkårResultat.erAutomatiskVurdert)
                }
            }
        }
    }

    @Test
    fun `Endring på automatisk vurderte vilkår(manuell vilkårsvurdering) skal settes til manuell ved endring`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling =
            behandlingService.opprettBehandling(nyOrdinærBehandling(søkersIdent = fnr, fagsakId = fagsak.id))
        val forrigeBehandlingSomErIverksatt =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsakId = behandling.fagsak.id)

        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                fnr,
                listOf(barnFnr),
                søkerAktør = personidentService.hentOgLagreAktør(fnr, true),
                barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr), true),
            )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vilkårsvurdering = vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(
            behandling = behandling,
            bekreftEndringerViaFrontend = true,
            forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErIverksatt,
        )
        val under18ÅrVilkårForBarn =
            vilkårsvurdering.personResultater.find { it.aktør.aktivFødselsnummer() == barnFnr }
                ?.tilRestPersonResultat()?.vilkårResultater?.find { it.vilkårType == Vilkår.UNDER_18_ÅR }

        val endretVilkårsvurdering: List<RestPersonResultat> =
            vilkårService.endreVilkår(
                behandlingId = behandling.id,
                vilkårId = under18ÅrVilkårForBarn!!.id,
                restPersonResultat =
                RestPersonResultat(
                    personIdent = barnFnr,
                    vilkårResultater = listOf(
                        under18ÅrVilkårForBarn.copy(
                            resultat = Resultat.OPPFYLT,
                            periodeFom = LocalDate.of(2019, 5, 8),
                        ),
                    ),
                ),
            )

        val endretUnder18ÅrVilkårForBarn =
            endretVilkårsvurdering.find { it.personIdent == barnFnr }
                ?.vilkårResultater?.find { it.vilkårType == Vilkår.UNDER_18_ÅR }
        assertFalse(endretUnder18ÅrVilkårForBarn!!.erAutomatiskVurdert)
    }

    @Test
    fun `Skal automatisk lagre ny vilkårsvurdering over den gamle`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()
        val barnFnr2 = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val forrigeBehandlingSomErIverksatt =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsakId = behandling.fagsak.id)

        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                fnr,
                listOf(barnFnr),
                søkerAktør = personidentService.hentOgLagreAktør(fnr, true),
                barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr), true),
            )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vilkårsvurdering = vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(
            behandling = behandling,
            bekreftEndringerViaFrontend = true,
            forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErIverksatt,
        )
        assertEquals(2, vilkårsvurdering.personResultater.size)

        val personopplysningGrunnlagMedEkstraBarn =
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                fnr,
                listOf(barnFnr, barnFnr2),
                søkerAktør = personidentService.hentOgLagreAktør(fnr, true),
                barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr, barnFnr2), true),
            )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlagMedEkstraBarn)

        val vilkårsvurderingMedEkstraBarn = vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(
            behandling = behandling,
            bekreftEndringerViaFrontend = true,
            forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErIverksatt,
        )
        assertEquals(3, vilkårsvurderingMedEkstraBarn.personResultater.size)
    }

    @Test
    fun `vurder ugyldig vilkårsvurdering`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                fnr,
                listOf(barnFnr),
                søkerAktør = personidentService.hentOgLagreAktør(fnr, true),
                barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr), true),
            )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)
    }

    @Test
    fun `Vilkårsvurdering kopieres riktig`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val forrigeBehandlingSomErIverksatt =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsakId = behandling.fagsak.id)

        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                fnr,
                listOf(barnFnr),
                søkerAktør = personidentService.hentOgLagreAktør(fnr, true),
                barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr), true),
            )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vilkårsvurdering = vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(
            behandling = behandling,
            bekreftEndringerViaFrontend = true,
            forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErIverksatt,
        )
            .also {
                it.personResultater
                    .forEach { personResultat ->
                        personResultat.leggTilBlankAnnenVurdering(AnnenVurderingType.OPPLYSNINGSPLIKT)
                    }
            }

        val kopiertVilkårsvurdering = vilkårsvurdering.kopier(inkluderAndreVurderinger = true)

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = kopiertVilkårsvurdering)
        val personResultater = vilkårsvurderingService
            .hentAktivForBehandling(behandlingId = behandling.id)!!.personResultater

        assertEquals(2, personResultater.size)
        Assertions.assertNotEquals(vilkårsvurdering.id, kopiertVilkårsvurdering.id)
        assertEquals(1, kopiertVilkårsvurdering.personResultater.first().andreVurderinger.size)
        assertEquals(
            AnnenVurderingType.OPPLYSNINGSPLIKT,
            kopiertVilkårsvurdering.personResultater.first().andreVurderinger.first().type,
        )
    }

    @Test
    fun `Resultatbegrunnelse kan ikke settes i kombinasjon med ugyldig vilkår`() {
        val vilkårsvurdering = lagVilkårsvurderingForEnSøkerMedEttBarn()

        val enPersonIBehandlingen = vilkårsvurdering.personResultater.elementAt(0)
        val bosattVilkårForEnPersonIBehandlingen =
            enPersonIBehandlingen.tilRestPersonResultat().vilkårResultater.find { it.vilkårType === Vilkår.BOSATT_I_RIKET }

        assertThrows<FunksjonellFeil> {
            vilkårService.endreVilkår(
                behandlingId = vilkårsvurdering.behandling.id,
                vilkårId = bosattVilkårForEnPersonIBehandlingen!!.id,
                restPersonResultat =
                RestPersonResultat(
                    personIdent = enPersonIBehandlingen.aktør.aktivFødselsnummer(),
                    vilkårResultater = listOf(
                        bosattVilkårForEnPersonIBehandlingen.copy(
                            resultat = Resultat.OPPFYLT,
                            resultatBegrunnelse = ResultatBegrunnelse.IKKE_AKTUELT,
                            vurderesEtter = Regelverk.EØS_FORORDNINGEN,
                            periodeFom = LocalDate.of(2019, 5, 8),
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun `Resultatbegrunnelse kan ikke settes i kombinasjon med ugyldig resultat`() {
        val vilkårsvurdering = lagVilkårsvurderingForEnSøkerMedEttBarn()

        val enPersonIBehandlingen = vilkårsvurdering.personResultater.elementAt(0)
        val oppholdVilkårForEnPersonIBehandlingen =
            enPersonIBehandlingen.tilRestPersonResultat().vilkårResultater.find { it.vilkårType === Vilkår.LOVLIG_OPPHOLD }

        assertThrows<FunksjonellFeil> {
            vilkårService.endreVilkår(
                behandlingId = vilkårsvurdering.behandling.id,
                vilkårId = oppholdVilkårForEnPersonIBehandlingen!!.id,
                restPersonResultat =
                RestPersonResultat(
                    personIdent = enPersonIBehandlingen.aktør.aktivFødselsnummer(),
                    vilkårResultater = listOf(
                        oppholdVilkårForEnPersonIBehandlingen.copy(
                            resultat = Resultat.IKKE_OPPFYLT,
                            resultatBegrunnelse = ResultatBegrunnelse.IKKE_AKTUELT,
                            vurderesEtter = Regelverk.EØS_FORORDNINGEN,
                            periodeFom = LocalDate.of(2019, 5, 8),
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun `Resultatbegrunnelse kan ikke settes i kombinasjon med ugyldig regelverk`() {
        val vilkårsvurdering = lagVilkårsvurderingForEnSøkerMedEttBarn()

        val enPersonIBehandlingen = vilkårsvurdering.personResultater.elementAt(0)
        val oppholdVilkårForEnPersonIBehandlingen =
            enPersonIBehandlingen.tilRestPersonResultat().vilkårResultater.find { it.vilkårType === Vilkår.LOVLIG_OPPHOLD }

        assertThrows<FunksjonellFeil> {
            vilkårService.endreVilkår(
                behandlingId = vilkårsvurdering.behandling.id,
                vilkårId = oppholdVilkårForEnPersonIBehandlingen!!.id,
                restPersonResultat =
                RestPersonResultat(
                    personIdent = enPersonIBehandlingen.aktør.aktivFødselsnummer(),
                    vilkårResultater = listOf(
                        oppholdVilkårForEnPersonIBehandlingen.copy(
                            resultat = Resultat.OPPFYLT,
                            resultatBegrunnelse = ResultatBegrunnelse.IKKE_AKTUELT,
                            vurderesEtter = Regelverk.NASJONALE_REGLER,
                            periodeFom = LocalDate.of(2019, 5, 8),
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun `Resultatbegrunnelse kaster ikke feil når brukt i kombinasjon med gyldig vilkår, resultat og regelverk`() {
        val vilkårsvurdering = lagVilkårsvurderingForEnSøkerMedEttBarn()

        val enPersonIBehandlingen = vilkårsvurdering.personResultater.elementAt(0)
        val oppholdVilkårForEnPersonIBehandlingen =
            enPersonIBehandlingen.tilRestPersonResultat().vilkårResultater.find { it.vilkårType === Vilkår.LOVLIG_OPPHOLD }

        assertDoesNotThrow {
            vilkårService.endreVilkår(
                behandlingId = vilkårsvurdering.behandling.id,
                vilkårId = oppholdVilkårForEnPersonIBehandlingen!!.id,
                restPersonResultat =
                RestPersonResultat(
                    personIdent = enPersonIBehandlingen.aktør.aktivFødselsnummer(),
                    vilkårResultater = listOf(
                        oppholdVilkårForEnPersonIBehandlingen.copy(
                            resultat = Resultat.OPPFYLT,
                            resultatBegrunnelse = ResultatBegrunnelse.IKKE_AKTUELT,
                            vurderesEtter = Regelverk.EØS_FORORDNINGEN,
                            periodeFom = LocalDate.of(2019, 5, 8),
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun `Vilkårsvurdering fra forrige behandling kopieres riktig`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        var behandling =
            behandlingService.opprettBehandling(nyOrdinærBehandling(søkersIdent = fnr, fagsakId = fagsak.id))
        val forrigeBehandlingSomErIverksatt =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsakId = behandling.fagsak.id)

        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                fnr,
                listOf(barnFnr),
                søkerAktør = personidentService.hentOgLagreAktør(fnr, true),
                barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr), true),
            )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vilkårsvurdering = vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(
            behandling = behandling,
            bekreftEndringerViaFrontend = true,
            forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErIverksatt,
        )
        assertEquals(2, vilkårsvurdering.personResultater.size)

        vilkårsvurdering.personResultater.map { personResultat ->
            personResultat.tilRestPersonResultat().vilkårResultater.map {
                vilkårService.endreVilkår(
                    behandlingId = behandling.id,
                    vilkårId = it.id,
                    restPersonResultat =
                    RestPersonResultat(
                        personIdent = personResultat.aktør.aktivFødselsnummer(),
                        vilkårResultater = listOf(
                            it.copy(
                                resultat = Resultat.OPPFYLT,
                                resultatBegrunnelse = if (it.vilkårType === Vilkår.LOVLIG_OPPHOLD) ResultatBegrunnelse.IKKE_AKTUELT else null,
                                vurderesEtter = Regelverk.EØS_FORORDNINGEN,
                                periodeFom = LocalDate.of(2019, 5, 8),
                            ),
                        ),
                    ),
                )
            }
        }

        behandling = markerBehandlingSomAvsluttet(behandling)

        val barnFnr2 = randomFnr()

        val behandling2 = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val personopplysningGrunnlag2 =
            lagTestPersonopplysningGrunnlag(
                behandling2.id,
                fnr,
                listOf(barnFnr, barnFnr2),
                søkerAktør = personidentService.hentOgLagreAktør(fnr, true),
                barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr, barnFnr2), true),
            )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag2)

        val vilkårsvurdering2 = vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(
            behandling = behandling2,
            bekreftEndringerViaFrontend = true,
            forrigeBehandlingSomErVedtatt = behandling,
        )

        assertEquals(3, vilkårsvurdering2.personResultater.size)

        vilkårsvurdering2.personResultater.forEach { personResultat ->
            personResultat.vilkårResultater.forEach { vilkårResultat ->
                if (personResultat.aktør.aktivFødselsnummer() == barnFnr2) {
                    assertEquals(behandling2.id, vilkårResultat.sistEndretIBehandlingId)
                } else {
                    if (vilkårResultat.vilkårType === Vilkår.LOVLIG_OPPHOLD) {
                        assertEquals(vilkårResultat.resultatBegrunnelse, vilkårResultat.resultatBegrunnelse)
                    } else {
                        assertEquals(null, vilkårResultat.resultatBegrunnelse)
                    }

                    assertEquals(Resultat.OPPFYLT, vilkårResultat.resultat)
                    assertEquals(behandling.id, vilkårResultat.sistEndretIBehandlingId)
                }
            }
        }
    }

    @Test
    fun `Peker til behandling oppdateres ved vurdering av revurdering`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        var behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val forrigeBehandlingSomErIverksatt =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsakId = behandling.fagsak.id)

        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                fnr,
                listOf(barnFnr),
                søkerAktør = personidentService.hentOgLagreAktør(fnr, true),
                barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr), true),
            )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vilkårsvurdering = vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(
            behandling = behandling,
            bekreftEndringerViaFrontend = true,
            forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErIverksatt,
        )

        val barn: Person = personopplysningGrunnlag.barna.find { it.aktør.aktivFødselsnummer() == barnFnr }!!
        vurderVilkårsvurderingTilInnvilget(vilkårsvurdering, barn)

        vilkårsvurderingService.oppdater(vilkårsvurdering)
        behandling = markerBehandlingSomAvsluttet(behandling)

        val barnFnr2 = randomFnr()

        val behandling2 = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        val personopplysningGrunnlag2 =
            lagTestPersonopplysningGrunnlag(
                behandling2.id,
                fnr,
                listOf(barnFnr, barnFnr2),
                søkerAktør = personidentService.hentOgLagreAktør(fnr, true),
                barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr, barnFnr2), true),
            )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag2)

        val vilkårsvurdering1 = vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(
            behandling = behandling2,
            bekreftEndringerViaFrontend = true,
            forrigeBehandlingSomErVedtatt = behandling,
        )

        assertEquals(3, vilkårsvurdering1.personResultater.size)

        val personResultat = vilkårsvurdering1.personResultater.find { it.aktør.aktivFødselsnummer() == barnFnr }!!
        val borMedSøkerVilkår = personResultat.vilkårResultater.find { it.vilkårType == Vilkår.BOR_MED_SØKER }!!
        assertEquals(behandling.id, borMedSøkerVilkår.sistEndretIBehandlingId)

        VilkårsvurderingUtils.muterPersonVilkårResultaterPut(
            personResultat,
            RestVilkårResultat(
                borMedSøkerVilkår.id,
                Vilkår.BOR_MED_SØKER,
                Resultat.OPPFYLT,
                LocalDate.of(2010, 6, 2),
                LocalDate.of(2011, 9, 1),
                "",
                "",
                LocalDateTime.now(),
                behandling.id,
            ),
        )

        val vilkårsvurderingEtterEndring = vilkårsvurderingService.oppdater(vilkårsvurdering1)
        val personResultatEtterEndring =
            vilkårsvurderingEtterEndring.personResultater.find { it.aktør.aktivFødselsnummer() == barnFnr }!!
        val borMedSøkerVilkårEtterEndring =
            personResultatEtterEndring.vilkårResultater.find { it.vilkårType == Vilkår.BOR_MED_SØKER }!!
        assertEquals(behandling2.id, borMedSøkerVilkårEtterEndring.sistEndretIBehandlingId)
    }

    @Test
    fun `Skal legge til både VURDERING_ANNET_GRUNNLAG og VURDERT_MEDLEMSKAP i utdypendeVilkårsvurderinger liste`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling =
            behandlingService.opprettBehandling(nyOrdinærBehandling(søkersIdent = fnr, fagsakId = fagsak.id))
        val forrigeBehandlingSomErIverksatt =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsakId = behandling.fagsak.id)

        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                fnr,
                listOf(barnFnr),
                søkerAktør = personidentService.hentOgLagreAktør(fnr, true),
                barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr), true),
            )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vilkårsvurdering = vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(
            behandling = behandling,
            bekreftEndringerViaFrontend = true,
            forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErIverksatt,
        )
        val under18ÅrVilkårForBarn =
            vilkårsvurdering.personResultater.find { it.aktør.aktivFødselsnummer() == barnFnr }
                ?.tilRestPersonResultat()?.vilkårResultater?.find { it.vilkårType == Vilkår.UNDER_18_ÅR }

        val endretVilkårsvurdering: List<RestPersonResultat> =
            vilkårService.endreVilkår(
                behandlingId = behandling.id,
                vilkårId = under18ÅrVilkårForBarn!!.id,
                restPersonResultat =
                RestPersonResultat(
                    personIdent = barnFnr,
                    vilkårResultater = listOf(
                        under18ÅrVilkårForBarn.copy(
                            resultat = Resultat.OPPFYLT,
                            periodeFom = LocalDate.of(2019, 5, 8),
                            utdypendeVilkårsvurderinger = listOf(
                                UtdypendeVilkårsvurdering.VURDERING_ANNET_GRUNNLAG,
                                UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP,
                            ),
                        ),
                    ),
                ),
            )

        val endretUnder18ÅrVilkårForBarn =
            endretVilkårsvurdering.find { it.personIdent == barnFnr }
                ?.vilkårResultater?.find { it.vilkårType == Vilkår.UNDER_18_ÅR }

        assertEquals(
            2,
            endretUnder18ÅrVilkårForBarn!!.utdypendeVilkårsvurderinger.size,
        )
    }

    @Test
    fun `skal lage vilkårsvurderingsperiode for vanlig migrering tilbake i tid`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()
        val forrigeVilkårsdato = LocalDate.of(2021, 8, 1)
        val nyMigreringsdato = LocalDate.of(2021, 1, 1)
        val barnetsFødselsdato = LocalDate.now().minusYears((LocalDate.now().year - nyMigreringsdato.year + 1).toLong())

        val behandlinger = lagMigreringsbehandling(fnr, barnFnr, barnetsFødselsdato, forrigeVilkårsdato)
        val behandling = behandlinger.second

        val vilkårsvurdering =
            vilkårsvurderingForNyBehandlingService.genererVilkårsvurderingForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
                behandling = behandling,
                forrigeBehandlingSomErVedtatt = behandlinger.first,
                nyMigreringsdato = nyMigreringsdato,
            )
        assertTrue { vilkårsvurdering.personResultater.isNotEmpty() }
        val søkerVilkårResultat =
            vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == fnr }.vilkårResultater
        assertTrue { søkerVilkårResultat.size == 2 }
        assertTrue {
            søkerVilkårResultat.all {
                it.periodeFom == nyMigreringsdato &&
                    it.periodeTom == null
            }
        }

        val barnVilkårResultat =
            vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == barnFnr }.vilkårResultater
        assertTrue { barnVilkårResultat.size == 5 }
        assertTrue {
            barnVilkårResultat.filter { it.vilkårType.påvirketVilkårForEndreMigreringsdato() }.all {
                it.periodeFom == nyMigreringsdato &&
                    it.periodeTom == null
            }
        }
        assertTrue {
            barnVilkårResultat.filter { it.vilkårType == Vilkår.UNDER_18_ÅR }.all {
                it.periodeFom == barnetsFødselsdato &&
                    it.periodeTom == barnetsFødselsdato.til18ÅrsVilkårsdato()
            }
        }
        assertTrue {
            barnVilkårResultat.filter { it.vilkårType == Vilkår.GIFT_PARTNERSKAP }.all {
                it.periodeFom == barnetsFødselsdato &&
                    it.periodeTom == null
            }
        }
    }

    @Test
    fun `skal lage utvidet barnetrygd vilkår for migreringsbehandling`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()
        val forrigeVilkårsdato = LocalDate.of(2021, 8, 1)
        val barnetsFødselsdato = LocalDate.now().minusYears(1)

        val behandlinger = lagMigreringsbehandling(fnr, barnFnr, barnetsFødselsdato, forrigeVilkårsdato)
        val nåVærendeBehandling = behandlinger.second

        val nyMigreringsdato = LocalDate.of(2021, 1, 1)
        var vilkårsvurdering =
            vilkårsvurderingForNyBehandlingService.genererVilkårsvurderingForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
                behandling = nåVærendeBehandling,
                forrigeBehandlingSomErVedtatt = behandlinger.first,
                nyMigreringsdato = nyMigreringsdato,
            )
        assertTrue { vilkårsvurdering.personResultater.isNotEmpty() }
        assertTrue { vilkårsvurdering.personResultater.any { it.aktør.aktivFødselsnummer() == fnr } }
        assertTrue { vilkårsvurdering.personResultater.find { it.aktør.aktivFødselsnummer() == fnr }!!.vilkårResultater.size == 2 }
        vilkårService.postVilkår(
            nåVærendeBehandling.id,
            RestNyttVilkår(
                personIdent = fnr,
                vilkårType = Vilkår.UTVIDET_BARNETRYGD,
            ),
        )
        vilkårsvurdering = vilkårService.hentVilkårsvurdering(nåVærendeBehandling.id)!!
        assertEquals(BehandlingUnderkategori.UTVIDET, vilkårsvurdering.behandling.underkategori)
        assertTrue { vilkårsvurdering.personResultater.find { it.aktør.aktivFødselsnummer() == fnr }!!.vilkårResultater.size == 3 }
        val personResultat = vilkårsvurdering.personResultater.find { it.aktør.aktivFødselsnummer() == fnr }!!
        assertTrue { personResultat.vilkårResultater.any { it.vilkårType == Vilkår.UTVIDET_BARNETRYGD } }
        val utvidetBarnetrygdVilkår =
            personResultat.vilkårResultater.first { it.vilkårType == Vilkår.UTVIDET_BARNETRYGD }
        assertEquals(Resultat.IKKE_VURDERT, utvidetBarnetrygdVilkår.resultat)
        assertNull(utvidetBarnetrygdVilkår.periodeFom)
        assertNull(utvidetBarnetrygdVilkår.periodeTom)
    }

    @Test
    fun `skal ikke lage utvidet barnetrygd vilkår for ordinær behandling`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                årsak = BehandlingÅrsak.SØKNAD,
            ),
        )
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = fnr,
            barnasIdenter = listOf(barnFnr),
            barnasFødselsdatoer = listOf(LocalDate.now().minusYears(1)),
            søkerAktør = personidentService.hentOgLagreAktør(fnr, true),
            barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr), true),
        )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)
        vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(behandling, false, null)
        val exception = assertThrows<RuntimeException> {
            vilkårService.postVilkår(
                behandling.id,
                RestNyttVilkår(
                    personIdent = fnr,
                    vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                ),
            )
        }
        assertEquals(
            "${Vilkår.UTVIDET_BARNETRYGD.beskrivelse} kan ikke legges til for behandling " +
                "${behandling.id} med behandlingType ${behandling.type.visningsnavn}",
            exception.message,
        )
    }

    @Test
    fun `skal kunne legge til utvidet barnetrygd vilkår for ordinær behandling dersom det er utvidet på vilkårsvurderingen allerede`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                underkategori = BehandlingUnderkategori.UTVIDET,
                årsak = BehandlingÅrsak.SØKNAD,
            ),
        )
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = fnr,
            barnasIdenter = listOf(barnFnr),
            barnasFødselsdatoer = listOf(LocalDate.now().minusYears(1)),
            søkerAktør = personidentService.hentOgLagreAktør(fnr, true),
            barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr), true),
        )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)
        val vilkårsvurdering =
            vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(behandling, false, null)
        assertEquals(
            1,
            vilkårsvurdering.personResultater.find { it.erSøkersResultater() }?.vilkårResultater?.filter { it.vilkårType == Vilkår.UTVIDET_BARNETRYGD }?.size,
        )

        val utvidetVilkår =
            vilkårsvurdering.personResultater.find { it.erSøkersResultater() }?.vilkårResultater?.single { it.vilkårType == Vilkår.UTVIDET_BARNETRYGD }
        vilkårService.endreVilkår(
            behandlingId = behandling.id,
            vilkårId = utvidetVilkår!!.id,
            restPersonResultat = RestPersonResultat(
                personIdent = fnr,
                vilkårResultater = listOf(
                    RestVilkårResultat(
                        id = utvidetVilkår.id,
                        vilkårType = utvidetVilkår.vilkårType,
                        resultat = Resultat.OPPFYLT,
                        periodeFom = LocalDate.now().minusYears(2),
                        periodeTom = null,
                        begrunnelse = "",
                        endretAv = "",
                        endretTidspunkt = LocalDateTime.now(),
                        behandlingId = behandling.id,
                    ),
                ),
            ),
        )

        assertDoesNotThrow {
            vilkårService.postVilkår(
                behandling.id,
                RestNyttVilkår(
                    personIdent = fnr,
                    vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                ),
            )
        }
    }

    @Test
    fun `skal ikke lage utvidet barnetrygd vilkår for barn i migreringsbehandling`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()
        val forrigeVilkårsdato = LocalDate.of(2021, 8, 1)
        val barnetsFødselsdato = LocalDate.now().minusYears(1)

        val behandlinger = lagMigreringsbehandling(fnr, barnFnr, barnetsFødselsdato, forrigeVilkårsdato)
        val nåVærendeBehandling = behandlinger.second

        val nyMigreringsdato = LocalDate.of(2021, 1, 1)
        val vilkårsvurdering =
            vilkårsvurderingForNyBehandlingService.genererVilkårsvurderingForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
                behandling = nåVærendeBehandling,
                forrigeBehandlingSomErVedtatt = behandlinger.first,
                nyMigreringsdato = nyMigreringsdato,
            )
        assertTrue { vilkårsvurdering.personResultater.isNotEmpty() }
        assertTrue { vilkårsvurdering.personResultater.any { it.aktør.aktivFødselsnummer() == fnr } }
        assertTrue { vilkårsvurdering.personResultater.find { it.aktør.aktivFødselsnummer() == fnr }!!.vilkårResultater.size == 2 }
        val exception = assertThrows<RuntimeException> {
            vilkårService.postVilkår(
                nåVærendeBehandling.id,
                RestNyttVilkår(
                    personIdent = barnFnr,
                    vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                ),
            )
        }
        assertEquals("${Vilkår.UTVIDET_BARNETRYGD.beskrivelse} kan ikke legges til for BARN", exception.message)
    }

    @Test
    fun `skal ikke slette bor med søker vilkår for migreringsbehandling`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()
        val forrigeVilkårsdato = LocalDate.of(2021, 8, 1)
        val barnetsFødselsdato = LocalDate.now().minusYears(1)

        val behandlinger = lagMigreringsbehandling(fnr, barnFnr, barnetsFødselsdato, forrigeVilkårsdato)
        val behandling = behandlinger.second
        vilkårsvurderingForNyBehandlingService.genererVilkårsvurderingForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
            behandling = behandling,
            forrigeBehandlingSomErVedtatt = behandlinger.first,
            nyMigreringsdato = LocalDate.of(2021, 1, 1),
        )

        val exception = assertThrows<RuntimeException> {
            vilkårService.deleteVilkår(
                behandling.id,
                RestSlettVilkår(
                    personIdent = fnr,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                ),
            )
        }
        assertEquals(
            "Vilkår ${Vilkår.BOR_MED_SØKER.beskrivelse} kan ikke slettes " +
                "for behandling ${behandling.id}",
            exception.message,
        )
    }

    @Test
    fun `skal ikke slette utvidet barnetrygd vilkår for førstegangsbehandling`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                årsak = BehandlingÅrsak.SØKNAD,
            ),
        )
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = fnr,
            barnasIdenter = listOf(barnFnr),
            barnasFødselsdatoer = listOf(LocalDate.now().minusYears(1)),
            søkerAktør = personidentService.hentOgLagreAktør(fnr, true),
            barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr), true),
        )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)
        vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(behandling, false, null)

        val exception = assertThrows<RuntimeException> {
            vilkårService.deleteVilkår(
                behandling.id,
                RestSlettVilkår(
                    personIdent = fnr,
                    vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                ),
            )
        }
        assertEquals(
            "Vilkår ${Vilkår.UTVIDET_BARNETRYGD.beskrivelse} kan ikke slettes " +
                "for behandling ${behandling.id}",
            exception.message,
        )
    }

    @Test
    fun `skal ikke slette utvidet barnetrygd vilkår for migreringsbehandling når det finnes i forrige behandling`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()
        val forrigeVilkårsdato = LocalDate.of(2021, 8, 1)
        val barnetsFødselsdato = LocalDate.now().minusYears(1)

        val behandlinger = lagMigreringsbehandling(fnr, barnFnr, barnetsFødselsdato, forrigeVilkårsdato)
        var forrigeBehandling = behandlinger.first
        val behandling = behandlinger.second

        val forrigeVilkårvurdering = vilkårService.hentVilkårsvurdering(forrigeBehandling.id)!!
        val forrigeSøkerPersonResultat =
            forrigeVilkårvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == fnr }
        val forrigeBarnPersonResultat =
            forrigeVilkårvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == barnFnr }
        val forrigeVilkårResultat = forrigeSøkerPersonResultat.vilkårResultater
        forrigeVilkårResultat.add(
            lagVilkårResultat(
                personResultat = forrigeSøkerPersonResultat,
                vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                periodeFom = LocalDate.of(2021, 5, 1),
                periodeTom = LocalDate.of(2021, 5, 31),
                behandlingId = forrigeBehandling.id,
            ),
        )
        vilkårsvurderingService.oppdater(
            forrigeVilkårvurdering.copy(
                personResultater = setOf(
                    forrigeSøkerPersonResultat,
                    forrigeBarnPersonResultat,
                ),
            ),
        )
        forrigeBehandling = behandlingHentOgPersisterService.hent(forrigeBehandling.id)
        forrigeBehandling.behandlingStegTilstand.add(
            BehandlingStegTilstand(
                behandling = forrigeBehandling,
                behandlingSteg = StegType.BEHANDLING_AVSLUTTET,
                behandlingStegStatus = BehandlingStegStatus.UTFØRT,
            ),
        )
        behandlingHentOgPersisterService.lagreEllerOppdater(forrigeBehandling)

        val vilkårsvurdering =
            vilkårsvurderingForNyBehandlingService.genererVilkårsvurderingForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
                behandling = behandling,
                forrigeBehandlingSomErVedtatt = forrigeBehandling,
                nyMigreringsdato = LocalDate.of(2021, 1, 1),
            )
        assertTrue { vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == fnr }.vilkårResultater.size == 3 }
        val exception = assertThrows<RuntimeException> {
            vilkårService.deleteVilkår(
                behandling.id,
                RestSlettVilkår(
                    personIdent = fnr,
                    vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                ),
            )
        }
        assertEquals(
            "Vilkår ${Vilkår.UTVIDET_BARNETRYGD.beskrivelse} kan ikke slettes " +
                "for behandling ${behandling.id}",
            exception.message,
        )
    }

    @Test
    fun `skal slette utvidet barnetrygd vilkår for migreringsbehandling`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()
        val forrigeVilkårsdato = LocalDate.of(2021, 8, 1)
        val barnetsFødselsdato = LocalDate.now().minusYears(1)

        val behandlinger = lagMigreringsbehandling(fnr, barnFnr, barnetsFødselsdato, forrigeVilkårsdato)
        val behandling = behandlinger.second
        vilkårsvurderingForNyBehandlingService.genererVilkårsvurderingForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
            behandling = behandling,
            forrigeBehandlingSomErVedtatt = behandlinger.first,
            nyMigreringsdato = LocalDate.of(2021, 1, 1),
        )

        vilkårService.postVilkår(
            behandling.id,
            RestNyttVilkår(personIdent = fnr, vilkårType = Vilkår.UTVIDET_BARNETRYGD),
        )

        val vilkårsvurderingFørSlett = vilkårService.hentVilkårsvurdering(behandling.id)!!

        assertEquals(BehandlingUnderkategori.UTVIDET, vilkårsvurderingFørSlett.behandling.underkategori)
        assertTrue {
            vilkårsvurderingFørSlett
                .personResultater.first { it.aktør.aktivFødselsnummer() == fnr }.vilkårResultater.size == 3
        }

        assertTrue {
            vilkårsvurderingFørSlett
                .personResultater.first { it.aktør.aktivFødselsnummer() == fnr }
                .vilkårResultater.any { it.vilkårType == Vilkår.UTVIDET_BARNETRYGD }
        }

        vilkårService.deleteVilkår(
            behandling.id,
            RestSlettVilkår(
                personIdent = fnr,
                vilkårType = Vilkår.UTVIDET_BARNETRYGD,
            ),
        )

        val vilkårsvurderingEtterSlett = vilkårService.hentVilkårsvurdering(behandling.id)!!

        assertEquals(BehandlingUnderkategori.ORDINÆR, vilkårsvurderingEtterSlett.behandling.underkategori)
        assertTrue {
            vilkårsvurderingEtterSlett
                .personResultater.first { it.aktør.aktivFødselsnummer() == fnr }.vilkårResultater.size == 2
        }

        assertTrue {
            vilkårsvurderingEtterSlett
                .personResultater.first { it.aktør.aktivFødselsnummer() == fnr }
                .vilkårResultater.none { it.vilkårType == Vilkår.UTVIDET_BARNETRYGD }
        }
    }

    @Test
    fun `skal slette utvidet barnetrygd vilkår for helmanuell migrering`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()
        val barnetsFødselsdato = LocalDate.of(2020, 8, 15)
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = BehandlingÅrsak.HELMANUELL_MIGRERING,
            ),
        )
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = fnr,
            barnasIdenter = listOf(barnFnr),
            barnasFødselsdatoer = listOf(barnetsFødselsdato),
            søkerAktør = personidentService.hentOgLagreAktør(fnr, true),
            barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr), true),
        )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val nyMigreringsdato = LocalDate.of(2021, 1, 1)
        vilkårsvurderingForNyBehandlingService.genererVilkårsvurderingForHelmanuellMigrering(
            behandling,
            nyMigreringsdato,
        )

        vilkårService.postVilkår(
            behandling.id,
            RestNyttVilkår(personIdent = fnr, vilkårType = Vilkår.UTVIDET_BARNETRYGD),
        )

        val vilkårsvurderingFørSlett = vilkårService.hentVilkårsvurdering(behandling.id)!!

        assertEquals(BehandlingUnderkategori.UTVIDET, vilkårsvurderingFørSlett.behandling.underkategori)
        assertTrue {
            vilkårsvurderingFørSlett
                .personResultater.first { it.aktør.aktivFødselsnummer() == fnr }.vilkårResultater.size == 3
        }

        assertTrue {
            vilkårsvurderingFørSlett
                .personResultater.first { it.aktør.aktivFødselsnummer() == fnr }
                .vilkårResultater.any { it.vilkårType == Vilkår.UTVIDET_BARNETRYGD }
        }

        vilkårService.deleteVilkår(
            behandling.id,
            RestSlettVilkår(
                personIdent = fnr,
                vilkårType = Vilkår.UTVIDET_BARNETRYGD,
            ),
        )

        val vilkårsvurderingEtterSlett = vilkårService.hentVilkårsvurdering(behandling.id)!!

        assertEquals(BehandlingUnderkategori.ORDINÆR, vilkårsvurderingEtterSlett.behandling.underkategori)
        assertTrue {
            vilkårsvurderingEtterSlett
                .personResultater.first { it.aktør.aktivFødselsnummer() == fnr }.vilkårResultater.size == 2
        }

        assertTrue {
            vilkårsvurderingEtterSlett
                .personResultater.first { it.aktør.aktivFødselsnummer() == fnr }
                .vilkårResultater.none { it.vilkårType == Vilkår.UTVIDET_BARNETRYGD }
        }
    }

    @Test
    fun `skal ikke endre vilkårsvurderingsperiode før migreringsdato for migreringsbehandling`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()
        val barnetsFødselsdato = LocalDate.of(2020, 8, 1)
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = BehandlingÅrsak.HELMANUELL_MIGRERING,
            ),
        )
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = fnr,
            barnasIdenter = listOf(barnFnr),
            barnasFødselsdatoer = listOf(barnetsFødselsdato),
            søkerAktør = personidentService.hentOgLagreAktør(fnr, true),
            barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr), true),
        )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val nyMigreringsdato = LocalDate.of(2021, 1, 1)
        behandlingService.lagreNedMigreringsdato(nyMigreringsdato, behandling)
        val vilkårsvurdering = vilkårsvurderingForNyBehandlingService.genererVilkårsvurderingForHelmanuellMigrering(
            behandling,
            nyMigreringsdato,
        )

        assertTrue { vilkårsvurdering.personResultater.isNotEmpty() }
        assertTrue { vilkårsvurdering.personResultater.size == 2 }

        val søkerPersonResultat = vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == fnr }
        assertTrue { søkerPersonResultat.vilkårResultater.isNotEmpty() }
        assertTrue { søkerPersonResultat.vilkårResultater.size == 2 }
        assertTrue {
            søkerPersonResultat.vilkårResultater.all {
                it.periodeTom == null &&
                    it.periodeFom == nyMigreringsdato
            }
        }

        val barnPersonResultat = vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == barnFnr }
        assertTrue { barnPersonResultat.vilkårResultater.isNotEmpty() }
        assertTrue { barnPersonResultat.vilkårResultater.size == 5 }
        assertTrue {
            barnPersonResultat.vilkårResultater.filter { !it.vilkårType.gjelderAlltidFraBarnetsFødselsdato() }.all {
                it.periodeTom == null &&
                    it.periodeFom == nyMigreringsdato
            }
        }

        val vilkårId = barnPersonResultat.vilkårResultater.single { it.vilkårType == Vilkår.BOR_MED_SØKER }.id
        val restVilkårResultat = RestVilkårResultat(
            id = vilkårId,
            vilkårType = Vilkår.BOR_MED_SØKER,
            resultat = Resultat.OPPFYLT,
            periodeFom = LocalDate.of(2020, 10, 1),
            periodeTom = null,
            begrunnelse = "Migrering",
            endretAv = "",
            endretTidspunkt = LocalDateTime.now(),
            behandlingId = behandling.id,
        )
        val exception = assertThrows<RuntimeException> {
            vilkårService.endreVilkår(
                behandling.id,
                vilkårId,
                RestPersonResultat(
                    barnFnr,
                    listOf(restVilkårResultat),
                ),
            )
        }
        assertEquals(
            "${Vilkår.BOR_MED_SØKER} kan ikke endres før $nyMigreringsdato " +
                "for fagsak=${behandling.fagsak.id}",
            exception.message,
        )
    }

    @Test
    fun `skal sette vurderes etter basert på behandlingstema`() {
        val behandling = kjørStegprosessForFGB(
            tilSteg = StegType.BEHANDLINGSRESULTAT,
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            brevmalService = brevmalService,

        )
        var vilkårsvurdering = vilkårService.hentVilkårsvurderingThrows(behandling.id)
        assertTrue {
            vilkårsvurdering.personResultater.all { personResultat ->
                personResultat.vilkårResultater.filter {
                    it.vilkårType !in listOf(Vilkår.UNDER_18_ÅR, Vilkår.GIFT_PARTNERSKAP)
                }.all { it.vurderesEtter == Regelverk.NASJONALE_REGLER }
            }
        }

        behandlingstemaService.oppdaterBehandlingstema(
            behandling,
            BehandlingKategori.EØS,
            BehandlingUnderkategori.ORDINÆR,
        )
        vilkårsvurdering = vilkårService.hentVilkårsvurderingThrows(behandling.id)
        assertTrue {
            vilkårsvurdering.personResultater.all { personResultat ->
                personResultat.vilkårResultater.filter {
                    it.vilkårType !in listOf(Vilkår.UNDER_18_ÅR, Vilkår.GIFT_PARTNERSKAP)
                }.all { it.vurderesEtter == Regelverk.NASJONALE_REGLER }
            }
        }

        vilkårService.postVilkår(behandling.id, RestNyttVilkår(ClientMocks.barnFnr[0], Vilkår.BOR_MED_SØKER))

        vilkårsvurdering = vilkårService.hentVilkårsvurderingThrows(behandling.id)
        assertTrue {
            vilkårsvurdering.personResultater.all { personResultat ->
                personResultat.vilkårResultater.filter {
                    it.vilkårType == Vilkår.BOR_MED_SØKER && it.resultat == Resultat.IKKE_VURDERT
                }.all { it.vurderesEtter == Regelverk.EØS_FORORDNINGEN }
            }
        }
    }

    private fun lagMigreringsbehandling(
        fnr: String,
        barnFnr: String,
        barnetsFødselsdato: LocalDate,
        forrigeVilkårsdato: LocalDate,
    ): Pair<Behandling, Behandling> {
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        var forrigeBehandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = BehandlingÅrsak.MIGRERING,
            ),
        )
        val forrigePersonopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            behandlingId = forrigeBehandling.id,
            søkerPersonIdent = fnr,
            barnasIdenter = listOf(barnFnr),
            barnasFødselsdatoer = listOf(barnetsFødselsdato),
            søkerAktør = personidentService.hentOgLagreAktør(fnr, true),
            barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr), true),
        )
        persongrunnlagService.lagreOgDeaktiverGammel(forrigePersonopplysningGrunnlag)

        var forrigeVilkårsvurdering = Vilkårsvurdering(behandling = forrigeBehandling)
        val søkerPersonResultat = PersonResultat(
            vilkårsvurdering = forrigeVilkårsvurdering,
            aktør = personidentService.hentOgLagreAktør(fnr, true),
        )
        søkerPersonResultat.setSortedVilkårResultater(
            lagSøkerVilkårResultat(
                søkerPersonResultat = søkerPersonResultat,
                periodeFom = forrigeVilkårsdato,
                behandlingId = forrigeBehandling.id,
            ),
        )
        val barnPersonResultat = PersonResultat(
            vilkårsvurdering = forrigeVilkårsvurdering,
            aktør = personidentService.hentOgLagreAktør(barnFnr, true),
        )
        barnPersonResultat.setSortedVilkårResultater(
            lagBarnVilkårResultat(
                barnPersonResultat = barnPersonResultat,
                barnetsFødselsdato = barnetsFødselsdato,
                periodeFom = forrigeVilkårsdato,
                behandlingId = forrigeBehandling.id,
                flytteSak = false,
            ),
        )
        forrigeVilkårsvurdering = forrigeVilkårsvurdering.apply {
            personResultater = setOf(
                søkerPersonResultat,
                barnPersonResultat,
            )
        }
        vilkårsvurderingService.lagreNyOgDeaktiverGammel(forrigeVilkårsvurdering)

        forrigeBehandling = markerBehandlingSomAvsluttet(forrigeBehandling)

        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO,
            ),
        )
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = fnr,
            barnasIdenter = listOf(barnFnr),
            barnasFødselsdatoer = listOf(barnetsFødselsdato),
            søkerAktør = personidentService.hentOgLagreAktør(fnr, true),
            barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr), true),
        )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)
        return Pair(forrigeBehandling, behandling)
    }

    private fun lagVilkårsvurderingForEnSøkerMedEttBarn(): Vilkårsvurdering {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        var behandling =
            behandlingService.opprettBehandling(nyOrdinærBehandling(søkersIdent = fnr, fagsakId = fagsak.id))

        val forrigeBehandlingSomErIverksatt =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsakId = behandling.fagsak.id)

        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                fnr,
                listOf(barnFnr),
                søkerAktør = personidentService.hentOgLagreAktør(fnr, true),
                barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr), true),
            )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        return vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(
            behandling = behandling,
            bekreftEndringerViaFrontend = true,
            forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErIverksatt,
        )
    }

    private fun markerBehandlingSomAvsluttet(behandling: Behandling): Behandling {
        behandling.status = BehandlingStatus.AVSLUTTET
        behandling.leggTilBehandlingStegTilstand(StegType.BEHANDLING_AVSLUTTET)
        return behandlingHentOgPersisterService.lagreOgFlush(behandling)
    }

    fun Vilkår.påvirketVilkårForEndreMigreringsdato() = this in listOf(
        Vilkår.BOSATT_I_RIKET,
        Vilkår.LOVLIG_OPPHOLD,
        Vilkår.BOR_MED_SØKER,
    )
}
