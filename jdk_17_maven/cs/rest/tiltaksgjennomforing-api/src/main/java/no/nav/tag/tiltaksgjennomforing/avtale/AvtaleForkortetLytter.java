package no.nav.tag.tiltaksgjennomforing.avtale;

import lombok.RequiredArgsConstructor;
import no.nav.tag.tiltaksgjennomforing.avtale.events.AvtaleForkortet;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AvtaleForkortetLytter {
    private final AvtaleForkortetRepository avtaleForkortetRepository;

    @EventListener
    public void avtaleForkortet(AvtaleForkortet event) {
        avtaleForkortetRepository.save(new AvtaleForkortetEntitet(event.getAvtale(), event.getAvtaleInnhold(), event.getUtførtAv(), event.getNySluttDato(), event.getGrunn(), event.getAnnetGrunn()));
    }
}
