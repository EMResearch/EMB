package no.nav.familie.ba.sak.integrasjoner.infotrygd.domene

import java.time.LocalDate

data class InfotrygdFødselhendelsesFeedTaskDto(val fnrBarn: List<String>)
data class InfotrygdVedtakFeedTaskDto(val fnrStoenadsmottaker: String, val behandlingId: Long)

data class InfotrygdFødselhendelsesFeedDto(val fnrBarn: String)
data class InfotrygdVedtakFeedDto(val fnrStoenadsmottaker: String, val datoStartNyBa: LocalDate)

data class StartBehandlingDto(val fnrStoenadsmottaker: String)
