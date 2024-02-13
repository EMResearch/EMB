package no.nav.familie.ba.sak.kjerne.steg

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpService
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.EøsSkjemaerForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.PersonopplysningGrunnlagForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.VilkårsvurderingForNyBehandlingService
import org.junit.jupiter.api.Test

class RegistrerPersongrunnlagEnhetTest {

    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()
    private val personopplysningGrunnlagForNyBehandlingService: PersonopplysningGrunnlagForNyBehandlingService = mockk()
    private val vilkårsvurderingForNyBehandlingService: VilkårsvurderingForNyBehandlingService = mockk()
    private val kompetanseService: KompetanseService = mockk()
    private val valutakursService: ValutakursService = mockk()
    private val utenlandskPeriodebeløpService: UtenlandskPeriodebeløpService = mockk()

    private val registrerPersongrunnlagSteg = RegistrerPersongrunnlag(
        behandlingHentOgPersisterService = behandlingHentOgPersisterService,
        vilkårsvurderingForNyBehandlingService = vilkårsvurderingForNyBehandlingService,
        personopplysningGrunnlagForNyBehandlingService = personopplysningGrunnlagForNyBehandlingService,
        eøsSkjemaerForNyBehandlingService = EøsSkjemaerForNyBehandlingService(
            kompetanseService = kompetanseService,
            utenlandskPeriodebeløpService = utenlandskPeriodebeløpService,
            valutakursService = valutakursService,
        ),
    )

    @Test
    fun `Kopierer kompetanser, valutakurser og utenlandsk periodebeløp til ny behandling`() {
        val mor = lagPerson(type = PersonType.SØKER)
        val barn1 = lagPerson(type = PersonType.BARN)
        val barn2 = lagPerson(type = PersonType.BARN)

        val behandling1 = lagBehandling()
        val behandling2 = lagBehandling()

        every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling2) } returns behandling1

        every {
            personopplysningGrunnlagForNyBehandlingService.opprettKopiEllerNyttPersonopplysningGrunnlag(
                behandling = behandling2,
                forrigeBehandlingSomErVedtatt = behandling1,
                søkerIdent = mor.aktør.aktivFødselsnummer(),
                barnasIdenter = listOf(barn1.aktør.aktivFødselsnummer(), barn2.aktør.aktivFødselsnummer()),
            )
        } just runs

        every {
            vilkårsvurderingForNyBehandlingService.opprettVilkårsvurderingUtenomHovedflyt(
                behandling = behandling2,
                forrigeBehandlingSomErVedtatt = behandling1,
            )
        } just runs

        every {
            kompetanseService.kopierOgErstattKompetanser(
                BehandlingId(behandling1.id),
                BehandlingId(behandling2.id),
            )
        } just runs
        every {
            valutakursService.kopierOgErstattValutakurser(
                BehandlingId(behandling1.id),
                BehandlingId(behandling2.id),
            )
        } just runs
        every {
            utenlandskPeriodebeløpService.kopierOgErstattUtenlandskPeriodebeløp(
                BehandlingId(behandling1.id),
                BehandlingId(behandling2.id),
            )
        } just runs

        registrerPersongrunnlagSteg.utførStegOgAngiNeste(
            behandling = behandling2,
            data = RegistrerPersongrunnlagDTO(
                ident = mor.aktør.aktivFødselsnummer(),
                barnasIdenter = listOf(barn1.aktør.aktivFødselsnummer(), barn2.aktør.aktivFødselsnummer()),
            ),
        )

        verify(exactly = 1) {
            kompetanseService.kopierOgErstattKompetanser(
                BehandlingId(behandling1.id),
                BehandlingId(behandling2.id),
            )
            valutakursService.kopierOgErstattValutakurser(
                BehandlingId(behandling1.id),
                BehandlingId(behandling2.id),
            )
            utenlandskPeriodebeløpService.kopierOgErstattUtenlandskPeriodebeløp(
                BehandlingId(behandling1.id),
                BehandlingId(behandling2.id),
            )
        }
    }
}
