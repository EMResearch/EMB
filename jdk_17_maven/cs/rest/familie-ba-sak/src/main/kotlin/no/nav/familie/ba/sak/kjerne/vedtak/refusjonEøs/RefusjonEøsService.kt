package no.nav.familie.ba.sak.kjerne.vedtak.refusjonEøs

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.ekstern.restDomene.RestRefusjonEøs
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RefusjonEøsService(
    @Autowired
    private val refusjonEøsRepository: RefusjonEøsRepository,

    @Autowired
    private val loggService: LoggService,
) {

    private fun hentRefusjonEøs(id: Long): RefusjonEøs {
        return refusjonEøsRepository.finnRefusjonEøs(id)
            ?: throw Feil("Finner ikke refusjon eøs med id=$id")
    }

    @Transactional
    fun leggTilRefusjonEøsPeriode(refusjonEøs: RestRefusjonEøs, behandlingId: Long): Long {
        val lagretPeriode = refusjonEøsRepository.save(
            RefusjonEøs(
                behandlingId = behandlingId,
                fom = refusjonEøs.fom,
                tom = refusjonEøs.tom,
                refusjonsbeløp = refusjonEøs.refusjonsbeløp,
                land = refusjonEøs.land,
                refusjonAvklart = refusjonEøs.refusjonAvklart,
            ),
        )
        loggService.loggRefusjonEøsPeriodeLagtTil(refusjonEøs = lagretPeriode)
        return lagretPeriode.id
    }

    @Transactional
    fun fjernRefusjonEøsPeriode(id: Long, behandlingId: Long) {
        loggService.loggRefusjonEøsPeriodeFjernet(
            refusjonEøs = hentRefusjonEøs(id),
        )
        refusjonEøsRepository.deleteById(id)
    }

    fun hentRefusjonEøsPerioder(behandlingId: Long) =
        refusjonEøsRepository.finnRefusjonEøsForBehandling(behandlingId = behandlingId)
            .map { tilRest(it) }

    private fun tilRest(it: RefusjonEøs) =
        RestRefusjonEøs(
            id = it.id,
            fom = it.fom,
            tom = it.tom,
            refusjonsbeløp = it.refusjonsbeløp,
            land = it.land,
            refusjonAvklart = it.refusjonAvklart,
        )

    @Transactional
    fun oppdaterRefusjonEøsPeriode(restRefusjonEøs: RestRefusjonEøs, id: Long) {
        val refusjonEøs = hentRefusjonEøs(id)

        refusjonEøs.fom = restRefusjonEøs.fom
        refusjonEøs.tom = restRefusjonEøs.tom
        refusjonEøs.refusjonsbeløp = restRefusjonEøs.refusjonsbeløp
        refusjonEøs.land = restRefusjonEøs.land
        refusjonEøs.refusjonAvklart = restRefusjonEøs.refusjonAvklart
    }
}
