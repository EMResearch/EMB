package no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak

import no.nav.familie.tilbake.vilkårsvurdering.domain.SærligGrunn

class SærligeGrunner(
    var erSærligeGrunnerTilReduksjon: Boolean = false,
    var særligeGrunner: List<SærligGrunn> = emptyList(),
)
