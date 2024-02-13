package no.nav.tag.tiltaksgjennomforing.datadeling;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface AvtaleMeldingEntitetRepository extends JpaRepository<AvtaleMeldingEntitet, UUID> {

    List<AvtaleMeldingEntitet> findAllByAvtaleId(UUID avtaleId);
    @Query(nativeQuery = true, value =
            "select * from avtale_melding where (avtale_id, tidspunkt) in (select avtale_id, max(tidspunkt) from avtale_melding group by avtale_id) and avtale_status in ('KLAR_FOR_OPPSTART', 'GJENNOMFØRES');")
    List<AvtaleMeldingEntitet> findNyesteAvtaleHendelseMeldingForAvtaleSomKanEndreStatus();

}
