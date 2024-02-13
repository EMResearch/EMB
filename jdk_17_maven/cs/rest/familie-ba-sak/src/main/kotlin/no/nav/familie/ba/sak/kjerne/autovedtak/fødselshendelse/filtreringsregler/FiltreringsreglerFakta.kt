package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import java.time.LocalDate

data class FiltreringsreglerFakta(
    val mor: Person,
    val morMottarLøpendeUtvidet: Boolean = false,
    val morOppfyllerVilkårForUtvidetBarnetrygdVedFødselsdato: Boolean,
    val morMottarEøsBarnetrygd: Boolean = false,
    val barnaFraHendelse: List<Person>,
    val restenAvBarna: List<PersonInfo>,
    val morLever: Boolean,
    val barnaLever: Boolean,
    val morHarVerge: Boolean,
    val erFagsakenMigrertEtterBarnFødt: Boolean,
    val løperBarnetrygdForBarnetPåAnnenForelder: Boolean,
    val morHarIkkeOpphørtBarnetrygd: Boolean,
    @JsonIgnore val dagensDato: LocalDate = LocalDate.now(),
)
