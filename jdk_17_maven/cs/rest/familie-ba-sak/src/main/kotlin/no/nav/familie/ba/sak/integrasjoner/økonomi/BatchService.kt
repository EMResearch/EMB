package no.nav.familie.ba.sak.integrasjoner.økonomi

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class BatchService(val batchRepository: BatchRepository) {

    @Transactional
    fun plukkLedigeBatchKjøringerFor(dato: LocalDate): Batch? {
        val batch = batchRepository.findByKjøredatoAndLedig(dato)
        if (batch != null) lagreNyStatus(batch, KjøreStatus.TATT)

        return batch
    }

    fun lagreNyStatus(batch: Batch, status: KjøreStatus) {
        batch.status = status
        batchRepository.saveAndFlush(batch)
    }

    fun lagreNyStatus(batchId: Long, status: KjøreStatus) {
        val batch = batchRepository.getReferenceById(batchId)
        batch.status = status
        batchRepository.saveAndFlush(batch)
    }
}
