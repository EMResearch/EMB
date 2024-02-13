package no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårResultatMedNyPeriode
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårResultatUtils.genererVilkårResultatForEtVilkårPåEnPerson
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingMigreringUtils
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingUtils
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.genererPersonResultatForPerson
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.gjelderAlltidFraBarnetsFødselsdato
import java.time.LocalDate

data class VilkårsvurderingForNyBehandlingUtils(
    val personopplysningGrunnlag: PersonopplysningGrunnlag,
) {
    fun genererInitiellVilkårsvurdering(
        behandling: Behandling,
        barnaAktørSomAlleredeErVurdert: List<Aktør>,
    ): Vilkårsvurdering {
        return Vilkårsvurdering(behandling = behandling).apply {
            when {
                behandling.opprettetÅrsak == BehandlingÅrsak.FØDSELSHENDELSE -> {
                    personResultater = lagPersonResultaterForFødselshendelse(
                        vilkårsvurdering = this,
                        barnaAktørSomAlleredeErVurdert = barnaAktørSomAlleredeErVurdert,
                    )
                }

                !behandling.skalBehandlesAutomatisk -> {
                    personResultater = lagPersonResultaterForManuellVilkårsvurdering(
                        vilkårsvurdering = this,
                    )
                }

                else -> personResultater = lagPersonResultaterForTomVilkårsvurdering(
                    vilkårsvurdering = this,
                )
            }
        }
    }

    fun genererVilkårsvurderingFraForrigeVedtattBehandling(
        initiellVilkårsvurdering: Vilkårsvurdering,
        forrigeBehandlingVilkårsvurdering: Vilkårsvurdering,
        behandling: Behandling,
        løpendeUnderkategori: BehandlingUnderkategori?,
        aktørerMedUtvidetAndelerIForrigeBehandling: List<Aktør>,
    ): Vilkårsvurdering {
        val (vilkårsvurdering) = VilkårsvurderingUtils.flyttResultaterTilInitielt(
            aktivVilkårsvurdering = forrigeBehandlingVilkårsvurdering,
            initiellVilkårsvurdering = initiellVilkårsvurdering,
            løpendeUnderkategori = løpendeUnderkategori,
            aktørerMedUtvidetAndelerIForrigeBehandling = aktørerMedUtvidetAndelerIForrigeBehandling,
        )

        return if (behandling.type == BehandlingType.REVURDERING) {
            hentVilkårsvurderingMedDødsdatoSomTomDato(vilkårsvurdering)
        } else {
            vilkårsvurdering
        }
    }

    fun hentVilkårsvurderingMedDødsdatoSomTomDato(vilkårsvurdering: Vilkårsvurdering): Vilkårsvurdering {
        vilkårsvurdering.personResultater.forEach { personResultat ->
            val person = personopplysningGrunnlag.søkerOgBarn.single { it.aktør == personResultat.aktør }

            if (person.erDød()) {
                val dødsDato = person.dødsfall!!.dødsfallDato

                Vilkår.values().forEach { vilkårType ->
                    val vilkårAvTypeMedSenesteTom =
                        personResultat.vilkårResultater.filter { it.vilkårType == vilkårType }
                            .maxByOrNull { it.periodeTom ?: TIDENES_ENDE }

                    if (vilkårAvTypeMedSenesteTom != null && dødsDato.isBefore(
                            vilkårAvTypeMedSenesteTom.periodeTom ?: TIDENES_ENDE,
                        ) && dødsDato.isAfter(vilkårAvTypeMedSenesteTom.periodeFom)
                    ) {
                        vilkårAvTypeMedSenesteTom.periodeTom = dødsDato
                        vilkårAvTypeMedSenesteTom.begrunnelse = "Dødsfall"
                    }
                }
            }
        }
        return vilkårsvurdering
    }

    private fun lagPersonResultaterForFødselshendelse(
        vilkårsvurdering: Vilkårsvurdering,
        barnaAktørSomAlleredeErVurdert: List<Aktør>,
    ): Set<PersonResultat> {
        val annenForelder = personopplysningGrunnlag.annenForelder
        val eldsteBarnSomVurderesSinFødselsdato =
            personopplysningGrunnlag.barna.filter { !barnaAktørSomAlleredeErVurdert.contains(it.aktør) }
                .maxByOrNull { it.fødselsdato }?.fødselsdato
                ?: throw Feil("Finner ingen barn på persongrunnlag")

        return personopplysningGrunnlag.søkerOgBarn.map { person ->
            val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = person.aktør)

            val vilkårForPerson = Vilkår.hentVilkårFor(
                personType = person.type,
                fagsakType = vilkårsvurdering.behandling.fagsak.type,
                behandlingUnderkategori = vilkårsvurdering.behandling.underkategori,
            )

            val vilkårResultater = vilkårForPerson.map { vilkår ->
                genererVilkårResultatForEtVilkårPåEnPerson(
                    person = person,
                    annenForelder = annenForelder,
                    eldsteBarnSinFødselsdato = eldsteBarnSomVurderesSinFødselsdato,
                    personResultat = personResultat,
                    vilkår = vilkår,
                )
            }

            personResultat.setSortedVilkårResultater(vilkårResultater.toSet())

            personResultat
        }.toSet()
    }

    private fun lagPersonResultaterForManuellVilkårsvurdering(
        vilkårsvurdering: Vilkårsvurdering,
    ): Set<PersonResultat> {
        return personopplysningGrunnlag.søkerOgBarn.map { person ->
            genererPersonResultatForPerson(vilkårsvurdering, person)
        }.toSet()
    }

    private fun lagPersonResultaterForTomVilkårsvurdering(
        vilkårsvurdering: Vilkårsvurdering,
    ): Set<PersonResultat> {
        return personopplysningGrunnlag.søkerOgBarn.map { person ->
            val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = person.aktør)

            val vilkårForPerson = Vilkår.hentVilkårFor(
                personType = person.type,
                fagsakType = vilkårsvurdering.behandling.fagsak.type,
                behandlingUnderkategori = vilkårsvurdering.behandling.underkategori,
            )

            val vilkårResultater = vilkårForPerson.map { vilkår ->
                VilkårResultat(
                    personResultat = personResultat,
                    erAutomatiskVurdert = true,
                    resultat = Resultat.IKKE_VURDERT,
                    vilkårType = vilkår,
                    begrunnelse = "",
                    sistEndretIBehandlingId = personResultat.vilkårsvurdering.behandling.id,
                )
            }.toSortedSet(VilkårResultat.VilkårResultatComparator)

            personResultat.setSortedVilkårResultater(vilkårResultater)

            personResultat
        }.toSet()
    }

    fun lagPersonResultaterForMigreringsbehandlingMedÅrsakEndreMigreringsdato(
        vilkårsvurdering: Vilkårsvurdering,
        forrigeBehandlingVilkårsvurdering: Vilkårsvurdering,
        nyMigreringsdato: LocalDate,
    ): Set<PersonResultat> {
        return personopplysningGrunnlag.søkerOgBarn.map { person ->
            val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = person.aktør)

            val oppfylteVilkårResultaterForPerson = forrigeBehandlingVilkårsvurdering.personResultater
                .single { it.aktør == person.aktør }.vilkårResultater
                .filter { it.erOppfylt() }

            val vilkårResultaterMedNyPeriode =
                VilkårsvurderingMigreringUtils.finnVilkårResultaterMedNyPeriodePgaNyMigreringsdato(
                    oppfylteVilkårResultaterForPerson,
                    person,
                    nyMigreringsdato,
                )

            val kopierteVilkårResultater = oppfylteVilkårResultaterForPerson.map { oppfyltVilkårResultat ->
                val vilkårResultatMedNyPeriode =
                    vilkårResultaterMedNyPeriode.find { it.vilkårResultat.id == oppfyltVilkårResultat.id }
                oppfyltVilkårResultat.kopierMedParent(personResultat).also { kopiertVilkårResultat ->
                    if (vilkårResultatMedNyPeriode != null) {
                        kopiertVilkårResultat.sistEndretIBehandlingId =
                            if (vilkårResultatMedNyPeriode.harNyPeriode()) vilkårsvurdering.behandling.id else kopiertVilkårResultat.sistEndretIBehandlingId
                        kopiertVilkårResultat.periodeFom = vilkårResultatMedNyPeriode.fom
                        kopiertVilkårResultat.periodeTom = vilkårResultatMedNyPeriode.tom
                        if (kopiertVilkårResultat.begrunnelse.isEmpty()) {
                            kopiertVilkårResultat.begrunnelse = "Migrering"
                        }
                    }
                }
            }.toSet()

            personResultat.setSortedVilkårResultater(kopierteVilkårResultater)

            personResultat
        }.toSet()
    }

    // Det kan hende UNDER_18 vilkåret ikke har fått endret fom og tom
    private fun VilkårResultatMedNyPeriode.harNyPeriode() =
        this.vilkårResultat.periodeFom != this.fom || this.vilkårResultat.periodeTom != this.tom

    fun lagPersonResultaterForHelmanuellMigrering(
        vilkårsvurdering: Vilkårsvurdering,
        nyMigreringsdato: LocalDate,
    ): Set<PersonResultat> {
        return personopplysningGrunnlag.søkerOgBarn.map { person ->
            val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = person.aktør)

            val vilkårTyperForPerson = Vilkår.hentVilkårFor(
                personType = person.type,
                fagsakType = vilkårsvurdering.behandling.fagsak.type,
                behandlingUnderkategori = vilkårsvurdering.behandling.underkategori,
            )
            val vilkårResultater = vilkårTyperForPerson.map { vilkår ->
                val fom = when {
                    vilkår.gjelderAlltidFraBarnetsFødselsdato() -> person.fødselsdato
                    nyMigreringsdato.isBefore(person.fødselsdato) -> person.fødselsdato
                    else -> nyMigreringsdato
                }

                val tom: LocalDate? = when (vilkår) {
                    Vilkår.UNDER_18_ÅR -> person.fødselsdato.plusYears(18)
                        .minusDays(1)

                    else -> null
                }

                val begrunnelse = "Migrering"

                VilkårResultat(
                    personResultat = personResultat,
                    erAutomatiskVurdert = false,
                    resultat = Resultat.OPPFYLT,
                    vilkårType = vilkår,
                    periodeFom = fom,
                    periodeTom = tom,
                    begrunnelse = begrunnelse,
                    sistEndretIBehandlingId = personResultat.vilkårsvurdering.behandling.id,
                )
            }.toSortedSet(VilkårResultat.VilkårResultatComparator)

            personResultat.setSortedVilkårResultater(vilkårResultater)

            personResultat
        }.toSet()
    }
}

fun førstegangskjøringAvVilkårsvurdering(aktivVilkårsvurdering: Vilkårsvurdering?): Boolean {
    return aktivVilkårsvurdering == null
}

fun finnAktørerMedUtvidetFraAndeler(andeler: List<AndelTilkjentYtelse>): List<Aktør> {
    return andeler.filter { it.erUtvidet() }.map { it.aktør }
}
