import no.nav.familie.ba.sak.config.ApplicationConfig
import org.springframework.boot.builder.SpringApplicationBuilder

object DevLauncher {

    @JvmStatic
    fun main(args: Array<String>) {
        System.setProperty("spring.profiles.active", "dev")
        System.setProperty("prosessering.enabled", "false")
        val app = SpringApplicationBuilder(ApplicationConfig::class.java)
            .profiles(
                "dev",
                "mock-brev-klient",
                "mock-økonomi",
                "mock-infotrygd-feed",
                "mock-infotrygd-barnetrygd",
                "mock-pdl",
                "mock-ident-client",
                "mock-tilbakekreving-klient",
                "task-scheduling",
            )
        app.run(*args)
    }
}
