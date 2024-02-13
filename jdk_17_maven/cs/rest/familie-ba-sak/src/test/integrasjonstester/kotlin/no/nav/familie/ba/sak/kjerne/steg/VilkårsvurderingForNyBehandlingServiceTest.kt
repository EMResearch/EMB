package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.til18ÅrsVilkårsdato
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.datagenerator.vilkårsvurdering.lagBarnVilkårResultat
import no.nav.familie.ba.sak.datagenerator.vilkårsvurdering.lagSøkerVilkårResultat
import no.nav.familie.ba.sak.datagenerator.vilkårsvurdering.lagVilkårsvurderingMedOverstyrendeResultater
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import no.nav.familie.ba.sak.kjerne.personident.Personident
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.VilkårsvurderingForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.VilkårsvurderingForNyBehandlingServiceTest.Companion.validerKopiertVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.gjelderAlltidFraBarnetsFødselsdato
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import kotlin.reflect.full.declaredMemberProperties

class VilkårsvurderingForNyBehandlingServiceTest(
    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val behandlingService: BehandlingService,

    @Autowired
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,

    @Autowired
    private val personidentService: PersonidentService,

    @Autowired
    private val persongrunnlagService: PersongrunnlagService,

    @Autowired
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,

    @Autowired
    private val vilkårsvurderingService: VilkårsvurderingService,

    @Autowired
    private val vilkårsvurderingForNyBehandlingService: VilkårsvurderingForNyBehandlingService,

    @Autowired
    private val databaseCleanupService: DatabaseCleanupService,

    @Autowired
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,

    @Autowired
    private val endretUtbetalingAndelRepository: EndretUtbetalingAndelRepository,

    @Autowired
    private val personRepository: PersonRepository,

    @Autowired
    private val aktørIdRepository: AktørIdRepository,

) : AbstractSpringIntegrationTest() {
    @BeforeAll
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `skal lage vilkårsvurderingsperiode for migrering ved flyttesak`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()
        val søkerFødselsdato = LocalDate.of(1984, 1, 14)
        val barnetsFødselsdato = LocalDate.now().minusMonths(6)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val forrigeBehandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
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
                periodeFom = søkerFødselsdato,
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
                behandlingId = forrigeBehandling.id,
                periodeFom = LocalDate.now().minusMonths(1),
                flytteSak = true,
            ),
        )

        forrigeVilkårsvurdering = forrigeVilkårsvurdering.apply {
            personResultater = setOf(
                søkerPersonResultat,
                barnPersonResultat,
            )
        }
        vilkårsvurderingService.lagreNyOgDeaktiverGammel(forrigeVilkårsvurdering)

        markerBehandlingSomAvsluttet(forrigeBehandling)

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

        val nyMigreringsdato = LocalDate.now().minusMonths(5)
        val vilkårsvurdering =
            vilkårsvurderingForNyBehandlingService.genererVilkårsvurderingForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
                behandling = behandling,
                forrigeBehandlingSomErVedtatt = forrigeBehandling,
                nyMigreringsdato = nyMigreringsdato,
            )
        Assertions.assertTrue { vilkårsvurdering.personResultater.isNotEmpty() }
        val søkerVilkårResultat =
            vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == fnr }.vilkårResultater
        Assertions.assertTrue { søkerVilkårResultat.size == 2 }
        Assertions.assertTrue {
            søkerVilkårResultat.all {
                it.periodeFom == søkerFødselsdato &&
                    it.periodeTom == null
            }
        }

        val barnVilkårResultat =
            vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == barnFnr }.vilkårResultater
        Assertions.assertTrue { barnVilkårResultat.size == 5 }
        Assertions.assertTrue {
            barnVilkårResultat.filter { it.vilkårType == Vilkår.BOR_MED_SØKER }.all {
                it.periodeFom == nyMigreringsdato &&
                    it.periodeTom == null
            }
        }
        Assertions.assertTrue {
            barnVilkårResultat.filter { it.vilkårType == Vilkår.UNDER_18_ÅR }.all {
                it.periodeFom == barnetsFødselsdato &&
                    it.periodeTom == barnetsFødselsdato.til18ÅrsVilkårsdato()
            }
        }

        Assertions.assertTrue {
            barnVilkårResultat.filter { it.vilkårType !in listOf(Vilkår.BOR_MED_SØKER, Vilkår.UNDER_18_ÅR) }.all {
                it.periodeFom == barnetsFødselsdato &&
                    it.periodeTom == null
            }
        }
    }

    @Test
    fun `skal lage vilkårsvurderingsperiode for migrering ved flere perioder`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()
        val søkerFødselsdato = LocalDate.of(1984, 8, 1)
        val barnetsFødselsdato = LocalDate.now().minusYears(10)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val forrigeBehandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
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
                periodeFom = søkerFødselsdato,
                behandlingId = forrigeBehandling.id,
            ),
        )
        val barnPersonResultat = PersonResultat(
            vilkårsvurdering = forrigeVilkårsvurdering,
            aktør = personidentService.hentOgLagreAktør(barnFnr, true),
        )
        barnPersonResultat.setSortedVilkårResultater(
            setOf(
                lagVilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.UNDER_18_ÅR,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barnetsFødselsdato,
                    periodeTom = barnetsFødselsdato.plusYears(18).minusMonths(1),
                    behandlingId = forrigeBehandling.id,
                ),
                lagVilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.GIFT_PARTNERSKAP,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barnetsFødselsdato,
                    periodeTom = null,
                    behandlingId = forrigeBehandling.id,
                ),
                lagVilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2021, 4, 14),
                    periodeTom = LocalDate.of(2021, 8, 16),
                    behandlingId = forrigeBehandling.id,
                ),
                lagVilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2021, 10, 5),
                    periodeTom = null,
                    behandlingId = forrigeBehandling.id,
                ),
                lagVilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barnetsFødselsdato,
                    periodeTom = null,
                    behandlingId = forrigeBehandling.id,
                ),
                lagVilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.LOVLIG_OPPHOLD,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barnetsFødselsdato,
                    periodeTom = null,
                    behandlingId = forrigeBehandling.id,
                ),
            ),
        )
        forrigeVilkårsvurdering = forrigeVilkårsvurdering.apply {
            personResultater = setOf(
                søkerPersonResultat,
                barnPersonResultat,
            )
        }
        vilkårsvurderingService.lagreNyOgDeaktiverGammel(forrigeVilkårsvurdering)

        markerBehandlingSomAvsluttet(forrigeBehandling)

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

        val nyMigreringsdato = LocalDate.of(2021, 1, 1)
        val vilkårsvurdering =
            vilkårsvurderingForNyBehandlingService.genererVilkårsvurderingForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
                behandling = behandling,
                forrigeBehandlingSomErVedtatt = forrigeBehandling,
                nyMigreringsdato = nyMigreringsdato,
            )
        Assertions.assertTrue { vilkårsvurdering.personResultater.isNotEmpty() }
        val søkerVilkårResultat =
            vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == fnr }.vilkårResultater
        Assertions.assertTrue { søkerVilkårResultat.size == 2 }
        Assertions.assertTrue {
            søkerVilkårResultat.all {
                it.periodeFom == søkerFødselsdato &&
                    it.periodeTom == null
            }
        }

        val barnVilkårResultat =
            vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == barnFnr }.vilkårResultater
        Assertions.assertTrue { barnVilkårResultat.size == 6 }
        Assertions.assertTrue {
            barnVilkårResultat.filter { it.vilkårType == Vilkår.BOR_MED_SØKER }.any {
                it.periodeFom == nyMigreringsdato &&
                    it.periodeTom == LocalDate.of(2021, 8, 16)
            }
        }
        Assertions.assertTrue {
            barnVilkårResultat.filter { it.vilkårType == Vilkår.BOR_MED_SØKER }.any {
                it.periodeFom == LocalDate.of(2021, 10, 5) &&
                    it.periodeTom == null
            }
        }
        Assertions.assertTrue {
            barnVilkårResultat.filter { it.vilkårType == Vilkår.UNDER_18_ÅR }.all {
                it.periodeFom == barnetsFødselsdato &&
                    it.periodeTom == barnetsFødselsdato.til18ÅrsVilkårsdato()
            }
        }
        Assertions.assertTrue {
            barnVilkårResultat.filter { it.vilkårType !in listOf(Vilkår.BOR_MED_SØKER, Vilkår.UNDER_18_ÅR) }.all {
                it.periodeFom == barnetsFødselsdato &&
                    it.periodeTom == null
            }
        }
    }

    @Test
    fun `genererVilkårsvurderingForMigreringsbehandlingMedÅrsakEndreMigreringsdato - skal ikke vurdere VilkårResultater som ikke er oppfylt fra forrige behandling ved kopiering til nye VilkårResultater`() {
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()
        val søkerFødselsdato = LocalDate.of(1984, 8, 1)
        val barnetsFødselsdato = LocalDate.now().minusYears(10)
        val nyMigreringsdato = LocalDate.now().minusYears(5)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val forrigeBehandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = BehandlingÅrsak.MIGRERING,
            ),
        )
        val forrigePersonopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            behandlingId = forrigeBehandling.id,
            søkerPersonIdent = søkerFnr,
            barnasIdenter = listOf(barnFnr),
            barnasFødselsdatoer = listOf(barnetsFødselsdato),
            søkerFødselsdato = søkerFødselsdato,
            søkerAktør = personidentService.hentOgLagreAktør(søkerFnr, true),
            barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr), true),
        )
        persongrunnlagService.lagreOgDeaktiverGammel(forrigePersonopplysningGrunnlag)

        var forrigeVilkårsvurdering = Vilkårsvurdering(behandling = forrigeBehandling)
        val søkerPersonResultat = PersonResultat(
            vilkårsvurdering = forrigeVilkårsvurdering,
            aktør = personidentService.hentOgLagreAktør(søkerFnr, true),
        )
        søkerPersonResultat.setSortedVilkårResultater(
            lagSøkerVilkårResultat(
                søkerPersonResultat = søkerPersonResultat,
                periodeFom = nyMigreringsdato.plusYears(1),
                behandlingId = forrigeBehandling.id,
            ),
        )
        val barnPersonResultat = PersonResultat(
            vilkårsvurdering = forrigeVilkårsvurdering,
            aktør = personidentService.hentOgLagreAktør(barnFnr, true),
        )
        barnPersonResultat.setSortedVilkårResultater(
            setOf(
                lagVilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.UNDER_18_ÅR,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = barnetsFødselsdato,
                    periodeTom = barnetsFødselsdato.plusYears(18).minusMonths(1),
                    behandlingId = forrigeBehandling.id,
                ),
                lagVilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.GIFT_PARTNERSKAP,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = nyMigreringsdato.plusYears(1),
                    periodeTom = null,
                    behandlingId = forrigeBehandling.id,
                ),
                lagVilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = nyMigreringsdato.plusYears(1),
                    periodeTom = nyMigreringsdato.plusYears(1).plusMonths(4),
                    behandlingId = forrigeBehandling.id,
                ),
                lagVilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    resultat = Resultat.IKKE_OPPFYLT,
                    periodeFom = null,
                    periodeTom = null,
                    behandlingId = forrigeBehandling.id,
                ),
                lagVilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = nyMigreringsdato.plusYears(1),
                    periodeTom = null,
                    behandlingId = forrigeBehandling.id,
                ),
                lagVilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.LOVLIG_OPPHOLD,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = nyMigreringsdato.plusYears(1),
                    periodeTom = null,
                    behandlingId = forrigeBehandling.id,
                ),
            ),
        )
        forrigeVilkårsvurdering = forrigeVilkårsvurdering.apply {
            personResultater = setOf(
                søkerPersonResultat,
                barnPersonResultat,
            )
        }
        vilkårsvurderingService.lagreNyOgDeaktiverGammel(forrigeVilkårsvurdering)

        markerBehandlingSomAvsluttet(forrigeBehandling)

        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO,
            ),
        )
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søkerFnr,
            barnasIdenter = listOf(barnFnr),
            barnasFødselsdatoer = listOf(barnetsFødselsdato),
            søkerFødselsdato = søkerFødselsdato,
            søkerAktør = personidentService.hentOgLagreAktør(søkerFnr, true),
            barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr), true),
        )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vilkårsvurdering =
            vilkårsvurderingForNyBehandlingService.genererVilkårsvurderingForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
                behandling = behandling,
                forrigeBehandlingSomErVedtatt = forrigeBehandling,
                nyMigreringsdato = nyMigreringsdato,
            )
        Assertions.assertTrue { vilkårsvurdering.personResultater.isNotEmpty() }
        val søkerVilkårResultat =
            vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == søkerFnr }.vilkårResultater
        Assertions.assertTrue { søkerVilkårResultat.size == 2 }
        Assertions.assertTrue {
            søkerVilkårResultat.all {
                it.periodeFom == nyMigreringsdato &&
                    it.periodeTom == null
            }
        }

        val barnVilkårResultat =
            vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == barnFnr }.vilkårResultater
        Assertions.assertTrue { barnVilkårResultat.size == 5 } // IKKE_OPPFYLT vilkår skal ikke kopieres over
        Assertions.assertTrue {
            barnVilkårResultat.filter { it.vilkårType == Vilkår.BOR_MED_SØKER }.any {
                it.periodeFom == nyMigreringsdato &&
                    it.periodeTom == nyMigreringsdato.plusYears(1).plusMonths(4)
            }
        }
        Assertions.assertTrue {
            barnVilkårResultat.filter { it.vilkårType == Vilkår.UNDER_18_ÅR }.all {
                it.periodeFom == barnetsFødselsdato &&
                    it.periodeTom == barnetsFødselsdato.til18ÅrsVilkårsdato()
            }
        }
        Assertions.assertTrue {
            barnVilkårResultat.filter { it.vilkårType == Vilkår.GIFT_PARTNERSKAP }.all {
                it.periodeFom == barnetsFødselsdato &&
                    it.periodeTom == null
            }
        }
        Assertions.assertTrue {
            barnVilkårResultat.filter {
                it.vilkårType !in listOf(
                    Vilkår.GIFT_PARTNERSKAP,
                    Vilkår.UNDER_18_ÅR,
                    Vilkår.BOR_MED_SØKER,
                )
            }.all {
                it.periodeFom == nyMigreringsdato &&
                    it.periodeTom == null
            }
        }
    }

    @Test
    fun `genererVilkårsvurderingForMigreringsbehandlingMedÅrsakEndreMigreringsdato - skal kopiere over alle felter, inkludert UtdypendeVilkårsvurdering, med unntak av fom og tom for VilkårResultatene som blir forskjøvet av ny migreringsdato`() {
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()
        val søkerFødselsdato = LocalDate.of(1984, 8, 1)
        val barnetsFødselsdato = LocalDate.now().minusYears(10)
        val nyMigreringsdato = LocalDate.now().minusYears(5)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val forrigeBehandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = BehandlingÅrsak.MIGRERING,
            ),
        )
        val forrigePersonopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            behandlingId = forrigeBehandling.id,
            søkerPersonIdent = søkerFnr,
            barnasIdenter = listOf(barnFnr),
            barnasFødselsdatoer = listOf(barnetsFødselsdato),
            søkerFødselsdato = søkerFødselsdato,
            søkerAktør = personidentService.hentOgLagreAktør(søkerFnr, true),
            barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr), true),
        )
        persongrunnlagService.lagreOgDeaktiverGammel(forrigePersonopplysningGrunnlag)

        var forrigeVilkårsvurdering = Vilkårsvurdering(behandling = forrigeBehandling)
        val søkerPersonResultat = PersonResultat(
            vilkårsvurdering = forrigeVilkårsvurdering,
            aktør = personidentService.hentOgLagreAktør(søkerFnr, true),
        )
        val deltBostedTom = nyMigreringsdato.plusYears(2)
        val deltBostedBegrunnelse = "Dette er en kopiert begrunnelse"
        søkerPersonResultat.setSortedVilkårResultater(
            lagSøkerVilkårResultat(
                søkerPersonResultat = søkerPersonResultat,
                periodeFom = nyMigreringsdato.plusYears(1),
                behandlingId = forrigeBehandling.id,
            ).plus(
                lagVilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                    periodeFom = nyMigreringsdato.plusMonths(5),
                    periodeTom = deltBostedTom,
                    behandlingId = forrigeBehandling.id,
                    begrunnelse = deltBostedBegrunnelse,
                    utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED),
                ),
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
                behandlingId = forrigeBehandling.id,
                periodeFom = nyMigreringsdato.plusMonths(5),
            ),
        )
        forrigeVilkårsvurdering = forrigeVilkårsvurdering.apply {
            personResultater = setOf(
                søkerPersonResultat,
                barnPersonResultat,
            )
        }
        vilkårsvurderingService.lagreNyOgDeaktiverGammel(forrigeVilkårsvurdering)

        markerBehandlingSomAvsluttet(forrigeBehandling)

        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO,
            ),
        )
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søkerFnr,
            barnasIdenter = listOf(barnFnr),
            barnasFødselsdatoer = listOf(barnetsFødselsdato),
            søkerFødselsdato = søkerFødselsdato,
            søkerAktør = personidentService.hentOgLagreAktør(søkerFnr, true),
            barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr), true),
        )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vilkårsvurdering =
            vilkårsvurderingForNyBehandlingService.genererVilkårsvurderingForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
                behandling = behandling,
                forrigeBehandlingSomErVedtatt = forrigeBehandling,
                nyMigreringsdato = nyMigreringsdato,
            )
        assertThat(vilkårsvurdering.personResultater).isNotEmpty
        val søkerVilkårResultat =
            vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == søkerFnr }.vilkårResultater
        assertThat(søkerVilkårResultat).hasSize(3)
        val utvidetBarnetrygdVilkår = søkerVilkårResultat.single { it.vilkårType == Vilkår.UTVIDET_BARNETRYGD }
        assertThat(utvidetBarnetrygdVilkår.utdypendeVilkårsvurderinger.single()).isEqualTo(
            UtdypendeVilkårsvurdering.DELT_BOSTED,
        )
        assertThat(utvidetBarnetrygdVilkår.periodeFom).isEqualTo(nyMigreringsdato)
        assertThat(utvidetBarnetrygdVilkår.periodeTom).isEqualTo(deltBostedTom)
        assertThat(utvidetBarnetrygdVilkår.begrunnelse).isEqualTo(deltBostedBegrunnelse)
    }

    @Test
    fun `genererVilkårsvurderingForMigreringsbehandlingMedÅrsakEndreMigreringsdato - skal kunne identifisere og kopiere forkjøvede vilkår som alltid starter fra fødselsdato når aktør har fått ny fødselsdato siden forrige behandling`() {
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()
        val søkerFødselsdato = LocalDate.of(1984, 8, 1)
        val barnetsFødselsdato = LocalDate.now().minusYears(10)
        val nyMigreringsdato = LocalDate.now().minusYears(5)

        val søkerAktør = personidentService.hentOgLagreAktør(søkerFnr, true)
        val barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr), true)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val forrigeBehandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = BehandlingÅrsak.MIGRERING,
            ),
        )
        val forrigePersonopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            behandlingId = forrigeBehandling.id,
            søkerPersonIdent = søkerFnr,
            barnasIdenter = listOf(barnFnr),
            barnasFødselsdatoer = listOf(barnetsFødselsdato),
            søkerFødselsdato = søkerFødselsdato,
            søkerAktør = søkerAktør,
            barnAktør = barnAktør,
        )
        persongrunnlagService.lagreOgDeaktiverGammel(forrigePersonopplysningGrunnlag)

        var forrigeVilkårsvurdering = Vilkårsvurdering(behandling = forrigeBehandling)
        val søkerPersonResultat = PersonResultat(
            vilkårsvurdering = forrigeVilkårsvurdering,
            aktør = personidentService.hentOgLagreAktør(søkerFnr, true),
        )
        val deltBostedTom = nyMigreringsdato.plusYears(2)
        val deltBostedBegrunnelse = "Dette er en kopiert begrunnelse"
        søkerPersonResultat.setSortedVilkårResultater(
            lagSøkerVilkårResultat(
                søkerPersonResultat = søkerPersonResultat,
                periodeFom = nyMigreringsdato.plusYears(1),
                behandlingId = forrigeBehandling.id,
            ).plus(
                lagVilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                    periodeFom = nyMigreringsdato.plusMonths(5),
                    periodeTom = deltBostedTom,
                    behandlingId = forrigeBehandling.id,
                    begrunnelse = deltBostedBegrunnelse,
                    utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED),
                ),
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
                behandlingId = forrigeBehandling.id,
                periodeFom = nyMigreringsdato.plusMonths(5),
            ),
        )
        forrigeVilkårsvurdering = forrigeVilkårsvurdering.apply {
            personResultater = setOf(
                søkerPersonResultat,
                barnPersonResultat,
            )
        }
        vilkårsvurderingService.lagreNyOgDeaktiverGammel(forrigeVilkårsvurdering)

        markerBehandlingSomAvsluttet(forrigeBehandling)

        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                årsak = BehandlingÅrsak.ENDRE_MIGRERINGSDATO,
            ),
        )
        // val oppdatertBarnAktør = listOf(leggTilNyIdentPåAktør(barnAktør.first(), randomFnr()))
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søkerFnr,
            barnasIdenter = listOf(barnFnr),
            // Justerer barnets fødselsdato slik at eksisterende vilkår for UNDER_18 og GIFT_PARNTERSKAP får "feil" fom og tom
            barnasFødselsdatoer = listOf(barnetsFødselsdato.minusMonths(2)),
            søkerFødselsdato = søkerFødselsdato,
            søkerAktør = søkerAktør,
            barnAktør = barnAktør,
        )
        persongrunnlagService.lagreOgDeaktiverGammel(personopplysningGrunnlag)

        val vilkårsvurdering =
            vilkårsvurderingForNyBehandlingService.genererVilkårsvurderingForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
                behandling = behandling,
                forrigeBehandlingSomErVedtatt = forrigeBehandling,
                nyMigreringsdato = nyMigreringsdato,
            )
        assertThat(vilkårsvurdering.personResultater).isNotEmpty
        val søkerVilkårResultat =
            vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == søkerFnr }.vilkårResultater
        assertThat(søkerVilkårResultat).hasSize(3)
        val barnVilkårResultat =
            vilkårsvurdering.personResultater.first {
                it.aktør.aktivFødselsnummer() == barnAktør.first().aktivFødselsnummer()
            }.vilkårResultater
        assertThat(barnVilkårResultat).hasSize(5)
        assertThat(
            barnVilkårResultat.filter { it.vilkårType.gjelderAlltidFraBarnetsFødselsdato() }
                .all { it.periodeFom == personopplysningGrunnlag.barna.first().fødselsdato },
        )
    }

    @Test
    fun `skal lage vilkårsvurderingsperiode for helmanuell migrering`() {
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
        val vilkårsvurdering = vilkårsvurderingForNyBehandlingService.genererVilkårsvurderingForHelmanuellMigrering(
            behandling,
            nyMigreringsdato,
        )

        Assertions.assertTrue { vilkårsvurdering.personResultater.isNotEmpty() }
        Assertions.assertTrue { vilkårsvurdering.personResultater.size == 2 }

        val søkerPersonResultat = vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == fnr }
        Assertions.assertTrue { søkerPersonResultat.vilkårResultater.isNotEmpty() }
        Assertions.assertTrue { søkerPersonResultat.vilkårResultater.size == 2 }
        Assertions.assertTrue {
            søkerPersonResultat.vilkårResultater.all {
                it.periodeTom == null &&
                    it.periodeFom == nyMigreringsdato
            }
        }

        val barnPersonResultat = vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == barnFnr }
        Assertions.assertTrue { barnPersonResultat.vilkårResultater.isNotEmpty() }
        Assertions.assertTrue { barnPersonResultat.vilkårResultater.size == 5 }
        Assertions.assertTrue {
            barnPersonResultat.vilkårResultater.any {
                it.vilkårType == Vilkår.UNDER_18_ÅR &&
                    it.periodeTom == barnetsFødselsdato.til18ÅrsVilkårsdato() &&
                    it.periodeFom == barnetsFødselsdato
            }
        }
        Assertions.assertTrue {
            barnPersonResultat.vilkårResultater.any {
                it.vilkårType == Vilkår.GIFT_PARTNERSKAP &&
                    it.periodeTom == null &&
                    it.periodeFom == barnetsFødselsdato
            }
        }
        Assertions.assertTrue {
            barnPersonResultat.vilkårResultater.filter { !it.vilkårType.gjelderAlltidFraBarnetsFødselsdato() }.all {
                it.periodeTom == null &&
                    it.periodeFom == nyMigreringsdato
            }
        }
    }

    @Test
    fun `skal lage vilkårsvurderingsperiode for helmanuell migrering med migreringsdato før barnetsfødselsdato`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()
        val barnetsFødselsdato = LocalDate.of(2021, 8, 15)
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
        val vilkårsvurdering = vilkårsvurderingForNyBehandlingService.genererVilkårsvurderingForHelmanuellMigrering(
            behandling,
            nyMigreringsdato,
        )

        Assertions.assertTrue { vilkårsvurdering.personResultater.isNotEmpty() }
        Assertions.assertTrue { vilkårsvurdering.personResultater.size == 2 }

        val søkerPersonResultat = vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == fnr }
        Assertions.assertTrue { søkerPersonResultat.vilkårResultater.isNotEmpty() }
        Assertions.assertTrue { søkerPersonResultat.vilkårResultater.size == 2 }
        Assertions.assertTrue {
            søkerPersonResultat.vilkårResultater.all {
                it.periodeTom == null &&
                    it.periodeFom == nyMigreringsdato
            }
        }

        val barnPersonResultat = vilkårsvurdering.personResultater.first { it.aktør.aktivFødselsnummer() == barnFnr }
        Assertions.assertTrue { barnPersonResultat.vilkårResultater.isNotEmpty() }
        Assertions.assertTrue { barnPersonResultat.vilkårResultater.size == 5 }
        Assertions.assertTrue {
            barnPersonResultat.vilkårResultater.any {
                it.vilkårType == Vilkår.UNDER_18_ÅR &&
                    it.periodeTom == barnetsFødselsdato.til18ÅrsVilkårsdato() &&
                    it.periodeFom == barnetsFødselsdato
            }
        }
        Assertions.assertTrue {
            barnPersonResultat.vilkårResultater.any {
                it.vilkårType == Vilkår.GIFT_PARTNERSKAP &&
                    it.periodeTom == null &&
                    it.periodeFom == barnetsFødselsdato
            }
        }
        Assertions.assertTrue {
            barnPersonResultat.vilkårResultater.filter { !it.vilkårType.gjelderAlltidFraBarnetsFødselsdato() }.all {
                it.periodeTom == null &&
                    it.periodeFom == barnetsFødselsdato
            }
        }
    }

    @Test
    fun `skal kopiere vilkårsvurdering og endrede utbetalingsandeler fra forrige behandling ved satsendring`() {
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()
        val søkerAktør = personidentService.hentOgLagreAktør(søkerFnr, true)
        val barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr), true)
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                årsak = BehandlingÅrsak.SØKNAD,
            ),
        )

        val personopplysningGrunnlag = persongrunnlagService.lagreOgDeaktiverGammel(
            PersonopplysningGrunnlag(
                behandlingId = behandling.id,
            ),
        )

        val søker = personRepository.saveAndFlush(
            lagPerson(
                personIdent = PersonIdent(søkerFnr),
                type = PersonType.SØKER,
                aktør = søkerAktør,
                personopplysningGrunnlag = personopplysningGrunnlag,
            ),
        )
        val barn = personRepository.saveAndFlush(
            lagPerson(
                personIdent = PersonIdent(barnFnr),
                type = PersonType.BARN,
                aktør = barnAktør[0],
                personopplysningGrunnlag = personopplysningGrunnlag,
            ),
        )

        val vilkårsvurdering =
            lagVilkårsvurderingMedOverstyrendeResultater(
                søker = søker,
                barna = listOf(barn),
                behandling = behandling,
                overstyrendeVilkårResultater = emptyMap(),
            )

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering)

        val endredeUtbetalingsAndeler = listOf(
            lagEndretUtbetalingAndel(
                behandlingId = behandling.id,
                barn = barn,
                fom = YearMonth.now(),
                tom = YearMonth.now().plusMonths(6),
                prosent = 50,
            ),
            lagEndretUtbetalingAndel(
                behandlingId = behandling.id,
                barn = barn,
                fom = YearMonth.now().plusMonths(7),
                tom = YearMonth.now().plusMonths(12),
                prosent = 100,
            ),
        )

        endretUtbetalingAndelRepository.saveAllAndFlush(endredeUtbetalingsAndeler)

        tilkjentYtelseRepository.saveAndFlush(lagInitiellTilkjentYtelse(behandling, ""))

        markerBehandlingSomAvsluttet(behandling)

        val behandling2 = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(
                fagsak = fagsak,
                behandlingType = BehandlingType.REVURDERING,
                årsak = BehandlingÅrsak.SATSENDRING,
            ),
        )

        val personopplysningGrunnlagB2 = persongrunnlagService.lagreOgDeaktiverGammel(
            PersonopplysningGrunnlag(
                behandlingId = behandling2.id,
            ),
        )
        val søkerB2 = lagPerson(
            personIdent = PersonIdent(søkerFnr),
            type = PersonType.SØKER,
            aktør = søkerAktør,
            personopplysningGrunnlag = personopplysningGrunnlagB2,
        )
        val barnB2 = lagPerson(
            personIdent = PersonIdent(barnFnr),
            type = PersonType.BARN,
            aktør = barnAktør[0],
            personopplysningGrunnlag = personopplysningGrunnlagB2,
        )

        personopplysningGrunnlagB2.personer.addAll(listOf(søkerB2, barnB2))
        personopplysningGrunnlagRepository.save(personopplysningGrunnlagB2)

        val forventetVilkårsvurdering =
            lagVilkårsvurderingMedOverstyrendeResultater(
                søker = søkerB2,
                barna = listOf(barnB2),
                behandling = behandling2,
                overstyrendeVilkårResultater = emptyMap(),
            )

        vilkårsvurderingForNyBehandlingService.opprettVilkårsvurderingUtenomHovedflyt(behandling2, behandling)

        val kopiertVilkårsvurdering = vilkårsvurderingForNyBehandlingService.hentVilkårsvurderingThrows(behandling2.id)

        val kopiertEndredeUtbetalingsandeler = endretUtbetalingAndelRepository.findByBehandlingId(behandling2.id)

        val baseEntitetFelter =
            BaseEntitet::class.declaredMemberProperties.map { it.name }.toTypedArray()

        assertThat(kopiertEndredeUtbetalingsandeler.size).isEqualTo(endredeUtbetalingsAndeler.size)
        assertThat(kopiertEndredeUtbetalingsandeler).usingRecursiveFieldByFieldElementComparatorIgnoringFields(
            "id",
            "behandlingId",
            "person",
            *baseEntitetFelter,
        )
            .isEqualTo(endredeUtbetalingsAndeler)

        assertThat(kopiertEndredeUtbetalingsandeler.all { kopiertEua -> endredeUtbetalingsAndeler.any { eua -> eua.person!!.aktør.aktørId == kopiertEua.person!!.aktør.aktørId } }).isTrue

        validerKopiertVilkårsvurdering(kopiertVilkårsvurdering, vilkårsvurdering, forventetVilkårsvurdering)
    }

    private fun markerBehandlingSomAvsluttet(behandling: Behandling): Behandling {
        behandling.status = BehandlingStatus.AVSLUTTET
        behandling.leggTilBehandlingStegTilstand(StegType.BEHANDLING_AVSLUTTET)
        return behandlingHentOgPersisterService.lagreOgFlush(behandling)
    }

    private fun leggTilNyIdentPåAktør(aktør: Aktør, nyttFnr: String): Aktør {
        aktør.personidenter.filter { it.aktiv }.map {
            it.aktiv = false
            it.gjelderTil = LocalDateTime.now()
        }
        val oppdatertAktør = aktørIdRepository.saveAndFlush(aktør)
        oppdatertAktør.personidenter.add(
            Personident(fødselsnummer = nyttFnr, aktør = oppdatertAktør),
        )
        return aktørIdRepository.saveAndFlush(oppdatertAktør)
    }
}
