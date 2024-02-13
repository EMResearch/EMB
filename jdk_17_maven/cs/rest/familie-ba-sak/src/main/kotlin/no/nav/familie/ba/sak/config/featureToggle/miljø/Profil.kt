package no.nav.familie.ba.sak.config.featureToggle.miljø

enum class Profil(val navn: String) {
    DevPostgresPreprod("dev-postgres-preprod"),
    Integrasjonstest("integrasjonstest"),
    Prod("prod"),
    Preprod("preprod"),
    Dev("dev"),
    Postgres("postgres"),
}
