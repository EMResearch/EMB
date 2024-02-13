package no.nav.familie.ba.sak.sikkerhet

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import no.nav.familie.ba.sak.config.AuditLoggerEvent
import no.nav.familie.ba.sak.util.BrukerContextUtil
import no.nav.familie.log.mdc.MDCConstants
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest

internal class AuditLoggerTest {

    private val auditLogger = AuditLogger("familie-ba-sak")
    private val navIdent = "Z1234567"
    private val method = "POST"
    private val requestUri = "/api/test/123"

    private lateinit var logger: Logger
    private lateinit var listAppender: ListAppender<ILoggingEvent>

    @BeforeEach
    internal fun setUp() {
        MDC.put(MDCConstants.MDC_CALL_ID, "00001111")
        val servletRequest = MockHttpServletRequest(method, requestUri)
        BrukerContextUtil.mockBrukerContext(preferredUsername = navIdent, servletRequest = servletRequest)
        logger = LoggerFactory.getLogger("auditLogger") as Logger
        listAppender = ListAppender<ILoggingEvent>()
        listAppender.start()
        logger.addAppender(listAppender)
    }

    @AfterEach
    internal fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
        MDC.remove(MDCConstants.MDC_CALL_ID)
    }

    @Test
    internal fun `logger melding uten custom strings`() {
        auditLogger.log(Sporingsdata(AuditLoggerEvent.ACCESS, "12345678901"))
        assertThat(listAppender.list).hasSize(1)
        assertThat(getMessage()).isEqualTo(expectedBaseLog)
    }

    @Test
    internal fun `logger melding med custom strings`() {
        auditLogger.log(
            Sporingsdata(
                event = AuditLoggerEvent.ACCESS,
                personIdent = "12345678901",
                custom1 = CustomKeyValue("k", "v"),
                custom2 = CustomKeyValue("k2", "v2"),
                custom3 = CustomKeyValue("k3", "v3"),
            ),
        )
        assertThat(listAppender.list).hasSize(1)
        assertThat(getMessage())
            .isEqualTo("${expectedBaseLog}cs3Label=k cs3=v cs5Label=k2 cs5=v2 cs6Label=k3 cs6=v3")
    }

    private fun getMessage() = listAppender.list[0].message.replace("""end=\d+""".toRegex(), "end=123")

    private val expectedBaseLog = "CEF:0|Familie|familie-ba-sak|1.0|audit:access|Saksbehandling|INFO|end=123 " +
        "suid=Z1234567 " +
        "duid=12345678901 " +
        "sproc=00001111 " +
        "requestMethod=POST " +
        "request=/api/test/123 "
}
