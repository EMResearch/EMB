package no.nav.familie.tilbake.pdfgen

import java.io.IOException
import java.nio.charset.StandardCharsets

object FileStructureUtil {

    // colorprofile fra https://pippin.gimp.org/sRGBz/
    val colorProfile: ByteArray
        get() = // colorprofile fra https://pippin.gimp.org/sRGBz/
            readResource("colorprofile/sRGBz.icc")

    fun readResource(location: String): ByteArray {
        val inputStream = FileStructureUtil::class.java.classLoader.getResourceAsStream(location)
        requireNotNull(inputStream) { "Fant ikke resource $location" }
        return try {
            inputStream.readAllBytes()
        } catch (e: IOException) {
            throw IllegalArgumentException("Klarte ikke å lese resource $location")
        }
    }

    fun readResourceAsString(location: String): String {
        return String(readResource(location), StandardCharsets.UTF_8)
    }
}
