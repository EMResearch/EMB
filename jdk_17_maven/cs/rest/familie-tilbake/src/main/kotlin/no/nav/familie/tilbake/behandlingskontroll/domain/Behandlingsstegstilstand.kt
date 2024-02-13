package no.nav.familie.tilbake.behandlingskontroll.domain

import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDate
import java.util.UUID

data class Behandlingsstegstilstand(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val behandlingssteg: Behandlingssteg,
    val behandlingsstegsstatus: Behandlingsstegstatus,
    @Column("ventearsak")
    val venteårsak: Venteårsak? = null,
    val tidsfrist: LocalDate? = null,
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)

enum class Behandlingssteg(
    val sekvens: Int,
    val kanSaksbehandles: Boolean,
    val kanBesluttes: Boolean,
    val behandlingsstatus: Behandlingsstatus,
    private val beskrivelse: String,
) {

    VARSEL(1, false, false, Behandlingsstatus.UTREDES, "Vurdere om varsel om tilbakekreving skal sendes til søker"),
    GRUNNLAG(2, false, false, Behandlingsstatus.UTREDES, "Mottat kravgrunnlag fra økonomi for tilbakekrevingsrevurdering"),
    BREVMOTTAKER(3, true, false, Behandlingsstatus.UTREDES, "Registrere brevmottakere manuelt. Erstatter Verge-steget"),

    @Deprecated("Erstattes av BREVMOTTAKER")
    VERGE(3, true, false, Behandlingsstatus.UTREDES, "Fakta om verge"),
    FAKTA(4, true, true, Behandlingsstatus.UTREDES, "Fakta om Feilutbetaling"),
    FORELDELSE(5, true, true, Behandlingsstatus.UTREDES, "Vurder om feilutbetalte perioder er foreldet"),
    VILKÅRSVURDERING(6, true, true, Behandlingsstatus.UTREDES, "Vurdere om og hva som skal tilbakekreves"),
    FORESLÅ_VEDTAK(7, true, true, Behandlingsstatus.UTREDES, "Foreslår vedtak"),
    FATTE_VEDTAK(8, true, false, Behandlingsstatus.FATTER_VEDTAK, "Fatter vedtak"),
    IVERKSETT_VEDTAK(
        9,
        false,
        false,
        Behandlingsstatus.IVERKSETTER_VEDTAK,
        "Iverksett vedtak fra en behandling.  Forutsetter at et vedtak er fattet",
    ),
    AVSLUTTET(10, false, false, Behandlingsstatus.AVSLUTTET, "Behandlingen er ferdig behandlet"),
    ;

    companion object {

        fun finnNesteBehandlingssteg(
            behandlingssteg: Behandlingssteg,
            harVerge: Boolean,
            harManuelleBrevmottakere: Boolean,
        ): Behandlingssteg {
            val nesteBehandlingssteg = fraSekvens(behandlingssteg.sekvens + 1, harManuelleBrevmottakere)
            if (nesteBehandlingssteg == VERGE && !harVerge) {
                // Hvis behandling opprettes ikke med verge, kan behandlingen flyttes til neste steg
                return fraSekvens(nesteBehandlingssteg.sekvens + 1)
            }
            return nesteBehandlingssteg
        }

        fun fraSekvens(sekvens: Int, brevmottakerErstatterVerge: Boolean = false): Behandlingssteg {
            for (behandlingssteg in values()) {
                if (sekvens == behandlingssteg.sekvens) {
                    return when (behandlingssteg) {
                        BREVMOTTAKER, VERGE -> if (brevmottakerErstatterVerge) BREVMOTTAKER else VERGE
                        else -> behandlingssteg
                    }
                }
            }
            throw IllegalArgumentException("Behandlingssteg finnes ikke med sekvens=$sekvens")
        }

        fun fraNavn(navn: String): Behandlingssteg {
            return values().firstOrNull { it.name == navn }
                ?: throw IllegalArgumentException("Ukjent Behandlingssteg $navn")
        }
    }
}

enum class Behandlingsstegstatus(private val beskrivelse: String) {
    VENTER("Steget er satt på vent, f.eks. venter på brukertilbakemelding eller kravgrunnlag"),
    KLAR("Klar til saksbehandling"),
    UTFØRT("Steget er ferdig utført"),
    AUTOUTFØRT("Steget utføres automatisk av systemet"),
    TILBAKEFØRT("Steget er avbrutt og tilbakeført til et tidligere steg"),
    AVBRUTT("Steget er avbrutt"),
    ;

    companion object {

        val aktiveStegStatuser = listOf(VENTER, KLAR)
        private val utførteStegStatuser = listOf(UTFØRT, AUTOUTFØRT)

        fun erStegAktiv(status: Behandlingsstegstatus): Boolean {
            return aktiveStegStatuser.contains(status)
        }

        fun erStegUtført(status: Behandlingsstegstatus): Boolean {
            return utførteStegStatuser.contains(status)
        }
    }
}

enum class Venteårsak(val defaultVenteTidIUker: Long, val beskrivelse: String) {

    VENT_PÅ_BRUKERTILBAKEMELDING(3, "Venter på tilbakemelding fra bruker"),
    VENT_PÅ_TILBAKEKREVINGSGRUNNLAG(4, "Venter på kravgrunnlag fra økonomi"),
    AVVENTER_DOKUMENTASJON(0, "Avventer dokumentasjon"),
    UTVIDET_TILSVAR_FRIST(0, "Utvidet tilsvarsfrist"),
    ENDRE_TILKJENT_YTELSE(0, "Mulig endring i tilkjent ytelse"),
    VENT_PÅ_MULIG_MOTREGNING(0, "Mulig motregning med annen ytelse"),
    ;

    companion object {

        fun venterPåBruker(venteårsak: Venteårsak?): Boolean {
            return venteårsak in listOf(VENT_PÅ_BRUKERTILBAKEMELDING, UTVIDET_TILSVAR_FRIST, AVVENTER_DOKUMENTASJON)
        }

        fun venterPåØkonomi(venteårsak: Venteårsak?): Boolean {
            return venteårsak in listOf(VENT_PÅ_TILBAKEKREVINGSGRUNNLAG, VENT_PÅ_MULIG_MOTREGNING)
        }
    }
}
