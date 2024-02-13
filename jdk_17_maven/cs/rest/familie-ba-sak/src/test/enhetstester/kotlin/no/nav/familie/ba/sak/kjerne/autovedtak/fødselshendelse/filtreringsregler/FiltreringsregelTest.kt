package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler

import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.tilfeldigSøker
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Evaluering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.erOppfylt
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class FiltreringsregelTest {

    private val gyldigAktørId = randomAktør()

    @Test
    fun `Regelevaluering skal resultere i Ja`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(aktør = gyldigAktørId)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(aktør = gyldigAktørId)
        val restenAvBarna: List<PersonInfo> = listOf()

        val evalueringer = FiltreringsregelEvaluering.evaluerFiltreringsregler(
            FiltreringsreglerFakta(
                mor = mor,
                barnaFraHendelse = listOf(barnet),
                restenAvBarna = restenAvBarna,
                morLever = true,
                barnaLever = true,
                morHarVerge = false,
                erFagsakenMigrertEtterBarnFødt = false,
                løperBarnetrygdForBarnetPåAnnenForelder = false,
                morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato = false,
                morHarIkkeOpphørtBarnetrygd = true,
            ),
        )

        assertThat(evalueringer.erOppfylt()).isTrue
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når mor mottar utvidet barnetrygd`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(aktør = gyldigAktørId)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(aktør = gyldigAktørId)
        val restenAvBarna: List<PersonInfo> = listOf()

        val evalueringer = FiltreringsregelEvaluering.evaluerFiltreringsregler(
            FiltreringsreglerFakta(
                mor = mor,
                morMottarLøpendeUtvidet = true,
                barnaFraHendelse = listOf(barnet),
                restenAvBarna = restenAvBarna,
                morLever = true,
                barnaLever = true,
                morHarVerge = false,
                erFagsakenMigrertEtterBarnFødt = false,
                løperBarnetrygdForBarnetPåAnnenForelder = false,
                morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato = false,
                morHarIkkeOpphørtBarnetrygd = true,

            ),
        )

        assertThat(evalueringer.erOppfylt()).isFalse
        assertEnesteRegelMedResultatNei(evalueringer, Filtreringsregel.MOR_MOTTAR_IKKE_LØPENDE_UTVIDET)
    }

    @Test
    fun `Regelevaluering skal gi resultat IKKE_OPPFYLT når mor har løpende EØS-barnetrygd`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(aktør = gyldigAktørId)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(aktør = gyldigAktørId)
        val restenAvBarna: List<PersonInfo> = listOf()

        val evalueringer = FiltreringsregelEvaluering.evaluerFiltreringsregler(
            FiltreringsreglerFakta(
                mor = mor,
                morMottarLøpendeUtvidet = false,
                barnaFraHendelse = listOf(barnet),
                restenAvBarna = restenAvBarna,
                morLever = true,
                barnaLever = true,
                morHarVerge = false,
                erFagsakenMigrertEtterBarnFødt = false,
                løperBarnetrygdForBarnetPåAnnenForelder = false,
                morMottarEøsBarnetrygd = true,
                morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato = false,
                morHarIkkeOpphørtBarnetrygd = true,
            ),
        )

        assertThat(evalueringer.erOppfylt()).isFalse
        assertEnesteRegelMedResultatNei(evalueringer, Filtreringsregel.MOR_HAR_IKKE_LØPENDE_EØS_BARNETRYGD)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når mor er under 18 år`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(17)).copy(aktør = gyldigAktørId)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(aktør = gyldigAktørId)
        val restenAvBarna: List<PersonInfo> = listOf()

        val evalueringer = FiltreringsregelEvaluering.evaluerFiltreringsregler(
            FiltreringsreglerFakta(
                mor = mor,
                barnaFraHendelse = listOf(barnet),
                restenAvBarna = restenAvBarna,
                morLever = true,
                barnaLever = true,
                morHarVerge = false,
                erFagsakenMigrertEtterBarnFødt = false,
                løperBarnetrygdForBarnetPåAnnenForelder = false,
                morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato = false,
                morHarIkkeOpphørtBarnetrygd = true,
            ),
        )

        assertThat(evalueringer.erOppfylt()).isFalse
        assertEnesteRegelMedResultatNei(evalueringer, Filtreringsregel.MOR_ER_OVER_18_ÅR)
    }

    @Test
    fun `Regelevaluering skal resultere i JA når det har gått mer enn 5 måneder siden forrige barn ble født`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(aktør = gyldigAktørId)
        val barnet1 = tilfeldigPerson(LocalDate.now().plusMonths(0)).copy(aktør = gyldigAktørId)
        val barnet2 = tilfeldigPerson(LocalDate.now().minusMonths(1)).copy(aktør = gyldigAktørId)
        val restenAvBarna: List<PersonInfo> = listOf(
            PersonInfo(LocalDate.now().minusMonths(8).minusDays(1)),
            PersonInfo(LocalDate.now().minusMonths(8)),
        )

        val evaluering = Filtreringsregel.MER_ENN_5_MND_SIDEN_FORRIGE_BARN.vurder(
            FiltreringsreglerFakta(
                mor = mor,
                barnaFraHendelse = listOf(barnet1, barnet2),
                restenAvBarna = restenAvBarna,
                morLever = true,
                barnaLever = true,
                morHarVerge = false,
                løperBarnetrygdForBarnetPåAnnenForelder = false,
                erFagsakenMigrertEtterBarnFødt = false,
                morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato = false,
                morHarIkkeOpphørtBarnetrygd = true,

            ),
        )
        assertThat(evaluering.resultat).isEqualTo(Resultat.OPPFYLT)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når det har gått mindre enn 5 måneder siden forrige barn ble født`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(aktør = gyldigAktørId)
        val barnet1 = tilfeldigPerson(LocalDate.now()).copy(aktør = gyldigAktørId)
        val barnet2 = tilfeldigPerson(LocalDate.now().minusMonths(1)).copy(aktør = gyldigAktørId)
        val restenAvBarna: List<PersonInfo> = listOf(
            PersonInfo(LocalDate.now().minusMonths(5).minusDays(1)),
            PersonInfo(LocalDate.now().minusMonths(8)),
        )

        val evaluering = Filtreringsregel.MER_ENN_5_MND_SIDEN_FORRIGE_BARN.vurder(
            FiltreringsreglerFakta(
                mor = mor,
                barnaFraHendelse = listOf(barnet1, barnet2),
                restenAvBarna = restenAvBarna,
                morLever = true,
                barnaLever = true,
                morHarVerge = false,
                erFagsakenMigrertEtterBarnFødt = false,
                løperBarnetrygdForBarnetPåAnnenForelder = false,
                morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato = false,
                morHarIkkeOpphørtBarnetrygd = true,
            ),
        )

        assertThat(evaluering.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når det er registrert dødsfall på mor`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(aktør = gyldigAktørId)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(aktør = gyldigAktørId)
        val restenAvBarna: List<PersonInfo> = listOf()

        val evalueringer = FiltreringsregelEvaluering.evaluerFiltreringsregler(
            FiltreringsreglerFakta(
                mor = mor,
                barnaFraHendelse = listOf(barnet),
                restenAvBarna = restenAvBarna,
                morLever = false,
                barnaLever = true,
                morHarVerge = false,
                erFagsakenMigrertEtterBarnFødt = false,
                løperBarnetrygdForBarnetPåAnnenForelder = false,
                morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato = false,
                morHarIkkeOpphørtBarnetrygd = true,
            ),
        )

        assertThat(evalueringer.erOppfylt()).isFalse
        assertEnesteRegelMedResultatNei(evalueringer, Filtreringsregel.MOR_LEVER)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når det er registrert dødsfall på barnet`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(aktør = gyldigAktørId)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(aktør = gyldigAktørId)
        val restenAvBarna: List<PersonInfo> = listOf()

        val evalueringer = FiltreringsregelEvaluering.evaluerFiltreringsregler(
            FiltreringsreglerFakta(
                mor = mor,
                barnaFraHendelse = listOf(barnet),
                restenAvBarna = restenAvBarna,
                morLever = true,
                barnaLever = false,
                morHarVerge = false,
                erFagsakenMigrertEtterBarnFødt = false,
                løperBarnetrygdForBarnetPåAnnenForelder = false,
                morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato = false,
                morHarIkkeOpphørtBarnetrygd = true,
            ),
        )

        assertThat(evalueringer.erOppfylt()).isFalse
        assertEnesteRegelMedResultatNei(evalueringer, Filtreringsregel.BARN_LEVER)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når mor har verge`() {
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(aktør = gyldigAktørId)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(aktør = gyldigAktørId)
        val restenAvBarna: List<PersonInfo> = listOf()

        val evalueringer = FiltreringsregelEvaluering.evaluerFiltreringsregler(
            FiltreringsreglerFakta(
                mor = mor,
                barnaFraHendelse = listOf(barnet),
                restenAvBarna = restenAvBarna,
                morLever = true,
                barnaLever = true,
                morHarVerge = true,
                erFagsakenMigrertEtterBarnFødt = false,
                løperBarnetrygdForBarnetPåAnnenForelder = false,
                morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato = false,
                morHarIkkeOpphørtBarnetrygd = true,
            ),
        )

        assertThat(evalueringer.erOppfylt()).isFalse
        assertEnesteRegelMedResultatNei(evalueringer, Filtreringsregel.MOR_HAR_IKKE_VERGE)
    }

    fun assertIkkeOppfyltFiltreringsregel(evalueringer: List<Evaluering>, filtreringsregel: Filtreringsregel) {
        evalueringer.forEach {
            if (it.evalueringÅrsaker.first().hentIdentifikator() == filtreringsregel.name) {
                Assertions.assertEquals(Resultat.IKKE_OPPFYLT, it.resultat)
                return
            } else {
                Assertions.assertEquals(Resultat.OPPFYLT, it.resultat)
            }
        }
    }

    @Test
    fun `Mor er under 18`() {
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("2020-10-23"), aktør = tilAktør("04086226621"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2019-10-23"), aktør = tilAktør("21111777001"))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2020-09-23"))

        val evalueringer =
            FiltreringsregelEvaluering.evaluerFiltreringsregler(
                FiltreringsreglerFakta(
                    mor = søkerPerson,
                    barnaFraHendelse = listOf(barn1Person),
                    restenAvBarna = listOf(barn2PersonInfo),
                    morLever = true,
                    barnaLever = true,
                    morHarVerge = false,
                    løperBarnetrygdForBarnetPåAnnenForelder = false,
                    erFagsakenMigrertEtterBarnFødt = false,
                    morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato = false,
                    morHarIkkeOpphørtBarnetrygd = true,
                ),
            )
        assertIkkeOppfyltFiltreringsregel(evalueringer, Filtreringsregel.MOR_ER_OVER_18_ÅR)
    }

    @Test
    fun `Barn med mindre mellomrom enn 5mnd`() {
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), aktør = tilAktør("04086226621"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), aktør = tilAktør("21111777001"))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2020-09-23"))

        val evalueringer =
            FiltreringsregelEvaluering.evaluerFiltreringsregler(
                FiltreringsreglerFakta(
                    mor = søkerPerson,
                    barnaFraHendelse = listOf(barn1Person),
                    restenAvBarna = listOf(barn2PersonInfo),
                    morLever = true,
                    barnaLever = true,
                    morHarVerge = false,
                    erFagsakenMigrertEtterBarnFødt = false,
                    løperBarnetrygdForBarnetPåAnnenForelder = false,
                    morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato = false,
                    morHarIkkeOpphørtBarnetrygd = true,
                ),
            )
        assertIkkeOppfyltFiltreringsregel(evalueringer, Filtreringsregel.MER_ENN_5_MND_SIDEN_FORRIGE_BARN)
    }

    @Test
    fun `Tvillinger født på samme dag skal gi oppfylt`() {
        val mor =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), aktør = tilAktør(randomFnr()))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), aktør = tilAktør(randomFnr()))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2020-10-23"))

        val evaluering = Filtreringsregel.MER_ENN_5_MND_SIDEN_FORRIGE_BARN.vurder(
            FiltreringsreglerFakta(
                mor = mor,
                barnaFraHendelse = listOf(barn1Person),
                restenAvBarna = listOf(barn2PersonInfo),
                morLever = true,
                barnaLever = true,
                morHarVerge = false,
                erFagsakenMigrertEtterBarnFødt = false,
                løperBarnetrygdForBarnetPåAnnenForelder = false,
                morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato = false,
                morHarIkkeOpphørtBarnetrygd = true,
            ),
        )
        assertThat(evaluering.resultat).isEqualTo(Resultat.OPPFYLT)
    }

    @Test
    fun `Mor lever ikke`() {
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), aktør = tilAktør("04086226621"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), aktør = tilAktør("21111777001"))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2018-09-23"))

        val evalueringer =
            FiltreringsregelEvaluering.evaluerFiltreringsregler(
                FiltreringsreglerFakta(
                    mor = søkerPerson,
                    barnaFraHendelse = listOf(barn1Person),
                    restenAvBarna = listOf(barn2PersonInfo),
                    morLever = false,
                    barnaLever = true,
                    morHarVerge = false,
                    erFagsakenMigrertEtterBarnFødt = false,
                    løperBarnetrygdForBarnetPåAnnenForelder = false,
                    morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato = false,
                    morHarIkkeOpphørtBarnetrygd = true,
                ),
            )
        assertIkkeOppfyltFiltreringsregel(evalueringer, Filtreringsregel.MOR_LEVER)
    }

    @Test
    fun `Barnet lever ikke`() {
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), aktør = tilAktør("04086226621"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), aktør = tilAktør("21111777001"))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2018-09-23"))

        val evalueringer =
            FiltreringsregelEvaluering.evaluerFiltreringsregler(
                FiltreringsreglerFakta(
                    mor = søkerPerson,
                    barnaFraHendelse = listOf(barn1Person),
                    restenAvBarna = listOf(barn2PersonInfo),
                    morLever = true,
                    barnaLever = false,
                    morHarVerge = false,
                    erFagsakenMigrertEtterBarnFødt = false,
                    løperBarnetrygdForBarnetPåAnnenForelder = false,
                    morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato = false,
                    morHarIkkeOpphørtBarnetrygd = true,
                ),
            )
        assertIkkeOppfyltFiltreringsregel(evalueringer, Filtreringsregel.BARN_LEVER)
    }

    @Test
    fun `Mor har verge`() {
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), aktør = tilAktør("04086226621"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), aktør = tilAktør("21111777001"))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2018-09-23"))

        val evalueringer =
            FiltreringsregelEvaluering.evaluerFiltreringsregler(
                FiltreringsreglerFakta(
                    mor = søkerPerson,
                    barnaFraHendelse = listOf(barn1Person),
                    restenAvBarna = listOf(barn2PersonInfo),
                    morLever = true,
                    barnaLever = true,
                    morHarVerge = true,
                    erFagsakenMigrertEtterBarnFødt = false,
                    løperBarnetrygdForBarnetPåAnnenForelder = false,
                    morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato = false,
                    morHarIkkeOpphørtBarnetrygd = true,
                ),
            )
        assertIkkeOppfyltFiltreringsregel(evalueringer, Filtreringsregel.MOR_HAR_IKKE_VERGE)
    }

    @Test
    fun `Mor er død og er under vergemål`() {
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), aktør = tilAktør("04086226621"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), aktør = tilAktør("21111777001"))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2018-09-23"))

        val evalueringer =
            FiltreringsregelEvaluering.evaluerFiltreringsregler(
                FiltreringsreglerFakta(
                    mor = søkerPerson,
                    barnaFraHendelse = listOf(barn1Person),
                    restenAvBarna = listOf(barn2PersonInfo),
                    morLever = false,
                    barnaLever = true,
                    morHarVerge = true,
                    erFagsakenMigrertEtterBarnFødt = false,
                    løperBarnetrygdForBarnetPåAnnenForelder = false,
                    morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato = false,
                    morHarIkkeOpphørtBarnetrygd = true,
                ),
            )
        assertIkkeOppfyltFiltreringsregel(evalueringer, Filtreringsregel.MOR_LEVER)
    }

    @Test
    fun `Flere barn født`() {
        val nyligFødselsdato = LocalDate.now().minusDays(2)
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), aktør = tilAktør("04086226621"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = nyligFødselsdato, aktør = tilAktør("21111777001"))
        val barn2Person =
            tilfeldigPerson(fødselsdato = nyligFødselsdato, aktør = tilAktør("23128438785"))
        val barn3PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2018-09-23"))

        val evalueringer =
            FiltreringsregelEvaluering.evaluerFiltreringsregler(
                FiltreringsreglerFakta(
                    mor = søkerPerson,
                    barnaFraHendelse = listOf(barn1Person, barn2Person),
                    restenAvBarna = listOf(barn3PersonInfo),
                    morLever = true,
                    barnaLever = true,
                    morHarVerge = false,
                    erFagsakenMigrertEtterBarnFødt = false,
                    løperBarnetrygdForBarnetPåAnnenForelder = false,
                    morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato = false,
                    morHarIkkeOpphørtBarnetrygd = true,
                ),
            )
        Assertions.assertTrue(evalueringer.erOppfylt())
    }

    @Test
    fun `Mor har ugyldig fødselsnummer`() {
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), aktør = tilAktør("23236789111"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), aktør = tilAktør("21111777001"))
        val barn3PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2018-09-23"))

        val evalueringer =
            FiltreringsregelEvaluering.evaluerFiltreringsregler(
                FiltreringsreglerFakta(
                    mor = søkerPerson,
                    barnaFraHendelse = listOf(barn1Person),
                    restenAvBarna = listOf(barn3PersonInfo),
                    morLever = true,
                    barnaLever = true,
                    morHarVerge = false,
                    løperBarnetrygdForBarnetPåAnnenForelder = false,
                    erFagsakenMigrertEtterBarnFødt = false,
                    morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato = false,
                    morHarIkkeOpphørtBarnetrygd = true,
                ),
            )
        assertIkkeOppfyltFiltreringsregel(evalueringer, Filtreringsregel.MOR_GYLDIG_FNR)
    }

    @Test
    fun `Barn med ugyldig fødselsnummer`() {
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), aktør = tilAktør("04086226621"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), aktør = tilAktør("23102000000"))
        val barn2Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2018-09-23"), aktør = tilAktør("23091823456"))

        val evalueringer =
            FiltreringsregelEvaluering.evaluerFiltreringsregler(
                FiltreringsreglerFakta(
                    mor = søkerPerson,
                    barnaFraHendelse = listOf(barn1Person, barn2Person),
                    restenAvBarna = listOf(),
                    morLever = true,
                    barnaLever = true,
                    morHarVerge = false,
                    erFagsakenMigrertEtterBarnFødt = false,
                    løperBarnetrygdForBarnetPåAnnenForelder = false,
                    morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato = false,
                    morHarIkkeOpphørtBarnetrygd = true,
                ),
            )
        assertIkkeOppfyltFiltreringsregel(evalueringer, Filtreringsregel.BARN_GYLDIG_FNR)
    }

    @Test
    fun `Fagsak migrert etter barn født`() {
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), aktør = tilAktør("04086226621"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2020-09-23"), aktør = tilAktør("23092023456"))

        val evalueringer =
            FiltreringsregelEvaluering.evaluerFiltreringsregler(
                FiltreringsreglerFakta(
                    mor = søkerPerson,
                    barnaFraHendelse = listOf(barn1Person),
                    restenAvBarna = listOf(),
                    morLever = true,
                    barnaLever = true,
                    morHarVerge = false,
                    erFagsakenMigrertEtterBarnFødt = true,
                    løperBarnetrygdForBarnetPåAnnenForelder = false,
                    dagensDato = LocalDate.parse("2020-10-23"),
                    morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato = false,
                    morHarIkkeOpphørtBarnetrygd = true,
                ),
            )
        assertIkkeOppfyltFiltreringsregel(
            evalueringer,
            Filtreringsregel.FAGSAK_IKKE_MIGRERT_UT_AV_INFOTRYGD_ETTER_BARN_FØDT,
        )
    }

    @Test
    fun `Saken er godkjent fordi barnet er født i denne måneden`() {
        val fødselsdatoIDenneMåned = LocalDate.now().withDayOfMonth(1)

        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), aktør = tilAktør("04086226621"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = fødselsdatoIDenneMåned, aktør = tilAktør("23091823456"))

        val evalueringer =
            FiltreringsregelEvaluering.evaluerFiltreringsregler(
                FiltreringsreglerFakta(
                    mor = søkerPerson,
                    barnaFraHendelse = listOf(barn1Person),
                    restenAvBarna = listOf(),
                    morLever = true,
                    barnaLever = true,
                    morHarVerge = false,
                    løperBarnetrygdForBarnetPåAnnenForelder = false,
                    erFagsakenMigrertEtterBarnFødt = false,
                    morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato = false,
                    morHarIkkeOpphørtBarnetrygd = true,
                ),
            )
        Assertions.assertTrue(evalueringer.erOppfylt())
    }

    @Test
    fun `Skal returnere ikke oppfylt for regelevaluering når det allerede løper barnetrygd for barnet på annen forelder`() {
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"))

        val evalueringer =
            FiltreringsregelEvaluering.evaluerFiltreringsregler(
                FiltreringsreglerFakta(
                    mor = søkerPerson,
                    barnaFraHendelse = listOf(barn1Person),
                    restenAvBarna = listOf(),
                    morLever = true,
                    barnaLever = true,
                    morHarVerge = false,
                    løperBarnetrygdForBarnetPåAnnenForelder = true,
                    erFagsakenMigrertEtterBarnFødt = false,
                    morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato = false,
                    morHarIkkeOpphørtBarnetrygd = true,
                ),
            )
        Assertions.assertTrue(!evalueringer.erOppfylt())
    }

    @Test
    fun `Skal returnere ikke oppfylt for regelevaluering når mor oppfyller vilkår for utvidet barnetrygd`() {
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"))

        val evalueringer =
            FiltreringsregelEvaluering.evaluerFiltreringsregler(
                FiltreringsreglerFakta(
                    mor = søkerPerson,
                    barnaFraHendelse = listOf(barn1Person),
                    restenAvBarna = listOf(),
                    morLever = true,
                    barnaLever = true,
                    morHarVerge = false,
                    løperBarnetrygdForBarnetPåAnnenForelder = false,
                    erFagsakenMigrertEtterBarnFødt = false,
                    morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato = true,
                    morHarIkkeOpphørtBarnetrygd = true,
                ),
            )
        assertThat(evalueringer.erOppfylt()).isFalse
    }

    @Test
    fun `Skal returnere oppfylt for regelevaluering når mor ikke oppfyller vilkår for utvidet barnetrygd`() {
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"))

        val evalueringer =
            FiltreringsregelEvaluering.evaluerFiltreringsregler(
                FiltreringsreglerFakta(
                    mor = søkerPerson,
                    barnaFraHendelse = listOf(barn1Person),
                    restenAvBarna = listOf(),
                    morLever = true,
                    barnaLever = true,
                    morHarVerge = false,
                    løperBarnetrygdForBarnetPåAnnenForelder = false,
                    erFagsakenMigrertEtterBarnFødt = false,
                    morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato = false,
                    morHarIkkeOpphørtBarnetrygd = true,
                ),
            )
        assertThat(evalueringer.erOppfylt()).isTrue
    }

    @Test
    fun `Skal returnere ikke oppfylt for regelevaluering når mor har opphørt barnetrygd`() {
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"))

        val evalueringer =
            FiltreringsregelEvaluering.evaluerFiltreringsregler(
                FiltreringsreglerFakta(
                    mor = søkerPerson,
                    barnaFraHendelse = listOf(barn1Person),
                    restenAvBarna = listOf(),
                    morLever = true,
                    barnaLever = true,
                    morHarVerge = false,
                    løperBarnetrygdForBarnetPåAnnenForelder = false,
                    erFagsakenMigrertEtterBarnFødt = false,
                    morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato = false,
                    morHarIkkeOpphørtBarnetrygd = false,
                ),
            )
        assertThat(evalueringer.erOppfylt()).isFalse
    }

    private fun assertEnesteRegelMedResultatNei(evalueringer: List<Evaluering>, filtreringsRegel: Filtreringsregel) {
        assertThat(1).isEqualTo(evalueringer.filter { it.resultat == Resultat.IKKE_OPPFYLT }.size)
        assertThat(filtreringsRegel.name)
            .isEqualTo(evalueringer.filter { it.resultat == Resultat.IKKE_OPPFYLT }[0].identifikator)
    }

    @Test
    fun `Filtreringsreglene skal følge en fagbestemt rekkefølge`() {
        val fagbestemtFiltreringsregelrekkefølge = listOf(
            Filtreringsregel.MOR_GYLDIG_FNR,
            Filtreringsregel.BARN_GYLDIG_FNR,
            Filtreringsregel.MOR_LEVER,
            Filtreringsregel.BARN_LEVER,
            Filtreringsregel.MER_ENN_5_MND_SIDEN_FORRIGE_BARN,
            Filtreringsregel.MOR_ER_OVER_18_ÅR,
            Filtreringsregel.MOR_HAR_IKKE_VERGE,
            Filtreringsregel.MOR_MOTTAR_IKKE_LØPENDE_UTVIDET,
            Filtreringsregel.MOR_HAR_IKKE_LØPENDE_EØS_BARNETRYGD,
            Filtreringsregel.FAGSAK_IKKE_MIGRERT_UT_AV_INFOTRYGD_ETTER_BARN_FØDT,
            Filtreringsregel.LØPER_IKKE_BARNETRYGD_FOR_BARNET,
            Filtreringsregel.MOR_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR_VED_FØDSELSDATO,
            Filtreringsregel.MOR_HAR_IKKE_OPPHØRT_BARNETRYGD,
        )
        assertThat(Filtreringsregel.values().size).isEqualTo(fagbestemtFiltreringsregelrekkefølge.size)
        assertThat(
            Filtreringsregel.values().zip(fagbestemtFiltreringsregelrekkefølge)
                .all { (x, y) -> x == y },
        ).isTrue
    }
}
