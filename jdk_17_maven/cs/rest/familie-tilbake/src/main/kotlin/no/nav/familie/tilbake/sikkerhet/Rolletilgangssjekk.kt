package no.nav.familie.tilbake.sikkerhet

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Rolletilgangssjekk(
    val minimumBehandlerrolle: Behandlerrolle,
    val handling: String,
    val auditLoggerEvent: AuditLoggerEvent,
    val henteParam: HenteParam = HenteParam.INGEN,
) // brukes kun i GET request/request uten body
