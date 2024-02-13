package no.nav.familie.ba.sak.datagenerator.brev

import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.MinimertPerson
import java.time.LocalDate

fun lagMinimertPerson(
    type: PersonType = PersonType.BARN,
    fødselsdato: LocalDate = LocalDate.now().minusYears(if (type == PersonType.BARN) 2 else 30),
    aktivPersonIdent: String = randomFnr(),
    aktørId: String = randomAktør(aktivPersonIdent).aktørId,
    dødsfallsdato: LocalDate? = null,
) = MinimertPerson(
    type = type,
    fødselsdato = fødselsdato,
    aktivPersonIdent = aktivPersonIdent,
    aktørId = aktørId,
    dødsfallsdato = dødsfallsdato,
)
