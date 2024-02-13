package no.nav.familie.ba.sak.ekstern.skatteetaten

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPeriode
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPeriode.Delingsprosent
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPerioder
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPerioderRequest
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPerioderResponse
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPerson
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPersonerResponse
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/skatt")
@ProtectedWithClaims(issuer = "azuread")
class SkatteetatenController(
    private val skatteetatenService: SkatteetatenService,
    private val featureToggleService: FeatureToggleService,
) {

    @GetMapping(
        value = ["/personer"],
        produces = ["application/json;charset=UTF-8"],
    )
    fun finnPersonerMedUtvidetBarnetrygd(
        @NotNull
        @RequestParam(value = "aar", required = true)
        aar: String,
    ): ResponseEntity<Ressurs<SkatteetatenPersonerResponse>> {
        logger.info("Treff på finnPersonerMedUtvidetBarnetrygd")
        val respons = if (featureToggleService.isEnabled(FeatureToggleConfig.SKATTEETATEN_API_EKTE_DATA)) {
            skatteetatenService.finnPersonerMedUtvidetBarnetrygd(aar)
        } else {
            SkatteetatenPersonerResponse(
                listeMedTestdataPerioder().filter { it.sisteVedtakPaaIdent.year == aar.toInt() }
                    .map { SkatteetatenPerson(it.ident, it.sisteVedtakPaaIdent) },
            )
        }
        return ResponseEntity(Ressurs.success(respons), HttpStatus.valueOf(200))
    }

    @GetMapping(
        value = ["/personer/test"],
        produces = ["application/json;charset=UTF-8"],
    )
    fun finnPersonerMedUtvidetBarnetrygdTest(
        @NotNull
        @RequestParam(value = "aar", required = true)
        aar: String,
    ): ResponseEntity<Ressurs<SkatteetatenPersonerResponse>> {
        logger.info("Treff på finnPersonerMedUtvidetBarnetrygdTest")
        val respons = skatteetatenService.finnPersonerMedUtvidetBarnetrygd(aar)
        return ResponseEntity(Ressurs.success(respons), HttpStatus.valueOf(200))
    }

    @PostMapping(
        value = ["/perioder"],
        produces = ["application/json;charset=UTF-8"],
        consumes = ["application/json"],
    )
    fun hentPerioderMedUtvidetBarnetrygd(
        @Valid @RequestBody
        perioderRequest: SkatteetatenPerioderRequest,
    ): ResponseEntity<Ressurs<SkatteetatenPerioderResponse>> {
        logger.info("Treff på hentPerioderMedUtvidetBarnetrygd")
        val response = if (featureToggleService.isEnabled(FeatureToggleConfig.SKATTEETATEN_API_EKTE_DATA)) {
            skatteetatenService.finnPerioderMedUtvidetBarnetrygd(perioderRequest.identer, perioderRequest.aar)
        } else {
            SkatteetatenPerioderResponse(listeMedTestdataPerioder().filter { it.sisteVedtakPaaIdent.year == perioderRequest.aar.toInt() && it.ident in perioderRequest.identer })
        }
        return ResponseEntity(
            Ressurs.Companion.success(response),
            HttpStatus.valueOf(200),
        )
    }

    @PostMapping(
        value = ["/perioder/test"],
        produces = ["application/json;charset=UTF-8"],
        consumes = ["application/json"],
    )
    fun hentPerioderMedUtvidetBarnetrygdForMidlertidigTest(
        @Valid @RequestBody
        perioderRequest: SkatteetatenPerioderRequest,
    ): ResponseEntity<Ressurs<SkatteetatenPerioderResponse>> {
        logger.info("Treff på hentPerioderMedUtvidetBarnetrygdForMidlertidigTest")
        val response =
            skatteetatenService.finnPerioderMedUtvidetBarnetrygd(perioderRequest.identer, perioderRequest.aar)

        return ResponseEntity(
            Ressurs.Companion.success(response),
            HttpStatus.valueOf(200),
        )
    }

    private fun listeMedTestdataPerioder(): List<SkatteetatenPerioder> {
        val fraMaaned = "2021-02"
        return listOf(
            SkatteetatenPerioder(
                "01838398495",
                LocalDateTime.of(2021, 1, 1, 0, 0),
                perioder = listOf(
                    SkatteetatenPeriode(fraMaaned, Delingsprosent._50, tomMaaned = "2022-12"),

                ),
            ),

            SkatteetatenPerioder(
                "09919094319",
                LocalDateTime.of(2021, 1, 1, 0, 0),
                perioder = listOf(
                    SkatteetatenPeriode(fraMaaned, Delingsprosent._0, tomMaaned = "2024-12"),

                ),
            ),

            SkatteetatenPerioder(
                "15830699233",
                LocalDateTime.of(2021, 1, 1, 0, 0),
                perioder = listOf(
                    SkatteetatenPeriode(fraMaaned, Delingsprosent.usikker, tomMaaned = "2024-12"),

                ),
            ),

            SkatteetatenPerioder(
                "01828499633",
                LocalDateTime.of(2021, 2, 1, 0, 0),
                perioder = listOf(
                    SkatteetatenPeriode(fraMaaned, Delingsprosent._50, tomMaaned = null),

                ),
            ),

            SkatteetatenPerioder(
                "27903249671",
                LocalDateTime.of(2021, 1, 1, 0, 0),
                perioder = listOf(
                    SkatteetatenPeriode("2021-01", Delingsprosent._50, tomMaaned = "2021-03"),
                    SkatteetatenPeriode("2021-04", Delingsprosent._0, tomMaaned = "2021-08"),
                    SkatteetatenPeriode("2021-09", Delingsprosent.usikker, tomMaaned = null),
                ),
            ),

            SkatteetatenPerioder(
                "24835498561",
                LocalDateTime.of(2020, 1, 3, 0, 0),
                perioder = listOf(
                    SkatteetatenPeriode("2020-01", Delingsprosent._50, tomMaaned = "2020-12"),

                ),
            ),

            SkatteetatenPerioder(
                "02889197172",
                LocalDateTime.of(2019, 2, 1, 0, 0),
                perioder = listOf(
                    SkatteetatenPeriode("2019-02", Delingsprosent._0, tomMaaned = "2019-09"),

                ),
            ),
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SkatteetatenController::class.java)
    }
}
