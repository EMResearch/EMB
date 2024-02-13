package no.nav.familie.tilbake.vilkårsvurdering.domain

import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.util.UUID

@Table("vilkarsvurdering")
data class Vilkårsvurdering(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val aktiv: Boolean = true,
    @MappedCollection(idColumn = "vilkarsvurdering_id")
    val perioder: Set<Vilkårsvurderingsperiode> = setOf(),
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)

@Table("vilkarsvurderingsperiode")
data class Vilkårsvurderingsperiode(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val periode: Månedsperiode,
    @Column("vilkarsvurderingsresultat")
    val vilkårsvurderingsresultat: Vilkårsvurderingsresultat,
    val begrunnelse: String,
    @MappedCollection(idColumn = "vilkarsvurderingsperiode_id")
    val aktsomhet: VilkårsvurderingAktsomhet? = null,
    @MappedCollection(idColumn = "vilkarsvurderingsperiode_id")
    val godTro: VilkårsvurderingGodTro? = null,
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)

@Table("vilkarsvurdering_god_tro")
data class VilkårsvurderingGodTro(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column("belop_er_i_behold")
    val beløpErIBehold: Boolean,
    @Column("belop_tilbakekreves")
    val beløpTilbakekreves: BigDecimal? = null,
    val begrunnelse: String,
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
) {

    val beløpSomErIBehold get() = if (this.beløpErIBehold) beløpTilbakekreves else BigDecimal.ZERO
}

@Table("vilkarsvurdering_aktsomhet")
data class VilkårsvurderingAktsomhet(
    @Id
    val id: UUID = UUID.randomUUID(),
    val aktsomhet: Aktsomhet,
    val ileggRenter: Boolean? = null,
    val andelTilbakekreves: BigDecimal? = null,
    @Column("manuelt_satt_belop")
    val manueltSattBeløp: BigDecimal? = null,
    val begrunnelse: String,
    @Column("serlige_grunner_til_reduksjon")
    val særligeGrunnerTilReduksjon: Boolean = false,
    @Column("tilbakekrev_smabelop")
    val tilbakekrevSmåbeløp: Boolean = true,
    @MappedCollection(idColumn = "vilkarsvurdering_aktsomhet_id")
    val vilkårsvurderingSærligeGrunner: Set<VilkårsvurderingSærligGrunn> = setOf(),
    @Column("serlige_grunner_begrunnelse")
    val særligeGrunnerBegrunnelse: String? = null,
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
) {

    init {
        require(!(andelTilbakekreves != null && manueltSattBeløp != null)) { "Kan ikke sette både prosenterSomTilbakekreves og beløpSomTilbakekreves" }
        if (aktsomhet == Aktsomhet.FORSETT) {
            check(!særligeGrunnerTilReduksjon) { "Ved FORSETT skal ikke særligeGrunnerTilReduksjon settes her" }
            check(manueltSattBeløp == null) { "Ved FORSETT er beløp automatisk, og skal ikke settes her" }
            check(andelTilbakekreves == null) { "Ved FORSETT er andel automatisk, og skal ikke settes her" }
            check(tilbakekrevSmåbeløp) { "Dette er gyldig bare for Simpel uaktsom" }
        }
        if (aktsomhet == Aktsomhet.GROV_UAKTSOMHET) {
            check(tilbakekrevSmåbeløp) { "Dette er gyldig bare for Simpel uaktsom" }
        }
    }

    val skalHaSærligeGrunner
        get() = Aktsomhet.GROV_UAKTSOMHET == aktsomhet || Aktsomhet.SIMPEL_UAKTSOMHET == aktsomhet && this.tilbakekrevSmåbeløp

    val særligeGrunner get() = vilkårsvurderingSærligeGrunner.map(VilkårsvurderingSærligGrunn::særligGrunn)
}

@Table("vilkarsvurdering_serlig_grunn")
data class VilkårsvurderingSærligGrunn(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column("serlig_grunn")
    val særligGrunn: SærligGrunn,
    val begrunnelse: String?,
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)

enum class SærligGrunn(val navn: String) {
    GRAD_AV_UAKTSOMHET("Graden av uaktsomhet hos den kravet retter seg mot"),
    HELT_ELLER_DELVIS_NAVS_FEIL("Om feilen helt eller delvis kan tilskrives NAV"),
    STØRRELSE_BELØP("Størrelsen på feilutbetalt beløp"),
    TID_FRA_UTBETALING("Hvor lang tid siden utbetalingen fant sted"),
    ANNET("Annet"),
}

interface Vurdering {

    val navn: String
}

enum class Aktsomhet(override val navn: String) : Vurdering {
    FORSETT("Forsett"),
    GROV_UAKTSOMHET("Grov uaktsomhet"),
    SIMPEL_UAKTSOMHET("Simpel uaktsomhet"),
}

enum class AnnenVurdering(override val navn: String) : Vurdering {

    GOD_TRO("Handlet i god tro"),
    FORELDET("Foreldet"),
}

enum class Vilkårsvurderingsresultat(val navn: String) {
    FORSTO_BURDE_FORSTÅTT("Ja, mottaker forsto eller burde forstått at utbetalingen skyldtes en feil (1. ledd, 1. punkt)"),
    MANGELFULLE_OPPLYSNINGER_FRA_BRUKER(
        "Ja, mottaker har forårsaket feilutbetalingen ved forsett " +
            "eller uaktsomt gitt mangelfulle opplysninger (1. ledd, 2 punkt)",
    ),
    FEIL_OPPLYSNINGER_FRA_BRUKER(
        "Ja, mottaker har forårsaket feilutbetalingen ved forsett eller " +
            "uaktsomt gitt feilaktige opplysninger (1. ledd, 2 punkt)",
    ),
    GOD_TRO("Nei, mottaker har mottatt beløpet i god tro (1. ledd)"),
    UDEFINERT("Ikke Definert"),
}
