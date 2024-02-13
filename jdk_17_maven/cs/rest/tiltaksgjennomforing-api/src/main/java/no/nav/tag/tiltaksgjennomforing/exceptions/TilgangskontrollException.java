package no.nav.tag.tiltaksgjennomforing.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
@Deprecated(since = "Bør ikke bruke denne exception lenger fordi det er vanskelig å se i browser hvilken exception som kastes. Det er kun feilkode 403 som vises, og message vises ikke noe sted. Bruk i stedet FeilkodeException.")
public class TilgangskontrollException extends RuntimeException {

    public TilgangskontrollException(String message) {
        super(message);
    }

    public TilgangskontrollException(String message, Throwable cause) {
        super(message, cause);
    }
}
