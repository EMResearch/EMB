package no.nav.familie.ba.sak.config

import jakarta.persistence.EntityManager
import jakarta.persistence.Table
import jakarta.persistence.metamodel.Metamodel
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.data.relational.core.mapping.RelationalMappingContext
import org.springframework.data.relational.core.sql.IdentifierProcessing
import org.springframework.stereotype.Service
import kotlin.reflect.full.findAnnotation
import org.springframework.data.relational.core.mapping.Table as JdbcTable

/**
 * Test utility service that allows to truncate all tables in the test database.
 * Inspired by: http://www.greggbolinger.com/truncate-all-tables-in-spring-boot-jpa-app/
 * @author Sebastien Dubois
 */
@Service
@Profile("dev", "postgres")
class DatabaseCleanupService(
    private val entityManager: EntityManager,
    private val environment: Environment,
    private val relationalMappingContext: RelationalMappingContext,
) {

    private val logger = LoggerFactory.getLogger(DatabaseCleanupService::class.java)

    private var tableNames: List<String>? = null
        /**
         * Uses the JPA metamodel to find all managed types then try to get the [Table] annotation's from each (if present) to discover the table name.
         * If the [Table] annotation is not defined then we skip that entity (oops :p)
         * JDBC tables must be found out in another way
         */
        get() {
            if (field == null) {
                val metaModel: Metamodel = entityManager.metamodel
                field = metaModel.managedTypes
                    .filter {
                        it.javaType.kotlin.findAnnotation<Table>() != null || it.javaType.kotlin.findAnnotation<JdbcTable>() != null
                    }
                    .map {
                        val tableAnnotation: Table? = it.javaType.kotlin.findAnnotation()
                        val jdbcTableAnnotation: JdbcTable? = it.javaType.kotlin.findAnnotation()
                        tableAnnotation?.name ?: jdbcTableAnnotation?.value
                            ?: throw IllegalStateException("should never get here")
                    } + getJdbcTableNames()
            }
            return field
        }

    private fun getJdbcTableNames(): List<String> {
        return relationalMappingContext.persistentEntities.map { entity ->
            entity.tableName.toSql(IdentifierProcessing.NONE)
        }
    }

    /**
     * Utility method that truncates all identified tables
     */
    @Transactional
    fun truncate() {
        logger.info("Truncating tables: $tableNames")
        entityManager.flush()
        if (environment.activeProfiles.contains("postgres")) {
            tableNames?.forEach { tableName ->
                entityManager.createNativeQuery("TRUNCATE TABLE $tableName CASCADE").executeUpdate()
            }
        } else {
            entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TO FALSE").executeUpdate()
            tableNames?.forEach { tableName ->
                entityManager.createNativeQuery("TRUNCATE TABLE $tableName").executeUpdate()
            }
            entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TO TRUE").executeUpdate()
        }
    }
}
