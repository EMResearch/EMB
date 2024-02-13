package no.nav.familie.ba.sak.kjerne.behandling.domene

import no.nav.familie.ba.sak.kjerne.behandling.Søknadsinfo
import no.nav.familie.kontrakter.ba.søknad.v4.Søknadstype.ORDINÆR
import no.nav.familie.kontrakter.ba.søknad.v4.Søknadstype.UTVIDET
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Service
class BehandlingSøknadsinfoService(
    private val behandlingSøknadsinfoRepository: BehandlingSøknadsinfoRepository,
) {

    @Transactional
    fun lagreNedSøknadsinfo(mottattDato: LocalDate, søknadsinfo: Søknadsinfo?, behandling: Behandling) {
        val behandlingSøknadsinfo = BehandlingSøknadsinfo(
            behandling = behandling,
            mottattDato = mottattDato.atStartOfDay(),
            journalpostId = søknadsinfo?.journalpostId,
            brevkode = søknadsinfo?.brevkode,
            erDigital = søknadsinfo?.erDigital,
        )
        behandlingSøknadsinfoRepository.save(behandlingSøknadsinfo)
    }

    fun hentSøknadMottattDato(behandlingId: Long): LocalDateTime? {
        return behandlingSøknadsinfoRepository.findByBehandlingId(behandlingId).minOfOrNull { it.mottattDato }
    }

    fun hentSøknadsstatistikk(fom: LocalDate, tom: LocalDate): SøknadsstatistikkForPeriode {
        val antallSøknaderPerGruppe =
            behandlingSøknadsinfoRepository.hentAntallSøknaderIPeriode(fom.atStartOfDay(), tom.atTime(LocalTime.MAX))

        val antallOrdinære =
            antallSøknaderPerGruppe.filter { it.brevkode == ORDINÆR.søknadskode }.sumOf { it.antall }
        val antallOrdinæreDigitale =
            antallSøknaderPerGruppe.singleOrNull { it.brevkode == ORDINÆR.søknadskode && it.erDigital }?.antall ?: 0

        val antallUtvidet =
            antallSøknaderPerGruppe.filter { it.brevkode == UTVIDET.søknadskode }.sumOf { it.antall }
        val antallUtvidetDigitale =
            antallSøknaderPerGruppe.singleOrNull { it.erDigital && it.brevkode == UTVIDET.søknadskode }?.antall ?: 0

        return SøknadsstatistikkForPeriode(
            fom = fom,
            tom = tom,
            ordinærBarnetrygd = AntallSøknader(
                totalt = antallOrdinære,
                papirsøknader = antallOrdinære - antallOrdinæreDigitale,
                digitaleSøknader = antallOrdinæreDigitale,
                digitaliseringsgrad = antallOrdinæreDigitale / antallOrdinære.toFloat(),
            ),
            utvidetBarnetrygd = AntallSøknader(
                totalt = antallUtvidet,
                papirsøknader = antallUtvidet - antallUtvidetDigitale,
                digitaleSøknader = antallUtvidetDigitale,
                digitaliseringsgrad = antallUtvidetDigitale / antallUtvidet.toFloat(),
            ),
        )
    }
}

class SøknadsstatistikkForPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
    val ordinærBarnetrygd: AntallSøknader,
    val utvidetBarnetrygd: AntallSøknader,
)

class AntallSøknader(
    val totalt: Int,
    val papirsøknader: Int,
    val digitaleSøknader: Int,
    val digitaliseringsgrad: Float,
)
