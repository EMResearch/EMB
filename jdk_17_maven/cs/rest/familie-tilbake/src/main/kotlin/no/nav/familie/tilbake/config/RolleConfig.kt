package no.nav.familie.tilbake.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class RolleConfig(
    @Value("\${rolle.barnetrygd.beslutter}")
    val beslutterRolleBarnetrygd: String,
    @Value("\${rolle.barnetrygd.saksbehandler}")
    val saksbehandlerRolleBarnetrygd: String,
    @Value("\${rolle.barnetrygd.veileder}")
    val veilederRolleBarnetrygd: String,
    @Value("\${rolle.enslig.beslutter}")
    val beslutterRolleEnslig: String,
    @Value("\${rolle.enslig.saksbehandler}")
    val saksbehandlerRolleEnslig: String,
    @Value("\${rolle.enslig.veileder}")
    val veilederRolleEnslig: String,
    @Value("\${rolle.kontantstøtte.beslutter}")
    val beslutterRolleKontantStøtte: String,
    @Value("\${rolle.kontantstøtte.saksbehandler}")
    val saksbehandlerRolleKontantStøtte: String,
    @Value("\${rolle.kontantstøtte.veileder}")
    val veilederRolleKontantStøtte: String,
    @Value("\${rolle.teamfamilie.forvalter}")
    val forvalterRolleTeamfamilie: String,
)
