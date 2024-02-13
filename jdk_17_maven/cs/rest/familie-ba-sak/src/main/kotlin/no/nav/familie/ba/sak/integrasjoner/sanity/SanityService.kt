package no.nav.familie.ba.sak.integrasjoner.sanity

import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.brev.domene.ISanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class SanityService(
    private val sanityKlient: SanityKlient,
    private val featureToggleService: FeatureToggleService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Cacheable("sanityBegrunnelser", cacheManager = "shortCache")
    fun hentSanityBegrunnelser(): Map<Standardbegrunnelse, SanityBegrunnelse> {
        val enumPåApiNavn = Standardbegrunnelse.values().associateBy { it.sanityApiNavn }
        val sanityBegrunnelser = sanityKlient.hentBegrunnelser()
        logManglerSanityBegrunnelseForEnum(enumPåApiNavn, sanityBegrunnelser, "SanityBegrunnelse")
        return sanityBegrunnelser
            .mapNotNull {
                val begrunnelseEnum = enumPåApiNavn[it.apiNavn]
                if (begrunnelseEnum == null) {
                    logger.warn("Finner ikke Standardbegrunnelse for ${it.apiNavn}")
                    null
                } else {
                    begrunnelseEnum to it
                }
            }.toMap()
    }

    @Cacheable("sanityEØSBegrunnelser", cacheManager = "shortCache")
    fun hentSanityEØSBegrunnelser(): Map<EØSStandardbegrunnelse, SanityEØSBegrunnelse> {
        val eøsPraksisEndringFeatureToggleErSlåttPå =
            featureToggleService.isEnabled(FeatureToggleConfig.EØS_PRAKSISENDRING_SEPTEMBER2023)

        // TODO: Fjern filtrering av begrunnelser etter at EØS praksisendringen er live i prod
        val enumPåApiNavn = if (eøsPraksisEndringFeatureToggleErSlåttPå) {
            EØSStandardbegrunnelse.values().associateBy { it.sanityApiNavn }
        } else {
            EØSStandardbegrunnelse.values().subtract(EØSStandardbegrunnelse.eøsPraksisendringBegrunnelser())
                .associateBy { it.sanityApiNavn }
        }

        val sanityEØSBegrunnelser = sanityKlient.hentEØSBegrunnelser()
        logManglerSanityBegrunnelseForEnum(enumPåApiNavn, sanityEØSBegrunnelser, "SanityEØSBegrunnelse")
        return sanityEØSBegrunnelser
            .mapNotNull {
                val begrunnelseEnum = enumPåApiNavn[it.apiNavn]
                if (begrunnelseEnum == null) {
                    logger.warn("Finner ikke EØSStandardbegrunnelse for ${it.apiNavn}")
                    null
                } else {
                    begrunnelseEnum to it
                }
            }.toMap()
    }

    private fun logManglerSanityBegrunnelseForEnum(
        enumPåApiNavn: Map<String, IVedtakBegrunnelse>,
        sanityBegrunnelser: List<ISanityBegrunnelse>,
        navn: String,
    ) {
        val sanityBegrunnelseApiNavn = sanityBegrunnelser.map { it.apiNavn }.toSet()
        enumPåApiNavn.values.forEach {
            if (!sanityBegrunnelseApiNavn.contains(it.sanityApiNavn)) {
                logger.warn("Finner ikke $navn for enum=$it sanityApiNavn=${it.sanityApiNavn}")
            }
        }
    }
}
