package no.nav.familie.tilbake.dokumentbestilling.innhentdokumentasjon

import io.kotest.matchers.shouldBe
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.dokumentbestilling.felles.header.Institusjon
import no.nav.familie.tilbake.dokumentbestilling.innhentdokumentasjon.handlebars.dto.InnhentDokumentasjonsbrevsdokument
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.Scanner

class TekstformatererInnhentDokumentasjonsbrevTest {

    private val metadata = Brevmetadata(
        sakspartId = "12345678901",
        sakspartsnavn = "Test",
        mottageradresse = Adresseinfo("12345678901", "Test"),
        behandlendeEnhetsNavn = "NAV Familie- og pensjonsytelser Skien",
        ansvarligSaksbehandler = "Bob",
        saksnummer = "1232456",
        språkkode = Språkkode.NB,
        ytelsestype = Ytelsestype.BARNETILSYN,
        gjelderDødsfall = false,
    )
    private val innhentDokumentasjonsbrevsdokument =
        InnhentDokumentasjonsbrevsdokument(
            brevmetadata = metadata,
            fritekstFraSaksbehandler = "Dette er ein fritekst.",
            fristdato = LocalDate.of(2020, 3, 2),
        )

    @Test
    fun `lagInnhentDokumentasjonBrevFritekst skal generere innhentdokumentasjonbrev`() {
        val generertBrev = TekstformatererInnhentDokumentasjonsbrev.lagFritekst(innhentDokumentasjonsbrevsdokument)

        val fasit = les("/innhentdokumentasjonbrev/innhentdokumentasjonbrev.txt")
        generertBrev shouldBe fasit
    }

    @Test
    fun `lagInnhentDokumentasjonBrevFritekst skal generere innhentdokumentasjonbrev dødsfall bruker`() {
        val metadata = metadata.copy(gjelderDødsfall = true)
        val dokument = innhentDokumentasjonsbrevsdokument.copy(brevmetadata = metadata)
        val generertBrev = TekstformatererInnhentDokumentasjonsbrev.lagFritekst(dokument)

        val fasit = les("/innhentdokumentasjonbrev/innhentdokumentasjonbrev_død_bruker.txt")
        generertBrev shouldBe fasit
    }

    @Test
    fun `lagInnhentDokumentasjonBrevFritekst skal generere innhentdokumentasjonbrev institusjon`() {
        val metadata = metadata.copy(institusjon = Institusjon("test", "123"))
        val dokument = innhentDokumentasjonsbrevsdokument.copy(brevmetadata = metadata)
        val generertBrev = TekstformatererInnhentDokumentasjonsbrev.lagFritekst(dokument)

        val fasit = les("/innhentdokumentasjonbrev/innhentdokumentasjonbrev_institusjon.txt")
        generertBrev shouldBe fasit
    }

    @Test
    fun `lagInnhentDokumentasjonBrevFritekst skal generere innhentdokumentasjonbrev for verge`() {
        val brevMetadata = metadata.copy(vergenavn = "John Doe", finnesVerge = true, finnesAnnenMottaker = true)
        val dokument = innhentDokumentasjonsbrevsdokument.copy(brevmetadata = brevMetadata)

        val generertBrev = TekstformatererInnhentDokumentasjonsbrev.lagFritekst(dokument)

        val fasit = les("/innhentdokumentasjonbrev/innhentdokumentasjonbrev.txt")
        val vergeTekst = les("/varselbrev/verge.txt")
        generertBrev shouldBe "$fasit${System.lineSeparator().repeat(2)}$vergeTekst"
    }

    @Test
    fun `lagInnhentDokumentasjonBrevFritekst skal generere innhentdokumentasjonbrev for verge organisasjon`() {
        val brevMetadata = metadata.copy(
            mottageradresse = Adresseinfo(
                ident = "12345678901",
                mottagernavn = "Semba AS c/o John Doe",
            ),
            sakspartsnavn = "Test",
            vergenavn = "John Doe",
            finnesVerge = true,
            finnesAnnenMottaker = true,
        )
        val dokument = innhentDokumentasjonsbrevsdokument.copy(brevmetadata = brevMetadata)

        val generertBrev = TekstformatererInnhentDokumentasjonsbrev.lagFritekst(dokument)

        val fasit = les("/innhentdokumentasjonbrev/innhentdokumentasjonbrev.txt")
        val vergeTekst = "Brev med likt innhold er sendt til Test"
        generertBrev shouldBe "$fasit${System.lineSeparator().repeat(2)}$vergeTekst"
    }

    @Test
    fun `lagInnhentDokumentasjonBrevFritekst skal generere innhentdokumentasjonbrev nynorsk`() {
        val brevMetadata = metadata.copy(språkkode = Språkkode.NN)
        val dokument =
            innhentDokumentasjonsbrevsdokument.copy(brevmetadata = brevMetadata.copy(ytelsestype = Ytelsestype.BARNETRYGD))

        val generertBrev = TekstformatererInnhentDokumentasjonsbrev.lagFritekst(dokument)

        val fasit = les("/innhentdokumentasjonbrev/innhentdokumentasjonbrev_nn.txt")
        generertBrev shouldBe fasit
    }

    @Test
    fun `lagInnhentDokumentasjonBrevFritekst skal generere innhentdokumentasjonbrev nynorsk dødsfall bruker`() {
        val brevMetadata = metadata.copy(språkkode = Språkkode.NN, gjelderDødsfall = true)
        val dokument =
            innhentDokumentasjonsbrevsdokument.copy(brevmetadata = brevMetadata.copy(ytelsestype = Ytelsestype.BARNETRYGD))

        val generertBrev = TekstformatererInnhentDokumentasjonsbrev.lagFritekst(dokument)

        val fasit = les("/innhentdokumentasjonbrev/innhentdokumentasjonbrev_nn_død_bruker.txt")
        generertBrev shouldBe fasit
    }

    @Test
    fun `lagInnhentDokumentasjonBrevFritekst skal generere innhentdokumentasjonbrev nynorsk institusjon`() {
        val brevMetadata = metadata.copy(språkkode = Språkkode.NN, institusjon = Institusjon("123", "123"))
        val dokument =
            innhentDokumentasjonsbrevsdokument.copy(brevmetadata = brevMetadata.copy(ytelsestype = Ytelsestype.BARNETRYGD))

        val generertBrev = TekstformatererInnhentDokumentasjonsbrev.lagFritekst(dokument)

        val fasit = les("/innhentdokumentasjonbrev/innhentdokumentasjonbrev_nn_institusjon.txt")
        generertBrev shouldBe fasit
    }

    @Test
    fun `lagInnhentDokumentasjonBrevOverskrift skal generere innhentdokumentasjonbrev overskrift`() {
        val overskrift = TekstformatererInnhentDokumentasjonsbrev.lagOverskrift(innhentDokumentasjonsbrevsdokument.brevmetadata)

        val fasit = "Vi trenger flere opplysninger"
        overskrift shouldBe fasit
    }

    @Test
    fun `lagInnhentDokumentasjonBrevOverskrift skal generere innhentdokumentasjonbrev overskrift nynorsk`() {
        val brevMetadata = metadata.copy(språkkode = Språkkode.NN)

        val overskrift = TekstformatererInnhentDokumentasjonsbrev
            .lagOverskrift(brevMetadata)

        val fasit = "Vi trenger fleire opplysningar"
        overskrift shouldBe fasit
    }

    private fun les(filnavn: String): String? {
        javaClass.getResourceAsStream(filnavn).use { resource ->
            Scanner(resource, StandardCharsets.UTF_8).use { scanner ->
                scanner.useDelimiter("\\A")
                return if (scanner.hasNext()) scanner.next() else null
            }
        }
    }
}
