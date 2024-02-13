package no.nav.tag.tiltaksgjennomforing.avtale;

import lombok.Data;
import lombok.experimental.Accessors;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

@Data
@Accessors(chain = true)
public class AvtalePredicate implements Predicate<Avtale> {

    private NavIdent veilederNavIdent;
    private BedriftNr bedriftNr;
    private Fnr deltakerFnr;
    private Tiltakstype tiltakstype;
    private Status status;
    private Boolean erUfordelt;
    private TilskuddPeriodeStatus tilskuddPeriodeStatus;
    private String navEnhet;
    private Integer avtaleNr;


    private static boolean erLiktHvisOppgitt(Object kriterie, Object avtaleVerdi) {
        return kriterie == null || kriterie.equals(avtaleVerdi);
    }

    @Override
    public boolean test(Avtale avtale) {
        return erLiktHvisOppgitt(veilederNavIdent, avtale.getVeilederNavIdent())
                && erLiktHvisOppgitt(bedriftNr, avtale.getBedriftNr())
                && erLiktHvisOppgitt(deltakerFnr, avtale.getDeltakerFnr())
                && erLiktHvisOppgitt(tiltakstype, avtale.getTiltakstype())
                && erLiktHvisOppgitt(status, avtale.statusSomEnum())
                && erLiktHvisOppgitt(tilskuddPeriodeStatus, avtale.getGjeldendeTilskuddsperiodestatus())
                && (erLiktHvisOppgitt(navEnhet, avtale.getEnhetGeografisk()) || erLiktHvisOppgitt(navEnhet, avtale.getEnhetOppfolging()))
                && erLiktHvisOppgitt(avtaleNr, avtale.getAvtaleNr());
    }

    public String generateHash() {
        List<String> liste = List.of(Objects.toString(veilederNavIdent, "")
                , Objects.toString(bedriftNr, "")
                , Objects.toString(deltakerFnr, "")
                , Objects.toString(tiltakstype, "")
                , Objects.toString(status, "")
                , Objects.toString(tilskuddPeriodeStatus, "")
                , Objects.toString(navEnhet, "")
                , Objects.toString(avtaleNr, ""));

        String predicateString = String.join(";", liste);

        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] hash = digest.digest(predicateString.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }


}
