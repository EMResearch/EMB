package no.nav.familie.ba.sak.kjerne.fagsak

import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.ekstern.restDomene.InstitusjonInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class FagsakIntegrationTest(
    @Autowired
    val fagsakService: FagsakService,
) : AbstractSpringIntegrationTest() {

    @Test
    fun `hentMinimalFagsakerForPerson() skal return begge fagsaker for en person`() {
        val personFnr = randomFnr()
        val fagsakOmsorgperson = fagsakService.hentEllerOpprettFagsak(personFnr)
        val fagsakInstitusjon = fagsakService.hentEllerOpprettFagsak(
            personFnr,
            false,
            FagsakType.INSTITUSJON,
            InstitusjonInfo("orgnr", null),
        )
        val fagsakEnsligMindreÅrig =
            fagsakService.hentEllerOpprettFagsak(personFnr, false, FagsakType.BARN_ENSLIG_MINDREÅRIG)

        val minimalFagsakList = fagsakService.hentMinimalFagsakerForPerson(fagsakOmsorgperson.aktør)

        assertThat(minimalFagsakList.data).hasSize(3).extracting("id")
            .contains(fagsakInstitusjon.id, fagsakOmsorgperson.id, fagsakEnsligMindreÅrig.id)
    }

    @Test
    fun `hentMinimalFagsakForPerson() skal return riktig fagsak for en person`() {
        val personFnr = randomFnr()
        val fagsakOmsorgperson = fagsakService.hentEllerOpprettFagsak(personFnr)
        val fagsakInstitusjon = fagsakService.hentEllerOpprettFagsak(
            personFnr,
            false,
            FagsakType.INSTITUSJON,
            InstitusjonInfo("orgnr", null),
        )
        val fagsakEnsligMindreÅrig =
            fagsakService.hentEllerOpprettFagsak(personFnr, false, FagsakType.BARN_ENSLIG_MINDREÅRIG)

        val defaultMinimalFagsak = fagsakService.hentMinimalFagsakForPerson(fagsakOmsorgperson.aktør)
        assertThat(defaultMinimalFagsak.data!!.id).isEqualTo(fagsakOmsorgperson.id)

        val omsorgpersonMinimalFagsak =
            fagsakService.hentMinimalFagsakForPerson(fagsakOmsorgperson.aktør, FagsakType.NORMAL)
        assertThat(omsorgpersonMinimalFagsak.data!!.id).isEqualTo(fagsakOmsorgperson.id)

        val institusjonMinimalFagsak =
            fagsakService.hentMinimalFagsakForPerson(fagsakOmsorgperson.aktør, FagsakType.INSTITUSJON)
        assertThat(institusjonMinimalFagsak.data!!.id).isEqualTo(fagsakInstitusjon.id)

        val ensligMindreÅrigMinimalFagsak =
            fagsakService.hentMinimalFagsakForPerson(fagsakOmsorgperson.aktør, FagsakType.BARN_ENSLIG_MINDREÅRIG)
        assertThat(ensligMindreÅrigMinimalFagsak.data!!.id).isEqualTo(fagsakEnsligMindreÅrig.id)
    }
}
