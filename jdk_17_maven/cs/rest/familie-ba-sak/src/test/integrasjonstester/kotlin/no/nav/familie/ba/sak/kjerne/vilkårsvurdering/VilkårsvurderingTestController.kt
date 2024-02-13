package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement.TilpassKompetanserTilRegelverkService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilMånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.util.VilkårsvurderingBuilder
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk.EØS_FORORDNINGEN
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE_MED_SØKER
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.OMFATTET_AV_NORSK_LOVGIVNING
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOR_MED_SØKER
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/test/vilkaarsvurdering")
@ProtectedWithClaims(issuer = "azuread")
@Validated
@Profile("!prod")
class VilkårsvurderingTestController(
    private val utvidetBehandlingService: UtvidetBehandlingService,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val beregningService: BeregningService,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val behandlingRepository: BehandlingRepository,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val aktørIdRepository: AktørIdRepository,
    private val tilpassKompetanserTilRegelverkService: TilpassKompetanserTilRegelverkService,
) {

    @PostMapping()
    fun opprettBehandlingMedVilkårsvurdering(
        @RequestBody personresultater: Map<LocalDate, Map<Vilkår, String>>,
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        val personer = personresultater.tilPersoner()
            .map { it.copy(aktør = aktørIdRepository.saveAndFlush(it.aktør)) }

        val søker = personer.first { it.type == PersonType.SØKER }
        val barn = personer.filter { it.type == PersonType.BARN }

        val fagsak = fagsakService.hentEllerOpprettFagsak(søker.aktør.aktivFødselsnummer())

        val behandling = behandlingService.opprettBehandling(
            NyBehandling(
                kategori = BehandlingKategori.EØS,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                søkersIdent = søker.aktør.aktivFødselsnummer(),
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                søknadMottattDato = LocalDate.now(),
                barnasIdenter = barn.map { it.aktør.aktivFødselsnummer() },
                fagsakId = fagsak.id,
            ),
        )

        // Opprett persongrunnlag
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.save(
            lagTestPersonopplysningGrunnlag(behandling.id, *personer.toTypedArray()),
        )

        // Opprett og lagre vilkårsvurdering
        val vilkårsvurdering = personresultater.tilVilkårsvurdering(
            behandling,
            personopplysningGrunnlag,
        )

        vilkårsvurderingService.lagreInitielt(
            vilkårsvurdering,
        )

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)
        tilpassKompetanserTilRegelverkService.tilpassKompetanserTilRegelverk(BehandlingId(behandling.id))

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandling.id)))
    }

    @PostMapping("/{behandlingId}")
    fun oppdaterVilkårsvurderingIBehandling(
        @PathVariable behandlingId: Long,
        @RequestBody personresultater: Map<LocalDate, Map<Vilkår, String>>,
    ): ResponseEntity<Ressurs<RestUtvidetBehandling>> {
        val personopplysningGrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId)
        val behandling = behandlingRepository.finnBehandling(behandlingId)

        val nyVilkårsvurdering = personresultater.tilVilkårsvurdering(
            behandling,
            personopplysningGrunnlag!!,
        )

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(nyVilkårsvurdering)

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)
        tilpassKompetanserTilRegelverkService.tilpassKompetanserTilRegelverk(BehandlingId(behandling.id))

        return ResponseEntity.ok(Ressurs.success(utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingId)))
    }
}

private fun Map<LocalDate, Map<Vilkår, String>>.tilPersoner(): List<Person> {
    return this.keys.mapIndexed { indeks, startTidspunkt ->
        when (indeks) {
            0 -> tilfeldigPerson(personType = PersonType.SØKER, fødselsdato = startTidspunkt)
            else -> tilfeldigPerson(personType = PersonType.BARN, fødselsdato = startTidspunkt)
        }
    }.map {
        it.copy(id = 0).also { it.sivilstander.clear() }
    } // tilfeldigPerson inneholder litt for mye, så fjerner det
}

fun Map<LocalDate, Map<Vilkår, String>>.tilVilkårsvurdering(
    behandling: Behandling,
    personopplysningGrunnlag: PersonopplysningGrunnlag,
): Vilkårsvurdering {
    val builder = VilkårsvurderingBuilder<Måned>(behandling)

    this.entries.forEach { (startTidspunkt, vilkårsresultater) ->
        val person = personopplysningGrunnlag.personer.first { it.fødselsdato == startTidspunkt }

        val personBuilder = builder.forPerson(person, startTidspunkt.tilMånedTidspunkt())
        vilkårsresultater.forEach { (vilkår, tidslinje) -> personBuilder.medVilkår(tidslinje, vilkår) }
        personBuilder.byggPerson()
    }

    return builder.byggVilkårsvurdering().leggPåUtdypendeVilkår()
}

private fun Vilkårsvurdering.leggPåUtdypendeVilkår(): Vilkårsvurdering {
    this.personResultater.forEach { personresultat ->
        personresultat.vilkårResultater.forEach {
            when {
                it.vilkårType == BOSATT_I_RIKET && personresultat.erSøkersResultater() && it.vurderesEtter == EØS_FORORDNINGEN ->
                    it.utdypendeVilkårsvurderinger = it.utdypendeVilkårsvurderinger + OMFATTET_AV_NORSK_LOVGIVNING
                it.vilkårType == BOSATT_I_RIKET && !personresultat.erSøkersResultater() && it.vurderesEtter == EØS_FORORDNINGEN ->
                    it.utdypendeVilkårsvurderinger = it.utdypendeVilkårsvurderinger + BARN_BOR_I_NORGE
                it.vilkårType == BOR_MED_SØKER && !personresultat.erSøkersResultater() && it.vurderesEtter == EØS_FORORDNINGEN ->
                    it.utdypendeVilkårsvurderinger = it.utdypendeVilkårsvurderinger + BARN_BOR_I_NORGE_MED_SØKER
                else -> Any()
            }
        }
    }

    return this
}
