package no.nav.familie.tilbake.common.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.NoRepositoryBean

/**
 * På grunn av att vi setter id's på våre entitetet så prøver spring å oppdatere våre entiteter i stedet for å ta insert
 */
@NoRepositoryBean
interface RepositoryInterface<T : Any, ID> : CrudRepository<T, ID> {

    @Deprecated("Støttes ikke, bruk insert/update")
    override fun <S : T> save(entity: S): S {
        error("Not implemented - Use InsertUpdateRepository - insert/update")
    }

    @Deprecated("Støttes ikke, bruk insertAll/updateAll")
    override fun <S : T> saveAll(entities: Iterable<S>): Iterable<S> {
        error("Not implemented - Use InsertUpdateRepository - insertAll/updateAll")
    }
}
