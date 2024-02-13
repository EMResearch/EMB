package no.nav.familie.tilbake.api

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Unprotected
@RequestMapping("/api/info")
class VersionController {

    @Operation(summary = "Hent applikasjonsinformasjon")
    @GetMapping
    fun hentInfo(): Ressurs<Info> {
        val appImage = System.getenv("NAIS_APP_IMAGE") ?: "udefinert"
        val appName = System.getenv("NAIS_APP_NAME") ?: "udefinert"
        val namespace = System.getenv("NAIS_NAMESPACE") ?: "udefinert"
        val clusterName = System.getenv("NAIS_CLUSTER_NAME") ?: "udefinert"

        return Ressurs.success(Info(appImage = appImage, appName = appName, namespace = namespace, clusterName = clusterName))
    }
}

data class Info(val appImage: String, val appName: String, val namespace: String, val clusterName: String)
