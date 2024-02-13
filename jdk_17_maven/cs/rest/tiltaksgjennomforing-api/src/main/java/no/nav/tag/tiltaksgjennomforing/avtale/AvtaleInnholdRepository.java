package no.nav.tag.tiltaksgjennomforing.avtale;

import io.micrometer.core.annotation.Timed;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AvtaleInnholdRepository extends JpaRepository<AvtaleInnhold, UUID> {

    @Timed(percentiles = { 0.5d, 0.75d, 0.9d, 0.99d, 0.999d })
    @Override
    List<AvtaleInnhold> findAllById(Iterable<UUID> ids);

    @Timed(percentiles = { 0.5d, 0.75d, 0.9d, 0.99d, 0.999d })
    @Query(value = "select ai from AvtaleInnhold ai where ai.journalpostId is null and ai.avtaleInngått is not null")
    List<AvtaleInnhold> finnAvtaleVersjonerTilJournalfoering();

    @Timed(percentiles = { 0.5d, 0.75d, 0.9d, 0.99d, 0.999d })
    List<AvtaleInnhold> findAllByAvtale(Avtale avtale);
}


