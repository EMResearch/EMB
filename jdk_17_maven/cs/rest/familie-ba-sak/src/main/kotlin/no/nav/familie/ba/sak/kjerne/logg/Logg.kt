package no.nav.familie.ba.sak.kjerne.logg

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import java.time.LocalDateTime

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "Logg")
@Table(name = "logg")
data class Logg(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "logg_seq_generator")
    @SequenceGenerator(name = "logg_seq_generator", sequenceName = "logg_seq", allocationSize = 50)
    val id: Long = 0,

    @Column(name = "opprettet_av", nullable = false, updatable = false)
    val opprettetAv: String = SikkerhetContext.hentSaksbehandlerNavn(),

    @Column(name = "opprettet_tid", nullable = false, updatable = false)
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "fk_behandling_id")
    val behandlingId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    val type: LoggType,

    @Column(name = "tittel")
    val tittel: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "rolle")
    val rolle: BehandlerRolle,

    /**
     * Feltet støtter markdown frontend.
     */
    @Column(name = "tekst")
    val tekst: String,
) {

    constructor(behandlingId: Long, type: LoggType, rolle: BehandlerRolle, tekst: String = "") : this(
        behandlingId = behandlingId,
        type = type,
        tittel = type.tittel,
        rolle = rolle,
        tekst = tekst,
    )
}

enum class LoggType(val visningsnavn: String, val tittel: String = visningsnavn) {
    AUTOVEDTAK_TIL_MANUELL_BEHANDLING("Autovedtak til manuell behandling", "Automatisk behandling stoppet"),
    VERGE_REGISTRERT("Verge ble registrert"),
    INSTITUSJON_REGISTRERT("Institusjon ble registrert"),
    FØDSELSHENDELSE("Fødselshendelse"), // Deprecated, bruk livshendelse
    LIVSHENDELSE("Livshendelse"),
    BEHANDLENDE_ENHET_ENDRET("Behandlende enhet endret", "Endret enhet på behandling"),
    BEHANDLING_OPPRETTET("Behandling opprettet"),
    BEHANDLINGSTYPE_ENDRET("Endret behandlingstype", "Endret behandlingstema"),
    BARN_LAGT_TIL("Barn lagt til på behandling"),
    DOKUMENT_MOTTATT("Dokument ble mottatt"),
    SØKNAD_REGISTRERT("Søknaden ble registrert"),
    VILKÅRSVURDERING("Vilkårsvurdering"),
    SEND_TIL_BESLUTTER("Send til beslutter", "Sendt til beslutter"),
    SEND_TIL_SYSTEM("Send til system", "Sendt til system"),
    GODKJENNE_VEDTAK("Godkjenne vedtak", "Vedtak godkjent"),
    MIGRERING_BEKREFTET("Migrering bekreftet", "Migrering bekreftet"),
    DISTRIBUERE_BREV("Distribuere brev", "Brev sendt"),
    BREV_IKKE_DISTRIBUERT("Brev ikke distribuert", "Brevet ble ikke distribuert fordi mottaker har ukjent adresse"),
    BREV_IKKE_DISTRIBUERT_UKJENT_DØDSBO(
        "Brev ikke distribuert. Ukjent dødsbo",
        "Mottaker har ukjent dødsboadresse, og brevet blir ikke sendt før adressen er satt",
    ),
    FERDIGSTILLE_BEHANDLING("Ferdigstille behandling", "Ferdigstilt behandling"),
    HENLEGG_BEHANDLING("Henlegg behandling", "Behandlingen er henlagt"),
    BEHANDLIG_SATT_PÅ_VENT("Behandlingen er satt på vent"),
    BEHANDLIG_GJENOPPTATT("Behandling gjenopptatt"),
    BEHANDLING_SATT_PÅ_MASKINELL_VENT("Behandlingen er satt på maskinell vent"),
    BEHANDLING_TATT_AV_MASKINELL_VENT("Behandlingen er tatt av maskinell vent"),
    VENTENDE_BEHANDLING_ENDRET("Behandlingen er oppdatert"),
    KORRIGERT_ETTERBETALING("Etterbetaling i brev er korrigert"),
    MANUELT_SMÅBARNSTILLEGG_JUSTERING("Småbarnstillegg er manuelt endret."),
    KORRIGERT_VEDTAK("Behandlingen er korrigering av vedtak"),
    FEILUTBETALT_VALUTA_LAGT_TIL("Feilutbetalt valuta lagt til"),
    FEILUTBETALT_VALUTA_FJERNET("Feilutbetalt valuta fjernet"),
    BREVMOTTAKER_LAGT_TIL_ELLER_FJERNET("Brevmottaker lagt til eller fjernet"),
    REFUSJON_EØS_LAGT_TIL("Refusjon EØS lagt til"),
    REFUSJON_EØS_FJERNET("Refusjon EØS fjernet"),
    MANUELL_DØDSFALL_DATO_REGISTRERT("Manuell dødsfallsdato registrert"),
}
