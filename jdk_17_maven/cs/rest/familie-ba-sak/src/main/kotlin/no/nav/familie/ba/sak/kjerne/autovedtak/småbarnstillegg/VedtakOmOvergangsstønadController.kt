package no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg

import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.infotrygd.Personident
import no.nav.familie.ba.sak.task.VedtakOmOvergangsstønadTask
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/overgangsstonad")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VedtakOmOvergangsstønadController(private val taskRepository: TaskRepositoryWrapper) {

    @PostMapping
    fun håndterVedtakOmOvergangsstønad(@RequestBody personIdent: Personident): Ressurs<String> {
        taskRepository.save(VedtakOmOvergangsstønadTask.opprettTask(personIdent.ident))
        return Ressurs.success("Ok", "Ok")
    }
}
