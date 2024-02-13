package no.nav.tag.tiltaksgjennomforing.datavarehus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;


public interface DvhMeldingEntitetRepository extends JpaRepository<DvhMeldingEntitet, UUID> {
    @Query(nativeQuery = true, value = "select * from dvh_melding where (avtale_id, tidspunkt) in (select avtale_id, max(tidspunkt) from dvh_melding group by avtale_id) and tiltak_status in ('KLAR_FOR_OPPSTART', 'GJENNOMFØRES');")
    List<DvhMeldingEntitet> findNyesteDvhMeldingForAvtaleSomKanEndreStatus();

    boolean existsByAvtaleId(UUID avtaleId);
}
