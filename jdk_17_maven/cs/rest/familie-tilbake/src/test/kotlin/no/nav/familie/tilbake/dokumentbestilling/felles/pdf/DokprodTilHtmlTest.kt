package no.nav.familie.tilbake.dokumentbestilling.felles.pdf

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class DokprodTilHtmlTest {

    @Test
    fun `dokprodInnholdTilHtml skal Konvertere Overskrift Og Avsnitt`() {
        val resultat =
            DokprodTilHtml.dokprodInnholdTilHtml("_Overskrift\nFørste avsnitt\n\nAndre avsnitt\n\nTredje avsnitt")
        resultat shouldBe "<div class=\"samepage\"><h2>Overskrift</h2><p>Første avsnitt</p>" +
            "</div><p>Andre avsnitt</p><p>Tredje avsnitt</p>"
    }

    @Test
    fun `dokprodInnholdTilHtml skal Konvertere Non Breaking Space`() {
        // utf8nonBreakingSpace = "\u00A0";
        val resultat = DokprodTilHtml.dokprodInnholdTilHtml("10\u00A0000\u00A0kroner")

        resultat shouldBe "<p>10&nbsp;000&nbsp;kroner</p>"
    }

    @Test
    fun `dokprodInnholdTilHtml skal Konvertere Bullet Points`() {
        val resultat = DokprodTilHtml.dokprodInnholdTilHtml("*-bulletpoint 1\nbulletpoint 2\nsiste bulletpoint-*")

        resultat shouldBe "<ul><li>bulletpoint 1</li><li>bulletpoint 2</li><li>siste bulletpoint</li></ul>"
    }

    @Test
    fun `dokprodInnholdTilHtml skal Konvertere Bullet Points Når Første Linje Er Tom`() {
        val resultat = DokprodTilHtml.dokprodInnholdTilHtml("*-\nbulletpoint 1\nbulletpoint 2\nsiste bulletpoint-*")

        resultat shouldBe "<ul><li>bulletpoint 1</li><li>bulletpoint 2</li><li>siste bulletpoint</li></ul>"
    }

    @Test
    fun `dokprodInnholdTilHtml skal Konvertere Halvhjertede Avsnitt`() {
        // halvhjertet avsnitt er hvor det er tatt kun ett linjeskift.
        val resultat = DokprodTilHtml.dokprodInnholdTilHtml("Foo\nBar")

        resultat shouldBe "<p>Foo<br/>Bar</p>"
    }

    @Test
    fun `dokprodInnholdTilHtml skal Spesialbehandle Hilsen`() {
        // halvhjertet avsnitt er hvor det er tatt kun ett linjeskift.
        val resultat = DokprodTilHtml.dokprodInnholdTilHtml("Med vennlig hilsen\nNAV Familie- og pensjonsytelser")

        resultat shouldBe "<p class=\"hilsen\">Med vennlig hilsen<br/>NAV Familie- og pensjonsytelser</p>"
    }

    @Test
    fun `dokprodInnholdTilHtml skal konvertere ampersand`() {
        val resultat = DokprodTilHtml.dokprodInnholdTilHtml("Foo & Bar")

        resultat shouldBe "<p>Foo &amp; Bar</p>"
    }
}
