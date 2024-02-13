package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import java.time.LocalDate

data class MinimertUregistrertBarn(
    val personIdent: String,
    val navn: String,
    val fødselsdato: LocalDate? = null,
)

fun BarnMedOpplysninger.tilMinimertUregistrertBarn() = MinimertUregistrertBarn(
    personIdent = this.ident,
    navn = this.navn,
    fødselsdato = this.fødselsdato,
)
