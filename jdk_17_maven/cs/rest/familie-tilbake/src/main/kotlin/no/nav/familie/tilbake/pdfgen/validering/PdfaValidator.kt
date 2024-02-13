package no.nav.familie.tilbake.pdfgen.validering

import org.verapdf.core.EncryptedPdfException
import org.verapdf.core.ModelParsingException
import org.verapdf.core.ValidationException
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider
import org.verapdf.pdfa.Foundries
import org.verapdf.pdfa.flavours.PDFAFlavour
import org.verapdf.pdfa.results.ValidationResult
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

/**
 * Bruker VeraPDF for å validere at produsert pdf er gyldig PDFA og dermed egnet for arkivering
 *
 *
 * For dokumentasjon, se https://docs.verapdf.org/develop/
 */
object PdfaValidator {

    fun validatePdf(pdf: ByteArray?) {
        try {
            validatePdf(ByteArrayInputStream(pdf))
        } catch (e: ModelParsingException) {
            throw PdfaValideringException("Feil ved parsing av pdf modell", e)
        } catch (e: EncryptedPdfException) {
            throw PdfaValideringException("Klarer ikke å håndtere kryptert pdf", e)
        } catch (e: IOException) {
            throw PdfaValideringException("IO exception ved validering av pdf", e)
        } catch (e: ValidationException) {
            throw PdfaValideringException("Validering av pdf feilet", e)
        }
    }

    private fun validatePdf(inputStream: InputStream?) {
        val flavour: PDFAFlavour = PDFAFlavour.fromString("2u")
        Foundries.defaultInstance().createValidator(flavour, false).use { validator ->
            Foundries.defaultInstance().createParser(inputStream, flavour).use { parser ->
                val result: ValidationResult = validator.validate(parser)
                if (!result.isCompliant) {
                    throw PdfaValideringException(result)
                }
            }
        }
    }

    init {
        VeraGreenfieldFoundryProvider.initialise()
    }
}
