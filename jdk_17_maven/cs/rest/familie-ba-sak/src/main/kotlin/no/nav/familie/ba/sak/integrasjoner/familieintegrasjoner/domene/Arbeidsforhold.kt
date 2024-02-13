package no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene

import java.time.LocalDate

class Arbeidsforhold(
    val navArbeidsforholdId: Long? = null,
    val arbeidsforholdId: String? = null,
    val arbeidstaker: Arbeidstaker? = null,
    val arbeidsgiver: Arbeidsgiver? = null,
    val type: String? = null,
    val ansettelsesperiode: Ansettelsesperiode? = null,
    val arbeidsavtaler: List<Arbeidsavtaler>? = null,
)

class Arbeidstaker(
    val type: String? = null,
    val offentligIdent: String? = null,
    val aktoerId: String? = null,
)

class Arbeidsgiver(
    val type: ArbeidsgiverType? = null,
    val organisasjonsnummer: String? = null,
    val offentligIdent: String? = null,
)

class Ansettelsesperiode(
    val periode: Periode? = null,
    val bruksperiode: Periode? = null,
)

class Arbeidsavtaler(
    val arbeidstidsordning: String? = null,
    val yrke: String? = null,
    val stillingsprosent: Double? = null,
    val antallTimerPrUke: Double? = null,
    val beregnetAntallTimerPrUke: Double? = null,
    val bruksperiode: Periode? = null,
    val gyldighetsperiode: Periode? = null,
)

class Periode(
    val fom: LocalDate? = null,
    val tom: LocalDate? = null,
)

enum class ArbeidsgiverType {
    Organisasjon,
    Person,
}

class ArbeidsforholdRequest(
    val personIdent: String,
    val ansettelsesperiodeFom: LocalDate,
)
