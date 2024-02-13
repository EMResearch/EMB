package uk.gov.pay.api.app;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.dropwizard.Application;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.CachingAuthenticator;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.exporter.MetricsServlet;
import org.glassfish.jersey.CommonProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.agreement.resource.AgreementsApiResource;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.app.config.PublicApiModule;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.auth.AccountAuthenticator;
import uk.gov.pay.api.exception.mapper.AgreementValidationExceptionMapper;
import uk.gov.pay.api.exception.mapper.AuthorisationRequestExceptionMapper;
import uk.gov.pay.api.exception.mapper.BadRefundsRequestExceptionMapper;
import uk.gov.pay.api.exception.mapper.BadRequestExceptionMapper;
import uk.gov.pay.api.exception.mapper.CancelAgreementExceptionMapper;
import uk.gov.pay.api.exception.mapper.CancelChargeExceptionMapper;
import uk.gov.pay.api.exception.mapper.CaptureChargeExceptionMapper;
import uk.gov.pay.api.exception.mapper.CreateAgreementExceptionMapper;
import uk.gov.pay.api.exception.mapper.CreateChargeExceptionMapper;
import uk.gov.pay.api.exception.mapper.CreateRefundExceptionMapper;
import uk.gov.pay.api.exception.mapper.DisputeValidationExceptionMapper;
import uk.gov.pay.api.exception.mapper.GetAgreementExceptionMapper;
import uk.gov.pay.api.exception.mapper.GetChargeExceptionMapper;
import uk.gov.pay.api.exception.mapper.GetEventsExceptionMapper;
import uk.gov.pay.api.exception.mapper.GetRefundExceptionMapper;
import uk.gov.pay.api.exception.mapper.GetRefundsExceptionMapper;
import uk.gov.pay.api.exception.mapper.InternalServerExceptionMapper;
import uk.gov.pay.api.exception.mapper.JsonProcessingExceptionMapper;
import uk.gov.pay.api.exception.mapper.PaymentValidationExceptionMapper;
import uk.gov.pay.api.exception.mapper.RefundsValidationExceptionMapper;
import uk.gov.pay.api.exception.mapper.SearchAgreementsExceptionMapper;
import uk.gov.pay.api.exception.mapper.SearchChargesExceptionMapper;
import uk.gov.pay.api.exception.mapper.SearchDisputesExceptionMapper;
import uk.gov.pay.api.exception.mapper.SearchRefundsExceptionMapper;
import uk.gov.pay.api.exception.mapper.ViolationExceptionMapper;
import uk.gov.pay.api.filter.AuthorizationValidationFilter;
import uk.gov.pay.api.filter.ClearMdcValuesFilter;
import uk.gov.pay.api.filter.LoggingMDCRequestFilter;
import uk.gov.pay.api.filter.RateLimiterFilter;
import uk.gov.pay.api.healthcheck.Ping;
import uk.gov.pay.api.ledger.resource.TransactionsResource;
import uk.gov.pay.api.managed.RedisClientManager;
import uk.gov.pay.api.resources.AuthorisationResource;
import uk.gov.pay.api.resources.HealthCheckResource;
import uk.gov.pay.api.resources.PaymentRefundsResource;
import uk.gov.pay.api.resources.PaymentsResource;
import uk.gov.pay.api.resources.RequestDeniedResource;
import uk.gov.pay.api.resources.SearchDisputesResource;
import uk.gov.pay.api.resources.SearchRefundsResource;
import uk.gov.pay.api.resources.SecuritytxtResource;
import uk.gov.pay.api.resources.telephone.TelephonePaymentNotificationResource;
import uk.gov.pay.api.validation.InjectingValidationFeature;
import uk.gov.service.payments.logging.GovUkPayDropwizardRequestJsonLogLayoutFactory;
import uk.gov.service.payments.logging.LoggingFilter;
import uk.gov.service.payments.logging.LogstashConsoleAppenderFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.FilterRegistration;

import static java.util.EnumSet.of;
import static javax.servlet.DispatcherType.REQUEST;

public class PublicApi extends Application<PublicApiConfig> {

    private static final Logger logger = LoggerFactory.getLogger(PublicApi.class);
    
    private static final String SERVICE_METRICS_NODE = "publicapi";

    @Override
    public void initialize(Bootstrap<PublicApiConfig> bootstrap) {
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(
                        bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        );
        bootstrap.getObjectMapper().getSubtypeResolver().registerSubtypes(LogstashConsoleAppenderFactory.class);
        bootstrap.getObjectMapper().getSubtypeResolver().registerSubtypes(GovUkPayDropwizardRequestJsonLogLayoutFactory.class);
    }

    @Override
    public void run(PublicApiConfig configuration, Environment environment) {
        initialiseSSLSocketFactory();

        final Injector injector = Guice.createInjector(new PublicApiModule(configuration, environment));

        environment.healthChecks().register("ping", new Ping());

        environment.jersey().register(injector.getInstance(HealthCheckResource.class));
        environment.jersey().register(injector.getInstance(PaymentsResource.class));
        environment.jersey().register(injector.getInstance(AgreementsApiResource.class));
        environment.jersey().register(injector.getInstance(PaymentRefundsResource.class));
        environment.jersey().register(injector.getInstance(RequestDeniedResource.class));
        environment.jersey().register(injector.getInstance(SearchRefundsResource.class));
        environment.jersey().register(injector.getInstance(TransactionsResource.class));
        environment.jersey().register(injector.getInstance(TelephonePaymentNotificationResource.class));
        environment.jersey().register(new InjectingValidationFeature(injector));
        environment.jersey().register(injector.getInstance(SecuritytxtResource.class));
        environment.jersey().register(injector.getInstance(AuthorisationResource.class));
        environment.jersey().register(injector.getInstance(SearchDisputesResource.class));

        environment.jersey().register(injector.getInstance(RateLimiterFilter.class));
        environment.jersey().register(injector.getInstance(LoggingMDCRequestFilter.class));

        environment.servlets().addFilter("ClearMdcValuesFilter", injector.getInstance(ClearMdcValuesFilter.class))
                .addMappingForUrlPatterns(of(REQUEST), true, "/v1/*");

        environment.servlets().addFilter("LoggingFilter", injector.getInstance(LoggingFilter.class))
                .addMappingForUrlPatterns(of(REQUEST), true, "/v1/*");

        FilterRegistration.Dynamic authorizationValidationFilter = environment.servlets().addFilter("AuthorizationValidationFilter", injector.getInstance(AuthorizationValidationFilter.class));
        authorizationValidationFilter.setInitParameter("excludedUrls", "/v1/auth");
        authorizationValidationFilter.addMappingForUrlPatterns(of(REQUEST), true, "/v1/*");

        /*
           Turn off 'FilteringJacksonJaxbJsonProvider' which overrides dropwizard JacksonMessageBodyProvider.
           Fails on Integration tests if not disabled. 
               - https://github.com/dropwizard/dropwizard/issues/1341
        */
        environment.jersey().property(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE, Boolean.TRUE);

        CachingAuthenticator<String, Account> cachingAuthenticator = new CachingAuthenticator<>(
                environment.metrics(),
                injector.getInstance(AccountAuthenticator.class),
                configuration.getAuthenticationCachePolicy());

        environment.jersey().register(new AuthDynamicFeature(
                new OAuthCredentialAuthFilter.Builder<Account>()
                        .setAuthenticator(cachingAuthenticator)
                        .setPrefix("Bearer")
                        .buildAuthFilter()));
        environment.jersey().register(new AuthValueFactoryProvider.Binder<>(Account.class));

        attachExceptionMappersTo(environment.jersey());

        CollectorRegistry collectorRegistry = CollectorRegistry.defaultRegistry;
        collectorRegistry.register(new DropwizardExports(environment.metrics()));
        environment.admin().addServlet("prometheusMetrics", new MetricsServlet(collectorRegistry)).addMapping("/metrics");

        environment.lifecycle().manage(injector.getInstance(RedisClientManager.class));
    }

    /**
     * Adding a call to initialise SSL socket factory at startup until we find a resolution for the following jersey client bug (JERSEY-3124).
     *
     * @see <a href="https://jersey.github.io/release-notes/2.24.html">https://jersey.github.io/release-notes/2.24.html</a>
     */
    private void initialiseSSLSocketFactory() {
        HttpsURLConnection.getDefaultSSLSocketFactory();
    }

    private void attachExceptionMappersTo(JerseyEnvironment jersey) {
        jersey.register(ViolationExceptionMapper.class);
        jersey.register(CreateChargeExceptionMapper.class);
        jersey.register(GetChargeExceptionMapper.class);
        jersey.register(GetEventsExceptionMapper.class);
        jersey.register(SearchChargesExceptionMapper.class);
        jersey.register(SearchRefundsExceptionMapper.class);
        jersey.register(CancelChargeExceptionMapper.class);
        jersey.register(PaymentValidationExceptionMapper.class);
        jersey.register(CreateAgreementExceptionMapper.class);
        jersey.register(SearchAgreementsExceptionMapper.class);
        jersey.register(GetAgreementExceptionMapper.class);
        jersey.register(CancelAgreementExceptionMapper.class);
        jersey.register(AgreementValidationExceptionMapper.class);
        jersey.register(RefundsValidationExceptionMapper.class);
        jersey.register(BadRefundsRequestExceptionMapper.class);
        jersey.register(BadRequestExceptionMapper.class);
        jersey.register(CreateRefundExceptionMapper.class);
        jersey.register(GetRefundExceptionMapper.class);
        jersey.register(GetRefundsExceptionMapper.class);
        jersey.register(CaptureChargeExceptionMapper.class);
        jersey.register(JsonProcessingExceptionMapper.class);
        jersey.register(AuthorisationRequestExceptionMapper.class);
        jersey.register(InternalServerExceptionMapper.class);
        jersey.register(DisputeValidationExceptionMapper.class);
        jersey.register(SearchDisputesExceptionMapper.class);
    }

    public static void main(String[] args) throws Exception {
        new PublicApi().run(args);
    }
}
