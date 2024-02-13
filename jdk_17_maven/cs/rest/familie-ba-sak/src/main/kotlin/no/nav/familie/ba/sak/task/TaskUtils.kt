package no.nav.familie.ba.sak.task

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.temporal.TemporalAdjusters

/**
 * Finner neste gyldige kjøringstidspunkt for tasker som kun skal kjøre på "dagtid".
 *
 * Dagtid er nå definert som hverdager mellom 06-21. Faste helligdager er tatt høyde for, men flytende
 * er ikke kodet inn.
 */
fun nesteGyldigeTriggertidForBehandlingIHverdager(
    minutesToAdd: Long = 0,
    triggerTid: LocalDateTime = LocalDateTime.now(),
): LocalDateTime {
    var date = triggerTid.plusMinutes(minutesToAdd)

    date = if (erKlokkenMellom21Og06(date.toLocalTime()) && date.erHverdag(1)) {
        kl06IdagEllerNesteDag(date)
    } else if (erKlokkenMellom21Og06(date.toLocalTime()) || !date.erHverdag(0)) {
        date.with(TemporalAdjusters.next(DayOfWeek.MONDAY)).withHour(6)
    } else {
        date
    }

    when {
        date.dayOfMonth == 1 && date.month == Month.JANUARY -> date = date.plusDays(1)
        date.dayOfMonth == 1 && date.month == Month.MAY -> date = date.plusDays(1)
        date.dayOfMonth == 17 && date.month == Month.MAY -> date = date.plusDays(1)
        date.dayOfMonth == 25 && date.month == Month.DECEMBER -> date = date.plusDays(2)
        date.dayOfMonth == 26 && date.month == Month.DECEMBER -> date = date.plusDays(1)
    }

    when (date.dayOfWeek) {
        DayOfWeek.SATURDAY -> date = date.plusDays(2)
        DayOfWeek.SUNDAY -> date = date.plusDays(1)
        else -> {
            // NOP
        }
    }

    return date
}

fun LocalDateTime.erHverdag(plusDays: Long): Boolean {
    return when (this.plusDays(plusDays).dayOfWeek) {
        DayOfWeek.MONDAY -> true
        DayOfWeek.TUESDAY -> true
        DayOfWeek.WEDNESDAY -> true
        DayOfWeek.THURSDAY -> true
        DayOfWeek.FRIDAY -> true
        DayOfWeek.SATURDAY -> false
        DayOfWeek.SUNDAY -> false
        else -> error("Not implemented")
    }
}

fun erKlokkenMellom21Og06(localTime: LocalTime = LocalTime.now()): Boolean {
    return localTime.isAfter(LocalTime.of(21, 0)) || localTime.isBefore(LocalTime.of(6, 0))
}

fun kl06IdagEllerNesteDag(date: LocalDateTime = LocalDateTime.now()): LocalDateTime {
    return if (date.toLocalTime().isBefore(LocalTime.of(6, 0))) {
        date.withHour(6)
    } else {
        date.plusDays(1).withHour(6)
    }
}
