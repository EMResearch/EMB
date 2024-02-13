package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.ekstern.restDomene.RestEndretUtbetalingAndel
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
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

class EndretUtbetalingAndelMedUtvidetAndelTest(
    @Autowired private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
) : AbstractVerdikjedetest() {

    @Test
    fun `Skal teste at endret utbetalingsandeler for ordinær og utvidet endrer utbetaling for søker og barn`() {
        val barnFødselsdato = LocalDate.now().minusYears(3)

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
        val restUtvidetBehandling = familieBaSakKlient().opprettBehandling(
            søkersIdent = søkersIdent,
            behandlingUnderkategori = BehandlingUnderkategori.UTVIDET,
            fagsakId = fagsak.data!!.id,
        ).data!!

        val restRegistrerSøknad =
            RestRegistrerSøknad(
                søknad = lagSøknadDTO(
                    søkerIdent = scenario.søker.ident,
                    barnasIdenter = scenario.barna.map { it.ident!! },
                    underkategori = BehandlingUnderkategori.UTVIDET,
                ),
                bekreftEndringerViaFrontend = false,
            )
        val restBehandlingEtterRegistrertSøknad: Ressurs<RestUtvidetBehandling> =
            familieBaSakKlient().registrererSøknad(
                behandlingId = restUtvidetBehandling.behandlingId,
                restRegistrerSøknad = restRegistrerSøknad,
            )

        restBehandlingEtterRegistrertSøknad.data!!.personResultater.forEach { restPersonResultat ->
            restPersonResultat.vilkårResultater.filter { it.resultat == Resultat.IKKE_VURDERT }.forEach {
                familieBaSakKlient().putVilkår(
                    behandlingId = restBehandlingEtterRegistrertSøknad.data?.behandlingId!!,
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

        val restBehandlingEtterBehandlingsresultat = familieBaSakKlient().validerVilkårsvurdering(
            behandlingId = restBehandlingEtterRegistrertSøknad.data?.behandlingId!!,
        ).data!!

        val endretFom = barnFødselsdato.nesteMåned()
        val endretTom = endretFom.plusMonths(2)

        val restEndretUtbetalingAndelUtvidetBarnetrygd = RestEndretUtbetalingAndel(
            id = null,
            personIdent = scenario.søker.ident,
            prosent = BigDecimal(0),
            fom = endretFom,
            tom = endretTom,
            årsak = Årsak.DELT_BOSTED,
            avtaletidspunktDeltBosted = LocalDate.now(),
            søknadstidspunkt = LocalDate.now(),
            begrunnelse = "begrunnelse",
            erTilknyttetAndeler = true,
        )

        familieBaSakKlient().leggTilEndretUtbetalingAndel(
            restBehandlingEtterBehandlingsresultat.behandlingId,
            restEndretUtbetalingAndelUtvidetBarnetrygd,
        )

        val restEndretUtbetalingAndelOrdinærBarnetrygd = RestEndretUtbetalingAndel(
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

        familieBaSakKlient().leggTilEndretUtbetalingAndel(
            restBehandlingEtterBehandlingsresultat.behandlingId,
            restEndretUtbetalingAndelOrdinærBarnetrygd,
        )

        familieBaSakKlient().behandlingsresultatStegOgGåVidereTilNesteSteg(
            behandlingId = restBehandlingEtterBehandlingsresultat.behandlingId,
        )

        val andelerTilkjentYtelseMedEndretPeriode =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = restBehandlingEtterBehandlingsresultat.behandlingId)

        val endredeAndelerTilkjentYtelse =
            andelerTilkjentYtelseMedEndretPeriode.filter { it.kalkulertUtbetalingsbeløp == 0 }

        Assertions.assertEquals(
            endredeAndelerTilkjentYtelse.single { it.aktør.aktivFødselsnummer() == scenario.barna.first().ident }.stønadFom,
            endretFom,
        )

        Assertions.assertEquals(
            endredeAndelerTilkjentYtelse.single { it.aktør.aktivFødselsnummer() == scenario.barna.first().ident }.stønadTom,
            endretTom,
        )

        Assertions.assertEquals(
            endredeAndelerTilkjentYtelse.single { it.aktør.aktivFødselsnummer() == scenario.søker.ident }.stønadFom,
            endretFom,
        )

        Assertions.assertEquals(
            endredeAndelerTilkjentYtelse.single { it.aktør.aktivFødselsnummer() == scenario.søker.ident }.stønadTom,
            endretTom,
        )
    }
}
