package no.nav.tag.tiltaksgjennomforing.varsel;

import io.micrometer.core.annotation.Timed;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtalerolle;
import no.nav.tag.tiltaksgjennomforing.avtale.Identifikator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface  VarselRepository extends JpaRepository<Varsel, UUID> {
    @Timed(percentiles = { 0.5d, 0.75d, 0.9d, 0.99d, 0.999d })
    @Override
    List<Varsel> findAll();

    @Timed(percentiles = { 0.5d, 0.75d, 0.9d, 0.99d, 0.999d })
    Varsel findByIdAndIdentifikatorIn(UUID varselId, Collection<Identifikator> identifikator);

    @Timed(percentiles = { 0.5d, 0.75d, 0.9d, 0.99d, 0.999d })
    List<Varsel> findAllByAvtaleIdAndIdentifikator(UUID avtaleId, Identifikator identifikator);

    @Timed(percentiles = { 0.5d, 0.75d, 0.9d, 0.99d, 0.999d })
    List<Varsel> findAllByAvtaleIdAndMottaker(UUID avtaleId, Avtalerolle mottaker);

    @Timed(percentiles = { 0.5d, 0.75d, 0.9d, 0.99d, 0.999d })
    List<Varsel> findAllByLestIsFalseAndBjelleIsTrueAndIdentifikatorIn(Collection<String> identifikator);

    @Timed(percentiles = { 0.5d, 0.75d, 0.9d, 0.99d, 0.999d })
    List<Varsel> findAllByLestIsFalseAndBjelleIsTrueAndAvtaleIdAndIdentifikatorIn(UUID avtaleId, Collection<Identifikator> identifikator);

    default List<Varsel> saveAll(Varsel... varsler) {
        return saveAll(List.of(varsler));
    }
}
