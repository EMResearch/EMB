package no.nav.familie.ba.sak.statistikk.saksstatistikk

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagring
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringRepository
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringType
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

@Component
class SaksstatistikkEventListener(
    private val saksstatistikkService: SaksstatistikkService,
    private val saksstatistikkMellomlagringRepository: SaksstatistikkMellomlagringRepository,
) : ApplicationListener<SaksstatistikkEvent> {

    override fun onApplicationEvent(event: SaksstatistikkEvent) {
        if (event.behandlingId != null) {
            saksstatistikkService.mapTilBehandlingDVH(event.behandlingId)?.also {
                saksstatistikkMellomlagringRepository.save(
                    SaksstatistikkMellomlagring(
                        funksjonellId = it.funksjonellId,
                        kontraktVersjon = it.versjon,
                        json = sakstatistikkObjectMapper.writeValueAsString(it),
                        type = SaksstatistikkMellomlagringType.BEHANDLING,
                        typeId = event.behandlingId,
                    ),
                )
            }
        } else if (event.fagsakId != null) {
            saksstatistikkService.mapTilSakDvh(event.fagsakId)?.also {
                saksstatistikkMellomlagringRepository.save(
                    SaksstatistikkMellomlagring(
                        funksjonellId = it.funksjonellId,
                        kontraktVersjon = it.versjon,
                        json = sakstatistikkObjectMapper.writeValueAsString(it),
                        type = SaksstatistikkMellomlagringType.SAK,
                        typeId = event.fagsakId,
                    ),
                )
            }
        }
    }
}

val sakstatistikkObjectMapper: ObjectMapper = objectMapper.copy()
    .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
    .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)
