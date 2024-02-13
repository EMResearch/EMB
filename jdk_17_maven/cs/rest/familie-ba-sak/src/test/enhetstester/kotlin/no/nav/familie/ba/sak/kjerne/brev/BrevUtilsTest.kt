package no.nav.familie.ba.sak.kjerne.brev

import io.mockk.mockk
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagSanityBegrunnelse
import no.nav.familie.ba.sak.common.lagSanityEøsBegrunnelse
import no.nav.familie.ba.sak.common.lagUtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.common.lagUtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.common.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.config.testSanityKlient
import no.nav.familie.ba.sak.datagenerator.vedtak.lagVedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertVedtaksperiode
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.EØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Opphørsperiode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class BrevUtilsTest {

    @Test
    fun `hent dokumenttittel dersom denne skal overstyres for behandlingen`() {
        assertNull(hentOverstyrtDokumenttittel(lagBehandling().copy(type = BehandlingType.FØRSTEGANGSBEHANDLING)))
        val revurdering = lagBehandling().copy(type = BehandlingType.REVURDERING)
        assertNull(hentOverstyrtDokumenttittel(revurdering))
        Assertions.assertEquals(
            "Vedtak om endret barnetrygd - barn 6 år",
            hentOverstyrtDokumenttittel(revurdering.copy(opprettetÅrsak = BehandlingÅrsak.OMREGNING_6ÅR)),
        )
        Assertions.assertEquals(
            "Vedtak om endret barnetrygd - barn 18 år",
            hentOverstyrtDokumenttittel(revurdering.copy(opprettetÅrsak = BehandlingÅrsak.OMREGNING_18ÅR)),
        )
        Assertions.assertEquals(
            "Vedtak om endret barnetrygd",
            hentOverstyrtDokumenttittel(revurdering.copy(resultat = Behandlingsresultat.INNVILGET_OG_ENDRET)),
        )
        Assertions.assertEquals(
            "Vedtak om fortsatt barnetrygd",
            hentOverstyrtDokumenttittel(revurdering.copy(resultat = Behandlingsresultat.FORTSATT_INNVILGET)),
        )
        assertNull(hentOverstyrtDokumenttittel(revurdering.copy(resultat = Behandlingsresultat.OPPHØRT)))
    }

    @Test
    fun `hentHjemmeltekst skal returnere sorterte hjemler`() {
        val utvidetVedtaksperioderMedBegrunnelser = listOf(
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET,
                        vedtaksperiodeMedBegrunnelser = lagVedtaksperiodeMedBegrunnelser(),
                    ),
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj()),
            ),
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_SATSENDRING,
                        vedtaksperiodeMedBegrunnelser = lagVedtaksperiodeMedBegrunnelser(),
                    ),
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj()),
            ),
        )

        Assertions.assertEquals(
            "barnetrygdloven §§ 2, 4, 10 og 11",
            hentHjemmeltekst(
                minimerteVedtaksperioder = utvidetVedtaksperioderMedBegrunnelser.map {
                    it.tilMinimertVedtaksperiode(
                        testSanityKlient.hentBegrunnelserMap(),
                        emptyMap(),
                    )
                },
                sanityBegrunnelser = mapOf(
                    Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET to lagSanityBegrunnelse(
                        apiNavn = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET.sanityApiNavn,
                        hjemler = listOf("11", "4", "2", "10"),
                    ),
                    Standardbegrunnelse.INNVILGET_SATSENDRING to lagSanityBegrunnelse(
                        apiNavn = Standardbegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                        hjemler = listOf("10"),
                    ),
                ),
                målform = Målform.NB,
                refusjonEøsHjemmelSkalMedIBrev = false,
            ),
        )
    }

    @Test
    fun `hentHjemmeltekst skal ikke inkludere hjemmel 17 og 18 hvis opplysningsplikt er oppfylt`() {
        val utvidetVedtaksperioderMedBegrunnelser = listOf(
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET,
                    ),
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj()),
            ),
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_SATSENDRING,
                    ),
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj()),
            ),
        )

        Assertions.assertEquals(
            "barnetrygdloven §§ 2, 4, 10 og 11",
            hentHjemmeltekst(
                minimerteVedtaksperioder = utvidetVedtaksperioderMedBegrunnelser.map {
                    it.tilMinimertVedtaksperiode(
                        sanityBegrunnelser = testSanityKlient.hentBegrunnelserMap(),
                        sanityEØSBegrunnelser = emptyMap(),
                    )
                },
                sanityBegrunnelser = mapOf(
                    Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET to lagSanityBegrunnelse(
                        apiNavn = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET.sanityApiNavn,
                        hjemler = listOf("11", "4", "2", "10"),
                    ),
                    Standardbegrunnelse.INNVILGET_SATSENDRING to lagSanityBegrunnelse(
                        apiNavn = Standardbegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                        hjemler = listOf("10"),
                    ),
                ),
                opplysningspliktHjemlerSkalMedIBrev = false,
                målform = Målform.NB,
                refusjonEøsHjemmelSkalMedIBrev = false,
            ),
        )
    }

    @Test
    fun `hentHjemmeltekst skal inkludere hjemmel 17 og 18 hvis opplysningsplikt ikke er oppfylt`() {
        val utvidetVedtaksperioderMedBegrunnelser = listOf(
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET,
                    ),
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj()),
            ),
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_SATSENDRING,
                    ),
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj()),
            ),
        )

        Assertions.assertEquals(
            "barnetrygdloven §§ 2, 4, 10, 11, 17 og 18",
            hentHjemmeltekst(
                minimerteVedtaksperioder = utvidetVedtaksperioderMedBegrunnelser.map {
                    it.tilMinimertVedtaksperiode(
                        sanityBegrunnelser = testSanityKlient.hentBegrunnelserMap(),
                        sanityEØSBegrunnelser = emptyMap(),
                    )
                },
                sanityBegrunnelser = mapOf(
                    Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET to lagSanityBegrunnelse(
                        apiNavn = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET.sanityApiNavn,
                        hjemler = listOf("11", "4", "2", "10"),
                    ),
                    Standardbegrunnelse.INNVILGET_SATSENDRING to lagSanityBegrunnelse(
                        apiNavn = Standardbegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                        hjemler = listOf("10"),
                    ),
                ),
                opplysningspliktHjemlerSkalMedIBrev = true,
                målform = Målform.NB,
                refusjonEøsHjemmelSkalMedIBrev = false,
            ),
        )
    }

    @Test
    fun `hentHjemmeltekst skal inkludere EØS-forordning 987 artikkel 60 hvis det eksisterer eøs refusjon på behandlingen`() {
        Assertions.assertEquals(
            "EØS-forordning 987/2009 artikkel 60",
            hentHjemmeltekst(
                minimerteVedtaksperioder = emptyList(),
                sanityBegrunnelser = emptyMap(),
                opplysningspliktHjemlerSkalMedIBrev = false,
                målform = Målform.NB,
                refusjonEøsHjemmelSkalMedIBrev = true,
            ),
        )
    }

    @Test
    fun `Skal gi riktig hjemmeltekst ved hjemler både fra barnetrygdloven og folketrygdloven`() {
        val utvidetVedtaksperioderMedBegrunnelser = listOf(
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_SØKER_OG_BARN_FRIVILLIG_MEDLEM,
                    ),
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj()),
            ),
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_SATSENDRING,
                    ),
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj()),
            ),
        )

        val sanityBegrunnelser = mapOf(
            Standardbegrunnelse.INNVILGET_SØKER_OG_BARN_FRIVILLIG_MEDLEM to lagSanityBegrunnelse(
                apiNavn = Standardbegrunnelse.INNVILGET_SØKER_OG_BARN_FRIVILLIG_MEDLEM.sanityApiNavn,
                hjemler = listOf("11", "4"),
                hjemlerFolketrygdloven = listOf("2-5", "2-8"),
            ),
            Standardbegrunnelse.INNVILGET_SATSENDRING to lagSanityBegrunnelse(
                apiNavn = Standardbegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                hjemler = listOf("10"),
            ),
        )

        Assertions.assertEquals(
            "barnetrygdloven §§ 4, 10 og 11 og folketrygdloven §§ 2-5 og 2-8",
            hentHjemmeltekst(
                minimerteVedtaksperioder = utvidetVedtaksperioderMedBegrunnelser.map {
                    it.tilMinimertVedtaksperiode(
                        sanityBegrunnelser = sanityBegrunnelser,
                        sanityEØSBegrunnelser = emptyMap(),
                    )
                },
                sanityBegrunnelser = sanityBegrunnelser,
                opplysningspliktHjemlerSkalMedIBrev = false,
                målform = Målform.NB,
                refusjonEøsHjemmelSkalMedIBrev = false,
            ),
        )
    }

    @Test
    fun `Skal gi riktig formattering ved hjemler fra barnetrygdloven og 2 EØS-forordninger`() {
        val utvidetVedtaksperioderMedBegrunnelser = listOf(
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET,
                    ),
                ),
                eøsBegrunnelser = listOf(
                    EØSBegrunnelse(
                        vedtaksperiodeMedBegrunnelser = mockk(),
                        begrunnelse = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR,
                    ),
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj()),
            ),
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_SATSENDRING,
                    ),
                ),
                eøsBegrunnelser = listOf(
                    EØSBegrunnelse(
                        vedtaksperiodeMedBegrunnelser = mockk(),
                        begrunnelse = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE,
                    ),
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj()),
            ),
        )

        val sanityBegrunnelser = mapOf(
            Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET to lagSanityBegrunnelse(
                apiNavn = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET.sanityApiNavn,
                hjemler = listOf("11", "4"),
            ),
            Standardbegrunnelse.INNVILGET_SATSENDRING to lagSanityBegrunnelse(
                apiNavn = Standardbegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                hjemler = listOf("10"),
            ),
        )

        val sanityEøsBegrunnelser = mapOf(
            EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR to lagSanityEøsBegrunnelse(
                apiNavn = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR.sanityApiNavn,
                hjemler = listOf("4"),
                hjemlerEØSForordningen883 = listOf("11-16"),
            ),
            EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE to lagSanityEøsBegrunnelse(
                apiNavn = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE.sanityApiNavn,
                hjemler = listOf("11"),
                hjemlerEØSForordningen987 = listOf("58", "60"),
            ),
        )

        Assertions.assertEquals(
            "barnetrygdloven §§ 4, 10 og 11, EØS-forordning 883/2004 artikkel 11-16 og EØS-forordning 987/2009 artikkel 58 og 60",
            hentHjemmeltekst(
                minimerteVedtaksperioder = utvidetVedtaksperioderMedBegrunnelser.map {
                    it.tilMinimertVedtaksperiode(
                        sanityBegrunnelser = sanityBegrunnelser,
                        sanityEØSBegrunnelser = sanityEøsBegrunnelser,
                    )
                },
                sanityBegrunnelser = sanityBegrunnelser,
                opplysningspliktHjemlerSkalMedIBrev = false,
                målform = Målform.NB,
                refusjonEøsHjemmelSkalMedIBrev = false,
            ),
        )
    }

    @Test
    fun `Skal gi riktig formattering ved hjemler fra Separasjonsavtale og to EØS-forordninger`() {
        val utvidetVedtaksperioderMedBegrunnelser = listOf(
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET,
                    ),
                ),
                eøsBegrunnelser = listOf(
                    EØSBegrunnelse(
                        vedtaksperiodeMedBegrunnelser = mockk(),
                        begrunnelse = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR,
                    ),
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj()),
            ),
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_SATSENDRING,
                    ),
                ),
                eøsBegrunnelser = listOf(
                    EØSBegrunnelse(
                        vedtaksperiodeMedBegrunnelser = mockk(),
                        begrunnelse = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE,
                    ),
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj()),
            ),
        )

        val sanityBegrunnelser = mapOf(
            Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET to lagSanityBegrunnelse(
                apiNavn = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET.sanityApiNavn,
                hjemler = listOf("11", "4"),
            ),
            Standardbegrunnelse.INNVILGET_SATSENDRING to lagSanityBegrunnelse(
                apiNavn = Standardbegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                hjemler = listOf("10"),
            ),
        )

        val sanityEøsBegrunnelser = mapOf(
            EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR to lagSanityEøsBegrunnelse(
                apiNavn = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR.sanityApiNavn,
                hjemler = listOf("4"),
                hjemlerEØSForordningen883 = listOf("11-16"),
                hjemlerSeperasjonsavtalenStorbritannina = listOf("29"),
            ),
            EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE to lagSanityEøsBegrunnelse(
                apiNavn = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE.sanityApiNavn,
                hjemler = listOf("11"),
                hjemlerEØSForordningen987 = listOf("58", "60"),
            ),
        )

        Assertions.assertEquals(
            "Separasjonsavtalen mellom Storbritannia og Norge artikkel 29, barnetrygdloven §§ 4, 10 og 11, EØS-forordning 883/2004 artikkel 11-16 og EØS-forordning 987/2009 artikkel 58 og 60",
            hentHjemmeltekst(
                minimerteVedtaksperioder = utvidetVedtaksperioderMedBegrunnelser.map {
                    it.tilMinimertVedtaksperiode(
                        sanityBegrunnelser = sanityBegrunnelser,
                        sanityEØSBegrunnelser = sanityEøsBegrunnelser,
                    )
                },
                sanityBegrunnelser = sanityBegrunnelser,
                opplysningspliktHjemlerSkalMedIBrev = false,
                målform = Målform.NB,
                refusjonEøsHjemmelSkalMedIBrev = false,
            ),
        )
    }

    @Test
    fun `Skal gi riktig formattering ved nynorsk og hjemler fra Separasjonsavtale og to EØS-forordninger`() {
        val utvidetVedtaksperioderMedBegrunnelser = listOf(
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET,
                    ),
                ),
                eøsBegrunnelser = listOf(
                    EØSBegrunnelse(
                        vedtaksperiodeMedBegrunnelser = mockk(),
                        begrunnelse = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR,
                    ),
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj()),
            ),
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_SATSENDRING,
                    ),
                ),
                eøsBegrunnelser = listOf(
                    EØSBegrunnelse(
                        vedtaksperiodeMedBegrunnelser = mockk(),
                        begrunnelse = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE,
                    ),
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj()),
            ),
        )

        val sanityBegrunnelser = mapOf(
            Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET to lagSanityBegrunnelse(
                apiNavn = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET.sanityApiNavn,
                hjemler = listOf("11", "4"),
            ),
            Standardbegrunnelse.INNVILGET_SATSENDRING to lagSanityBegrunnelse(
                apiNavn = Standardbegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                hjemler = listOf("10"),
            ),
        )

        val sanityEøsBegrunnelser = mapOf(
            EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR to lagSanityEøsBegrunnelse(
                apiNavn = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR.sanityApiNavn,
                hjemler = listOf("4"),
                hjemlerEØSForordningen883 = listOf("11-16"),
                hjemlerSeperasjonsavtalenStorbritannina = listOf("29"),
            ),
            EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE to lagSanityEøsBegrunnelse(
                apiNavn = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE.sanityApiNavn,
                hjemler = listOf("11"),
                hjemlerEØSForordningen987 = listOf("58", "60"),
            ),
        )

        Assertions.assertEquals(
            "Separasjonsavtalen mellom Storbritannia og Noreg artikkel 29, barnetrygdlova §§ 4, 10 og 11, EØS-forordning 883/2004 artikkel 11-16 og EØS-forordning 987/2009 artikkel 58 og 60",
            hentHjemmeltekst(
                minimerteVedtaksperioder = utvidetVedtaksperioderMedBegrunnelser.map {
                    it.tilMinimertVedtaksperiode(
                        sanityBegrunnelser = sanityBegrunnelser,
                        sanityEØSBegrunnelser = sanityEøsBegrunnelser,
                    )
                },
                sanityBegrunnelser = sanityBegrunnelser,
                opplysningspliktHjemlerSkalMedIBrev = false,
                målform = Målform.NN,
                refusjonEøsHjemmelSkalMedIBrev = false,
            ),
        )
    }

    @Test
    fun `Skal slå sammen hjemlene riktig når det er 3 eller flere hjemler på 'siste' hjemmeltype`() {
        val utvidetVedtaksperioderMedBegrunnelser = listOf(
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET,
                    ),
                ),
                eøsBegrunnelser = listOf(
                    EØSBegrunnelse(
                        vedtaksperiodeMedBegrunnelser = mockk(),
                        begrunnelse = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR,
                    ),
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj()),
            ),
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_SATSENDRING,
                    ),
                ),
                eøsBegrunnelser = listOf(
                    EØSBegrunnelse(
                        vedtaksperiodeMedBegrunnelser = mockk(),
                        begrunnelse = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE,
                    ),
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj()),
            ),
        )

        val sanityBegrunnelser = mapOf(
            Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET to lagSanityBegrunnelse(
                apiNavn = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET.sanityApiNavn,
                hjemler = listOf("11", "4"),
            ),
            Standardbegrunnelse.INNVILGET_SATSENDRING to lagSanityBegrunnelse(
                apiNavn = Standardbegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                hjemler = listOf("10"),
            ),
        )

        val sanityEøsBegrunnelser = mapOf(
            EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR to lagSanityEøsBegrunnelse(
                apiNavn = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR.sanityApiNavn,
                hjemler = listOf("4"),
                hjemlerEØSForordningen883 = listOf("2", "11-16", "67", "68"),
                hjemlerSeperasjonsavtalenStorbritannina = listOf("29"),
            ),
            EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE to lagSanityEøsBegrunnelse(
                apiNavn = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE.sanityApiNavn,
                hjemler = listOf("11"),
            ),
        )

        Assertions.assertEquals(
            "Separasjonsavtalen mellom Storbritannia og Noreg artikkel 29, barnetrygdlova §§ 4, 10 og 11 og EØS-forordning 883/2004 artikkel 2, 11-16, 67 og 68",
            hentHjemmeltekst(
                minimerteVedtaksperioder = utvidetVedtaksperioderMedBegrunnelser.map {
                    it.tilMinimertVedtaksperiode(
                        sanityBegrunnelser = sanityBegrunnelser,
                        sanityEØSBegrunnelser = sanityEøsBegrunnelser,
                    )
                },
                sanityBegrunnelser = sanityBegrunnelser,
                opplysningspliktHjemlerSkalMedIBrev = false,
                målform = Målform.NN,
                refusjonEøsHjemmelSkalMedIBrev = false,
            ),
        )
    }

    @Test
    fun `Skal kun ta med en hjemmel 1 gang hvis flere begrunnelser er knyttet til samme hjemmel`() {
        val utvidetVedtaksperioderMedBegrunnelser = listOf(
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET,
                    ),
                ),
                eøsBegrunnelser = listOf(
                    EØSBegrunnelse(
                        vedtaksperiodeMedBegrunnelser = mockk(),
                        begrunnelse = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR,
                    ),
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj()),
            ),
            lagUtvidetVedtaksperiodeMedBegrunnelser(
                begrunnelser = listOf(
                    lagVedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_SATSENDRING,
                    ),
                ),
                eøsBegrunnelser = listOf(
                    EØSBegrunnelse(
                        vedtaksperiodeMedBegrunnelser = mockk(),
                        begrunnelse = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE,
                    ),
                ),
                utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj()),
            ),
        )

        val sanityBegrunnelser = mapOf(
            Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET to lagSanityBegrunnelse(
                apiNavn = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET.sanityApiNavn,
                hjemler = listOf("11", "4"),
            ),
            Standardbegrunnelse.INNVILGET_SATSENDRING to lagSanityBegrunnelse(
                apiNavn = Standardbegrunnelse.INNVILGET_SATSENDRING.sanityApiNavn,
                hjemler = listOf("10"),
            ),
        )

        val sanityEøsBegrunnelser = mapOf(
            EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR to lagSanityEøsBegrunnelse(
                apiNavn = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR.sanityApiNavn,
                hjemler = listOf("4"),
                hjemlerEØSForordningen883 = listOf("2", "11-16", "67", "68"),
                hjemlerSeperasjonsavtalenStorbritannina = listOf("29"),
                hjemlerEØSForordningen987 = listOf("58"),
            ),
            EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE to lagSanityEøsBegrunnelse(
                apiNavn = EØSStandardbegrunnelse.INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE.sanityApiNavn,
                hjemler = listOf("11"),
                hjemlerEØSForordningen883 = listOf("2", "67", "68"),
                hjemlerSeperasjonsavtalenStorbritannina = listOf("29"),
                hjemlerEØSForordningen987 = listOf("58"),

            ),
        )

        Assertions.assertEquals(
            "Separasjonsavtalen mellom Storbritannia og Noreg artikkel 29, barnetrygdlova §§ 4, 10 og 11, EØS-forordning 883/2004 artikkel 2, 11-16, 67 og 68 og EØS-forordning 987/2009 artikkel 58",
            hentHjemmeltekst(
                minimerteVedtaksperioder = utvidetVedtaksperioderMedBegrunnelser.map {
                    it.tilMinimertVedtaksperiode(
                        sanityBegrunnelser = sanityBegrunnelser,
                        sanityEØSBegrunnelser = sanityEøsBegrunnelser,
                    )
                },
                sanityBegrunnelser = sanityBegrunnelser,
                opplysningspliktHjemlerSkalMedIBrev = false,
                målform = Målform.NN,
                refusjonEøsHjemmelSkalMedIBrev = false,
            ),
        )
    }

    @Test
    fun `Skal gi riktig dato for opphørstester`() {
        val sisteFom = LocalDate.now().minusMonths(2)

        val opphørsperioder = listOf(
            Opphørsperiode(
                periodeFom = LocalDate.now().minusYears(1),
                periodeTom = LocalDate.now().minusYears(1).plusMonths(2),
            ),
            Opphørsperiode(
                periodeFom = LocalDate.now().minusMonths(5),
                periodeTom = LocalDate.now().minusMonths(4),
            ),
            Opphørsperiode(
                periodeFom = sisteFom,
                periodeTom = LocalDate.now(),
            ),
        )

        Assertions.assertEquals(sisteFom.tilMånedÅr(), hentVirkningstidspunkt(opphørsperioder, 0L))
    }
}
