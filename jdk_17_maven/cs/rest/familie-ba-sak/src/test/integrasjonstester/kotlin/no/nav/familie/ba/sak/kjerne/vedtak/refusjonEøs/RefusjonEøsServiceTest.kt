package no.nav.familie.ba.sak.kjerne.vedtak.refusjonEøs

import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.ekstern.restDomene.RestRefusjonEøs
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.Month

class RefusjonEøsServiceTest(
    @Autowired val refusjonEøsService: RefusjonEøsService,
    @Autowired val aktørIdRepository: AktørIdRepository,
    @Autowired val fagsakRepository: FagsakRepository,
    @Autowired val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
) : AbstractSpringIntegrationTest() {

    @Test
    fun kanLagreEndreOgSlette() {
        val fagsak =
            defaultFagsak(aktør = randomAktør().also { aktørIdRepository.save(it) }).let { fagsakRepository.save(it) }
        val behandling =
            lagBehandling(fagsak = fagsak).let { behandlingHentOgPersisterService.lagreEllerOppdater(it, false) }
        val refusjonEøs = RestRefusjonEøs(
            id = 0,
            fom = LocalDate.of(2020, Month.JANUARY, 1),
            tom = LocalDate.of(2021, Month.MAY, 31),
            refusjonsbeløp = 1234,
            land = "SE",
            refusjonAvklart = true,
        )

        val id = refusjonEøsService.leggTilRefusjonEøsPeriode(refusjonEøs = refusjonEøs, behandlingId = behandling.id)

        refusjonEøsService.hentRefusjonEøsPerioder(behandlingId = behandling.id)
            .also { Assertions.assertThat(it[0].id).isEqualTo(id) }
            .also { Assertions.assertThat(it[0].fom).isEqualTo("2020-01-01") }
            .also { Assertions.assertThat(it[0].tom).isEqualTo("2021-05-31") }

        refusjonEøsService.oppdaterRefusjonEøsPeriode(
            restRefusjonEøs = RestRefusjonEøs(
                id = id,
                fom = LocalDate.of(2020, Month.JANUARY, 1),
                tom = LocalDate.of(2020, Month.MAY, 31),
                refusjonsbeløp = 1,
                land = "NL",
                refusjonAvklart = false,
            ),
            id = id,
        )

        refusjonEøsService.hentRefusjonEøsPerioder(behandlingId = behandling.id)
            .also { Assertions.assertThat(it[0].id).isEqualTo(id) }
            .also { Assertions.assertThat(it[0].tom).isEqualTo("2020-05-31") }
            .also { Assertions.assertThat(it[0].refusjonsbeløp).isEqualTo(1) }
            .also { Assertions.assertThat(it[0].land).isEqualTo("NL") }
            .also { Assertions.assertThat(it[0].refusjonAvklart).isEqualTo(false) }

        val refusjonEøs2 = RestRefusjonEøs(
            id = 0,
            fom = LocalDate.of(2019, Month.DECEMBER, 1),
            tom = LocalDate.of(2019, Month.DECEMBER, 31),
            refusjonsbeløp = 100,
            land = "DK",
            refusjonAvklart = false,
        )

        val id2 = refusjonEøsService.leggTilRefusjonEøsPeriode(refusjonEøs = refusjonEøs2, behandlingId = behandling.id)

        refusjonEøsService.hentRefusjonEøsPerioder(behandlingId = behandling.id)
            .also { Assertions.assertThat(it.size).isEqualTo(2) }
            .also { Assertions.assertThat(it[0].id).isEqualTo(id2) }

        refusjonEøsService.fjernRefusjonEøsPeriode(id = id, behandlingId = behandling.id)

        refusjonEøsService.hentRefusjonEøsPerioder(behandlingId = behandling.id)
            .also { Assertions.assertThat(it.size).isEqualTo(1) }
            .also { Assertions.assertThat(it[0].id).isEqualTo(id2) }
    }
}
