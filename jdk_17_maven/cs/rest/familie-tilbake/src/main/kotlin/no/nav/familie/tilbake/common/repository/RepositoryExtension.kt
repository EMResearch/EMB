package no.nav.familie.tilbake.common.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.findByIdOrNull

inline fun <reified T, ID> CrudRepository<T, ID>.findByIdOrThrow(id: ID): T {
    return findByIdOrNull(id) ?: throw IllegalStateException("Finner ikke ${T::class.simpleName} med id=$id")
}
