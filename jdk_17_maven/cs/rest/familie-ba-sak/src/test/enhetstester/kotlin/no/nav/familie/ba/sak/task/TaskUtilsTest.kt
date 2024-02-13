package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.core.env.Environment
import java.time.LocalDateTime

class TaskUtilsTest {

    @ParameterizedTest
    @CsvSource(
        "2020-06-09T13:37:00, 2020-06-09T14:37:00", // Innenfor dagtid
        "2020-06-09T21:37:00, 2020-06-10T06:37:00", // Utenfor dagtid, men på en hverdag. Venter til dagen etter klokken 6
        "2020-06-12T19:37:00, 2020-06-12T20:37:00", // Innenfor dagtid på en fredag
        "2020-06-12T21:37:00, 2020-06-15T06:37:00", // Utenfor dagtid på en fredag. Venter til mandag morgen
        "2020-06-13T09:37:00, 2020-06-15T06:37:00", // Lørdag morgen. Venter til mandag morgen
        "2020-06-13T19:37:00, 2020-06-15T06:37:00", // Lørdag kveld. Venter til mandag morgen
        "2020-06-14T09:37:00, 2020-06-15T06:37:00", // Søndag morgen. Venter til mandag morgen
        "2020-06-14T19:37:00, 2020-06-15T06:37:00", // Søndag kveld. Venter til mandag morgen
        "2020-06-14T21:37:00, 2020-06-15T06:37:00", // Søndag etter 21. Venter til mandag morgen
        "2020-05-17T15:37:00, 2020-05-18T06:37:00", // Innenfor dagtid 17 mai. Venter til morgenen etter
        "2020-05-17T05:37:00, 2020-05-18T06:37:00", // Før dagtid 17 mai. Venter til morgenen etter
        "2020-05-17T22:37:00, 2020-05-18T06:37:00", // Etter dagtid 17 mai. Venter til morgenen etter
        "2021-05-14T21:30:00, 2021-05-18T06:30:00", // 14 mai er fredag, 17 mai er mandag og fridag. Venter til 18 mai klokken 6
    )
    fun `skal returnere neste arbeidsdag `(input: LocalDateTime, expected: LocalDateTime) {
        mockkStatic(LocalDateTime::class)
        mockk<Environment>(relaxed = true)

        every { LocalDateTime.now() } returns input

        assertEquals(expected, nesteGyldigeTriggertidForBehandlingIHverdager(60))
    }
}
