package no.nav.familie.tilbake.dokumentbestilling.felles.header

import io.kotest.matchers.shouldBe
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TekstformatererHeaderTest {

    private val brevmetadata = Brevmetadata(
        sakspartId = "12345678901",
        sakspartsnavn = "Test",
        vergenavn = "John Doe",
        mottageradresse = Adresseinfo("12345678901", "Test"),
        behandlendeEnhetsNavn = "NAV Familie- og pensjonsytelser Skien",
        ansvarligSaksbehandler = "Bob",
        saksnummer = "1232456",
        språkkode = Språkkode.NB,
        ytelsestype = Ytelsestype.BARNETILSYN,
        gjelderDødsfall = false,
    )

    @Test
    fun `lagHeader brev til bruker`() {
        val generertHeader: String = TekstformatererHeader.lagHeader(brevmetadata, "Dette er en header")
        generertHeader shouldBe personHeader()
    }

    @Test
    fun `lagHeader brev til institusjon`() {
        val generertHeader: String =
            TekstformatererHeader.lagHeader(
                brevmetadata = brevmetadata.copy(institusjon = Institusjon("987654321", "Test & institusjon")),
                overskrift = "Dette er en header",
            )
        generertHeader shouldBe institusjonHeader()
    }

    private fun personHeader(): String {
        return """<div id="dato">Dato: ${dagensDato()}</div>
<h1 id="hovedoverskrift">Dette er en header</h1>
<div id="person">
Navn: Test<br/>
Fødselsnummer: 12345678901
</div>"""
    }

    private fun institusjonHeader(): String {
        return """<div id="dato">Dato: ${dagensDato()}</div>
<h1 id="hovedoverskrift">Dette er en header</h1>
<div id="institusjon">
Navn: Test &amp; institusjon<br/>
Organisasjonsnummer: 987654321
</div>
<div id="person">
Gjelder: Test<br/>
Fødselsnummer: 12345678901
</div>"""
    }

    private val format = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private fun dagensDato(): String {
        return format.format(LocalDate.now())
    }
}
