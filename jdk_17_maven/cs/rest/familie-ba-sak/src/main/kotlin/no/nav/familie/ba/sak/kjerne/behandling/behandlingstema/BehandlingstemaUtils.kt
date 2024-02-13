package no.nav.familie.ba.sak.kjerne.behandling.behandlingstema

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.finnHøyesteKategori
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjer
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.innholdForTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk

fun bestemKategoriVedOpprettelse(
    overstyrtKategori: BehandlingKategori?,
    behandlingType: BehandlingType,
    behandlingÅrsak: BehandlingÅrsak,
    // siste iverksatt behandling som har løpende utbetaling. Hvis løpende utbetaling ikke finnes, settes det til NASJONAL
    kategoriFraLøpendeBehandling: BehandlingKategori,
): BehandlingKategori {
    return when {
        behandlingType == BehandlingType.FØRSTEGANGSBEHANDLING ||
            behandlingType == BehandlingType.REVURDERING && behandlingÅrsak == BehandlingÅrsak.SØKNAD -> {
            overstyrtKategori
                ?: throw FunksjonellFeil(
                    "Behandling med type ${behandlingType.visningsnavn} " +
                        "og årsak ${behandlingÅrsak.visningsnavn} $ krever behandlingskategori",
                )
        }
        behandlingType == BehandlingType.MIGRERING_FRA_INFOTRYGD && behandlingÅrsak.erFørstegangMigreringsårsak() -> {
            overstyrtKategori ?: throw FunksjonellFeil(
                "Behandling med type ${behandlingType.visningsnavn} " +
                    "og årsak ${behandlingÅrsak.visningsnavn} $ krever behandlingskategori",
            )
        }
        else -> {
            kategoriFraLøpendeBehandling
        }
    }
}

fun bestemKategori(
    overstyrtKategori: BehandlingKategori?,
    // kategori fra siste iverksatt behandling eller NASJONAL når det ikke finnes noe
    kategoriFraSisteIverksattBehandling: BehandlingKategori,
    kategoriFraInneværendeBehandling: BehandlingKategori,
): BehandlingKategori {
    // når saksbehandler overstyrer behandlingstema manuelt
    if (overstyrtKategori != null) return overstyrtKategori

    // når saken har en løpende EØS utbetaling
    if (kategoriFraSisteIverksattBehandling == BehandlingKategori.EØS) return BehandlingKategori.EØS

    // når løpende utbetaling er NASJONAL og inneværende behandling får EØS
    val oppdatertKategori =
        listOf(kategoriFraSisteIverksattBehandling, kategoriFraInneværendeBehandling).finnHøyesteKategori()

    return oppdatertKategori ?: BehandlingKategori.NASJONAL
}

fun bestemUnderkategori(
    overstyrtUnderkategori: BehandlingUnderkategori?,
    underkategoriFraLøpendeBehandling: BehandlingUnderkategori?,
    underkategoriFraInneværendeBehandling: BehandlingUnderkategori? = null,
): BehandlingUnderkategori {
    if (underkategoriFraLøpendeBehandling == BehandlingUnderkategori.UTVIDET) return BehandlingUnderkategori.UTVIDET

    val oppdatertUnderkategori = overstyrtUnderkategori ?: underkategoriFraInneværendeBehandling

    return oppdatertUnderkategori ?: BehandlingUnderkategori.ORDINÆR
}

fun utledLøpendeUnderkategori(andeler: List<AndelTilkjentYtelse>): BehandlingUnderkategori {
    return if (andeler.any { it.erUtvidet() && it.erLøpende() }) BehandlingUnderkategori.UTVIDET else BehandlingUnderkategori.ORDINÆR
}

fun utledLøpendeKategori(
    barnasTidslinjer: Map<Aktør, VilkårsvurderingTidslinjer.BarnetsTidslinjer>?,
): BehandlingKategori {
    if (barnasTidslinjer == null) return BehandlingKategori.NASJONAL

    val nå = MånedTidspunkt.nå()

    val etBarnHarMinstEnLøpendeEØSPeriode = barnasTidslinjer
        .values
        .map { it.egetRegelverkResultatTidslinje.innholdForTidspunkt(nå) }
        .any { it.innhold?.regelverk == Regelverk.EØS_FORORDNINGEN }

    return if (etBarnHarMinstEnLøpendeEØSPeriode) {
        BehandlingKategori.EØS
    } else {
        BehandlingKategori.NASJONAL
    }
}
