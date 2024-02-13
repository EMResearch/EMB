package no.nav.familie.tilbake

import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class DatabaseChangesTest {

    companion object {

        // Denne knekker bygg med høyere db-versjon enn main. Oppdater kun når du er klar for å merge db-endringer.
        const val MERGED_DB_VERSION = 34
    }

    /**
     * Hvis du har en databaseoppdatering vil denne testen feile, slik at ikke branch blir deployet ved en feil
     */
    @Test
    internal fun `valider migreringsscript`() {
        val resourcesPath = Paths.get(Paths.get("").toAbsolutePath().toString(), "/src/main/resources/db/migration")
        if (Files.walk(resourcesPath).anyMatch { it.toFile().isDirectory && it.fileName.toString() != "migration" }) {
            throw RuntimeException("Fant directory med annet navn enn migration")
        }
        val migreringsscript = Files.walk(resourcesPath).map { it.fileName.toString() }.filter { it.endsWith(".sql") }.toList()
        if (migreringsscript.isEmpty()) {
            throw RuntimeException("Fant ikke noen migreringsscript")
        }
        migreringsscript
            .forEach {
                val fileVersion = it.substring(1, it.indexOf("_"))
                if (fileVersion.toInt() > MERGED_DB_VERSION) {
                    throw RuntimeException("Det finnes migreringsscript som har høyere versjon enn det som er merget")
                }
            }
    }
}
