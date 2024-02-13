package no.nav.tag.tiltaksgjennomforing.utils;

import java.time.*;

// Inspirasjon fra https://medium.com/agorapulse-stories/how-to-solve-now-problem-in-your-java-tests-7c7f4a6d703c
public class Now {
    private static ThreadLocal<Clock> clock = ThreadLocal.withInitial(Clock::systemDefaultZone);

    public static void fixedDate(LocalDate localDate) {
        Instant instant = ZonedDateTime.of(localDate.atStartOfDay(), ZoneId.systemDefault()).toInstant();
        clock.set(Clock.fixed(instant, ZoneId.systemDefault()));
    }

    public static void resetClock() {
        Now.clock.set(Clock.systemDefaultZone());
    }

    public static Instant instant() {
        return Instant.now(clock.get());
    }

    public static LocalDate localDate() {
        return LocalDate.now(clock.get());
    }

    public static LocalDateTime localDateTime() {
        return LocalDateTime.now(clock.get());
    }

    public static YearMonth yearMonth() {
        return YearMonth.now(clock.get());
    }
}
