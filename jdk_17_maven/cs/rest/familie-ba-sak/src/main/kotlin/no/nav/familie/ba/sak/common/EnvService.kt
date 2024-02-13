package no.nav.familie.ba.sak.common

import no.nav.familie.ba.sak.config.featureToggle.miljø.Profil
import no.nav.familie.ba.sak.config.featureToggle.miljø.erAktiv
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

@Service
class EnvService(private val environment: Environment) {

    fun erProd() = environment.erAktiv(Profil.Prod)

    fun erPreprod() = environment.erAktiv(Profil.Preprod)

    fun erDev() = environment.erAktiv(Profil.Dev) || environment.erAktiv(Profil.Postgres) || environment.erAktiv(Profil.DevPostgresPreprod)
}
