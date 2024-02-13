package no.nav.familie.ba.sak.kjerne.forrigebehandling

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Uendelighet
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonth
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class EndringIVilkårsvurderingUtilTest {

    private val jan22 = YearMonth.of(2022, 1)
    private val feb22 = YearMonth.of(2022, 2)
    private val mai22 = YearMonth.of(2022, 5)
    private val jun22 = YearMonth.of(2022, 6)

    @Test
    fun `Endring i vilkårsvurdering - skal ikke lage periode med endring dersom vilkårresultatene er helt like`() {
        val fødselsdato = LocalDate.of(2015, 1, 1)
        val vilkårResultater = setOf(
            VilkårResultat(
                personResultat = null,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.OPPFYLT,
                periodeFom = fødselsdato,
                periodeTom = LocalDate.of(2020, 1, 1),
                begrunnelse = "begrunnelse",
                sistEndretIBehandlingId = 0,
                utdypendeVilkårsvurderinger = listOf(
                    UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                    UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP,
                ),
                vurderesEtter = Regelverk.NASJONALE_REGLER,
            ),
            VilkårResultat(
                personResultat = null,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.IKKE_OPPFYLT,
                periodeFom = LocalDate.of(2020, 1, 2),
                periodeTom = LocalDate.of(2022, 1, 1),
                begrunnelse = "begrunnelse",
                sistEndretIBehandlingId = 0,
                utdypendeVilkårsvurderinger = listOf(
                    UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP,
                    UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                ),
                vurderesEtter = Regelverk.NASJONALE_REGLER,
            ),
        )

        val aktør = randomAktør()

        val person = lagPerson(aktør = aktør, type = PersonType.BARN, fødselsdato = fødselsdato)

        val perioderMedEndring = EndringIVilkårsvurderingUtil.lagEndringIVilkårsvurderingTidslinje(
            nåværendePersonResultater = setOf(lagPersonResultatFraVilkårResultater(vilkårResultater, aktør)),
            forrigePersonResultater = setOf(lagPersonResultatFraVilkårResultater(vilkårResultater, aktør)),
            personerIBehandling = setOf(person),
            personerIForrigeBehandling = setOf(person),
        ).perioder().filter { it.innhold == true }

        Assertions.assertTrue(perioderMedEndring.isEmpty())

        val endringstidspunkt = EndringIVilkårsvurderingUtil.utledEndringstidspunktForVilkårsvurdering(
            nåværendePersonResultat = setOf(lagPersonResultatFraVilkårResultater(vilkårResultater, aktør)),
            forrigePersonResultat = setOf(lagPersonResultatFraVilkårResultater(vilkårResultater, aktør)),
            personerIBehandling = setOf(person),
            personerIForrigeBehandling = setOf(person),
        )

        Assertions.assertNull(endringstidspunkt)
    }

    @Test
    fun `Endring i vilkårsvurdering - skal returnere periode med endring dersom det har vært endringer i regelverk`() {
        val fødselsdato = jan22.førsteDagIInneværendeMåned()
        val nåværendeVilkårResultat = setOf(
            VilkårResultat(
                personResultat = null,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.OPPFYLT,
                periodeFom = fødselsdato,
                periodeTom = mai22.sisteDagIInneværendeMåned(),
                begrunnelse = "begrunnelse",
                sistEndretIBehandlingId = 0,
                utdypendeVilkårsvurderinger = listOf(
                    UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                    UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP,
                ),
                vurderesEtter = Regelverk.NASJONALE_REGLER,
            ),
        )

        val forrigeVilkårResultat = setOf(
            VilkårResultat(
                personResultat = null,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.OPPFYLT,
                periodeFom = fødselsdato,
                periodeTom = mai22.sisteDagIInneværendeMåned(),
                begrunnelse = "begrunnelse",
                sistEndretIBehandlingId = 0,
                utdypendeVilkårsvurderinger = listOf(
                    UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                    UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP,
                ),
                vurderesEtter = Regelverk.EØS_FORORDNINGEN,
            ),
        )

        val aktør = randomAktør()
        val person = lagPerson(aktør = aktør, type = PersonType.BARN, fødselsdato = fødselsdato)

        val perioderMedEndring = EndringIVilkårsvurderingUtil.lagEndringIVilkårsvurderingTidslinje(
            nåværendePersonResultater = setOf(lagPersonResultatFraVilkårResultater(nåværendeVilkårResultat, aktør)),
            forrigePersonResultater = setOf(lagPersonResultatFraVilkårResultater(forrigeVilkårResultat, aktør)),
            personerIBehandling = setOf(person),
            personerIForrigeBehandling = setOf(person),
        ).perioder().filter { it.innhold == true }

        Assertions.assertEquals(1, perioderMedEndring.size)
        Assertions.assertEquals(feb22, perioderMedEndring.single().fraOgMed.tilYearMonth())
        Assertions.assertEquals(mai22, perioderMedEndring.single().tilOgMed.tilYearMonth())

        val endringstidspunkt = EndringIVilkårsvurderingUtil.utledEndringstidspunktForVilkårsvurdering(
            nåværendePersonResultat = setOf(lagPersonResultatFraVilkårResultater(nåværendeVilkårResultat, aktør)),
            forrigePersonResultat = setOf(lagPersonResultatFraVilkårResultater(forrigeVilkårResultat, aktør)),
            personerIBehandling = setOf(person),
            personerIForrigeBehandling = setOf(person),
        )

        Assertions.assertEquals(feb22, endringstidspunkt)
    }

    @Test
    fun `Endring i vilkårsvurdering - skal returnere periode med endring dersom det har oppstått splitt i vilkårsvurderingen`() {
        val fødselsdato = jan22.førsteDagIInneværendeMåned()
        val forrigeVilkårResultat = setOf(
            VilkårResultat(
                personResultat = null,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.OPPFYLT,
                periodeFom = fødselsdato,
                periodeTom = null,
                begrunnelse = "",
                sistEndretIBehandlingId = 0,
                utdypendeVilkårsvurderinger = listOf(),
                vurderesEtter = Regelverk.NASJONALE_REGLER,
            ),
        )

        val nåværendeVilkårResultat = setOf(
            VilkårResultat(
                personResultat = null,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.OPPFYLT,
                periodeFom = fødselsdato,
                periodeTom = mai22.atDay(7),
                begrunnelse = "",
                sistEndretIBehandlingId = 0,
                utdypendeVilkårsvurderinger = listOf(),
                vurderesEtter = Regelverk.NASJONALE_REGLER,
            ),
            VilkårResultat(
                personResultat = null,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.OPPFYLT,
                periodeFom = mai22.atDay(8),
                periodeTom = null,
                begrunnelse = "",
                sistEndretIBehandlingId = 0,
                utdypendeVilkårsvurderinger = listOf(),
                vurderesEtter = Regelverk.NASJONALE_REGLER,
            ),
        )

        val aktør = randomAktør()
        val person = lagPerson(aktør = aktør, type = PersonType.BARN, fødselsdato = fødselsdato)

        val perioderMedEndring = EndringIVilkårsvurderingUtil.lagEndringIVilkårsvurderingTidslinje(
            nåværendePersonResultater = setOf(lagPersonResultatFraVilkårResultater(nåværendeVilkårResultat, aktør)),
            forrigePersonResultater = setOf(lagPersonResultatFraVilkårResultater(forrigeVilkårResultat, aktør)),
            personerIBehandling = setOf(person),
            personerIForrigeBehandling = setOf(person),
        ).perioder().filter { it.innhold == true }

        Assertions.assertEquals(1, perioderMedEndring.size)
        Assertions.assertEquals(jun22, perioderMedEndring.single().fraOgMed.tilYearMonth())
        Assertions.assertEquals(Uendelighet.FREMTID, perioderMedEndring.single().tilOgMed.uendelighet)

        val endringstidspunkt = EndringIVilkårsvurderingUtil.utledEndringstidspunktForVilkårsvurdering(
            nåværendePersonResultat = setOf(lagPersonResultatFraVilkårResultater(nåværendeVilkårResultat, aktør)),
            forrigePersonResultat = setOf(lagPersonResultatFraVilkårResultater(forrigeVilkårResultat, aktør)),
            personerIBehandling = setOf(person),
            personerIForrigeBehandling = setOf(person),
        )

        Assertions.assertEquals(jun22, endringstidspunkt)
    }

    @Test
    fun `Endring i vilkårsvurdering - skal ikke lage periode med endring hvis det kun er opphørt`() {
        val fødselsdato = LocalDate.of(2015, 1, 1)
        val nåværendeVilkårResultat = setOf(
            VilkårResultat(
                personResultat = null,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.OPPFYLT,
                periodeFom = fødselsdato,
                periodeTom = LocalDate.of(2020, 1, 1),
                begrunnelse = "begrunnelse",
                sistEndretIBehandlingId = 0,
                utdypendeVilkårsvurderinger = listOf(
                    UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                    UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP,
                ),
                vurderesEtter = Regelverk.NASJONALE_REGLER,
            ),
        )

        val forrigeVilkårResultat = setOf(
            VilkårResultat(
                personResultat = null,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.OPPFYLT,
                periodeFom = fødselsdato,
                periodeTom = null,
                begrunnelse = "begrunnelse",
                sistEndretIBehandlingId = 0,
                utdypendeVilkårsvurderinger = listOf(
                    UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                    UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP,
                ),
                vurderesEtter = Regelverk.NASJONALE_REGLER,
            ),
        )

        val aktør = randomAktør()
        val person = lagPerson(aktør = aktør, type = PersonType.BARN, fødselsdato = fødselsdato)

        val perioderMedEndring = EndringIVilkårsvurderingUtil.lagEndringIVilkårsvurderingTidslinje(
            nåværendePersonResultater = setOf(lagPersonResultatFraVilkårResultater(nåværendeVilkårResultat, aktør)),
            forrigePersonResultater = setOf(lagPersonResultatFraVilkårResultater(forrigeVilkårResultat, aktør)),
            personerIBehandling = setOf(person),
            personerIForrigeBehandling = setOf(person),
        ).perioder().filter { it.innhold == true }

        Assertions.assertTrue(perioderMedEndring.isEmpty())

        val endringstidspunkt = EndringIVilkårsvurderingUtil.utledEndringstidspunktForVilkårsvurdering(
            nåværendePersonResultat = setOf(lagPersonResultatFraVilkårResultater(nåværendeVilkårResultat, aktør)),
            forrigePersonResultat = setOf(lagPersonResultatFraVilkårResultater(forrigeVilkårResultat, aktør)),
            personerIBehandling = setOf(person),
            personerIForrigeBehandling = setOf(person),
        )

        Assertions.assertNull(endringstidspunkt)
    }

    @Test
    fun `Endring i vilkårsvurdering - skal ikke lage periode med endring hvis eneste endring er å sette obligatoriske utdypende vilkårsvurderinger`() {
        val fødselsdato = LocalDate.of(2015, 1, 1)
        val forrigeVilkårResultat = setOf(
            VilkårResultat(
                personResultat = null,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.OPPFYLT,
                periodeFom = fødselsdato,
                periodeTom = null,
                begrunnelse = "migrering",
                sistEndretIBehandlingId = 0,
                utdypendeVilkårsvurderinger = listOf(),
                vurderesEtter = Regelverk.EØS_FORORDNINGEN,
            ),
        )

        val nåværendeVilkårResultat = setOf(
            VilkårResultat(
                personResultat = null,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.OPPFYLT,
                periodeFom = fødselsdato,
                periodeTom = null,
                begrunnelse = "migrering",
                sistEndretIBehandlingId = 0,
                utdypendeVilkårsvurderinger = listOf(
                    UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE,
                ),
                vurderesEtter = Regelverk.EØS_FORORDNINGEN,
            ),
        )

        val aktør = randomAktør()
        val person = lagPerson(aktør = aktør, type = PersonType.BARN, fødselsdato = fødselsdato)

        val perioderMedEndring = EndringIVilkårsvurderingUtil.lagEndringIVilkårsvurderingTidslinje(
            nåværendePersonResultater = setOf(lagPersonResultatFraVilkårResultater(nåværendeVilkårResultat, aktør)),
            forrigePersonResultater = setOf(lagPersonResultatFraVilkårResultater(forrigeVilkårResultat, aktør)),
            personerIBehandling = setOf(person),
            personerIForrigeBehandling = setOf(person),
        ).perioder().filter { it.innhold == true }

        Assertions.assertTrue(perioderMedEndring.isEmpty())

        val endringstidspunkt = EndringIVilkårsvurderingUtil.utledEndringstidspunktForVilkårsvurdering(
            nåværendePersonResultat = setOf(lagPersonResultatFraVilkårResultater(nåværendeVilkårResultat, aktør)),
            forrigePersonResultat = setOf(lagPersonResultatFraVilkårResultater(forrigeVilkårResultat, aktør)),
            personerIBehandling = setOf(person),
            personerIForrigeBehandling = setOf(person),
        )

        Assertions.assertNull(endringstidspunkt)
    }

    private fun lagPersonResultatFraVilkårResultater(vilkårResultater: Set<VilkårResultat>, aktør: Aktør): PersonResultat {
        val vilkårsvurdering = lagVilkårsvurdering(behandling = lagBehandling(), resultat = Resultat.OPPFYLT, søkerAktør = randomAktør())
        val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = aktør)

        personResultat.setSortedVilkårResultater(vilkårResultater)

        return personResultat
    }
}
