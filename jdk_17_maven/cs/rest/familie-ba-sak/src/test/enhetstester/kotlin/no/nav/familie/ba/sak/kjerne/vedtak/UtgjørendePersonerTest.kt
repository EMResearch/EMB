package no.nav.familie.ba.sak.kjerne.vedtak

import io.mockk.mockk
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagTriggesAv
import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertPersonResultat
import no.nav.familie.ba.sak.kjerne.brev.hentPersonerForAlleUtgjørendeVilkår
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilMinimertPerson
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UtgjørendePersonerTest {

    private val featureToggleService: FeatureToggleService = mockk()

    @Test
    fun `Skal hente riktige personer fra vilkårsvurderingen basert på innvilgelsesbegrunnelse`() {
        val søkerFnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()

        val søkerAktørId = tilAktør(søkerFnr)
        val barn1AktørId = tilAktør(barn1Fnr)

        val behandling = lagBehandling()
        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr, listOf(barn1Fnr, barn2Fnr))

        val vilkårsvurdering = Vilkårsvurdering(
            behandling = behandling,
        )

        val søkerPersonResultat =
            PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = søkerAktørId)
        søkerPersonResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.LOVLIG_OPPHOLD,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2009, 12, 24),
                    periodeTom = LocalDate.of(2010, 6, 1),
                    begrunnelse = "",
                    sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                ),
                VilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2008, 12, 24),
                    periodeTom = LocalDate.of(2010, 6, 1),
                    begrunnelse = "",
                    sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                ),
            ),
        )

        val barn1PersonResultat =
            PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1AktørId)

        barn1PersonResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = barn1PersonResultat,
                    vilkårType = Vilkår.LOVLIG_OPPHOLD,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2009, 12, 24),
                    periodeTom = LocalDate.of(2010, 6, 1),
                    begrunnelse = "",
                    sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                ),
                VilkårResultat(
                    personResultat = barn1PersonResultat,
                    vilkårType = Vilkår.GIFT_PARTNERSKAP,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2009, 11, 24),
                    periodeTom = LocalDate.of(2010, 6, 1),
                    begrunnelse = "",
                    sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                ),
                VilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2009, 12, 24),
                    periodeTom = LocalDate.of(2010, 6, 1),
                    begrunnelse = "",
                    sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                ),
            ),
        )

        val barn2PersonResultat =
            PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1AktørId)

        barn2PersonResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = barn1PersonResultat,
                    vilkårType = Vilkår.LOVLIG_OPPHOLD,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2010, 2, 24),
                    periodeTom = LocalDate.of(2010, 6, 1),
                    begrunnelse = "",
                    sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                ),
                VilkårResultat(
                    personResultat = barn1PersonResultat,
                    vilkårType = Vilkår.GIFT_PARTNERSKAP,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2009, 11, 24),
                    periodeTom = LocalDate.of(2010, 6, 1),
                    begrunnelse = "",
                    sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                ),
            ),
        )

        vilkårsvurdering.personResultater = setOf(søkerPersonResultat, barn1PersonResultat, barn2PersonResultat)

        val personerMedUtgjørendeVilkårLovligOpphold = hentPersonerForAlleUtgjørendeVilkår(
            minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
            vedtaksperiode = Periode(
                fom = LocalDate.of(2010, 1, 1),
                tom = LocalDate.of(2010, 6, 1),
            ),
            oppdatertBegrunnelseType = VedtakBegrunnelseType.INNVILGET,
            triggesAv = lagTriggesAv(vilkår = setOf(Vilkår.LOVLIG_OPPHOLD)),
            aktuellePersonerForVedtaksperiode = personopplysningGrunnlag.personer.toList()
                .map { it.tilMinimertPerson() },
            erFørsteVedtaksperiodePåFagsak = false,

            begrunnelse = Standardbegrunnelse.INNVILGET_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE,
        )

        assertEquals(2, personerMedUtgjørendeVilkårLovligOpphold.size)
        assertEquals(
            listOf(søkerFnr, barn1Fnr).sorted(),
            personerMedUtgjørendeVilkårLovligOpphold.map { it.personIdent }.sorted(),
        )

        val personerMedUtgjørendeVilkårBosattIRiket = hentPersonerForAlleUtgjørendeVilkår(
            minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
            vedtaksperiode = Periode(
                fom = LocalDate.of(2010, 1, 1),
                tom = LocalDate.of(2010, 6, 1),
            ),
            oppdatertBegrunnelseType = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET.vedtakBegrunnelseType,
            triggesAv = lagTriggesAv(vilkår = setOf(Vilkår.BOSATT_I_RIKET)),
            aktuellePersonerForVedtaksperiode = personopplysningGrunnlag.personer.toList()
                .map { it.tilMinimertPerson() },
            erFørsteVedtaksperiodePåFagsak = false,

            begrunnelse = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET,
        )

        assertEquals(1, personerMedUtgjørendeVilkårBosattIRiket.size)
        assertEquals(barn1Fnr, personerMedUtgjørendeVilkårBosattIRiket.first().personIdent)
    }

    @Test
    fun `Skal hente riktige personer fra vilkårsvurderingen basert på reduksjon og opphørsbegrunnelser`() {
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()
        val barn2Fnr = randomFnr()

        val barnAktørId = tilAktør(barnFnr)
        val barn2AktørId = tilAktør(barn2Fnr)

        val behandling = lagBehandling()
        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                søkerFnr,
                listOf(barnFnr, barn2Fnr),
                barnasFødselsdatoer = listOf(LocalDate.of(2010, 12, 24), LocalDate.of(2010, 12, 24)),
            )

        val vilkårsvurdering = Vilkårsvurdering(
            behandling = behandling,
        )

        val barnPersonResultat =
            PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barnAktørId)

        barnPersonResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2010, 12, 24),
                    periodeTom = LocalDate.of(2021, 3, 31),
                    begrunnelse = "",
                    sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                ),
            ),
        )

        val barn2PersonResultat =
            PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn2AktørId)

        barn2PersonResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = barn2PersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2010, 12, 24),
                    periodeTom = LocalDate.of(2021, 1, 31),
                    begrunnelse = "",
                    sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                ),
            ),
        )

        vilkårsvurdering.personResultater = setOf(barnPersonResultat, barn2PersonResultat)

        val personerMedUtgjørendeVilkårBosattIRiket = hentPersonerForAlleUtgjørendeVilkår(
            minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
            vedtaksperiode = Periode(
                fom = LocalDate.of(2021, 2, 1),
                tom = TIDENES_ENDE,
            ),
            oppdatertBegrunnelseType = Standardbegrunnelse.REDUKSJON_BOSATT_I_RIKTET.vedtakBegrunnelseType,
            triggesAv = lagTriggesAv(vilkår = setOf(Vilkår.BOSATT_I_RIKET)),
            aktuellePersonerForVedtaksperiode = personopplysningGrunnlag.personer.toList()
                .map { it.tilMinimertPerson() },
            erFørsteVedtaksperiodePåFagsak = false,

            begrunnelse = Standardbegrunnelse.REDUKSJON_BOSATT_I_RIKTET,
        )

        assertEquals(1, personerMedUtgjørendeVilkårBosattIRiket.size)
        assertEquals(
            barn2Fnr,
            personerMedUtgjørendeVilkårBosattIRiket.first().personIdent,
        )

        val personerMedUtgjørendeVilkårBarnUtvandret = hentPersonerForAlleUtgjørendeVilkår(
            minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
            vedtaksperiode = Periode(
                fom = LocalDate.of(2021, 4, 1),
                tom = TIDENES_ENDE,
            ),
            oppdatertBegrunnelseType = Standardbegrunnelse.OPPHØR_UTVANDRET.vedtakBegrunnelseType,
            triggesAv = lagTriggesAv(vilkår = setOf(Vilkår.BOSATT_I_RIKET)),
            aktuellePersonerForVedtaksperiode = personopplysningGrunnlag.personer.toList()
                .map { it.tilMinimertPerson() },
            erFørsteVedtaksperiodePåFagsak = false,

            begrunnelse = Standardbegrunnelse.OPPHØR_UTVANDRET,
        )

        assertEquals(1, personerMedUtgjørendeVilkårBarnUtvandret.size)
        assertEquals(
            barnFnr,
            personerMedUtgjørendeVilkårBarnUtvandret.first().personIdent,
        )
    }

    @Test
    fun `Skal kun hente medlemskapsbegrunnelser ved medlemskap og ikke hente medlemskapsbegrunnelser ellers`() {
        val søkerFnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()

        val barn1AktørId = tilAktør(barn1Fnr)
        val barn2AktørId = tilAktør(barn2Fnr)

        val behandling = lagBehandling()
        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                søkerFnr,
                listOf(barn1Fnr, barn2Fnr),
            )

        val vilkårsvurdering = Vilkårsvurdering(
            behandling = behandling,
        )

        val barn1PersonResultat =
            PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1AktørId)
        val barn2PersonResultat =
            PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn2AktørId)

        barn1PersonResultat.setSortedVilkårResultater(
            setOf(
                lagVilkårResultat(
                    barn1PersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    periodeFom = LocalDate.of(2021, 11, 1),
                    utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP),
                ),
            ),
        )
        barn2PersonResultat.setSortedVilkårResultater(
            setOf(
                lagVilkårResultat(
                    barn2PersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    periodeFom = LocalDate.of(2021, 11, 1),
                    utdypendeVilkårsvurderinger = emptyList(),
                ),
            ),
        )

        vilkårsvurdering.personResultater =
            setOf(barn1PersonResultat, barn2PersonResultat)

        val personerMedUtgjørendeVilkårBosattIRiketMedlemskap = hentPersonerForAlleUtgjørendeVilkår(
            minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
            vedtaksperiode = Periode(
                fom = LocalDate.of(2021, 12, 1),
                tom = TIDENES_ENDE,
            ),
            oppdatertBegrunnelseType = VedtakBegrunnelseType.INNVILGET,
            triggesAv = lagTriggesAv(vilkår = setOf(Vilkår.BOSATT_I_RIKET), medlemskap = true),
            aktuellePersonerForVedtaksperiode = personopplysningGrunnlag.personer.toList()
                .map { it.tilMinimertPerson() },
            erFørsteVedtaksperiodePåFagsak = false,

            begrunnelse = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET,
        )

        val personerMedUtgjørendeVilkårBosattIRiket = hentPersonerForAlleUtgjørendeVilkår(
            minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
            vedtaksperiode = Periode(
                fom = LocalDate.of(2021, 12, 1),
                tom = TIDENES_ENDE,
            ),
            oppdatertBegrunnelseType = VedtakBegrunnelseType.INNVILGET,
            triggesAv = lagTriggesAv(vilkår = setOf(Vilkår.BOSATT_I_RIKET)),
            aktuellePersonerForVedtaksperiode = personopplysningGrunnlag.personer.toList()
                .map { it.tilMinimertPerson() },
            erFørsteVedtaksperiodePåFagsak = false,

            begrunnelse = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET,
        )

        assertEquals(1, personerMedUtgjørendeVilkårBosattIRiketMedlemskap.size)
        assertEquals(
            barn1Fnr,
            personerMedUtgjørendeVilkårBosattIRiketMedlemskap.first().personIdent,
        )

        assertEquals(1, personerMedUtgjørendeVilkårBosattIRiket.size)
        assertEquals(
            barn2Fnr,
            personerMedUtgjørendeVilkårBosattIRiket.first().personIdent,
        )
    }

    @Test
    fun `Skal ta med riktig personer på avslag som er samtidige`() {
        val søkerFnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()

        val barn1AktørId = tilAktør(barn1Fnr)
        val barn2AktørId = tilAktør(barn2Fnr)

        val behandling = lagBehandling()
        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                søkerFnr,
                listOf(barn1Fnr, barn2Fnr),
            )

        val vilkårsvurdering = Vilkårsvurdering(
            behandling = behandling,
        )

        val barn1PersonResultat =
            PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn1AktørId)
        val barn2PersonResultat =
            PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn2AktørId)

        val avslagBegrunnelse1 = Standardbegrunnelse.AVSLAG_IKKE_AVTALE_OM_DELT_BOSTED
        barn1PersonResultat.setSortedVilkårResultater(
            setOf(
                lagVilkårResultat(
                    barn1PersonResultat,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    periodeFom = null,
                    periodeTom = null,
                    resultat = Resultat.IKKE_OPPFYLT,
                    erEksplisittAvslagPåSøknad = true,
                    standardbegrunnelser = listOf(avslagBegrunnelse1),
                ),
            ),
        )

        val avslagBegrunnelse2 = Standardbegrunnelse.AVSLAG_BOR_HOS_SØKER
        barn2PersonResultat.setSortedVilkårResultater(
            setOf(
                lagVilkårResultat(
                    barn2PersonResultat,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    periodeFom = null,
                    periodeTom = null,
                    resultat = Resultat.IKKE_OPPFYLT,
                    erEksplisittAvslagPåSøknad = true,
                    standardbegrunnelser = listOf(avslagBegrunnelse2),
                ),
            ),
        )

        vilkårsvurdering.personResultater =
            setOf(barn1PersonResultat, barn2PersonResultat)

        val personerMedUtgjørendeVilkårAvslag1 = hentPersonerForAlleUtgjørendeVilkår(
            minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
            vedtaksperiode = Periode(
                fom = TIDENES_MORGEN,
                tom = TIDENES_ENDE,
            ),
            oppdatertBegrunnelseType = VedtakBegrunnelseType.AVSLAG,
            triggesAv = lagTriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER), medlemskap = true),
            aktuellePersonerForVedtaksperiode = personopplysningGrunnlag.personer.toList()
                .map { it.tilMinimertPerson() },
            erFørsteVedtaksperiodePåFagsak = false,

            begrunnelse = avslagBegrunnelse1,
        )

        val personerMedUtgjørendeVilkårAvslag2 = hentPersonerForAlleUtgjørendeVilkår(
            minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
            vedtaksperiode = Periode(
                fom = TIDENES_MORGEN,
                tom = TIDENES_ENDE,
            ),
            oppdatertBegrunnelseType = VedtakBegrunnelseType.AVSLAG,
            triggesAv = lagTriggesAv(vilkår = setOf(Vilkår.BOR_MED_SØKER)),
            aktuellePersonerForVedtaksperiode = personopplysningGrunnlag.personer.toList()
                .map { it.tilMinimertPerson() },
            erFørsteVedtaksperiodePåFagsak = false,

            begrunnelse = avslagBegrunnelse2,
        )

        assertEquals(1, personerMedUtgjørendeVilkårAvslag1.size)
        assertEquals(
            barn1Fnr,
            personerMedUtgjørendeVilkårAvslag1.first().personIdent,
        )

        assertEquals(1, personerMedUtgjørendeVilkårAvslag2.size)
        assertEquals(
            barn2Fnr,
            personerMedUtgjørendeVilkårAvslag2.first().personIdent,
        )
    }
}
