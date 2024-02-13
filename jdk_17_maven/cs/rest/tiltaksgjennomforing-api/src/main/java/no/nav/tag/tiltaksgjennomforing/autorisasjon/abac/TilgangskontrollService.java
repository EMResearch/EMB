package no.nav.tag.tiltaksgjennomforing.autorisasjon.abac;

import java.util.Map;
import java.util.Set;

import no.nav.tag.tiltaksgjennomforing.avtale.Fnr;
import no.nav.tag.tiltaksgjennomforing.avtale.InternBruker;

public interface TilgangskontrollService {
  boolean harSkrivetilgangTilKandidat(InternBruker internBruker, Fnr fnr);

  Map<Fnr, Boolean> skriveTilganger(InternBruker internBruker, Set<Fnr> fnr);
}
