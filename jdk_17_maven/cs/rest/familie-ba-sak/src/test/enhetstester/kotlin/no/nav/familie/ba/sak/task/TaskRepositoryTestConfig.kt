package no.nav.familie.ba.sak.task

import io.mockk.clearMocks
import io.mockk.mockk
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@TestConfiguration
class TaskRepositoryTestConfig {

    @Bean
    @Profile("mock-task-repository")
    fun mockTaskRepository(): TaskRepositoryWrapper {
        return clearMockTaskRepository(mockk(relaxed = true))
    }

    @Bean
    @Profile("mock-task-service")
    @Primary
    fun mockTaskService(): OpprettTaskService {
        return clearMockTaskService(mockk(relaxed = true))
    }

    companion object {
        fun clearMockTaskRepository(mockTaskRepository: TaskRepositoryWrapper): TaskRepositoryWrapper {
            clearMocks(mockTaskRepository)

            return mockTaskRepository
        }

        fun clearMockTaskService(mockTaskService: OpprettTaskService): OpprettTaskService {
            clearMocks(mockTaskService)

            return mockTaskService
        }
    }
}
