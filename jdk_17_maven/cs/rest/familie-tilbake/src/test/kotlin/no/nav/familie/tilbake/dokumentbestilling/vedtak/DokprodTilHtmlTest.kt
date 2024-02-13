package no.nav.familie.tilbake.dokumentbestilling.vedtak

import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.DokprodTilHtml
import org.junit.jupiter.api.Test

class DokprodTilHtmlTest {

    @Test
    fun `dokprodInnholdTilHtml skal konvertere overskrift og avsnitt`() {
        val resultat: String =
            DokprodTilHtml.dokprodInnholdTilHtml("_Overskrift\nFørste avsnitt\n\nAndre avsnitt\n\nTredje avsnitt")

        resultat shouldBe "<div class=\"samepage\"><h2>Overskrift</h2><p>Første avsnitt</p>" +
            "</div><p>Andre avsnitt</p><p>Tredje avsnitt</p>"
    }

    @Test
    fun `dokprodInnholdTilHtml skal konvertere non break space`() {
        // utf8nonBreakingSpace = "\u00A0";
        val resultat: String = DokprodTilHtml.dokprodInnholdTilHtml("10\u00A0000\u00A0kroner")

        resultat shouldBe "<p>10&nbsp;000&nbsp;kroner</p>"
    }

    @Test
    fun `dokprodInnholdTilHtml skal konvertere bullet points`() {
        val resultat: String = DokprodTilHtml.dokprodInnholdTilHtml("*-bulletpoint 1\nbulletpoint 2\nsiste bulletpoint-*")

        resultat shouldBe "<ul><li>bulletpoint 1</li><li>bulletpoint 2</li><li>siste bulletpoint</li></ul>"
    }

    @Test
    fun `dokprodInnholdTilHtml skal konvertere bullet points når første linje er tom`() {
        val resultat: String = DokprodTilHtml.dokprodInnholdTilHtml("*-\nbulletpoint 1\nbulletpoint 2\nsiste bulletpoint-*")

        resultat shouldBe "<ul><li>bulletpoint 1</li><li>bulletpoint 2</li><li>siste bulletpoint</li></ul>"
    }

    @Test
    fun `dokprodInnholdTilHtml skal konvertere halvhjertede avsnitt`() {
        // halvhjertet avsnitt er hvor det er tatt kun ett linjeskift.
        val resultat: String = DokprodTilHtml.dokprodInnholdTilHtml("Foo\nBar")

        resultat shouldBe "<p>Foo<br/>Bar</p>"
    }

    @Test
    fun `dokprodInnholdTilHtml skal spesialbehandle hilsen`() {
        // halvhjertet avsnitt er hvor det er tatt kun ett linjeskift.
        val resultat: String = DokprodTilHtml.dokprodInnholdTilHtml("Med vennlig hilsen\nNAV Familie- og pensjonsytelser")

        resultat shouldBe "<p class=\"hilsen\">Med vennlig hilsen<br/>NAV Familie- og pensjonsytelser</p>"
    }
}
