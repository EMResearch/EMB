package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.ekstern.restDomene.RestEndretUtbetalingAndel
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.kontrakter.felles.Ressurs
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class EndretUtbetalingAndelTest(
    @Autowired private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    @Autowired private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
) : AbstractVerdikjedetest() {

    @Test
    fun `Skal teste at endret utbetalingsandel overskriver eksisterende utbetalingsandel`() {
        val (scenario, restUtvidetBehandling) = genererBehandlingsresultat()

        val endretFom = YearMonth.of(2021, 9)
        val endretTom = YearMonth.of(2021, 11)

        val restEndretUtbetalingAndel = RestEndretUtbetalingAndel(
            id = null,
            personIdent = scenario.barna.first().ident,
            prosent = BigDecimal(0),
            fom = endretFom,
            tom = endretTom,
            årsak = Årsak.DELT_BOSTED, avtaletidspunktDeltBosted = LocalDate.now(),
            søknadstidspunkt = LocalDate.now(),
            begrunnelse = "begrunnelse",
            erTilknyttetAndeler = true,
        )

        familieBaSakKlient().leggTilEndretUtbetalingAndel(
            restUtvidetBehandling.data!!.behandlingId,
            restEndretUtbetalingAndel,
        )

        val andelerTilkjentYtelseMedEndretPeriode =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = restUtvidetBehandling.data!!.behandlingId)

        val endretAndeleTilkjentYtelse =
            andelerTilkjentYtelseMedEndretPeriode.single { it.kalkulertUtbetalingsbeløp == 0 }

        Assertions.assertEquals(
            endretFom,
            endretAndeleTilkjentYtelse.stønadFom,
        )

        Assertions.assertEquals(
            endretTom,
            endretAndeleTilkjentYtelse.stønadTom,
        )

        val utbetalingAndeleTilkjentYtelse =
            andelerTilkjentYtelseMedEndretPeriode.filter { it.kalkulertUtbetalingsbeløp != 0 }

        Assertions.assertNotNull(
            utbetalingAndeleTilkjentYtelse.firstOrNull { it.stønadTom == endretFom.minusMonths(1) },
        )

        Assertions.assertNotNull(
            utbetalingAndeleTilkjentYtelse.firstOrNull { it.stønadFom == endretTom.plusMonths(1) },
        )
    }

    @Test
    fun `Skal teste at fjernet endret utbetalingsandel oppretter tidligere eksisterende utbetalingsandel`() {
        val (scenario, restUtvidetBehandling) = genererBehandlingsresultat()

        val endretFom = YearMonth.of(2021, 9)
        val endretTom = YearMonth.of(2021, 11)

        val restEndretUtbetalingAndel = RestEndretUtbetalingAndel(
            id = null,
            personIdent = scenario.barna.first().ident,
            prosent = BigDecimal(0),
            fom = endretFom,
            tom = endretTom,
            årsak = Årsak.DELT_BOSTED,
            avtaletidspunktDeltBosted = LocalDate.now(),
            søknadstidspunkt = LocalDate.now(),
            begrunnelse = "begrunnelse",
            erTilknyttetAndeler = true,
        )

        val restUtvidetBehandlingEtterEndretPeriode = familieBaSakKlient().leggTilEndretUtbetalingAndel(
            restUtvidetBehandling.data!!.behandlingId,
            restEndretUtbetalingAndel,
        )

        val endretUtbetalingAndelId =
            restUtvidetBehandlingEtterEndretPeriode.data!!.endretUtbetalingAndeler.first().id

        familieBaSakKlient().fjernEndretUtbetalingAndel(
            restUtvidetBehandling.data!!.behandlingId,
            endretUtbetalingAndelId!!,
        )

        val andelerTilkjentYtelseEtterFjeringAvEndretUtbetaling =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = restUtvidetBehandling.data!!.behandlingId)

        Assertions.assertNotNull(
            andelerTilkjentYtelseEtterFjeringAvEndretUtbetaling.firstOrNull { it.stønadFom == endretFom },
        )

        Assertions.assertNotNull(
            andelerTilkjentYtelseEtterFjeringAvEndretUtbetaling.firstOrNull { it.stønadTom == YearMonth.of(2021, 12) },
        )
    }

    private fun genererBehandlingsresultat(): Pair<RestScenario, Ressurs<RestUtvidetBehandling>> {
        val barnFødselsdato = LocalDate.of(2020, 1, 3)

        val scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(fødselsdato = "1996-01-12", fornavn = "Mor", etternavn = "Søker"),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = barnFødselsdato.toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen",
                        bostedsadresser = emptyList(),
                    ),
                ),
            ),
        )

        val søkersIdent = scenario.søker.ident!!

        val fagsak = familieBaSakKlient().opprettFagsak(søkersIdent = søkersIdent)
        val restFagsakMedBehandling = familieBaSakKlient().opprettBehandling(
            søkersIdent = søkersIdent,
            behandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
            fagsakId = fagsak.data!!.id,
        )

        val behandling = behandlingHentOgPersisterService.hent(restFagsakMedBehandling.data!!.behandlingId)
        val restRegistrerSøknad =
            RestRegistrerSøknad(
                søknad = lagSøknadDTO(
                    søkerIdent = scenario.søker.ident,
                    barnasIdenter = scenario.barna.map { it.ident!! },
                    underkategori = BehandlingUnderkategori.ORDINÆR,
                ),
                bekreftEndringerViaFrontend = false,
            )
        val restUtvidetBehandling: Ressurs<RestUtvidetBehandling> =
            familieBaSakKlient().registrererSøknad(
                behandlingId = behandling.id,
                restRegistrerSøknad = restRegistrerSøknad,
            )

        restUtvidetBehandling.data!!.personResultater.forEach { restPersonResultat ->
            restPersonResultat.vilkårResultater.filter { it.resultat == Resultat.IKKE_VURDERT }.forEach {
                familieBaSakKlient().putVilkår(
                    behandlingId = restUtvidetBehandling.data!!.behandlingId,
                    vilkårId = it.id,
                    restPersonResultat =
                    RestPersonResultat(
                        personIdent = restPersonResultat.personIdent,
                        vilkårResultater = listOf(
                            it.copy(
                                resultat = Resultat.OPPFYLT,
                                periodeFom = barnFødselsdato,
                                utdypendeVilkårsvurderinger = listOfNotNull(
                                    if (it.vilkårType == Vilkår.BOR_MED_SØKER) UtdypendeVilkårsvurdering.DELT_BOSTED else null,
                                ),
                            ),
                        ),
                    ),
                )
            }
        }

        familieBaSakKlient().validerVilkårsvurdering(
            behandlingId = restUtvidetBehandling.data!!.behandlingId,
        )

        val restFagsakEtterBehandlingsresultat =
            familieBaSakKlient().behandlingsresultatStegOgGåVidereTilNesteSteg(
                behandlingId = restUtvidetBehandling.data!!.behandlingId,
            )
        return Pair(scenario, restFagsakEtterBehandlingsresultat)
    }
}
