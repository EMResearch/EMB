package no.nav.tag.tiltaksgjennomforing;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class ApiBeskriver {

    public static final String API_BESKRIVELSE_ATTRIBUTT = "API_BESKRIVELSE";

    @Around("@annotation(no.nav.tag.tiltaksgjennomforing.ApiBeskrivelse)")
    public Object validateAspect(ProceedingJoinPoint pjp) throws Throwable {
        var request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        var methodSig = (MethodSignature) pjp.getSignature();
        var annotasjon = methodSig.getMethod().getAnnotation(ApiBeskrivelse.class);
        request.setAttribute(API_BESKRIVELSE_ATTRIBUTT, annotasjon.value());
        return pjp.proceed();
    }
}