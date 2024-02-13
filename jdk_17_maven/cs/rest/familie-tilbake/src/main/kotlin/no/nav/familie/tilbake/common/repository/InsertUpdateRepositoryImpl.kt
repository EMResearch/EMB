package no.nav.familie.tilbake.common.repository

import org.springframework.data.jdbc.core.JdbcAggregateOperations
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class InsertUpdateRepositoryImpl<T : Any>(val entityOperations: JdbcAggregateOperations) : InsertUpdateRepository<T> {

    @Transactional
    override fun insert(t: T): T {
        return entityOperations.insert(t)
    }

    @Transactional
    override fun insertAll(list: List<T>): List<T> {
        return list.map(this::insert)
    }

    @Transactional
    override fun update(t: T): T {
        return entityOperations.update(t)
    }

    @Transactional
    override fun updateAll(list: List<T>): List<T> {
        return list.map(this::update)
    }
}
