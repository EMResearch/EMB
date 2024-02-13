package no.nav.tag.tiltaksgjennomforing.avtale;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;


public interface AvtaleForkortetRepository extends JpaRepository<AvtaleForkortetEntitet, UUID> {

}
