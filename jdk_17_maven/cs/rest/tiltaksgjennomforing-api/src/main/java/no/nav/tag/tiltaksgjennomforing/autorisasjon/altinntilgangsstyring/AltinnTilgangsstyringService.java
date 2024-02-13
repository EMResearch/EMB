package no.nav.tag.tiltaksgjennomforing.autorisasjon.altinntilgangsstyring;

import lombok.extern.slf4j.Slf4j;
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.AltinnConfig;
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.AltinnrettigheterProxyKlient;
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.AltinnrettigheterProxyKlientConfig;
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.ProxyConfig;
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.error.exceptions.AltinnrettigheterProxyKlientFallbackException;
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.model.*;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.TokenUtils;
import no.nav.tag.tiltaksgjennomforing.avtale.BedriftNr;
import no.nav.tag.tiltaksgjennomforing.avtale.Fnr;
import no.nav.tag.tiltaksgjennomforing.avtale.Tiltakstype;
import no.nav.tag.tiltaksgjennomforing.exceptions.AltinnFeilException;
import no.nav.tag.tiltaksgjennomforing.exceptions.TiltaksgjennomforingException;
import no.nav.tag.tiltaksgjennomforing.utils.MultiValueMap;
import no.nav.tag.tiltaksgjennomforing.utils.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class AltinnTilgangsstyringService {
    private final AltinnTilgangsstyringProperties altinnTilgangsstyringProperties;
    private final AltinnrettigheterProxyKlient klient;

    public AltinnTilgangsstyringService(
            AltinnTilgangsstyringProperties altinnTilgangsstyringProperties,
            TokenUtils tokenUtils,
            @Value("${spring.application.name}") String applicationName) {

        if (Utils.erNoenTomme(altinnTilgangsstyringProperties.getArbtreningServiceCode(),
                altinnTilgangsstyringProperties.getArbtreningServiceEdition(),
                altinnTilgangsstyringProperties.getLtsMidlertidigServiceCode(),
                altinnTilgangsstyringProperties.getLtsMidlertidigServiceEdition(),
                altinnTilgangsstyringProperties.getLtsVarigServiceCode(),
                altinnTilgangsstyringProperties.getLtsVarigServiceEdition(),
                altinnTilgangsstyringProperties.getSommerjobbServiceCode(),
                altinnTilgangsstyringProperties.getSommerjobbServiceEdition())) {
            throw new TiltaksgjennomforingException("Altinn konfigurasjon ikke komplett");
        }
        this.altinnTilgangsstyringProperties = altinnTilgangsstyringProperties;

        String altinnProxyUrl = altinnTilgangsstyringProperties.getProxyUri().toString();
        String altinnProxyFallbackUrl = altinnTilgangsstyringProperties.getUri().toString();

        AltinnrettigheterProxyKlientConfig proxyKlientConfig = new AltinnrettigheterProxyKlientConfig(
                new ProxyConfig(applicationName, altinnProxyUrl),
                new AltinnConfig(
                        altinnProxyFallbackUrl,
                        altinnTilgangsstyringProperties.getAltinnApiKey(),
                        altinnTilgangsstyringProperties.getApiGwApiKey()
                )
        );
        this.klient = new AltinnrettigheterProxyKlient(proxyKlientConfig);

    }

    public Map<BedriftNr, Collection<Tiltakstype>> hentTilganger(Fnr fnr, HentArbeidsgiverToken hentArbeidsgiverToken) {
        MultiValueMap<BedriftNr, Tiltakstype> tilganger = MultiValueMap.empty();
        String arbeidsgiverToken = hentArbeidsgiverToken.hentArbeidsgiverToken();

        AltinnReportee[] arbeidstreningOrger = kallAltinn(altinnTilgangsstyringProperties.getArbtreningServiceCode(), altinnTilgangsstyringProperties.getArbtreningServiceEdition(), fnr, arbeidsgiverToken);
        leggTil(tilganger, arbeidstreningOrger, Tiltakstype.ARBEIDSTRENING);

        AltinnReportee[] varigLtsOrger = kallAltinn(altinnTilgangsstyringProperties.getLtsVarigServiceCode(), altinnTilgangsstyringProperties.getLtsVarigServiceEdition(), fnr, arbeidsgiverToken);
        leggTil(tilganger, varigLtsOrger, Tiltakstype.VARIG_LONNSTILSKUDD);

        AltinnReportee[] midlLtsOrger = kallAltinn(altinnTilgangsstyringProperties.getLtsMidlertidigServiceCode(), altinnTilgangsstyringProperties.getLtsMidlertidigServiceEdition(), fnr, arbeidsgiverToken);
        leggTil(tilganger, midlLtsOrger, Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD);

        AltinnReportee[] sommerjobbOrger = kallAltinn(altinnTilgangsstyringProperties.getSommerjobbServiceCode(), altinnTilgangsstyringProperties.getSommerjobbServiceEdition(), fnr, arbeidsgiverToken);
        leggTil(tilganger, sommerjobbOrger, Tiltakstype.SOMMERJOBB);

        AltinnReportee[] mentorOrger = kallAltinn(altinnTilgangsstyringProperties.getMentorServiceCode(), altinnTilgangsstyringProperties.getMentorServiceEdition(), fnr, arbeidsgiverToken);
        leggTil(tilganger, mentorOrger, Tiltakstype.MENTOR);

        AltinnReportee[] inkluderingstilskuddOrger = kallAltinn(altinnTilgangsstyringProperties.getInkluderingstilskuddServiceCode(), altinnTilgangsstyringProperties.getInkluderingstilskuddServiceEdition(), fnr,
                arbeidsgiverToken);
        leggTil(tilganger, inkluderingstilskuddOrger, Tiltakstype.INKLUDERINGSTILSKUDD);

        return tilganger.toMap();
    }

    private void leggTil(MultiValueMap<BedriftNr, Tiltakstype> tilganger, AltinnReportee[] arbeidstreningOrger, Tiltakstype tiltakstype) {
        for (AltinnReportee altinnReportee : arbeidstreningOrger) {
            if (!altinnReportee.getType().equals("Enterprise")) {
                tilganger.put(new BedriftNr(altinnReportee.getOrganizationNumber()), tiltakstype);
            }
        }
    }

    public Set<AltinnReportee> hentAltinnOrganisasjoner(Fnr fnr, HentArbeidsgiverToken hentArbeidsgiverToken) {
        return new HashSet<>(List.of(kallAltinn(null, null, fnr, hentArbeidsgiverToken.hentArbeidsgiverToken())));
    }

    private AltinnReportee[] kallAltinn(Integer serviceCode, Integer serviceEdition, Fnr fnr, String arbeidsgiverToken) {
        try {
            List<AltinnReportee> reportees;
            if (serviceCode != null && serviceEdition != null) {
                reportees = klient.hentOrganisasjoner(
                        new SelvbetjeningToken(arbeidsgiverToken),
                        new Subject(fnr.asString()), new ServiceCode(serviceCode.toString()), new ServiceEdition(serviceEdition.toString()),
                        true
                );
            } else {
                reportees = klient.hentOrganisasjoner(new SelvbetjeningToken(arbeidsgiverToken), new Subject(fnr.asString()), true);
            }
            return reportees.toArray(new AltinnReportee[0]);

        } catch (AltinnrettigheterProxyKlientFallbackException exception) {
            log.warn("Feil ved kall mot Altinn.", exception);
            throw new AltinnFeilException();
        }
    }
}
