package no.nav.tag.tiltaksgjennomforing.autorisasjon.abac.adapter;

import static java.util.Optional.ofNullable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import no.nav.tag.tiltaksgjennomforing.Miljø;
import no.nav.tag.tiltaksgjennomforing.featuretoggles.enhet.AxsysService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

;

@Component
@RequiredArgsConstructor
@Profile(value = { Miljø.LOCAL, Miljø.DEV_FSS })
public class ClearCacheInterceptor implements HandlerInterceptor, WebMvcConfigurer {

    public static final String CLEAR_CACHE_HEADER = "x-clear-cache";
    private final AbacAdapter abacAdapter;
    private final AxsysService axsysService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (ofNullable(request.getHeader(CLEAR_CACHE_HEADER)).map(Boolean::valueOf).orElse(false)) {
            abacAdapter.cacheEvict();
            axsysService.cacheEvict();
        }
        return HandlerInterceptor.super.preHandle(request, response, handler);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this);
    }

}
