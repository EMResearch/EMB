package no.nav.familie.tilbake.pdfgen.validering

import org.verapdf.pdfa.results.TestAssertion
import org.verapdf.pdfa.results.ValidationResult
import java.util.stream.Collectors

class PdfaValideringException : RuntimeException {
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(result: ValidationResult) : this(formater(result))

    companion object {

        private fun formater(result: ValidationResult): String {
            val feilmeldinger: List<String> = result.testAssertions.stream()
                .filter { ta -> ta.status !== TestAssertion.Status.PASSED }
                .map { ta -> ta.status.toString() + ":" + ta.message }
                .collect(Collectors.toList())
            return "Validering av pdf feilet. Validerer versjon " + result.pdfaFlavour
                .toString() + " feil er: " + java.lang.String.join(", ", feilmeldinger)
        }
    }
}
