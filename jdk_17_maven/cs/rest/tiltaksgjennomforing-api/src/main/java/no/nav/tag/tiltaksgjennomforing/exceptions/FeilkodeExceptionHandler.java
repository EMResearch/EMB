package no.nav.tag.tiltaksgjennomforing.exceptions;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@ControllerAdvice
public class FeilkodeExceptionHandler extends ResponseEntityExceptionHandler {
    private static final String FEILKODE = "feilkode";

    @ExceptionHandler({ FeilkodeException.class })
    public ResponseEntity<Object> feilkodeException(FeilkodeException e) {
        log.info("Feilkode inntruffet: {}", e.getFeilkode());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .header(FEILKODE, e.getFeilkode().name())
                .build();
    }
}
