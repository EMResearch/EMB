package no.nav.familie.tilbake.behandlingskontroll

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandling.domain.Fagsystemsbehandling
import no.nav.familie.tilbake.behandling.domain.Varsel
import no.nav.familie.tilbake.behandling.domain.Varselsperiode
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg.AVSLUTTET
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg.FAKTA
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg.FORELDELSE
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg.GRUNNLAG
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg.VARSEL
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg.VILKÅRSVURDERING
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus.AUTOUTFØRT
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus.AVBRUTT
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus.KLAR
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus.UTFØRT
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus.VENTER
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

internal class BehandlingskontrollServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var behandlingskontrollService: BehandlingskontrollService

    private val behandling = Testdata.behandling

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(behandling)
    }

    @Test
    fun `fortsettBehandling skal oppdatere til varselssteg etter behandling er opprettet med varsel`() {
        val fagsystemsbehandling = lagFagsystemsbehandling(Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL)
        val varsel = Varsel(
            varseltekst = "testverdi",
            varselbeløp = 1000L,
            perioder = setOf(
                Varselsperiode(
                    fom = LocalDate.now().minusMonths(2),
                    tom = LocalDate.now(),
                ),
            ),
        )
        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(
            lagretBehandling.copy(
                fagsystemsbehandling = setOf(fagsystemsbehandling),
                varsler = setOf(varsel),
            ),
        )

        behandlingskontrollService.fortsettBehandling(behandlingId = behandling.id)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        behandlingsstegstilstand.size shouldBe 1
        val sisteStegstilstand = behandlingsstegstilstand[0]
        sisteStegstilstand.behandlingssteg shouldBe VARSEL
        sisteStegstilstand.behandlingsstegsstatus shouldBe VENTER
        assertBehandlingsstatus(behandling.id, Behandlingsstatus.UTREDES)
        sisteStegstilstand.venteårsak shouldBe Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING
        sisteStegstilstand.tidsfrist shouldBe behandling.opprettetDato.plusWeeks(3)
    }

    @Test
    fun `fortsettBehandling skal ikke fortsette til grunnlagssteg når behandling venter på varsel steg`() {
        behandlingskontrollService.fortsettBehandling(behandlingId = behandling.id)
        var behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        behandlingsstegstilstand.size shouldBe 1
        behandlingsstegstilstand[0].behandlingssteg shouldBe VARSEL
        behandlingsstegstilstand[0].behandlingsstegsstatus shouldBe VENTER
        assertBehandlingsstatus(behandling.id, Behandlingsstatus.UTREDES)

        behandlingskontrollService.fortsettBehandling(behandlingId = behandling.id)

        behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        behandlingsstegstilstand.size shouldBe 1
        val nyStegstilstand = behandlingsstegstilstand[0]
        nyStegstilstand.behandlingssteg shouldBe VARSEL
        nyStegstilstand.behandlingsstegsstatus shouldBe VENTER
        assertBehandlingsstatus(behandling.id, Behandlingsstatus.UTREDES)
        nyStegstilstand.venteårsak shouldBe Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING
        nyStegstilstand.tidsfrist shouldBe behandling.opprettetDato.plusWeeks(3)
    }

    @Test
    fun `fortsettBehandling skal oppdatere til grunnlagssteg etter behandling er opprettet uten varsel`() {
        val fagsystemsbehandling = lagFagsystemsbehandling(Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL)
        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(
            lagretBehandling.copy(
                fagsystemsbehandling = setOf(fagsystemsbehandling),
                varsler = emptySet(),
            ),
        )

        behandlingskontrollService.fortsettBehandling(behandlingId = behandling.id)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        behandlingsstegstilstand.size shouldBe 1
        val sisteStegstilstand = behandlingsstegstilstand[0]
        sisteStegstilstand.behandlingssteg shouldBe GRUNNLAG
        sisteStegstilstand.behandlingsstegsstatus shouldBe VENTER
        assertBehandlingsstatus(behandling.id, Behandlingsstatus.UTREDES)
        sisteStegstilstand.venteårsak shouldBe Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG
        sisteStegstilstand.tidsfrist shouldBe behandling.opprettetDato.plusWeeks(4)
    }

    @Test
    fun `fortsettBehandling skal fortsette til grunnlagssteg når varselsrespons ble mottatt uten kravgrunnlag`() {
        lagBehandlingsstegstilstand(setOf(Behandlingsstegsinfo(VARSEL, UTFØRT)))

        behandlingskontrollService.fortsettBehandling(behandlingId = behandling.id)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        behandlingsstegstilstand.size shouldBe 2
        val aktivtstegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstand)
        aktivtstegstilstand.shouldNotBeNull()
        aktivtstegstilstand.behandlingssteg shouldBe GRUNNLAG
        aktivtstegstilstand.behandlingsstegsstatus shouldBe VENTER
        assertBehandlingsstatus(behandling.id, Behandlingsstatus.UTREDES)
        aktivtstegstilstand.venteårsak shouldBe Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG
        aktivtstegstilstand.tidsfrist shouldBe behandling.opprettetDato.plusWeeks(4)
    }

    @Test
    fun `fortsettBehandling skal fortsette til grunnlagssteg når varselsrespons ble mottatt med sperret kravgrunnlag`() {
        lagBehandlingsstegstilstand(setOf(Behandlingsstegsinfo(VARSEL, UTFØRT)))
        val kravgrunnlag = Testdata.kravgrunnlag431
        val oppdatertKravgrunnlag = kravgrunnlag.copy(sperret = true)
        kravgrunnlagRepository.insert(oppdatertKravgrunnlag)

        behandlingskontrollService.fortsettBehandling(behandlingId = behandling.id)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        behandlingsstegstilstand.size shouldBe 2
        val aktivtstegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstand)
        aktivtstegstilstand.shouldNotBeNull()
        aktivtstegstilstand.behandlingssteg shouldBe GRUNNLAG
        aktivtstegstilstand.behandlingsstegsstatus shouldBe VENTER
        assertBehandlingsstatus(behandling.id, Behandlingsstatus.UTREDES)
        aktivtstegstilstand.venteårsak shouldBe Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG
        aktivtstegstilstand.tidsfrist shouldBe oppdatertKravgrunnlag.sporbar.endret.endretTid.plusWeeks(4).toLocalDate()
    }

    @Test
    fun `fortsettBehandling skal fortsette til fakta steg når varselsrespons ble mottatt med aktivt kravgrunnlag`() {
        val behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(behandling.copy(verger = emptySet()))

        lagBehandlingsstegstilstand(setOf(Behandlingsstegsinfo(VARSEL, UTFØRT)))
        val kravgrunnlag = Testdata.kravgrunnlag431
        kravgrunnlagRepository.insert(kravgrunnlag)

        behandlingskontrollService.fortsettBehandling(behandlingId = behandling.id)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        behandlingsstegstilstand.size shouldBe 2
        val aktivtstegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstand)
        aktivtstegstilstand.shouldNotBeNull()
        aktivtstegstilstand.behandlingssteg shouldBe FAKTA
        aktivtstegstilstand.behandlingsstegsstatus shouldBe KLAR
        assertBehandlingsstatus(behandling.id, Behandlingsstatus.UTREDES)
        aktivtstegstilstand.venteårsak.shouldBeNull()
        aktivtstegstilstand.tidsfrist.shouldBeNull()
    }

    @Test
    fun `fortsettBehandling skal oppdatere til fakta steg etter behandling er opprettet uten varsel og mottok kravgrunnlag`() {
        val fagsystemsbehandling = lagFagsystemsbehandling(Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL)
        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(
            lagretBehandling.copy(
                fagsystemsbehandling = setOf(fagsystemsbehandling),
                varsler = emptySet(),
            ),
        )
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)

        behandlingskontrollService.fortsettBehandling(behandlingId = behandling.id)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        behandlingsstegstilstand.size shouldBe 1
        val sisteStegstilstand = behandlingsstegstilstand[0]
        sisteStegstilstand.behandlingssteg shouldBe FAKTA
        sisteStegstilstand.behandlingsstegsstatus shouldBe KLAR
        assertBehandlingsstatus(behandling.id, Behandlingsstatus.UTREDES)
        sisteStegstilstand.venteårsak.shouldBeNull()
        sisteStegstilstand.tidsfrist.shouldBeNull()
    }

    @Test
    fun `fortsettBehandling skal oppdatere til foreldelsessteg etter fakta steg er utført`() {
        lagBehandlingsstegstilstand(setOf(Behandlingsstegsinfo(FAKTA, UTFØRT)))
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)

        behandlingskontrollService.fortsettBehandling(behandlingId = behandling.id)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        behandlingsstegstilstand.size shouldBe 2
        val sisteStegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstand)
        sisteStegstilstand.shouldNotBeNull()
        sisteStegstilstand.behandlingssteg shouldBe FORELDELSE
        sisteStegstilstand.behandlingsstegsstatus shouldBe KLAR
        assertBehandlingsstatus(behandling.id, Behandlingsstatus.UTREDES)
        sisteStegstilstand.venteårsak.shouldBeNull()
        sisteStegstilstand.tidsfrist.shouldBeNull()
    }

    @Test
    fun `fortsettBehandling skal oppdatere til vilkårsvurderingssteg etter foreldelse steg er utført`() {
        lagBehandlingsstegstilstand(
            setOf(
                Behandlingsstegsinfo(FAKTA, UTFØRT),
                Behandlingsstegsinfo(FORELDELSE, UTFØRT),
            ),
        )

        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)
        behandlingskontrollService.fortsettBehandling(behandlingId = behandling.id)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        behandlingsstegstilstand.size shouldBe 3
        val sisteStegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstand)
        sisteStegstilstand.shouldNotBeNull()
        sisteStegstilstand.behandlingssteg shouldBe VILKÅRSVURDERING
        sisteStegstilstand.behandlingsstegsstatus shouldBe KLAR
        assertBehandlingsstatus(behandling.id, Behandlingsstatus.UTREDES)
        sisteStegstilstand.venteårsak.shouldBeNull()
        sisteStegstilstand.tidsfrist.shouldBeNull()
    }

    @Test
    fun `fortsettBehandling skal ikke oppdatere til foreldelsessteg når fakta steg ikke er utført`() {
        lagBehandlingsstegstilstand(
            setOf(
                Behandlingsstegsinfo(VARSEL, UTFØRT),
                Behandlingsstegsinfo(GRUNNLAG, UTFØRT),
                Behandlingsstegsinfo(FAKTA, KLAR),
            ),
        )

        behandlingskontrollService.fortsettBehandling(behandlingId = behandling.id)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        behandlingsstegstilstand.size shouldBe 3
        val sisteStegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstand)
        sisteStegstilstand.shouldNotBeNull()
        sisteStegstilstand.behandlingssteg shouldBe FAKTA
        sisteStegstilstand.behandlingsstegsstatus shouldBe KLAR
        sisteStegstilstand.venteårsak.shouldBeNull()
        sisteStegstilstand.tidsfrist.shouldBeNull()
    }

    @Test
    fun `fortsettBehandling skal oppdatere til fakta steg etter mottok endr melding`() {
        lagBehandlingsstegstilstand(
            setOf(
                Behandlingsstegsinfo(VARSEL, UTFØRT),
                Behandlingsstegsinfo(GRUNNLAG, UTFØRT),
                Behandlingsstegsinfo(FAKTA, AVBRUTT),
                Behandlingsstegsinfo(FORELDELSE, AVBRUTT),
                Behandlingsstegsinfo(VILKÅRSVURDERING, AVBRUTT),
            ),
        )

        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)

        behandlingskontrollService.fortsettBehandling(behandlingId = behandling.id)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        behandlingsstegstilstand.size shouldBe 5
        val sisteStegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstand)
        sisteStegstilstand.shouldNotBeNull()
        sisteStegstilstand.behandlingssteg shouldBe FAKTA
        sisteStegstilstand.behandlingsstegsstatus shouldBe KLAR
        assertBehandlingsstatus(behandling.id, Behandlingsstatus.UTREDES)
        sisteStegstilstand.venteårsak.shouldBeNull()
        sisteStegstilstand.tidsfrist.shouldBeNull()
    }

    @Test
    fun `tilbakehoppBehandlingssteg skal oppdatere til varselssteg når manuelt varsel sendt og behandling er i vilkår steg `() {
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)
        lagBehandlingsstegstilstand(
            setOf(
                Behandlingsstegsinfo(VARSEL, UTFØRT),
                Behandlingsstegsinfo(GRUNNLAG, UTFØRT),
                Behandlingsstegsinfo(FAKTA, UTFØRT),
                Behandlingsstegsinfo(FORELDELSE, AUTOUTFØRT),
                Behandlingsstegsinfo(VILKÅRSVURDERING, KLAR),
            ),
        )

        behandlingskontrollService
            .tilbakehoppBehandlingssteg(
                behandlingId = behandling.id,
                behandlingsstegsinfo =
                lagBehandlingsstegsinfo(VARSEL, Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING),
            )
        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        behandlingsstegstilstand.size shouldBe 5

        val sisteStegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstand)
        sisteStegstilstand.shouldNotBeNull()
        sisteStegstilstand.behandlingssteg shouldBe VARSEL
        sisteStegstilstand.behandlingsstegsstatus shouldBe VENTER
        assertBehandlingsstatus(behandling.id, Behandlingsstatus.UTREDES)
        sisteStegstilstand.venteårsak shouldBe Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING
        sisteStegstilstand.tidsfrist shouldBe behandling.opprettetDato.plusWeeks(3)

        behandlingsstegstilstand.first { GRUNNLAG == it.behandlingssteg }.behandlingsstegsstatus shouldBe UTFØRT
        behandlingsstegstilstand.first { FAKTA == it.behandlingssteg }.behandlingsstegsstatus shouldBe UTFØRT
        behandlingsstegstilstand.first { FORELDELSE == it.behandlingssteg }.behandlingsstegsstatus shouldBe AUTOUTFØRT
        behandlingsstegstilstand.first { VILKÅRSVURDERING == it.behandlingssteg }.behandlingsstegsstatus shouldBe AVBRUTT
    }

    @Test
    fun `tilbakehoppBehandlingssteg skal oppdatere til varselssteg når mottok sper melding og behandling er i vilkår steg `() {
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)
        lagBehandlingsstegstilstand(
            setOf(
                Behandlingsstegsinfo(VARSEL, UTFØRT),
                Behandlingsstegsinfo(GRUNNLAG, UTFØRT),
                Behandlingsstegsinfo(FAKTA, UTFØRT),
                Behandlingsstegsinfo(FORELDELSE, AUTOUTFØRT),
                Behandlingsstegsinfo(VILKÅRSVURDERING, KLAR),
            ),
        )

        behandlingskontrollService
            .tilbakehoppBehandlingssteg(
                behandlingId = behandling.id,
                behandlingsstegsinfo =
                lagBehandlingsstegsinfo(GRUNNLAG, Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG),
            )

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        behandlingsstegstilstand.size shouldBe 5

        val sisteStegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstand)
        sisteStegstilstand.shouldNotBeNull()
        sisteStegstilstand.behandlingssteg shouldBe GRUNNLAG
        sisteStegstilstand.behandlingsstegsstatus shouldBe VENTER
        assertBehandlingsstatus(behandling.id, Behandlingsstatus.UTREDES)
        sisteStegstilstand.venteårsak shouldBe Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG
        sisteStegstilstand.tidsfrist shouldBe behandling.opprettetDato.plusWeeks(4)

        behandlingsstegstilstand.first { VARSEL == it.behandlingssteg }.behandlingsstegsstatus shouldBe UTFØRT
        behandlingsstegstilstand.first { FAKTA == it.behandlingssteg }.behandlingsstegsstatus shouldBe UTFØRT
        behandlingsstegstilstand.first { FORELDELSE == it.behandlingssteg }.behandlingsstegsstatus shouldBe AUTOUTFØRT
        behandlingsstegstilstand.first { VILKÅRSVURDERING == it.behandlingssteg }.behandlingsstegsstatus shouldBe AVBRUTT
    }

    @Test
    fun `settBehandlingPåVent skal sette behandling på vent med avventer dokumentasjon når behandling er i fakta steg`() {
        val tidsfrist: LocalDate = LocalDate.now().plusWeeks(2)
        lagBehandlingsstegstilstand(
            setOf(
                Behandlingsstegsinfo(VARSEL, UTFØRT),
                Behandlingsstegsinfo(GRUNNLAG, UTFØRT),
                Behandlingsstegsinfo(FAKTA, KLAR),
            ),
        )

        behandlingskontrollService.settBehandlingPåVent(
            behandlingId = behandling.id,
            venteårsak = Venteårsak.AVVENTER_DOKUMENTASJON,
            tidsfrist = tidsfrist,
        )

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        behandlingsstegstilstand.size shouldBe 3

        val sisteStegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstand)
        sisteStegstilstand.shouldNotBeNull()
        sisteStegstilstand.behandlingssteg shouldBe FAKTA
        sisteStegstilstand.behandlingsstegsstatus shouldBe VENTER
        assertBehandlingsstatus(behandling.id, Behandlingsstatus.UTREDES)
        sisteStegstilstand.venteårsak shouldBe Venteårsak.AVVENTER_DOKUMENTASJON
        sisteStegstilstand.tidsfrist shouldBe tidsfrist
    }

    @Test
    fun `settBehandlingPåVent skal ikke sette behandling på vent med avventer dokumentasjon når behandling er avsluttet`() {
        val tidsfrist: LocalDate = LocalDate.now().plusWeeks(2)
        lagBehandlingsstegstilstand(
            setOf(
                Behandlingsstegsinfo(VARSEL, AVBRUTT),
                Behandlingsstegsinfo(AVSLUTTET, UTFØRT),
            ),
        )

        val exception = shouldThrow<RuntimeException>(block = {
            behandlingskontrollService.settBehandlingPåVent(
                behandlingId = behandling.id,
                venteårsak = Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
                tidsfrist = tidsfrist.minusDays(5),
            )
        })
        exception.message shouldBe "Behandling ${behandling.id} har ikke aktivt steg"
    }

    @Test
    fun `settBehandlingPåVent skal utvide fristen med brukerstilbakemelding når behandling er i varsel steg`() {
        val tidsfrist: LocalDate =
            behandling.opprettetDato.plusWeeks(Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING.defaultVenteTidIUker)
        lagBehandlingsstegstilstand(
            setOf(
                Behandlingsstegsinfo(
                    VARSEL,
                    VENTER,
                    venteårsak = Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
                    tidsfrist = tidsfrist,
                ),
            ),
        )

        behandlingskontrollService.settBehandlingPåVent(
            behandlingId = behandling.id,
            venteårsak = Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
            tidsfrist = tidsfrist.plusWeeks(2),
        )

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        behandlingsstegstilstand.size shouldBe 1

        val sisteStegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstand)
        sisteStegstilstand.shouldNotBeNull()
        sisteStegstilstand.behandlingssteg shouldBe VARSEL
        sisteStegstilstand.behandlingsstegsstatus shouldBe VENTER
        assertBehandlingsstatus(behandling.id, Behandlingsstatus.UTREDES)
        sisteStegstilstand.venteårsak shouldBe Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING
        sisteStegstilstand.tidsfrist shouldBe tidsfrist.plusWeeks(2)
    }

    private fun lagBehandlingsstegstilstand(stegMetadata: Set<Behandlingsstegsinfo>) {
        stegMetadata.map {
            behandlingsstegstilstandRepository.insert(
                Behandlingsstegstilstand(
                    behandlingId = behandling.id,
                    behandlingssteg = it.behandlingssteg,
                    behandlingsstegsstatus = it.behandlingsstegstatus,
                    venteårsak = it.venteårsak,
                    tidsfrist = it.tidsfrist,
                ),
            )
        }
    }

    private fun lagFagsystemsbehandling(tilbakekrevingsvalg: Tilbakekrevingsvalg): Fagsystemsbehandling {
        return Fagsystemsbehandling(
            eksternId = "123",
            tilbakekrevingsvalg = tilbakekrevingsvalg,
            resultat = "testverdi",
            årsak = "testverdi",
            revurderingsvedtaksdato = LocalDate.now().minusDays(1),
        )
    }

    private fun lagBehandlingsstegsinfo(
        behandlingssteg: Behandlingssteg,
        venteårsak: Venteårsak,
    ): Behandlingsstegsinfo {
        return Behandlingsstegsinfo(
            behandlingssteg = behandlingssteg,
            behandlingsstegstatus = VENTER,
            venteårsak = venteårsak,
            tidsfrist = behandling.opprettetDato.plusWeeks(venteårsak.defaultVenteTidIUker),
        )
    }

    private fun assertBehandlingsstatus(behandlingId: UUID, behandlingsstatus: Behandlingsstatus) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandling.status shouldBe behandlingsstatus
    }
}
