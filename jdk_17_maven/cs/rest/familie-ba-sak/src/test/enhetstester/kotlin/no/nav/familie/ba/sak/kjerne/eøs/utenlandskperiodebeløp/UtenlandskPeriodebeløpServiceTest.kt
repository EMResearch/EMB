package no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.eøs.assertEqualsUnordered
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement.TilpassUtenlandskePeriodebeløpTilKompetanserService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseRepository
import no.nav.familie.ba.sak.kjerne.eøs.util.UtenlandskPeriodebeløpBuilder
import no.nav.familie.ba.sak.kjerne.eøs.util.mockPeriodeBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilLocalDate
import no.nav.familie.ba.sak.kjerne.tidslinje.util.KompetanseBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class UtenlandskPeriodebeløpServiceTest {

    val utenlandskPeriodebeløpRepository: PeriodeOgBarnSkjemaRepository<UtenlandskPeriodebeløp> =
        mockPeriodeBarnSkjemaRepository()
    val kompetanseRepository: KompetanseRepository = mockk()

    val utenlandskPeriodebeløpService = UtenlandskPeriodebeløpService(
        utenlandskPeriodebeløpRepository,
        emptyList(),
    )

    val tilpassUtenlandskePeriodebeløpTilKompetanserService = TilpassUtenlandskePeriodebeløpTilKompetanserService(
        utenlandskPeriodebeløpRepository,
        emptyList(),
        kompetanseRepository,
    )

    @BeforeEach
    fun init() {
        utenlandskPeriodebeløpRepository.deleteAll()
    }

    @Test
    fun `skal tilpasse utenlandsk periodebeløp til endrede kompetanser`() {
        val behandlingId = BehandlingId(10L)

        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = jan(2020).tilLocalDate())
        val barn2 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = jan(2020).tilLocalDate())
        val barn3 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = jan(2020).tilLocalDate())

        UtenlandskPeriodebeløpBuilder(jan(2020), behandlingId)
            .medBeløp("4444   555 666", "EUR", "N", barn1, barn2, barn3)
            .lagreTil(utenlandskPeriodebeløpRepository)

        val kompetanser = KompetanseBuilder(jan(2020), behandlingId)
            .medKompetanse("SS   SSSSS", barn1, annenForeldersAktivitetsland = "N")
            .medKompetanse("  PPP", barn1, barn2, barn3, annenForeldersAktivitetsland = "N")
            .medKompetanse("--   ----", barn2, barn3, annenForeldersAktivitetsland = "N")
            .byggKompetanser()

        every { kompetanseRepository.finnFraBehandlingId(behandlingId.id) } returns kompetanser

        tilpassUtenlandskePeriodebeløpTilKompetanserService
            .tilpassUtenlandskPeriodebeløpTilKompetanser(behandlingId)

        val faktiskeUtenlandskePeriodebeløp = utenlandskPeriodebeløpService.hentUtenlandskePeriodebeløp(behandlingId)

        val forventedeUtenlandskePeriodebeløp = UtenlandskPeriodebeløpBuilder(jan(2020), behandlingId)
            .medBeløp("44   --555", "EUR", "N", barn1)
            .bygg()

        assertEqualsUnordered(forventedeUtenlandskePeriodebeløp, faktiskeUtenlandskePeriodebeløp)
    }

    @Test
    fun `Slette et utenlandskPeriodebeløp-skjema skal resultere i et skjema uten innhold, men som fortsatt har utbetalingsland`() {
        val behandlingId = BehandlingId(10L)

        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = jan(2020).tilLocalDate())

        val lagretUtenlandskPeriodebeløp = UtenlandskPeriodebeløpBuilder(jan(2020), behandlingId)
            .medBeløp("44444444", "EUR", "SE", barn1)
            .lagreTil(utenlandskPeriodebeløpRepository).single()

        utenlandskPeriodebeløpService.slettUtenlandskPeriodebeløp(behandlingId, lagretUtenlandskPeriodebeløp.id)

        val faktiskUtenlandskPeriodebeløp =
            utenlandskPeriodebeløpService.hentUtenlandskePeriodebeløp(behandlingId).single()

        assertEquals("SE", faktiskUtenlandskPeriodebeløp.utbetalingsland)
        assertNull(faktiskUtenlandskPeriodebeløp.beløp)
        assertNull(faktiskUtenlandskPeriodebeløp.valutakode)
        assertNull(faktiskUtenlandskPeriodebeløp.intervall)
        assertNull(faktiskUtenlandskPeriodebeløp.kalkulertMånedligBeløp)
        assertEquals(lagretUtenlandskPeriodebeløp.fom, faktiskUtenlandskPeriodebeløp.fom)
        assertEquals(lagretUtenlandskPeriodebeløp.tom, faktiskUtenlandskPeriodebeløp.tom)
        assertEquals(lagretUtenlandskPeriodebeløp.barnAktører, faktiskUtenlandskPeriodebeløp.barnAktører)
    }

    @Test
    fun `Skal kunne lukke åpen utenlandskPeriodebeløp-skjema ved å sende inn identisk skjema med satt tom-dato`() {
        val behandlingId = BehandlingId(10L)

        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = jan(2020).tilLocalDate())

        UtenlandskPeriodebeløpBuilder(jan(2020), behandlingId)
            .medBeløp("4>", "EUR", "SE", barn1)
            .medIntervall(Intervall.UKENTLIG)
            .lagreTil(utenlandskPeriodebeløpRepository).single()

        // Oppdaterer UtenlandskPeriodeBeløp med identisk innhold, men med lukket tom for andre mnd.
        val oppdatertUtenlandskPeriodebeløp =
            UtenlandskPeriodebeløpBuilder(jan(2020)).medBeløp("44", "EUR", "SE", barn1).medIntervall(Intervall.UKENTLIG)
                .bygg().first()
        utenlandskPeriodebeløpService.oppdaterUtenlandskPeriodebeløp(behandlingId, oppdatertUtenlandskPeriodebeløp)

        // Forventer en liste på 2 elementer hvor det første dekker 2 mnd og det andre dekker fra mnd 3 og til uendelig (null). Det siste elementet skal ha beløp, valutakode og intervall satt til null, mens utbetalingsland skal være "SE".
        val faktiskUtenlandskPeriodebeløp = utenlandskPeriodebeløpService.hentUtenlandskePeriodebeløp(behandlingId)

        assertNotNull(faktiskUtenlandskPeriodebeløp)

        assertEquals(2, faktiskUtenlandskPeriodebeløp.size)
        assertNull(faktiskUtenlandskPeriodebeløp.elementAt(1).beløp)
        assertNull(faktiskUtenlandskPeriodebeløp.elementAt(1).valutakode)
        assertNull(faktiskUtenlandskPeriodebeløp.elementAt(1).intervall)
        assertNull(faktiskUtenlandskPeriodebeløp.elementAt(1).kalkulertMånedligBeløp)
        assertEquals("SE", faktiskUtenlandskPeriodebeløp.elementAt(1).utbetalingsland)
    }
}
