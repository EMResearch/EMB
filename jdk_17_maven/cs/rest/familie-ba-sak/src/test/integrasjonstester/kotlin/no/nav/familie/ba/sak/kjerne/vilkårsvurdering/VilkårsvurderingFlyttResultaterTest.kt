package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagPersonResultat
import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.behandling.kjørStegprosessForBehandling
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class VilkårsvurderingFlyttResultaterTest(
    @Autowired
    private val vilkårsvurderingService: VilkårsvurderingService,

    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val databaseCleanupService: DatabaseCleanupService,

    @Autowired
    private val personidentService: PersonidentService,

    @Autowired
    private val persongrunnlagService: PersongrunnlagService,

    @Autowired
    private val vedtakService: VedtakService,

    @Autowired
    private val stegService: StegService,

    @Autowired
    private val vedtaksperiodeService: VedtaksperiodeService,

    @Autowired
    private val endretUtbetalingAndelHentOgPersisterService: EndretUtbetalingAndelHentOgPersisterService,

    @Autowired
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,

    @Autowired
    private val brevmalService: BrevmalService,

    @Autowired
    private val featureToggleService: FeatureToggleService,

) : AbstractSpringIntegrationTest() {

    @BeforeAll
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `Skal ikke endre på forrige behandling sin vilkårsvurdering ved flytting av resultater`() {
        val søker = lagPerson(type = PersonType.SØKER)

        val barn1Fnr = ClientMocks.barnFnr[0]
        val barn1Aktør = personidentService.hentAktør(barn1Fnr)

        val barn2Fnr = ClientMocks.barnFnr[1]
        val barn2Aktør = personidentService.hentAktør(barn2Fnr)

        // Lager førstegangsbehandling med utvidet vilkåret avslått
        val vilkårsvurderingMedUtvidetAvslått = Vilkårsvurdering(behandling = lagBehandling())
        val søkerPersonResultat = lagPersonResultat(
            vilkårsvurdering = vilkårsvurderingMedUtvidetAvslått,
            person = søker,
            periodeFom = LocalDate.now().minusMonths(8),
            periodeTom = LocalDate.now().plusYears(2),
            lagFullstendigVilkårResultat = true,
            personType = PersonType.SØKER,
            resultat = Resultat.OPPFYLT,
        )

        søkerPersonResultat.addVilkårResultat(
            lagVilkårResultat(
                personResultat = søkerPersonResultat,
                vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                resultat = Resultat.IKKE_OPPFYLT,
                periodeFom = LocalDate.now().minusMonths(8),
                periodeTom = LocalDate.now().plusYears(2),
                behandlingId = vilkårsvurderingMedUtvidetAvslått.behandling.id,
            ),
        )

        val førstegangsbehandlingPersonResultater = setOf(
            søkerPersonResultat,
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurderingMedUtvidetAvslått,
                person = lagPerson(type = PersonType.BARN, aktør = barn1Aktør, fødselsdato = ClientMocks.personInfo[barn1Fnr]!!.fødselsdato),
                periodeFom = LocalDate.now().minusMonths(8),
                periodeTom = LocalDate.now().plusYears(2),
                lagFullstendigVilkårResultat = true,
                personType = PersonType.BARN,
                resultat = Resultat.OPPFYLT,
            ),
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurderingMedUtvidetAvslått,
                person = lagPerson(type = PersonType.BARN, aktør = barn2Aktør, fødselsdato = ClientMocks.personInfo[barn2Fnr]!!.fødselsdato),
                periodeFom = LocalDate.now().minusMonths(8),
                periodeTom = LocalDate.now().plusYears(2),
                lagFullstendigVilkårResultat = true,
                personType = PersonType.BARN,
                resultat = Resultat.OPPFYLT,
            ),
        )

        vilkårsvurderingMedUtvidetAvslått.personResultater = førstegangsbehandlingPersonResultater

        val førstegangsbehandling = kjørStegprosessForBehandling(
            søkerFnr = søker.aktør.aktivFødselsnummer(),
            barnasIdenter = listOf(barn1Fnr, barn2Fnr),
            vedtakService = vedtakService,
            underkategori = BehandlingUnderkategori.UTVIDET,
            behandlingÅrsak = BehandlingÅrsak.SØKNAD,
            overstyrendeVilkårsvurdering = vilkårsvurderingMedUtvidetAvslått,
            behandlingstype = BehandlingType.FØRSTEGANGSBEHANDLING,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            endretUtbetalingAndelHentOgPersisterService = endretUtbetalingAndelHentOgPersisterService,
            fagsakService = fagsakService,
            persongrunnlagService = persongrunnlagService,
            andelerTilkjentYtelseOgEndreteUtbetalingerService = andelerTilkjentYtelseOgEndreteUtbetalingerService,
            brevmalService = brevmalService,
        )

        val vilkårsvurderingFraForrigeBehandlingFørNyRevurdering =
            vilkårsvurderingService.hentAktivForBehandling(behandlingId = førstegangsbehandling.id)

        // Lager revurdering når utvidet ikke løper, så underkategorien er ordinær
        kjørStegprosessForBehandling(
            tilSteg = StegType.REGISTRERE_PERSONGRUNNLAG,
            søkerFnr = søker.aktør.aktivFødselsnummer(),
            barnasIdenter = listOf(barn1Fnr, barn2Fnr),
            vedtakService = vedtakService,
            underkategori = BehandlingUnderkategori.ORDINÆR,
            behandlingÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
            overstyrendeVilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling()),
            behandlingstype = BehandlingType.REVURDERING,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            endretUtbetalingAndelHentOgPersisterService = endretUtbetalingAndelHentOgPersisterService,
            fagsakService = fagsakService,
            persongrunnlagService = persongrunnlagService,
            andelerTilkjentYtelseOgEndreteUtbetalingerService = andelerTilkjentYtelseOgEndreteUtbetalingerService,
            brevmalService = brevmalService,
        )

        // Sjekker at vilkårsvurderingen fra forrige behandling ikke er endret
        val vilkårsvurderingFraForrigeBehandlingEtterNyRevurdering =
            vilkårsvurderingService.hentAktivForBehandling(behandlingId = førstegangsbehandling.id)

        val søkersVilkår =
            vilkårsvurderingFraForrigeBehandlingEtterNyRevurdering?.personResultater?.find { it.erSøkersResultater() }?.vilkårResultater
        Assertions.assertEquals(
            3,
            søkersVilkår?.size,
        )
        Assertions.assertEquals(søkerPersonResultat.vilkårResultater, søkersVilkår)

        Assertions.assertEquals(
            5,
            vilkårsvurderingFraForrigeBehandlingEtterNyRevurdering?.personResultater?.find { it.aktør.aktivFødselsnummer() == barn1Fnr }?.vilkårResultater?.size,
        )
        Assertions.assertEquals(
            vilkårsvurderingMedUtvidetAvslått.personResultater.find { it.aktør.aktivFødselsnummer() == barn1Fnr }?.vilkårResultater,
            vilkårsvurderingFraForrigeBehandlingEtterNyRevurdering?.personResultater?.find { it.aktør.aktivFødselsnummer() == barn1Fnr }?.vilkårResultater,
        )

        Assertions.assertEquals(
            5,
            vilkårsvurderingFraForrigeBehandlingEtterNyRevurdering?.personResultater?.find { it.aktør.aktivFødselsnummer() == barn2Fnr }?.vilkårResultater?.size,
        )
        Assertions.assertEquals(
            vilkårsvurderingMedUtvidetAvslått.personResultater.find { it.aktør.aktivFødselsnummer() == barn2Fnr }?.vilkårResultater,
            vilkårsvurderingFraForrigeBehandlingEtterNyRevurdering?.personResultater?.find { it.aktør.aktivFødselsnummer() == barn2Fnr }?.vilkårResultater,
        )

        Assertions.assertEquals(
            vilkårsvurderingFraForrigeBehandlingFørNyRevurdering?.id,
            vilkårsvurderingFraForrigeBehandlingEtterNyRevurdering?.id,
        )
    }
}
