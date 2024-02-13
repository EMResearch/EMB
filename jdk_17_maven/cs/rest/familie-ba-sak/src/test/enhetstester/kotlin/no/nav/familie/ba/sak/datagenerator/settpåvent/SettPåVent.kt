package no.nav.familie.ba.sak.datagenerator.settpåvent

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVent
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentÅrsak
import java.time.LocalDate

fun lagSettPåVent(
    behandling: Behandling = lagBehandling(),
    frist: LocalDate = LocalDate.now(),
    tidTattAvVent: LocalDate = LocalDate.now(),
    tidSattPåVent: LocalDate = LocalDate.now(),
    årsak: SettPåVentÅrsak = SettPåVentÅrsak.AVVENTER_DOKUMENTASJON,
    aktiv: Boolean = true,
) = SettPåVent(
    behandling = behandling,
    frist = frist,
    tidTattAvVent = tidTattAvVent,
    tidSattPåVent = tidSattPåVent,
    årsak = årsak,
    aktiv = aktiv,
)
