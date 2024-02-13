package no.nav.familie.tilbake.config

import com.ibm.mq.constants.CMQC
import com.ibm.mq.jakarta.jms.MQQueueConnectionFactory
import com.ibm.msg.client.jakarta.jms.JmsConstants
import com.ibm.msg.client.jakarta.wmq.common.CommonConstants
import jakarta.jms.ConnectionFactory
import jakarta.jms.JMSException
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.jms.config.DefaultJmsListenerContainerFactory
import org.springframework.jms.config.JmsListenerContainerFactory
import org.springframework.jms.connection.JmsTransactionManager
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter

private const val UTF_8_WITH_PUA = 1208

@Configuration
@Profile("!integrasjonstest")
class OppdragMQConfig(
    @Value("\${oppdrag.mq.hostname}") val hostname: String,
    @Value("\${oppdrag.mq.queuemanager}") val queuemanager: String,
    @Value("\${oppdrag.mq.channel}") val channel: String,
    @Value("\${oppdrag.mq.port}") val port: Int,
    @Value("\${CREDENTIAL_USERNAME}") val user: String,
    @Value("\${CREDENTIAL_PASSWORD}") val password: String,
    val environment: Environment,
) {

    private val logger = LoggerFactory.getLogger(OppdragMQConfig::class.java)

    @Bean
    @Throws(JMSException::class)
    fun mqQueueConnectionFactory(): JmsPoolConnectionFactory {
        val targetFactory = MQQueueConnectionFactory()
        targetFactory.hostName = hostname
        targetFactory.queueManager = queuemanager
        targetFactory.channel = channel
        targetFactory.port = port
        targetFactory.transportType = CommonConstants.WMQ_CM_CLIENT
        targetFactory.ccsid = UTF_8_WITH_PUA
        targetFactory.setIntProperty(JmsConstants.JMS_IBM_ENCODING, CMQC.MQENC_NATIVE)
        targetFactory.setBooleanProperty(JmsConstants.USER_AUTHENTICATION_MQCSP, true)
        targetFactory.setIntProperty(JmsConstants.JMS_IBM_CHARACTER_SET, UTF_8_WITH_PUA)

        val cf = UserCredentialsConnectionFactoryAdapter()
        cf.setUsername(user)
        cf.setPassword(password)
        cf.setTargetConnectionFactory(targetFactory)

        val pooledFactory = JmsPoolConnectionFactory()
        pooledFactory.connectionFactory = cf
        pooledFactory.maxConnections = 10
        pooledFactory.maxSessionsPerConnection = 10

        logger.info("MQ bruker $user")

        return pooledFactory
    }

    @Bean
    fun jmsListenerContainerFactory(
        @Qualifier("mqQueueConnectionFactory") connectionFactory: ConnectionFactory,
        configurer: DefaultJmsListenerContainerFactoryConfigurer,
    ): JmsListenerContainerFactory<*> {
        val factory = DefaultJmsListenerContainerFactory()
        configurer.configure(factory, connectionFactory)

        val transactionManager = JmsTransactionManager()
        transactionManager.connectionFactory = connectionFactory
        factory.setTransactionManager(transactionManager)
        factory.setSessionTransacted(true)
        if (environment.activeProfiles.any { it.contains("local") }) {
            factory.setAutoStartup(false)
        }
        return factory
    }
}
