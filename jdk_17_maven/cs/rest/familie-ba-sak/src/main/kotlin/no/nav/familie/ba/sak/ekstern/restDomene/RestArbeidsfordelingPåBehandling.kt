package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling

data class RestArbeidsfordelingPåBehandling(
    val behandlendeEnhetId: String,
    val behandlendeEnhetNavn: String,
    val manueltOverstyrt: Boolean = false,
)

fun ArbeidsfordelingPåBehandling.tilRestArbeidsfordelingPåBehandling() = RestArbeidsfordelingPåBehandling(
    behandlendeEnhetId = this.behandlendeEnhetId,
    behandlendeEnhetNavn = this.behandlendeEnhetNavn,
    manueltOverstyrt = this.manueltOverstyrt,
)
