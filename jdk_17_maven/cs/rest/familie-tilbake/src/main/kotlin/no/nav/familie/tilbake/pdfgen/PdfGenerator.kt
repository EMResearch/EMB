package no.nav.familie.tilbake.pdfgen

import com.openhtmltopdf.extend.FSSupplier
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import com.openhtmltopdf.slf4j.Slf4jLogger
import com.openhtmltopdf.svgsupport.BatikSVGDrawer
import com.openhtmltopdf.util.XRLog
import no.nav.familie.tilbake.pdfgen.validering.PdfaValidator
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Locale

class PdfGenerator {

    companion object {

        private val FONT_CACHE: MutableMap<String, ByteArray?> = HashMap()

        private fun lagBodyStartTag(dokumentvariant: Dokumentvariant): String {
            return when (dokumentvariant) {
                Dokumentvariant.ENDELIG -> "<body>"
                Dokumentvariant.UTKAST -> "<body class=\"utkast\">"
            }
        }

        init {
            XRLog.setLoggingEnabled(true)
            XRLog.setLoggerImpl(Slf4jLogger())
        }
    }

    fun genererPDFMedLogo(html: String, dokumentvariant: Dokumentvariant): ByteArray {
        val logo = FileStructureUtil.readResourceAsString("formats/pdf/nav_logo_svg.html")
        return genererPDF(logo + html, dokumentvariant)
    }

    fun genererPDF(html: String, dokumentvariant: Dokumentvariant): ByteArray {
        val baos = ByteArrayOutputStream()
        genererPDF(html, baos, dokumentvariant)
        val bytes = baos.toByteArray()
        if (dokumentvariant == Dokumentvariant.ENDELIG) {
            // validering er for treig for å brukes for interaktiv bruk, tar typisk 1-2 sekunder pr dokument
            // validering er også bare nødvendig før journalføring, så det er OK
            PdfaValidator.validatePdf(bytes)
        }
        return bytes
    }

    private fun genererPDF(htmlContent: String, outputStream: ByteArrayOutputStream, dokumentvariant: Dokumentvariant) {
        val htmlDocument = appendHtmlMetadata(htmlContent, DocFormat.PDF, dokumentvariant)
        val builder = PdfRendererBuilder()
        try {
            builder.useFont(
                fontSupplier("SourceSansPro-Regular.ttf"),
                "Source Sans Pro",
                400,
                BaseRendererBuilder.FontStyle.NORMAL,
                true,
            )
                .useFont(
                    fontSupplier("SourceSansPro-Bold.ttf"),
                    "Source Sans Pro",
                    700,
                    BaseRendererBuilder.FontStyle.OBLIQUE,
                    true,
                )
                .useFont(
                    fontSupplier("SourceSansPro-It.ttf"),
                    "Source Sans Pro",
                    400,
                    BaseRendererBuilder.FontStyle.ITALIC,
                    true,
                )
                .useColorProfile(FileStructureUtil.colorProfile)
                .useSVGDrawer(BatikSVGDrawer())
                .usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_2_U)
                .withHtmlContent(htmlDocument, "")
                .toStream(outputStream)
                .useFastMode()
                .buildPdfRenderer()
                .createPDF()
        } catch (e: IOException) {
            throw RuntimeException("Feil ved generering av pdf", e)
        }
    }

    private fun appendHtmlMetadata(html: String, format: DocFormat, dokumentvariant: Dokumentvariant): String {
        // nødvendig doctype for å støtte non-breaking space i openhtmltopdf
        return "<!DOCTYPE html PUBLIC" +
            " \"-//OPENHTMLTOPDF//DOC XHTML Character Entities Only 1.0//EN\" \"\">" +
            "<html>" +
            "<head>" +
            "<meta charset=\"UTF-8\" />" +
            "<style>" +
            hentCss(format) +
            "</style>" +
            "</head>" +
            lagBodyStartTag(dokumentvariant) +
            "<div id=\"content\">" +
            html +
            "</div>" +
            "</body>" +
            "</html>"
    }

    private fun fontSupplier(fontName: String): FSSupplier<InputStream> {
        if (FONT_CACHE.containsKey(fontName)) {
            val bytes = FONT_CACHE[fontName]
            return FSSupplier { ByteArrayInputStream(bytes) }
        }
        val bytes = FileStructureUtil.readResource("fonts/$fontName")
        FONT_CACHE[fontName] = bytes
        return FSSupplier { ByteArrayInputStream(bytes) }
    }

    private fun hentCss(format: DocFormat): String {
        return FileStructureUtil.readResourceAsString("formats/" + format.name.lowercase(Locale.getDefault()) + "/style.css")
    }
}
