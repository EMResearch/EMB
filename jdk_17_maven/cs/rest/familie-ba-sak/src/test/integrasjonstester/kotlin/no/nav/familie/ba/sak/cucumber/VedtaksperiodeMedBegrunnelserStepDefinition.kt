package no.nav.familie.ba.sak.cucumber

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Og
import io.cucumber.java.no.Så
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.cucumber.domeneparser.Domenebegrep
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.mapForventetVedtaksperioderMedBegrunnelser
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.parseAktørId
import no.nav.familie.ba.sak.cucumber.domeneparser.parseDato
import no.nav.familie.ba.sak.cucumber.domeneparser.parseLong
import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import org.assertj.core.api.Assertions
import java.time.LocalDate

class VedtaksperiodeMedBegrunnelserStepDefinition {

    private var fagsaker: Map<Long, Fagsak> = emptyMap()
    private var behandlinger = mutableMapOf<Long, Behandling>()
    private var behandlingTilForrigeBehandling = mutableMapOf<Long, Long?>()
    private var vedtaksliste = mutableListOf<Vedtak>()
    private var persongrunnlag = mapOf<Long, PersonopplysningGrunnlag>()
    private var personResultater = mutableMapOf<Long, Set<PersonResultat>>()
    private var vedtaksperioderMedBegrunnelser = listOf<VedtaksperiodeMedBegrunnelser>()
    private var kompetanser = mutableMapOf<Long, List<Kompetanse>>()
    private var endredeUtbetalinger = mutableMapOf<Long, List<EndretUtbetalingAndel>>()
    private var andelerTilkjentYtelse = mutableMapOf<Long, List<AndelTilkjentYtelse>>()
    private var overstyrteEndringstidspunkt = mapOf<Long, LocalDate>()
    private var overgangsstønad = mapOf<Long, List<InternPeriodeOvergangsstønad>>()
    private var uregistrerteBarn = listOf<BarnMedOpplysninger>()
    private var dagensDato: LocalDate = LocalDate.now()

    private var gjeldendeBehandlingId: Long? = null

    /**
     * Mulige verdier: | FagsakId | Fagsaktype |
     */
    @Gitt("følgende fagsaker")
    fun `følgende fagsaker`(dataTable: DataTable) {
        fagsaker = lagFagsaker(dataTable)
    }

    /**
     * Mulige verdier:
     * | BehandlingId | ForrigeBehandlingId | FagsakId | Behandlingsresultat | Behandlingsårsak |
     */
    @Gitt("følgende vedtak")
    fun `følgende vedtak`(dataTable: DataTable) {
        lagVedtak(dataTable, behandlinger, behandlingTilForrigeBehandling, vedtaksliste, fagsaker)
    }

    @Og("dagens dato er {}")
    fun `dagens dato er`(dagensDatoString: String) {
        dagensDato = parseDato(dagensDatoString)
    }

    /**
     * Mulige verdier: | BehandlingId |  AktørId | Persontype | Fødselsdato |
     */
    @Og("følgende persongrunnlag")
    fun `følgende persongrunnlag`(dataTable: DataTable) {
        persongrunnlag = lagPersonGrunnlag(dataTable)
    }

    @Og("lag personresultater for behandling {}")
    fun `lag personresultater`(behandlingId: Long) {
        val persongrunnlagForBehandling = persongrunnlag.finnPersonGrunnlagForBehandling(behandlingId)
        val behandling = behandlinger.finnBehandling(behandlingId)
        personResultater[behandlingId] = lagPersonresultater(persongrunnlagForBehandling, behandling)
    }

    /**
     * Mulige verdier: | AktørId | Vilkår | Utdypende vilkår | Fra dato | Til dato | Resultat | Er eksplisitt avslag |
     */
    @Og("legg til nye vilkårresultater for behandling {}")
    fun `legg til nye vilkårresultater for behandling`(behandlingId: Long, dataTable: DataTable) {
        val vilkårResultaterPerPerson = dataTable.asMaps().groupBy { parseAktørId(it) }
        val personResultatForBehandling = personResultater[behandlingId]
            ?: error("Finner ikke personresultater for behandling med id $behandlingId")

        personResultater[behandlingId] =
            leggTilVilkårResultatPåPersonResultat(personResultatForBehandling, vilkårResultaterPerPerson, behandlingId)
    }

    /**
     * Mulige verdier: | BehandlingId | Endringstidspunkt |
     */
    @Og("med overstyrt endringstidspunkt")
    fun settEndringstidspunkt(dataTable: DataTable) {
        overstyrteEndringstidspunkt = dataTable.asMaps().associate { rad ->
            parseLong(Domenebegrep.BEHANDLING_ID, rad) to
                parseDato(DomenebegrepVedtaksperiodeMedBegrunnelser.ENDRINGSTIDSPUNKT, rad)
        }
    }

    /**
     * Mulige verdier: | AktørId | Fra dato | Til dato | Resultat | BehandlingId |
     */
    @Og("med kompetanser")
    fun `med kompetanser`(dataTable: DataTable) {
        val nyeKompetanserPerBarn = dataTable.asMaps()
        kompetanser = lagKompetanser(nyeKompetanserPerBarn, persongrunnlag)
    }

    /**
     * Mulige verdier: | AktørId | Fra dato | Til dato | BehandlingId |  Årsak | Prosent |
     */
    @Og("med endrede utbetalinger")
    fun `med endrede utbetalinger`(dataTable: DataTable) {
        val nyeEndredeUtbetalingAndeler = dataTable.asMaps()
        endredeUtbetalinger = lagEndredeUtbetalinger(nyeEndredeUtbetalingAndeler, persongrunnlag)
    }

    /**
     * Mulige verdier: | AktørId | BehandlingId | Fra dato | Til dato | Beløp | Ytelse type | Prosent | Sats |
     */
    @Og("med andeler tilkjent ytelse")
    fun `med andeler tilkjent ytelse`(dataTable: DataTable) {
        andelerTilkjentYtelse = lagAndelerTilkjentYtelse(dataTable, behandlinger, persongrunnlag)
    }

    /**
     * Mulige verdier: | BehandlingId | AktørId | Fra dato | Til dato |
     */
    @Og("med overgangsstønad")
    fun `med overgangsstønad`(dataTable: DataTable) {
        overgangsstønad = lagOvergangsstønad(
            dataTable = dataTable,
            persongrunnlag = persongrunnlag,
            tidligereBehandlinger = behandlingTilForrigeBehandling,
            dagensDato = LocalDate.now(),
        )
    }

    @Og("med uregistrerte barn")
    fun `med uregistrerte barn`() {
        uregistrerteBarn = listOf(BarnMedOpplysninger(ident = ""))
    }

    @Når("vedtaksperioder med begrunnelser genereres for behandling {}")
    fun `generer vedtaksperiode med begrunnelse`(behandlingId: Long) {
        gjeldendeBehandlingId = behandlingId

        vedtaksperioderMedBegrunnelser = lagVedtaksPerioder(
            behandlingId = behandlingId,
            vedtaksListe = vedtaksliste,
            behandlingTilForrigeBehandling = behandlingTilForrigeBehandling,
            personGrunnlag = persongrunnlag,
            personResultater = personResultater,
            kompetanser = kompetanser,
            endredeUtbetalinger = endredeUtbetalinger,
            andelerTilkjentYtelse = andelerTilkjentYtelse,
            overstyrteEndringstidspunkt = overstyrteEndringstidspunkt,
            overgangsstønad = overgangsstønad,
            uregistrerteBarn = uregistrerteBarn,
            nåDato = dagensDato,
        )
    }

    @Så("forvent følgende vedtaksperioder med begrunnelser")
    fun `forvent følgende vedtaksperioder med begrunnelser`(dataTable: DataTable) {
        val forventedeVedtaksperioder = mapForventetVedtaksperioderMedBegrunnelser(
            dataTable = dataTable,
            vedtak = vedtaksliste.find { it.behandling.id == gjeldendeBehandlingId }
                ?: throw Feil("Fant ingen vedtak for behandling $gjeldendeBehandlingId"),
        )

        val vedtaksperioderComparator = compareBy<VedtaksperiodeMedBegrunnelser>({ it.type }, { it.fom }, { it.tom })
        Assertions.assertThat(vedtaksperioderMedBegrunnelser.sortedWith(vedtaksperioderComparator))
            .usingRecursiveComparison().ignoringFieldsMatchingRegexes(".*endretTidspunkt", ".*opprettetTidspunkt")
            .isEqualTo(forventedeVedtaksperioder.sortedWith(vedtaksperioderComparator))
    }
}
