package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling

fun skalTaMedBarnFraForrigeBehandling(behandling: Behandling) =
    !behandling.erMigrering() && !behandling.erTekniskBehandling()
