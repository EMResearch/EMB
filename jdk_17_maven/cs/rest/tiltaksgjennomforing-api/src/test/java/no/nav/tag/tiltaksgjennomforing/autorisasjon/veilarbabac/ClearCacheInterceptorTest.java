package no.nav.tag.tiltaksgjennomforing.autorisasjon.veilarbabac;

import no.nav.tag.tiltaksgjennomforing.autorisasjon.abac.adapter.AbacAdapter;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.abac.adapter.ClearCacheInterceptor;
import no.nav.tag.tiltaksgjennomforing.featuretoggles.enhet.AxsysService;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;

import static org.mockito.Mockito.*;

public class ClearCacheInterceptorTest {
    
    private HttpServletRequest request = mock(HttpServletRequest.class);
    private AbacAdapter abacAdapter = mock(AbacAdapter.class);
    private AxsysService axsysService = mock(AxsysService.class);
    private ClearCacheInterceptor clearCacheInterceptor = new ClearCacheInterceptor(abacAdapter, axsysService);

    @Test
    public void skal_evicte_cache_hvis_header_er_true() throws Exception {
        when(request.getHeader(ClearCacheInterceptor.CLEAR_CACHE_HEADER)).thenReturn("true");
        clearCacheInterceptor.preHandle(request, null, null);
        verify(abacAdapter).cacheEvict();
        verify(axsysService).cacheEvict();
    }
    
    @Test
    public void skal_ikke_evicte_cache_hvis_header_er_false() throws Exception {
        when(request.getHeader(ClearCacheInterceptor.CLEAR_CACHE_HEADER)).thenReturn("false");
        clearCacheInterceptor.preHandle(request, null, null);
        verifyNoMoreInteractions(abacAdapter, axsysService);
    }
    
    @Test
    public void skal_ikke_evicte_cache_hvis_header_er_tilfeldig_streng() throws Exception {
        when(request.getHeader(ClearCacheInterceptor.CLEAR_CACHE_HEADER)).thenReturn("ajsdfbgjd");
        clearCacheInterceptor.preHandle(request, null, null);
        verifyNoMoreInteractions(abacAdapter, axsysService);
    }
    
    @Test
    public void skal_ikke_evicte_cache_hvis_header_er_udefinert() throws Exception {
        clearCacheInterceptor.preHandle(request, null, null);
        verifyNoMoreInteractions(abacAdapter, axsysService);
    }
}
