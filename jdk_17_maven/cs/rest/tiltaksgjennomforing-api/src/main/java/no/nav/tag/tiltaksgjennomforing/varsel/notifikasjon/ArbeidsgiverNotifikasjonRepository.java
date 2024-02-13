package no.nav.tag.tiltaksgjennomforing.varsel.notifikasjon;

import io.micrometer.core.annotation.Timed;
import no.nav.tag.tiltaksgjennomforing.avtale.HendelseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ArbeidsgiverNotifikasjonRepository extends JpaRepository<ArbeidsgiverNotifikasjon, UUID> {

    @Timed(percentiles = {0.5d, 0.75d, 0.9d, 0.99d, 0.999d})
    List<ArbeidsgiverNotifikasjon> findArbeidsgiverNotifikasjonByAvtaleIdAndVarselSendtVellykketAndNotifikasjonAktiv(
            UUID avtaleId,
            boolean varselSendtVellykket,
            boolean notifikasjonAktiv);

    @Timed(percentiles = {0.5d, 0.75d, 0.9d, 0.99d, 0.999d})
    List<ArbeidsgiverNotifikasjon> findAllByAvtaleId(UUID avtaleId);

    @Timed(percentiles = {0.5d, 0.75d, 0.9d, 0.99d, 0.999d})
    @Query("FROM ArbeidsgiverNotifikasjon " +
            "where avtaleId in (?1) and (statusResponse = 'NyBeskjedVellykket' or statusResponse = 'NyOppgaveVellykket')")
    List<ArbeidsgiverNotifikasjon> findAllByAvtaleIdForDeleting(UUID avtaleId);

    @Timed(percentiles = {0.5d, 0.75d, 0.9d, 0.99d, 0.999d})
    List<ArbeidsgiverNotifikasjon> findArbeidsgiverNotifikasjonByAvtaleIdAndHendelseTypeAndStatusResponse(
            UUID avtaleId,
            HendelseType hendelsetype,
            String statusResponse);

    @Timed(percentiles = {0.5d, 0.75d, 0.9d, 0.99d, 0.999d})
    ArbeidsgiverNotifikasjon findArbeidsgiverNotifikasjonsByAvtaleIdAndNotifikasjonReferanseIdAndOperasjonType(
            UUID avtaleId, String notifikasjonReferanseId, NotifikasjonOperasjonType operasjonType);

}
