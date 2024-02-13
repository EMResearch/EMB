package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.eøs.assertEqualsUnordered
import no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement.TilpassValutakurserTilUtenlandskePeriodebeløpService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.medBehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpRepository
import no.nav.familie.ba.sak.kjerne.eøs.util.UtenlandskPeriodebeløpBuilder
import no.nav.familie.ba.sak.kjerne.eøs.util.ValutakursBuilder
import no.nav.familie.ba.sak.kjerne.eøs.util.mockPeriodeBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilLocalDate
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

internal class ValutakursServiceTest {
    val valutakursRepository: PeriodeOgBarnSkjemaRepository<Valutakurs> = mockPeriodeBarnSkjemaRepository()
    val utenlandskPeriodebeløpRepository: UtenlandskPeriodebeløpRepository = mockk()

    val valutakursService = ValutakursService(
        valutakursRepository,
        emptyList(),
    )

    val tilpassValutakurserTilUtenlandskePeriodebeløpService = TilpassValutakurserTilUtenlandskePeriodebeløpService(
        valutakursRepository,
        utenlandskPeriodebeløpRepository,
        emptyList(),
    )

    @BeforeEach
    fun init() {
        valutakursRepository.deleteAll()
    }

    @Test
    fun `skal tilpasse utenlandsk periodebeløp til endrede kompetanser`() {
        val behandlingId = BehandlingId(10L)

        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = jan(2020).tilLocalDate())
        val barn2 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = jan(2020).tilLocalDate())
        val barn3 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = jan(2020).tilLocalDate())

        ValutakursBuilder(jan(2020), behandlingId)
            .medKurs("4444   555 666", "EUR", barn1, barn2, barn3)
            .lagreTil(valutakursRepository)

        val utenlandskePeriodebeløp = UtenlandskPeriodebeløpBuilder(jan(2020), behandlingId)
            .medBeløp("  777777777", "EUR", "N", barn1)
            .bygg()

        every { utenlandskPeriodebeløpRepository.finnFraBehandlingId(behandlingId.id) } returns utenlandskePeriodebeløp

        tilpassValutakurserTilUtenlandskePeriodebeløpService.tilpassValutakursTilUtenlandskPeriodebeløp(behandlingId)

        val faktiskeValutakurser = valutakursService.hentValutakurser(behandlingId)

        val forventedeValutakurser = ValutakursBuilder(jan(2020), behandlingId)
            .medKurs("  44$$$555$", "EUR", barn1)
            .bygg()

        assertEqualsUnordered(forventedeValutakurser, faktiskeValutakurser)
    }

    @Test
    fun `slette et valutakurs-skjema skal resultere i et skjema uten innhold, men som fortsatt har valutakoden`() {
        val behandlingId = BehandlingId(10L)

        val lagretValutakurs = valutakursRepository.saveAll(
            listOf(
                Valutakurs(
                    fom = YearMonth.now(),
                    tom = YearMonth.now(),
                    barnAktører = setOf(tilfeldigPerson().aktør),
                    valutakursdato = LocalDate.now(),
                    valutakode = "EUR",
                    kurs = BigDecimal.TEN,
                ),
            ).medBehandlingId(behandlingId),
        ).single()

        valutakursService.slettValutakurs(behandlingId, lagretValutakurs.id)

        val faktiskValutakurs = valutakursService.hentValutakurser(behandlingId).single()

        assertEquals("EUR", faktiskValutakurs.valutakode)
        assertNull(faktiskValutakurs.valutakursdato)
        assertNull(faktiskValutakurs.kurs)

        assertEquals(lagretValutakurs.fom, faktiskValutakurs.fom)
        assertEquals(lagretValutakurs.tom, faktiskValutakurs.tom)
        assertEquals(lagretValutakurs.barnAktører, faktiskValutakurs.barnAktører)
    }

    @Test
    fun `skal kunne lukke åpen valutakurs ved å sende inn identisk skjema med til-og-med-dato`() {
        val behandlingId = BehandlingId(10L)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN)

        // Åpen (til-og-med er null) valutakurs for ett barn
        ValutakursBuilder(jan(2020), behandlingId)
            .medKurs("4>", "EUR", barn1)
            .lagreTil(valutakursRepository)

        // Endrer kun til-og-med dato fra uendelig (null) til en gitt dato
        val oppdatertKompetanse = valutakurs(jan(2020), "444", "EUR", barn1)
        valutakursService.oppdaterValutakurs(behandlingId, oppdatertKompetanse)

        // Forventer skjema uten innhold (MEN MED VALUTAKODE) fra oppdatert dato og fremover
        val forventedeValutakurser = ValutakursBuilder(jan(2020), behandlingId)
            .medKurs("444$>", "EUR", barn1)
            .bygg()

        val faktiskeValutakurser = valutakursService.hentValutakurser(behandlingId)
        assertEqualsUnordered(forventedeValutakurser, faktiskeValutakurser)
    }
}

fun valutakurs(tidspunkt: Tidspunkt<Måned>, s: String, valutakode: String, vararg barn: Person) =
    ValutakursBuilder(tidspunkt).medKurs(s, valutakode, *barn).bygg().first()
