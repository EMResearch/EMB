package no.nav.tag.tiltaksgjennomforing.sporingslogg;

import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface SporingsloggRepository extends CrudRepository<Sporingslogg, UUID> {
}
