package no.nav.familie.ba.sak.config

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.common.guttenBarnesenFødselsdato
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.tilddMMyy
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonException
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlIdentRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.VergeResponse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.ForelderBarnRelasjonMaskert
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.IdentInformasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.VergeData
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.mockBarnAutomatiskBehandling
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.mockBarnAutomatiskBehandling2
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.mockBarnAutomatiskBehandling2Fnr
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.mockBarnAutomatiskBehandlingFnr
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.mockBarnAutomatiskBehandlingSkalFeile
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.mockBarnAutomatiskBehandlingSkalFeileFnr
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.mockSøkerAutomatiskBehandling
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.mockSøkerAutomatiskBehandlingAktør
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.mockSøkerAutomatiskBehandlingFnr
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.Personident
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import no.nav.familie.leader.LeaderClient
import no.nav.familie.unleash.UnleashService
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import java.lang.Integer.min
import java.time.LocalDate

@TestConfiguration
class ClientMocks {

    @Bean
    @Profile("mock-pdl")
    @Primary
    fun mockPersonopplysningerService(): PersonopplysningerService {
        val mockPersonopplysningerService = mockk<PersonopplysningerService>(relaxed = false)

        clearPdlMocks(mockPersonopplysningerService)

        return mockPersonopplysningerService
    }

    @Bean
    @Profile("mock-ident-client")
    @Primary
    fun mockPdlIdentRestClient(): PdlIdentRestClient {
        val mockPdlIdentRestClient = mockk<PdlIdentRestClient>(relaxed = false)

        clearPdlIdentRestClient(mockPdlIdentRestClient)

        return mockPdlIdentRestClient
    }

    @Bean
    @Primary
    @Profile("mock-pdl-test-søk")
    fun mockPDL(): PersonopplysningerService {
        val mockPersonopplysningerService = mockk<PersonopplysningerService>()

        val farId = "12345678910"
        val morId = "21345678910"
        val barnId = "31245678910"

        val farAktør = tilAktør(farId)
        val morAktør = tilAktør(morId)
        val barnAktør = tilAktør(barnId)

        every {
            mockPersonopplysningerService.hentPersoninfoEnkel(any())
        } returns personInfo.getValue(INTEGRASJONER_FNR)

        every {
            mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(farAktør)
        } returns PersonInfo(fødselsdato = LocalDate.of(1969, 5, 1), kjønn = Kjønn.MANN, navn = "Far Mocksen")

        every {
            mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(morAktør)
        } returns PersonInfo(fødselsdato = LocalDate.of(1979, 5, 1), kjønn = Kjønn.KVINNE, navn = "Mor Mocksen")

        every {
            mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(barnAktør)
        } returns PersonInfo(
            fødselsdato = LocalDate.of(2009, 5, 1),
            kjønn = Kjønn.MANN,
            navn = "Barn Mocksen",
            forelderBarnRelasjon = setOf(
                ForelderBarnRelasjon(
                    farAktør,
                    FORELDERBARNRELASJONROLLE.FAR,
                    "Far Mocksen",
                    LocalDate.of(1969, 5, 1),
                ),
                ForelderBarnRelasjon(
                    morAktør,
                    FORELDERBARNRELASJONROLLE.MOR,
                    "Mor Mocksen",
                    LocalDate.of(1979, 5, 1),
                ),
            ),
        )

        every {
            mockPersonopplysningerService.hentGjeldendeStatsborgerskap(any())
        } answers {
            Statsborgerskap(
                "NOR",
                LocalDate.of(1990, 1, 25),
                LocalDate.of(1990, 1, 25),
                null,
            )
        }

        every {
            mockPersonopplysningerService.hentGjeldendeOpphold(any())
        } answers {
            Opphold(
                type = OPPHOLDSTILLATELSE.PERMANENT,
                oppholdFra = LocalDate.of(1990, 1, 25),
                oppholdTil = LocalDate.of(2499, 1, 1),
            )
        }

        every {
            mockPersonopplysningerService.hentLandkodeAlpha2UtenlandskBostedsadresse(any())
        } returns "NO"

        val ukjentId = "43125678910"
        val ukjentAktør = tilAktør(ukjentId)

        every {
            mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(ukjentAktør)
        } throws HttpClientErrorException(HttpStatus.NOT_FOUND, "ikke funnet")

        val feilId = "41235678910"
        val feilIdAktør = tilAktør(feilId)

        every {
            mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(feilIdAktør)
        } throws IntegrasjonException("feil id")

        return mockPersonopplysningerService
    }

    @Bean
    @Primary
    fun mockEnvService(): EnvService {
        val mockEnvService = mockk<EnvService>(relaxed = true)

        every {
            mockEnvService.erProd()
        } answers {
            true
        }

        every {
            mockEnvService.erPreprod()
        } answers {
            true
        }

        every {
            mockEnvService.erDev()
        } answers {
            true
        }

        return mockEnvService
    }

    companion object {
        fun clearFeatureToggleMocks(
            mockFeatureToggleService: FeatureToggleService,
        ) {
            clearMocks(mockFeatureToggleService)

            val mockFeatureToggleServiceAnswer = System.getProperty("mockFeatureToggleAnswer")?.toBoolean() ?: true

            val featureSlot = slot<String>()
            every {
                mockFeatureToggleService.isEnabled(capture(featureSlot))
            } answers {
                System.getProperty(featureSlot.captured)?.toBoolean() ?: mockFeatureToggleServiceAnswer
            }
            every {
                mockFeatureToggleService.isEnabled(capture(featureSlot), any())
            } answers {
                System.getProperty(featureSlot.captured)?.toBoolean() ?: mockFeatureToggleServiceAnswer
            }
        }

        fun clearUnleashServiceMocks(mockUnleashService: UnleashService) {
            val mockUnleashServiceAnswer = System.getProperty("mockFeatureToggleAnswer")?.toBoolean() ?: true

            val featureSlot = slot<String>()
            every {
                mockUnleashService.isEnabled(toggleId = capture(featureSlot))
            } answers {
                System.getProperty(featureSlot.captured)?.toBoolean() ?: mockUnleashServiceAnswer
            }
            every {
                mockUnleashService.isEnabled(toggleId = capture(featureSlot), defaultValue = any())
            } answers {
                System.getProperty(featureSlot.captured)?.toBoolean() ?: mockUnleashServiceAnswer
            }

            every {
                mockUnleashService.isEnabled(toggleId = capture(featureSlot), properties = any())
            } answers {
                System.getProperty(featureSlot.captured)?.toBoolean() ?: mockUnleashServiceAnswer
            }
        }

        fun clearPdlIdentRestClient(
            mockPdlIdentRestClient: PdlIdentRestClient,
        ) {
            clearMocks(mockPdlIdentRestClient)

            val identSlot = slot<String>()
            every {
                mockPdlIdentRestClient.hentIdenter(capture(identSlot), true)
            } answers {
                listOf(
                    IdentInformasjon(identSlot.captured, false, "FOLKEREGISTERIDENT"),
                    IdentInformasjon(randomFnr(), true, "FOLKEREGISTERIDENT"),
                )
            }

            val identSlot2 = slot<String>()
            every {
                mockPdlIdentRestClient.hentIdenter(capture(identSlot2), false)
            } answers {
                listOf(
                    IdentInformasjon(
                        identSlot2.captured.substring(0, min(11, identSlot2.captured.length)),
                        false,
                        "FOLKEREGISTERIDENT",
                    ),
                    IdentInformasjon(
                        identSlot2.captured.substring(0, min(11, identSlot2.captured.length)) + "00",
                        false,
                        "AKTORID",
                    ),
                )
            }
        }

        fun clearPdlMocks(
            mockPersonopplysningerService: PersonopplysningerService,
        ) {
            clearMocks(mockPersonopplysningerService)

            every {
                mockPersonopplysningerService.hentGjeldendeStatsborgerskap(any())
            } answers {
                Statsborgerskap(
                    "NOR",
                    LocalDate.of(1990, 1, 25),
                    LocalDate.of(1990, 1, 25),
                    null,
                )
            }

            every {
                mockPersonopplysningerService.hentGjeldendeOpphold(any())
            } answers {
                Opphold(
                    type = OPPHOLDSTILLATELSE.PERMANENT,
                    oppholdFra = LocalDate.of(1990, 1, 25),
                    oppholdTil = LocalDate.of(2499, 1, 1),
                )
            }

            every {
                mockPersonopplysningerService.hentVergeData(any())
            } returns VergeData(false)

            every {
                mockPersonopplysningerService.harVerge(any())
            } returns VergeResponse(false)

            every {
                mockPersonopplysningerService.hentLandkodeAlpha2UtenlandskBostedsadresse(any())
            } returns "NO"

            val idSlotForHentPersoninfo = slot<Aktør>()
            every {
                mockPersonopplysningerService.hentPersoninfoEnkel(capture(idSlotForHentPersoninfo))
            } answers {
                when (val id = idSlotForHentPersoninfo.captured.aktivFødselsnummer()) {
                    barnFnr[0], barnFnr[1] -> personInfo.getValue(id)
                    søkerFnr[0], søkerFnr[1] -> personInfo.getValue(id)
                    "09121079074" -> personInfo.getValue(id)
                    "10031000033" -> personInfo.getValue(id)
                    "04068203010" -> personInfo.getValue(id)
                    else -> personInfo.getValue(INTEGRASJONER_FNR)
                }
            }

            val idSlotPersoninfoNavnOgAdresse = slot<Aktør>()
            every {
                mockPersonopplysningerService.hentPersoninfoNavnOgAdresse(capture(idSlotPersoninfoNavnOgAdresse))
            } answers {
                when (val id = idSlotPersoninfoNavnOgAdresse.captured.aktivFødselsnummer()) {
                    barnFnr[0], barnFnr[1] -> personInfo.getValue(id)
                    søkerFnr[0], søkerFnr[1] -> personInfo.getValue(id)
                    "09121079074" -> personInfo.getValue(id)
                    "10031000033" -> personInfo.getValue(id)
                    "04068203010" -> personInfo.getValue(id)
                    else -> personInfo.getValue(INTEGRASJONER_FNR)
                }
            }

            val idSlot = slot<Aktør>()
            every {
                mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(capture(idSlot))
            } answers {
                when (val id = idSlot.captured.aktivFødselsnummer()) {
                    "00000000000" -> throw HttpClientErrorException(
                        HttpStatus.NOT_FOUND,
                        "Fant ikke forespurte data på person.",
                    )

                    barnFnr[0], barnFnr[1], "09121079074", "10031000033", "04068203010" -> personInfo.getValue(id)

                    søkerFnr[0] -> personInfo.getValue(id).copy(
                        forelderBarnRelasjon = setOf(
                            ForelderBarnRelasjon(
                                aktør = tilAktør(barnFnr[0]),
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                                navn = personInfo.getValue(barnFnr[0]).navn,
                                fødselsdato = personInfo.getValue(barnFnr[0]).fødselsdato,
                            ),
                            ForelderBarnRelasjon(
                                aktør = tilAktør(barnFnr[1]),
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                                navn = personInfo.getValue(barnFnr[1]).navn,
                                fødselsdato = personInfo.getValue(barnFnr[1]).fødselsdato,
                            ),
                            ForelderBarnRelasjon(
                                aktør = tilAktør(søkerFnr[1]),
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.MEDMOR,
                            ),
                        ),
                    )

                    søkerFnr[1] -> personInfo.getValue(id).copy(
                        forelderBarnRelasjon = setOf(
                            ForelderBarnRelasjon(
                                aktør = tilAktør(barnFnr[0]),
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                                navn = personInfo.getValue(barnFnr[0]).navn,
                                fødselsdato = personInfo.getValue(barnFnr[0]).fødselsdato,
                            ),
                            ForelderBarnRelasjon(
                                aktør = tilAktør(barnFnr[1]),
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                                navn = personInfo.getValue(barnFnr[1]).navn,
                                fødselsdato = personInfo.getValue(barnFnr[1]).fødselsdato,
                            ),
                            ForelderBarnRelasjon(
                                aktør = tilAktør(søkerFnr[0]),
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.FAR,
                            ),
                        ),
                    )

                    søkerFnr[2] -> personInfo.getValue(id).copy(
                        forelderBarnRelasjon = setOf(
                            ForelderBarnRelasjon(
                                aktør = tilAktør(barnFnr[0]),
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                                navn = personInfo.getValue(barnFnr[0]).navn,
                                fødselsdato = personInfo.getValue(barnFnr[0]).fødselsdato,
                            ),
                            ForelderBarnRelasjon(
                                aktør = tilAktør(barnFnr[1]),
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                                navn = personInfo.getValue(barnFnr[1]).navn,
                                fødselsdato = personInfo.getValue(barnFnr[1]).fødselsdato,
                                adressebeskyttelseGradering = personInfo.getValue(barnFnr[1]).adressebeskyttelseGradering,
                            ),
                            ForelderBarnRelasjon(
                                aktør = tilAktør(søkerFnr[0]),
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.FAR,
                            ),
                        ),
                        forelderBarnRelasjonMaskert = setOf(
                            ForelderBarnRelasjonMaskert(
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                                adressebeskyttelseGradering = personInfo.getValue(
                                    BARN_DET_IKKE_GIS_TILGANG_TIL_FNR,
                                ).adressebeskyttelseGradering!!,
                            ),
                        ),
                    )

                    INTEGRASJONER_FNR -> personInfo.getValue(id).copy(
                        forelderBarnRelasjon = setOf(
                            ForelderBarnRelasjon(
                                aktør = tilAktør(barnFnr[0]),
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                                navn = personInfo.getValue(barnFnr[0]).navn,
                                fødselsdato = personInfo.getValue(barnFnr[0]).fødselsdato,
                            ),
                            ForelderBarnRelasjon(
                                aktør = tilAktør(barnFnr[1]),
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
                                navn = personInfo.getValue(barnFnr[1]).navn,
                                fødselsdato = personInfo.getValue(barnFnr[1]).fødselsdato,
                            ),
                            ForelderBarnRelasjon(
                                aktør = tilAktør(søkerFnr[1]),
                                relasjonsrolle = FORELDERBARNRELASJONROLLE.MEDMOR,
                            ),
                        ),
                    )

                    mockBarnAutomatiskBehandlingFnr -> personInfo.getValue(id)
                    mockBarnAutomatiskBehandling2Fnr -> personInfo.getValue(id)
                    mockSøkerAutomatiskBehandlingFnr -> personInfo.getValue(id)
                    mockBarnAutomatiskBehandlingSkalFeileFnr -> personInfo.getValue(id)
                    else -> personInfo.getValue(INTEGRASJONER_FNR)
                }
            }

            every {
                mockPersonopplysningerService.hentAdressebeskyttelseSomSystembruker(capture(idSlot))
            } answers {
                if (BARN_DET_IKKE_GIS_TILGANG_TIL_FNR == idSlot.captured.aktivFødselsnummer()) {
                    ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG
                } else {
                    ADRESSEBESKYTTELSEGRADERING.UGRADERT
                }
            }

            every { mockPersonopplysningerService.harVerge(mockSøkerAutomatiskBehandlingAktør) } returns VergeResponse(
                harVerge = false,
            )
        }

        val søkerFnr = arrayOf("12345678910", "11223344556", "12345678911")
        private val barnFødselsdatoer = arrayOf(
            guttenBarnesenFødselsdato,
            LocalDate.now().withDayOfMonth(18).minusYears(2),
        )
        val barnFnr = arrayOf(barnFødselsdatoer[0].tilddMMyy() + "50033", barnFødselsdatoer[1].tilddMMyy() + "50033")
        const val BARN_DET_IKKE_GIS_TILGANG_TIL_FNR = "12345678912"
        const val INTEGRASJONER_FNR = "10000111111"
        val bostedsadresse = Bostedsadresse(
            matrikkeladresse = Matrikkeladresse(
                matrikkelId = 123L,
                bruksenhetsnummer = "H301",
                tilleggsnavn = "navn",
                postnummer = "0202",
                kommunenummer = "2231",
            ),
        )
        private val bostedsadresseHistorikk = mutableListOf(
            Bostedsadresse(
                angittFlyttedato = LocalDate.now().minusDays(15),
                gyldigTilOgMed = null,
                matrikkeladresse = Matrikkeladresse(
                    matrikkelId = 123L,
                    bruksenhetsnummer = "H301",
                    tilleggsnavn = "navn",
                    postnummer = "0202",
                    kommunenummer = "2231",
                ),
            ),
            Bostedsadresse(
                angittFlyttedato = LocalDate.now().minusYears(1),
                gyldigTilOgMed = LocalDate.now().minusDays(16),
                matrikkeladresse = Matrikkeladresse(
                    matrikkelId = 123L,
                    bruksenhetsnummer = "H301",
                    tilleggsnavn = "navn",
                    postnummer = "0202",
                    kommunenummer = "2231",
                ),
            ),
        )

        private val sivilstandHistorisk = listOf(
            Sivilstand(type = SIVILSTAND.GIFT, gyldigFraOgMed = LocalDate.now().minusMonths(8)),
            Sivilstand(type = SIVILSTAND.SKILT, gyldigFraOgMed = LocalDate.now().minusMonths(4)),
        )

        val personInfo = mapOf(
            søkerFnr[0] to PersonInfo(
                fødselsdato = LocalDate.of(1990, 2, 19),
                kjønn = Kjønn.KVINNE,
                navn = "Mor Moresen",
                bostedsadresser = bostedsadresseHistorikk,
                sivilstander = sivilstandHistorisk,
                statsborgerskap = listOf(
                    Statsborgerskap(
                        land = "DNK",
                        bekreftelsesdato = LocalDate.now().minusYears(1),
                        gyldigFraOgMed = null,
                        gyldigTilOgMed = null,
                    ),
                ),
            ),
            søkerFnr[1] to PersonInfo(
                fødselsdato = LocalDate.of(1995, 2, 19),
                bostedsadresser = mutableListOf(),
                sivilstander = listOf(
                    Sivilstand(
                        type = SIVILSTAND.GIFT,
                        gyldigFraOgMed = LocalDate.now().minusMonths(8),
                    ),
                ),
                kjønn = Kjønn.MANN,
                navn = "Far Faresen",
            ),
            søkerFnr[2] to PersonInfo(
                fødselsdato = LocalDate.of(1985, 7, 10),
                bostedsadresser = mutableListOf(),
                sivilstander = listOf(
                    Sivilstand(
                        type = SIVILSTAND.GIFT,
                        gyldigFraOgMed = LocalDate.now().minusMonths(8),
                    ),
                ),
                kjønn = Kjønn.KVINNE,
                navn = "Moder Jord",
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
            ),
            barnFnr[0] to PersonInfo(
                fødselsdato = barnFødselsdatoer[0],
                bostedsadresser = mutableListOf(bostedsadresse),
                sivilstander = listOf(
                    Sivilstand(
                        type = SIVILSTAND.UOPPGITT,
                        gyldigFraOgMed = LocalDate.now().minusMonths(8),
                    ),
                ),
                kjønn = Kjønn.MANN,
                navn = "Gutten Barnesen",
            ),
            barnFnr[1] to PersonInfo(
                fødselsdato = barnFødselsdatoer[1],
                bostedsadresser = mutableListOf(bostedsadresse),
                sivilstander = listOf(
                    Sivilstand(
                        type = SIVILSTAND.GIFT,
                        gyldigFraOgMed = LocalDate.now().minusMonths(8),
                    ),
                ),
                kjønn = Kjønn.KVINNE,
                navn = "Jenta Barnesen",
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.FORTROLIG,
            ),
            mockBarnAutomatiskBehandlingFnr to mockBarnAutomatiskBehandling,
            mockBarnAutomatiskBehandling2Fnr to mockBarnAutomatiskBehandling2,
            mockSøkerAutomatiskBehandlingFnr to mockSøkerAutomatiskBehandling,
            mockBarnAutomatiskBehandlingSkalFeileFnr to mockBarnAutomatiskBehandlingSkalFeile,
            "09121079074" to PersonInfo(
                fødselsdato = LocalDate.of(2010, 12, 9),
                bostedsadresser = mutableListOf(bostedsadresse),
                sivilstander = listOf(Sivilstand(type = SIVILSTAND.UGIFT)),
                kjønn = Kjønn.KVINNE,
                navn = "Litt eldre barn",
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
            ),
            "10031000033" to PersonInfo(
                fødselsdato = LocalDate.of(2015, 2, 10),
                bostedsadresser = mutableListOf(bostedsadresse),
                sivilstander = listOf(Sivilstand(type = SIVILSTAND.UGIFT)),
                kjønn = Kjønn.KVINNE,
                navn = "Jenten 2015",
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
            ),
            "04068203010" to PersonInfo(
                fødselsdato = LocalDate.of(1982, 6, 4),
                bostedsadresser = mutableListOf(),
                sivilstander = listOf(Sivilstand(type = SIVILSTAND.UGIFT)),
                kjønn = Kjønn.KVINNE,
                navn = "Moder Jord",
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.UGRADERT,
            ),
            INTEGRASJONER_FNR to PersonInfo(
                fødselsdato = LocalDate.of(1965, 2, 19),
                bostedsadresser = mutableListOf(bostedsadresse),
                kjønn = Kjønn.KVINNE,
                navn = "Mor Integrasjon person",
                sivilstander = sivilstandHistorisk,
            ),
            BARN_DET_IKKE_GIS_TILGANG_TIL_FNR to PersonInfo(
                fødselsdato = LocalDate.of(2019, 6, 22),
                bostedsadresser = mutableListOf(bostedsadresse),
                sivilstander = listOf(
                    Sivilstand(
                        type = SIVILSTAND.UGIFT,
                        gyldigFraOgMed = LocalDate.now().minusMonths(8),
                    ),
                ),
                kjønn = Kjønn.KVINNE,
                navn = "Maskert Banditt",
                adressebeskyttelseGradering = ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG,
            ),
        )
    }

    @Bean
    @Profile("mock-leader-client")
    @Primary
    fun mockLeaderClient() {
        mockkStatic(LeaderClient::class)
        every { LeaderClient.isLeader() } returns true
    }
}

fun mockHentPersoninfoForMedIdenter(
    mockPersonopplysningerService: PersonopplysningerService,
    søkerFnr: String,
    barnFnr: String,
) {
    every {
        mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(eq(tilAktør(barnFnr)))
    } returns PersonInfo(
        fødselsdato = LocalDate.of(2018, 5, 1),
        kjønn = Kjønn.KVINNE,
        navn = "Barn Barnesen",
        sivilstander = listOf(Sivilstand(type = SIVILSTAND.GIFT, gyldigFraOgMed = LocalDate.now().minusMonths(8))),
    )

    every {
        mockPersonopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(eq(tilAktør(søkerFnr)))
    } returns PersonInfo(fødselsdato = LocalDate.of(1990, 2, 19), kjønn = Kjønn.KVINNE, navn = "Mor Moresen")
}

fun tilAktør(fnr: String, toSisteSiffrer: String = "00") = Aktør(fnr + toSisteSiffrer).also {
    it.personidenter.add(Personident(fnr, aktør = it))
}

val TEST_PDF = ClientMocks::class.java.getResource("/dokument/mockvedtak.pdf").readBytes()
