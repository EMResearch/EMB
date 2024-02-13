import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.config.ApplicationConfig
import no.nav.familie.ba.sak.config.featureToggle.miljø.Profil
import org.springframework.boot.builder.SpringApplicationBuilder
import java.io.BufferedReader
import java.io.InputStreamReader

fun main(args: Array<String>) {
    System.setProperty("spring.profiles.active", Profil.DevPostgresPreprod.navn)
    val springBuilder = SpringApplicationBuilder(ApplicationConfig::class.java).profiles(
        "mock-økonomi",
        "mock-infotrygd-feed",
        "mock-tilbakekreving-klient",
        "task-scheduling",
        "mock-infotrygd-barnetrygd",
        "mock-leader-client",
    )

    if (args.contains("--dbcontainer")) {
        springBuilder.initializers(DbContainerInitializer())
    }

    if (!args.contains("--manuellMiljø")) {
        settClientIdOgSecret()
    }

    springBuilder.run(* args)
}

private fun settClientIdOgSecret() {
    val cmd = "src/test/resources/hentMiljøvariabler.sh"

    val process = ProcessBuilder(cmd).start()

    if (process.waitFor() == 1) {
        error("Klarte ikke hente variabler fra Nais. Er du logget på Naisdevice og gcloud?")
    }

    val inputStream = BufferedReader(InputStreamReader(process.inputStream))
    inputStream.readLine() // "Switched to context dev-gcp"
    inputStream.readLine().split(";")
        .map { it.split("=") }
        .map { System.setProperty(it[0], it[1]) }
    inputStream.close()
}
