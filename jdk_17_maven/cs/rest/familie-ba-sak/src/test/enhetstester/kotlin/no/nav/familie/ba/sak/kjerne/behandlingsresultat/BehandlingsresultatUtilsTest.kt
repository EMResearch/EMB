package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import io.mockk.clearStaticMockk
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.YearMonth
import java.util.stream.Stream

class BehandlingsresultatUtilsTest {

    val søker = tilfeldigPerson()

    @BeforeEach
    fun reset() {
        clearStaticMockk(YearMonth::class)
    }

    @ParameterizedTest(name = "Søknadsresultat {0}, Endringsresultat {1} og Opphørsresultat {2} skal kombineres til behandlingsresultat {3}")
    @MethodSource("hentKombinasjonerOgBehandlingsResultat")
    internal fun `Kombiner resultater - skal kombinere til riktig behandlingsresultat gitt forskjellige kombinasjoner av resultater`(
        søknadsresultat: Søknadsresultat?,
        endringsresultat: Endringsresultat,
        opphørsresultat: Opphørsresultat,
        behandlingsresultat: Behandlingsresultat,
    ) {
        val kombinertResultat = BehandlingsresultatUtils.kombinerResultaterTilBehandlingsresultat(
            søknadsresultat,
            endringsresultat,
            opphørsresultat,
        )

        assertEquals(kombinertResultat, behandlingsresultat)
    }

    @ParameterizedTest(name = "Søknadsresultat {0}, Endringsresultat {1} og Opphørsresultat {2} skal kaste feil")
    @MethodSource("hentUgyldigeKombinasjoner")
    internal fun `Kombiner resultater - skal kaste feil ved ugyldige kombinasjoner av resultat`(
        søknadsresultat: Søknadsresultat?,
        endringsresultat: Endringsresultat,
        opphørsresultat: Opphørsresultat,
    ) {
        assertThrows<FunksjonellFeil> {
            BehandlingsresultatUtils.kombinerResultaterTilBehandlingsresultat(
                søknadsresultat,
                endringsresultat,
                opphørsresultat,
            )
        }
    }

    companion object {
        @JvmStatic
        fun hentKombinasjonerOgBehandlingsResultat() =
            Stream.of(
                Arguments.of(Søknadsresultat.INGEN_RELEVANTE_ENDRINGER, Endringsresultat.INGEN_ENDRING, Opphørsresultat.IKKE_OPPHØRT, Behandlingsresultat.FORTSATT_INNVILGET),
                Arguments.of(Søknadsresultat.INGEN_RELEVANTE_ENDRINGER, Endringsresultat.ENDRING, Opphørsresultat.IKKE_OPPHØRT, Behandlingsresultat.ENDRET_OG_FORTSATT_INNVILGET),
                Arguments.of(Søknadsresultat.INNVILGET, Endringsresultat.ENDRING, Opphørsresultat.OPPHØRT, Behandlingsresultat.INNVILGET_ENDRET_OG_OPPHØRT),
                Arguments.of(Søknadsresultat.INNVILGET, Endringsresultat.ENDRING, Opphørsresultat.FORTSATT_OPPHØRT, Behandlingsresultat.INNVILGET_OG_ENDRET),
                Arguments.of(Søknadsresultat.INNVILGET, Endringsresultat.ENDRING, Opphørsresultat.IKKE_OPPHØRT, Behandlingsresultat.INNVILGET_OG_ENDRET),
                Arguments.of(Søknadsresultat.INNVILGET, Endringsresultat.INGEN_ENDRING, Opphørsresultat.OPPHØRT, Behandlingsresultat.INNVILGET_OG_OPPHØRT),
                Arguments.of(Søknadsresultat.INNVILGET, Endringsresultat.INGEN_ENDRING, Opphørsresultat.FORTSATT_OPPHØRT, Behandlingsresultat.INNVILGET),
                Arguments.of(Søknadsresultat.INNVILGET, Endringsresultat.INGEN_ENDRING, Opphørsresultat.IKKE_OPPHØRT, Behandlingsresultat.INNVILGET),
                Arguments.of(Søknadsresultat.AVSLÅTT, Endringsresultat.ENDRING, Opphørsresultat.OPPHØRT, Behandlingsresultat.AVSLÅTT_ENDRET_OG_OPPHØRT),
                Arguments.of(Søknadsresultat.AVSLÅTT, Endringsresultat.ENDRING, Opphørsresultat.FORTSATT_OPPHØRT, Behandlingsresultat.AVSLÅTT_OG_ENDRET),
                Arguments.of(Søknadsresultat.AVSLÅTT, Endringsresultat.ENDRING, Opphørsresultat.IKKE_OPPHØRT, Behandlingsresultat.AVSLÅTT_OG_ENDRET),
                Arguments.of(Søknadsresultat.AVSLÅTT, Endringsresultat.INGEN_ENDRING, Opphørsresultat.OPPHØRT, Behandlingsresultat.AVSLÅTT_OG_OPPHØRT),
                Arguments.of(Søknadsresultat.AVSLÅTT, Endringsresultat.INGEN_ENDRING, Opphørsresultat.FORTSATT_OPPHØRT, Behandlingsresultat.AVSLÅTT),
                Arguments.of(Søknadsresultat.AVSLÅTT, Endringsresultat.INGEN_ENDRING, Opphørsresultat.IKKE_OPPHØRT, Behandlingsresultat.AVSLÅTT),
                Arguments.of(Søknadsresultat.DELVIS_INNVILGET, Endringsresultat.ENDRING, Opphørsresultat.OPPHØRT, Behandlingsresultat.DELVIS_INNVILGET_ENDRET_OG_OPPHØRT),
                Arguments.of(Søknadsresultat.DELVIS_INNVILGET, Endringsresultat.ENDRING, Opphørsresultat.FORTSATT_OPPHØRT, Behandlingsresultat.DELVIS_INNVILGET_OG_ENDRET),
                Arguments.of(Søknadsresultat.DELVIS_INNVILGET, Endringsresultat.ENDRING, Opphørsresultat.IKKE_OPPHØRT, Behandlingsresultat.DELVIS_INNVILGET_OG_ENDRET),
                Arguments.of(Søknadsresultat.DELVIS_INNVILGET, Endringsresultat.INGEN_ENDRING, Opphørsresultat.OPPHØRT, Behandlingsresultat.DELVIS_INNVILGET_OG_OPPHØRT),
                Arguments.of(Søknadsresultat.DELVIS_INNVILGET, Endringsresultat.INGEN_ENDRING, Opphørsresultat.FORTSATT_OPPHØRT, Behandlingsresultat.DELVIS_INNVILGET),
                Arguments.of(Søknadsresultat.DELVIS_INNVILGET, Endringsresultat.INGEN_ENDRING, Opphørsresultat.IKKE_OPPHØRT, Behandlingsresultat.DELVIS_INNVILGET),
                Arguments.of(null, Endringsresultat.ENDRING, Opphørsresultat.OPPHØRT, Behandlingsresultat.ENDRET_OG_OPPHØRT),
                Arguments.of(null, Endringsresultat.ENDRING, Opphørsresultat.FORTSATT_OPPHØRT, Behandlingsresultat.ENDRET_UTBETALING),
                Arguments.of(null, Endringsresultat.ENDRING, Opphørsresultat.IKKE_OPPHØRT, Behandlingsresultat.ENDRET_UTBETALING),
                Arguments.of(null, Endringsresultat.INGEN_ENDRING, Opphørsresultat.OPPHØRT, Behandlingsresultat.OPPHØRT),
                Arguments.of(null, Endringsresultat.INGEN_ENDRING, Opphørsresultat.FORTSATT_OPPHØRT, Behandlingsresultat.FORTSATT_OPPHØRT),
                Arguments.of(null, Endringsresultat.INGEN_ENDRING, Opphørsresultat.IKKE_OPPHØRT, Behandlingsresultat.FORTSATT_INNVILGET),
            )

        @JvmStatic
        fun hentUgyldigeKombinasjoner() =
            Stream.of(
                Arguments.of(Søknadsresultat.INGEN_RELEVANTE_ENDRINGER, Endringsresultat.INGEN_ENDRING, Opphørsresultat.OPPHØRT),
                Arguments.of(Søknadsresultat.INGEN_RELEVANTE_ENDRINGER, Endringsresultat.INGEN_ENDRING, Opphørsresultat.FORTSATT_OPPHØRT),
                Arguments.of(Søknadsresultat.INGEN_RELEVANTE_ENDRINGER, Endringsresultat.ENDRING, Opphørsresultat.OPPHØRT),
                Arguments.of(Søknadsresultat.INGEN_RELEVANTE_ENDRINGER, Endringsresultat.ENDRING, Opphørsresultat.FORTSATT_OPPHØRT),
            )
    }
}
