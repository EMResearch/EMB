package no.nav.tag.tiltaksgjennomforing.avtale;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FilterSokRepository extends JpaRepository<FilterSok, String> {

    Optional<FilterSok> findFilterSokBySokId(String sokId);
}
