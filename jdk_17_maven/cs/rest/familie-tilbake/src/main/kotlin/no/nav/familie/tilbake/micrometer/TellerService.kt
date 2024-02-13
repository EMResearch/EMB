package no.nav.familie.tilbake.micrometer

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Tags
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultat
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.micrometer.domain.Meldingstelling
import no.nav.familie.tilbake.micrometer.domain.MeldingstellingRepository
import no.nav.familie.tilbake.micrometer.domain.Meldingstype
import no.nav.familie.tilbake.micrometer.domain.Mottaksstatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional
class TellerService(
    private val fagsakRepository: FagsakRepository,
    private val meldingstellingRepository: MeldingstellingRepository,
) {

    fun tellKobletKravgrunnlag(fagsystem: Fagsystem) =
        tellMelding(fagsystem, Meldingstype.KRAVGRUNNLAG, Mottaksstatus.KOBLET)

    fun tellUkobletKravgrunnlag(fagsystem: Fagsystem) =
        tellMelding(fagsystem, Meldingstype.KRAVGRUNNLAG, Mottaksstatus.UKOBLET)

    fun tellKobletStatusmelding(fagsystem: Fagsystem) =
        tellMelding(fagsystem, Meldingstype.STATUSMELDING, Mottaksstatus.KOBLET)

    fun tellUkobletStatusmelding(fagsystem: Fagsystem) =
        tellMelding(fagsystem, Meldingstype.STATUSMELDING, Mottaksstatus.UKOBLET)

    fun tellMelding(fagsystem: Fagsystem, type: Meldingstype, status: Mottaksstatus) {
        val meldingstelling = meldingstellingRepository.findByFagsystemAndTypeAndStatusAndDato(
            fagsystem,
            type,
            status,
            LocalDate.now(),
        )
        if (meldingstelling == null) {
            meldingstellingRepository.insert(
                Meldingstelling(
                    fagsystem = fagsystem,
                    type = type,
                    status = status,
                ),
            )
        } else {
            meldingstellingRepository.oppdaterTeller(fagsystem, type, status)
        }
    }

    fun tellBrevSendt(fagsak: Fagsak, brevtype: Brevtype) {
        Metrics.counter(
            "Brevteller",
            Tags.of(
                "fagsystem",
                fagsak.fagsystem.name,
                "brevtype",
                brevtype.name,
            ),
        ).increment()
    }

    fun tellVedtak(behandlingsresultatstype: Behandlingsresultatstype, behandling: Behandling) {
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val vedtakstype = if (behandlingsresultatstype in Behandlingsresultat.ALLE_HENLEGGELSESKODER) {
            Behandlingsresultatstype.HENLAGT.name
        } else {
            behandlingsresultatstype.name
        }

        Metrics.counter(
            "Vedtaksteller",
            Tags.of(
                "fagsystem",
                fagsak.fagsystem.name,
                "vedtakstype",
                vedtakstype,
            ),
        ).increment()
    }
}
