package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering.utfall.VilkårIkkeOppfyltÅrsak
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering.utfall.VilkårKanskjeOppfyltÅrsak
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering.utfall.VilkårOppfyltÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonEnkel
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class VilkårsvurderingMetrics(
    private val persongrunnlagService: PersongrunnlagService,
) {

    private val vilkårsvurderingUtfall = mutableMapOf<PersonType, Map<String, Counter>>()
    private val vilkårsvurderingFørsteUtfall = mutableMapOf<PersonType, Map<String, Counter>>()

    val personTypeToDisplayedType = mapOf(
        PersonType.SØKER to "Mor",
        PersonType.BARN to "Barn",
        PersonType.ANNENPART to "Medforelder",
    )

    enum class VilkårTellerType(val navn: String) {
        UTFALL("familie.ba.behandling.vilkaarsvurdering"),
        FØRSTEUTFALL("familie.ba.behandling.vilkaarsvurdering.foerstutfall"),
    }

    init {
        initVilkårMetrikker(VilkårTellerType.UTFALL, vilkårsvurderingUtfall)
        initVilkårMetrikker(VilkårTellerType.FØRSTEUTFALL, vilkårsvurderingFørsteUtfall)
    }

    private fun initVilkårMetrikker(
        vilkårTellerType: VilkårTellerType,
        utfallMap: MutableMap<PersonType, Map<String, Counter>>,
    ) {
        PersonType.values().forEach { personType ->
            val vilkårUtfallMap = mutableMapOf<String, Counter>()
            listOf(
                Pair(Resultat.IKKE_OPPFYLT, VilkårIkkeOppfyltÅrsak.values()),
                Pair(Resultat.IKKE_VURDERT, VilkårKanskjeOppfyltÅrsak.values()),
                Pair(Resultat.OPPFYLT, VilkårOppfyltÅrsak.values()),
            )
                .forEach { (resultat, årsaker) ->
                    årsaker
                        .forEach { årsak ->
                            if (vilkårUtfallMap[årsak.toString()] != null) {
                                error("Årsak $årsak deler navn med minst en annen årsak")
                            }

                            vilkårUtfallMap[årsak.toString()] =
                                Metrics.counter(
                                    vilkårTellerType.navn,
                                    "vilkaar",
                                    årsak.hentIdentifikator(),
                                    "resultat",
                                    resultat.name,
                                    "personType",
                                    personTypeToDisplayedType[personType],
                                    "beskrivelse",
                                    årsak.hentMetrikkBeskrivelse(),
                                )
                        }
                }

            utfallMap[personType] = vilkårUtfallMap
        }
    }

    fun tellMetrikker(vilkårsvurdering: Vilkårsvurdering) {
        val personer = persongrunnlagService.hentSøkerOgBarnPåBehandling(vilkårsvurdering.behandling.id)
            ?: error("Finner ikke aktivt persongrunnlag ved telling av metrikker")

        vilkårsvurdering.personResultater.forEach { personResultat ->
            val person = personer.firstOrNull { it.aktør == personResultat.aktør }
                ?: error("Finner ikke person")

            val negativeVilkår = personResultat.vilkårResultater.filter { vilkårResultat ->
                vilkårResultat.resultat == Resultat.IKKE_OPPFYLT
            }

            if (negativeVilkår.isNotEmpty()) {
                logger.info("Behandling: ${vilkårsvurdering.behandling.id}, personType=${person.type}. Vilkår som får negativt resultat og årsakene: ${negativeVilkår.map { "${it.vilkårType}=${it.evalueringÅrsaker}" }}.")
                secureLogger.info("Behandling: ${vilkårsvurdering.behandling.id}, person=${person.aktør.aktivFødselsnummer()}. Vilkår som får negativt resultat og årsakene: ${negativeVilkår.map { "${it.vilkårType}=${it.evalueringÅrsaker}" }}.")
            }

            personResultat.vilkårResultater.forEach { vilkårResultat ->
                vilkårResultat.evalueringÅrsaker.forEach { årsak ->
                    vilkårsvurderingUtfall[person.type]?.get(årsak)?.increment()
                }
            }
        }

        økTellereForStansetIAutomatiskVilkårsvurdering(vilkårsvurdering)
    }

    private fun økTellereForStansetIAutomatiskVilkårsvurdering(vilkårsvurdering: Vilkårsvurdering) {
        Vilkår.hentFødselshendelseVilkårsreglerRekkefølge()
            .map { mapVilkårTilVilkårResultater(vilkårsvurdering, it) }
            .firstOrNull { vilkårResultatGruppertPåPerson ->
                vilkårResultatGruppertPåPerson.any { it.second?.resultat == Resultat.IKKE_OPPFYLT }
            }
            ?.let { vilkårResultatGruppertPåPerson ->
                val vilkårResultatSøker =
                    vilkårResultatGruppertPåPerson.firstOrNull { it.first.type == PersonType.SØKER && it.second != null }
                val vilkårResultatBarn =
                    vilkårResultatGruppertPåPerson.firstOrNull { it.first.type == PersonType.BARN && it.second != null }

                when {
                    vilkårResultatSøker != null -> {
                        økTellerForFørsteUtfallVilkårVedAutomatiskSaksbehandling(
                            vilkårResultatSøker.second!!,
                        )
                    }
                    vilkårResultatBarn != null -> {
                        økTellerForFørsteUtfallVilkårVedAutomatiskSaksbehandling(
                            vilkårResultatBarn.second!!,
                        )
                    }
                }
            }
    }

    private fun mapVilkårTilVilkårResultater(
        vilkårsvurdering: Vilkårsvurdering,
        vilkår: Vilkår,
    ): List<Pair<PersonEnkel, VilkårResultat?>> {
        val personer = persongrunnlagService.hentSøkerOgBarnPåBehandling(vilkårsvurdering.behandling.id)
            ?: error("Finner ikke aktivt persongrunnlag ved telling av metrikker")

        return personer.map { person ->
            val personResultat = vilkårsvurdering.personResultater.firstOrNull { personResultat ->
                personResultat.aktør == person.aktør
            }

            Pair(
                person,
                personResultat?.vilkårResultater?.find { it.vilkårType == vilkår && it.resultat == Resultat.IKKE_OPPFYLT },
            )
        }
    }

    private fun økTellerForFørsteUtfallVilkårVedAutomatiskSaksbehandling(vilkårResultat: VilkårResultat) {
        val behandlingId = vilkårResultat.personResultat?.vilkårsvurdering?.behandling?.id!!
        val personer = persongrunnlagService.hentSøkerOgBarnPåBehandling(behandlingId)
            ?: error("Finner ikke aktivt persongrunnlag ved telling av metrikker")

        val person = personer.firstOrNull { it.aktør == vilkårResultat.personResultat?.aktør }
            ?: error("Finner ikke person")

        logger.info("Første vilkår med feil=$vilkårResultat, på personType=${person.type}, på behandling $behandlingId")
        secureLogger.info("Første vilkår med feil=$vilkårResultat, på person=${person.aktør.aktivFødselsnummer()}, på behandling $behandlingId")
        vilkårResultat.evalueringÅrsaker.forEach { årsak ->
            vilkårsvurderingFørsteUtfall[person.type]?.get(årsak)?.increment()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(VilkårsvurderingMetrics::class.java)
    }
}
