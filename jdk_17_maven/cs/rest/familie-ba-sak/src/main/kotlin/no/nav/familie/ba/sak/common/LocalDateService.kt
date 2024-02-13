package no.nav.familie.ba.sak.common

import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class LocalDateService {
    fun now(): LocalDate = LocalDate.now()
}
