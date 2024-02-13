package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.PersonopplysningGrunnlagForNyBehandlingService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.YearMonth
import kotlin.reflect.full.declaredMemberProperties

class PersonopplysningGrunnlagForNyBehandlingServiceTest(

    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val behandlingService: BehandlingService,

    @Autowired
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,

    @Autowired
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,

    @Autowired
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,

    @Autowired
    private val personopplysningGrunnlagForNyBehandlingService: PersonopplysningGrunnlagForNyBehandlingService,
) : AbstractSpringIntegrationTest() {

    @Test
    fun `opprettKopiEllerNyttPersonopplysningGrunnlag - skal opprette nytt PersonopplysningGrunnlag som kopi av personopplysningsgrunnlag fra forrige behandling ved satsendring`() {
        val morId = randomFnr()
        val barnId = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(morId)
        val førsteBehandling =
            behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        personopplysningGrunnlagForNyBehandlingService.opprettKopiEllerNyttPersonopplysningGrunnlag(
            førsteBehandling,
            null,
            morId,
            listOf(barnId),
        )

        val grunnlagFraFørsteBehandling =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = førsteBehandling.id)

        // Legger til andel tilkjent ytelse på barn
        tilkjentYtelseRepository.saveAndFlush(
            lagInitiellTilkjentYtelse(førsteBehandling, "").also {
                it.andelerTilkjentYtelse.addAll(
                    listOf(
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 5),
                            tom = YearMonth.of(2025, 5),
                            person = grunnlagFraFørsteBehandling!!.personer.first { person -> person.aktør.aktivFødselsnummer() == barnId },
                            behandling = førsteBehandling,
                            tilkjentYtelse = it,
                        ),
                    ),
                )
            },
        )

        avsluttOgLagreBehandling(førsteBehandling)

        assertThat(grunnlagFraFørsteBehandling!!.personer.size).isEqualTo(2)
        assertThat(grunnlagFraFørsteBehandling.personer.any { it.aktør.aktivFødselsnummer() == morId })
        assertThat(grunnlagFraFørsteBehandling.personer.any { it.aktør.aktivFødselsnummer() == barnId })

        val satsendring = lagBehandling(
            fagsak,
            behandlingType = BehandlingType.REVURDERING,
            årsak = BehandlingÅrsak.SATSENDRING,
        )

        val satsendringBehandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            satsendring,
        )

        personopplysningGrunnlagForNyBehandlingService.opprettKopiEllerNyttPersonopplysningGrunnlag(
            satsendringBehandling,
            førsteBehandling,
            morId,
            listOf(barnId),
        )

        val grunnlagFraSatsendringBehandling =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = satsendringBehandling.id)

        assertThat(grunnlagFraSatsendringBehandling!!.personer.size).isEqualTo(2)
        assertThat(grunnlagFraSatsendringBehandling.personer.any { it.aktør.aktivFødselsnummer() == morId })
        assertThat(grunnlagFraSatsendringBehandling.personer.any { it.aktør.aktivFødselsnummer() == barnId })
        assertThat(grunnlagFraSatsendringBehandling.id)
            .isNotEqualTo(grunnlagFraFørsteBehandling.id)
        assertThat(grunnlagFraSatsendringBehandling.behandlingId).isNotEqualTo(grunnlagFraFørsteBehandling.behandlingId)
        validerAtPersonerIGrunnlagErLike(grunnlagFraFørsteBehandling, grunnlagFraSatsendringBehandling, false)
    }

    @Test
    fun `opprettKopiEllerNyttPersonopplysningGrunnlag - skal opprette nytt PersonopplysningGrunnlag som kopi av personopplysningsgrunnlag fra forrige behandling med barn som hadde andeler tilkjent ytelse`() {
        val morId = randomFnr()
        val barn1Id = randomFnr()
        val barn2Id = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(morId)
        val førsteBehandling =
            behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        personopplysningGrunnlagForNyBehandlingService.opprettKopiEllerNyttPersonopplysningGrunnlag(
            førsteBehandling,
            null,
            morId,
            listOf(barn1Id, barn2Id),
        )

        val grunnlagFraFørsteBehandling =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = førsteBehandling.id)

        // Legger til andel tilkjent ytelse på barn
        tilkjentYtelseRepository.saveAndFlush(
            lagInitiellTilkjentYtelse(førsteBehandling, "").also {
                it.andelerTilkjentYtelse.addAll(
                    listOf(
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2023, 5),
                            tom = YearMonth.of(2025, 5),
                            person = grunnlagFraFørsteBehandling!!.personer.first { person -> person.aktør.aktivFødselsnummer() == barn2Id },
                            behandling = førsteBehandling,
                            tilkjentYtelse = it,
                        ),
                    ),
                )
            },
        )

        avsluttOgLagreBehandling(førsteBehandling)

        assertThat(grunnlagFraFørsteBehandling!!.personer.size).isEqualTo(3)
        assertThat(grunnlagFraFørsteBehandling.personer.any { it.aktør.aktivFødselsnummer() == morId }).isTrue
        assertThat(grunnlagFraFørsteBehandling.personer.any { it.aktør.aktivFødselsnummer() == barn1Id }).isTrue
        assertThat(grunnlagFraFørsteBehandling.personer.any { it.aktør.aktivFødselsnummer() == barn2Id }).isTrue

        val satsendring = lagBehandling(
            fagsak,
            behandlingType = BehandlingType.REVURDERING,
            årsak = BehandlingÅrsak.SATSENDRING,
        )

        val satsendringBehandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            satsendring,
        )

        personopplysningGrunnlagForNyBehandlingService.opprettKopiEllerNyttPersonopplysningGrunnlag(
            satsendringBehandling,
            førsteBehandling,
            morId,
            listOf(barn1Id, barn2Id),
        )

        val grunnlagFraSatsendringBehandling =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = satsendringBehandling.id)

        assertThat(grunnlagFraSatsendringBehandling!!.personer.size).isEqualTo(2)
        assertThat(grunnlagFraSatsendringBehandling.personer.any { it.aktør.aktivFødselsnummer() == morId }).isTrue
        assertThat(grunnlagFraSatsendringBehandling.personer.any { it.aktør.aktivFødselsnummer() == barn2Id }).isTrue
        assertThat(grunnlagFraSatsendringBehandling.personer.any { it.aktør.aktivFødselsnummer() == barn1Id }).isFalse

        assertThat(grunnlagFraSatsendringBehandling.id)
            .isNotEqualTo(grunnlagFraFørsteBehandling.id)
        assertThat(grunnlagFraSatsendringBehandling.behandlingId).isNotEqualTo(grunnlagFraFørsteBehandling.behandlingId)

        grunnlagFraFørsteBehandling.personer.removeAll { it.aktør.aktivFødselsnummer() == barn1Id }
        validerAtPersonerIGrunnlagErLike(grunnlagFraFørsteBehandling, grunnlagFraSatsendringBehandling, false)
    }

    private fun avsluttOgLagreBehandling(behandling: Behandling) {
        behandling.status = BehandlingStatus.AVSLUTTET
        behandling.leggTilBehandlingStegTilstand(StegType.BEHANDLING_AVSLUTTET)
        behandlingHentOgPersisterService.lagreEllerOppdater(behandling)
    }

    companion object {
        fun validerAtPersonerIGrunnlagErLike(
            personopplysningGrunnlagFørsteBehandling: PersonopplysningGrunnlag,
            personopplysningGrunnlagSatsendringBehandling: PersonopplysningGrunnlag,
            erEnhetstest: Boolean,
        ) {
            personopplysningGrunnlagFørsteBehandling.personer.fold(mutableListOf<Pair<Person, Person>>()) { acc, person ->
                acc.add(
                    Pair(
                        person,
                        personopplysningGrunnlagSatsendringBehandling.personer.first { it.aktør.aktivFødselsnummer() == person.aktør.aktivFødselsnummer() },
                    ),
                )
                acc
            }.forEach {
                validerAtSubEntiteterAvPersonErLike(
                    it.first.bostedsadresser,
                    it.second.bostedsadresser,
                    it.first.bostedsadresser.firstOrNull()?.person,
                    it.second.bostedsadresser.firstOrNull()?.person,
                    erEnhetstest,
                )
                validerAtSubEntiteterAvPersonErLike(
                    it.first.sivilstander,
                    it.second.sivilstander,
                    it.first.sivilstander.firstOrNull()?.person,
                    it.second.sivilstander.firstOrNull()?.person,
                )

                assertThat(it.first.sivilstander).containsExactlyInAnyOrderElementsOf(it.second.sivilstander)

                validerAtSubEntiteterAvPersonErLike(
                    it.first.statsborgerskap,
                    it.second.statsborgerskap,
                    it.first.statsborgerskap.firstOrNull()?.person,
                    it.second.statsborgerskap.firstOrNull()?.person,
                )

                validerAtSubEntiteterAvPersonErLike(
                    it.first.opphold,
                    it.second.opphold,
                    it.first.opphold.firstOrNull()?.person,
                    it.second.opphold.firstOrNull()?.person,
                )

                validerAtSubEntiteterAvPersonErLike(
                    it.first.arbeidsforhold,
                    it.second.arbeidsforhold,
                    it.first.arbeidsforhold.firstOrNull()?.person,
                    it.second.arbeidsforhold.firstOrNull()?.person,
                )

                if (it.first.dødsfall != null) {
                    validerAtSubEntiteterAvPersonErLike(
                        listOf(it.first.dødsfall),
                        listOf(it.second.dødsfall),
                        it.first.dødsfall?.person,
                        it.second.dødsfall?.person,
                    )
                }
            }
        }

        fun validerAtSubEntiteterAvPersonErLike(
            forrige: List<Any?>,
            kopiert: List<Any?>,
            forrigePerson: Person?,
            kopiertPerson: Person?,
            erEnhetstestOgBostedsadresse: Boolean = false,
        ) {
            val baseEntitetFelter =
                BaseEntitet::class.declaredMemberProperties.map { it.name }.toTypedArray()

            // Sammenligner ikke id, person og BaseEntitet-felter. id skal være ulik, person sjekkes separat og likhet med BaseEntitet-felter bryr vi oss ikke om.
            assertThat(kopiert).usingRecursiveFieldByFieldElementComparatorIgnoringFields(
                "id",
                "person",
                *baseEntitetFelter,
            ).isEqualTo(forrige)

            // Id skal alltid være ulik. Har ikke mulighet til å sette id til bostedsadresser i enhetstester
            if (kopiert.isNotEmpty() && !erEnhetstestOgBostedsadresse) {
                assertThat(kopiert).usingRecursiveFieldByFieldElementComparatorOnFields("id").isNotEqualTo(forrige)
            }

            if (kopiertPerson != null) {
                // Ignorerer sub-entiteter i sjekk da disse sjekkes hver for seg.
                assertThat(kopiertPerson).usingRecursiveComparison().ignoringFields(
                    "id",
                    "personopplysningGrunnlag",
                    "bostedsadresser",
                    "statsborgerskap",
                    "opphold",
                    "arbeidsforhold",
                    "sivilstander",
                    "dødsfall",
                    *baseEntitetFelter,
                ).isEqualTo(forrigePerson)
                assertThat(kopiertPerson.id).isNotEqualTo(forrigePerson?.id)
            }
        }
    }
}
