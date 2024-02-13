package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.StringListConverter
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.ekstern.restDomene.RestVilkårResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelseListConverter
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.time.LocalDate

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "VilkårResultat")
@Table(name = "VILKAR_RESULTAT")
class VilkårResultat(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vilkar_resultat_seq_generator")
    @SequenceGenerator(
        name = "vilkar_resultat_seq_generator",
        sequenceName = "vilkar_resultat_seq",
        allocationSize = 50,
    )
    val id: Long = 0,

    // Denne må være nullable=true slik at man kan slette vilkår fra person resultat
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "fk_person_resultat_id")
    var personResultat: PersonResultat?,

    @Enumerated(EnumType.STRING)
    @Column(name = "vilkar")
    val vilkårType: Vilkår,

    @Enumerated(EnumType.STRING)
    @Column(name = "resultat")
    var resultat: Resultat,

    @Enumerated(EnumType.STRING)
    @Column(name = "resultat_begrunnelse")
    var resultatBegrunnelse: ResultatBegrunnelse? = null,

    @Column(name = "periode_fom")
    var periodeFom: LocalDate? = null,

    @Column(name = "periode_tom")
    var periodeTom: LocalDate? = null,

    @Column(name = "begrunnelse", columnDefinition = "TEXT", nullable = false)
    var begrunnelse: String,

    @Column(name = "sist_endret_i_behandling_id", nullable = false)
    var sistEndretIBehandlingId: Long,

    @Column(name = "er_automatisk_vurdert", nullable = false)
    var erAutomatiskVurdert: Boolean = false,

    @Column(name = "er_eksplisitt_avslag_paa_soknad")
    var erEksplisittAvslagPåSøknad: Boolean? = null,

    @Column(name = "evaluering_aarsak")
    @Convert(converter = StringListConverter::class)
    val evalueringÅrsaker: List<String> = emptyList(),

    @Column(name = "regel_input", columnDefinition = "TEXT")
    var regelInput: String? = null,

    @Column(name = "regel_output", columnDefinition = "TEXT")
    var regelOutput: String? = null,

    @Column(name = "vedtak_begrunnelse_spesifikasjoner")
    @Convert(converter = IVedtakBegrunnelseListConverter::class)
    var standardbegrunnelser: List<IVedtakBegrunnelse> = emptyList(),

    @Enumerated(EnumType.STRING)
    @Column(name = "vurderes_etter")
    var vurderesEtter: Regelverk? = personResultat?.let { vilkårType.defaultRegelverk(it.vilkårsvurdering.behandling.kategori) },

    @Column(name = "utdypende_vilkarsvurderinger")
    @Convert(converter = UtdypendeVilkårsvurderingerConverter::class)
    var utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering> = emptyList(),
) : BaseEntitet() {

    override fun toString(): String {
        return "VilkårResultat(" +
            "id=$id, " +
            "vilkårType=$vilkårType, " +
            "periodeFom=$periodeFom, " +
            "periodeTom=$periodeTom, " +
            "resultat=$resultat, " +
            "evalueringÅrsaker=$evalueringÅrsaker" +
            ")"
    }

    fun nullstill() {
        periodeFom = null
        periodeTom = null
        begrunnelse = ""
        resultat = Resultat.IKKE_VURDERT
    }

    fun oppdater(restVilkårResultat: RestVilkårResultat) {
        periodeFom = restVilkårResultat.periodeFom
        periodeTom = restVilkårResultat.periodeTom
        begrunnelse = restVilkårResultat.begrunnelse
        resultat = restVilkårResultat.resultat
        resultatBegrunnelse = restVilkårResultat.resultatBegrunnelse
        erAutomatiskVurdert = false
        erEksplisittAvslagPåSøknad = restVilkårResultat.erEksplisittAvslagPåSøknad
        oppdaterPekerTilBehandling()
        vurderesEtter = restVilkårResultat.vurderesEtter
        utdypendeVilkårsvurderinger = restVilkårResultat.utdypendeVilkårsvurderinger
    }

    fun kopierMedParent(nyPersonResultat: PersonResultat? = null): VilkårResultat {
        return VilkårResultat(
            personResultat = nyPersonResultat ?: personResultat,
            erAutomatiskVurdert = erAutomatiskVurdert,
            vilkårType = vilkårType,
            resultat = resultat,
            resultatBegrunnelse = resultatBegrunnelse,
            periodeFom = periodeFom,
            periodeTom = periodeTom,
            begrunnelse = begrunnelse,
            sistEndretIBehandlingId = sistEndretIBehandlingId,
            regelInput = regelInput,
            regelOutput = regelOutput,
            erEksplisittAvslagPåSøknad = erEksplisittAvslagPåSøknad,
            vurderesEtter = vurderesEtter,
            utdypendeVilkårsvurderinger = utdypendeVilkårsvurderinger,
            standardbegrunnelser = standardbegrunnelser,
        )
    }

    fun kopierMedNyPeriode(fom: LocalDate, tom: LocalDate, behandlingId: Long): VilkårResultat {
        return VilkårResultat(
            personResultat = personResultat,
            erAutomatiskVurdert = erAutomatiskVurdert,
            vilkårType = vilkårType,
            resultat = resultat,
            resultatBegrunnelse = resultatBegrunnelse,
            periodeFom = if (fom == TIDENES_MORGEN) null else fom,
            periodeTom = if (tom == TIDENES_ENDE) null else tom,
            begrunnelse = begrunnelse,
            regelInput = regelInput,
            regelOutput = regelOutput,
            sistEndretIBehandlingId = behandlingId,
            erEksplisittAvslagPåSøknad = erEksplisittAvslagPåSøknad,
            vurderesEtter = vurderesEtter,
            utdypendeVilkårsvurderinger = utdypendeVilkårsvurderinger,
        )
    }

    fun tilKopiForNyttPersonResultat(nyttPersonResultat: PersonResultat): VilkårResultat {
        return VilkårResultat(
            personResultat = nyttPersonResultat,
            erAutomatiskVurdert = erAutomatiskVurdert,
            vilkårType = vilkårType,
            resultat = resultat,
            resultatBegrunnelse = resultatBegrunnelse,
            periodeFom = periodeFom,
            periodeTom = periodeTom,
            begrunnelse = begrunnelse,
            sistEndretIBehandlingId = nyttPersonResultat.vilkårsvurdering.behandling.id,
            regelInput = regelInput,
            regelOutput = regelOutput,
            erEksplisittAvslagPåSøknad = erEksplisittAvslagPåSøknad,
            vurderesEtter = vurderesEtter,
            utdypendeVilkårsvurderinger = utdypendeVilkårsvurderinger,
            standardbegrunnelser = standardbegrunnelser,
        )
    }

    fun oppdaterPekerTilBehandling() {
        sistEndretIBehandlingId = personResultat!!.vilkårsvurdering.behandling.id
    }

    fun erAvslagUtenPeriode() =
        this.erEksplisittAvslagPåSøknad == true && this.periodeFom == null && this.periodeTom == null

    fun harFremtidigTom() = this.periodeTom == null || this.periodeTom!!.isAfter(LocalDate.now().sisteDagIMåned())

    fun erOppfylt() = this.resultat == Resultat.OPPFYLT

    companion object {

        val VilkårResultatComparator = compareBy<VilkårResultat>({ it.periodeFom }, { it.resultat }, { it.vilkårType })
    }
}

enum class Regelverk {
    NASJONALE_REGLER, EØS_FORORDNINGEN
}

enum class ResultatBegrunnelse(
    val gyldigForVilkår: List<Vilkår>,
    val gyldigIKombinasjonMedResultat: List<Resultat>,
    val gyldigForRegelverk: List<Regelverk>,
) {
    IKKE_AKTUELT(
        gyldigForVilkår = listOf(Vilkår.LOVLIG_OPPHOLD),
        gyldigIKombinasjonMedResultat = listOf(Resultat.OPPFYLT),
        gyldigForRegelverk = listOf(Regelverk.EØS_FORORDNINGEN),
    ),
}
