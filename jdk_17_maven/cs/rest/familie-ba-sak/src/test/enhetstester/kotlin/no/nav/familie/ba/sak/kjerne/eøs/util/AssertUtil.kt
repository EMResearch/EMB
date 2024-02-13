package no.nav.familie.ba.sak.kjerne.eøs

import org.junit.jupiter.api.Assertions

fun <T> assertEqualsUnordered(
    expected: Collection<T>,
    actual: Collection<T>,
) {
    Assertions.assertEquals(
        expected.size,
        actual.size,
        "Forskjellig antall. Forventet ${expected.size} men fikk ${actual.size}",
    )
    Assertions.assertTrue(
        expected.containsAll(actual),
        "Forvantet liste inneholder ikke alle elementene fra faktisk liste",
    )
    Assertions.assertTrue(
        actual.containsAll(expected),
        "Faktisk liste inneholder ikke alle elementene fra forventet liste",
    )
}
