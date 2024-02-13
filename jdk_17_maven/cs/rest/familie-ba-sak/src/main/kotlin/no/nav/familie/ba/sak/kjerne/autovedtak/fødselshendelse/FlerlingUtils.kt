package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse

import no.nav.familie.ba.sak.integrasjoner.pdl.domene.ForelderBarnRelasjon
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import org.slf4j.Logger

fun finnBarnSomSkalBehandlesForMor(
    nyBehandlingHendelse: NyBehandlingHendelse,
    barnaTilMor: List<ForelderBarnRelasjon>,
    barnaSomHarBlittBehandlet: List<String>,
    secureLogger: Logger? = null,
): Pair<List<String>, List<String>> {
    val barnaPåHendelse =
        barnaTilMor.filter { nyBehandlingHendelse.barnasIdenter.contains(it.aktør.aktivFødselsnummer()) }
    val andreBarnFødtInnenEnDag = barnaTilMor.filter {
        barnaPåHendelse.any { barnPåHendelse ->
            barnPåHendelse.aktør != it.aktør &&
                (
                    barnPåHendelse.fødselsdato == it.fødselsdato ||
                        barnPåHendelse.fødselsdato?.plusDays(1) == it.fødselsdato ||
                        barnPåHendelse.fødselsdato?.minusDays(1) == it.fødselsdato
                    )
        }
    }

    val alleBarnSomKanBehandles = (barnaPåHendelse + andreBarnFødtInnenEnDag).map { it.aktør.aktivFødselsnummer() }
    val barnSomSkalBehandlesForMor = alleBarnSomKanBehandles
        .filter { !barnaSomHarBlittBehandlet.contains(it) }

    secureLogger?.info(
        "Behandler fødselshendelse på ${nyBehandlingHendelse.morsIdent}. " +
            "Alle barna til mor: ${barnaTilMor.map { it.toSecureString() }}\n" +
            "Barn på hendelse: ${barnaPåHendelse.map { it.aktør.aktivFødselsnummer() }}\n" +
            "Barn med tilstøtende fødselsdato som også behandles: ${andreBarnFødtInnenEnDag.map { it.aktør.aktivFødselsnummer() }}\n" +
            "Barn som faktisk skal behandles for mor: ${barnSomSkalBehandlesForMor.map { it }}",
    )

    return Pair(barnSomSkalBehandlesForMor, alleBarnSomKanBehandles)
}

fun barnPåHendelseBlirAlleredeBehandletIÅpenBehandling(
    barnaPåHendelse: List<Aktør>,
    barnaPåÅpenBehandling: List<Aktør>,
): Boolean {
    return barnaPåHendelse.all { barnaPåÅpenBehandling.contains(it) }
}
