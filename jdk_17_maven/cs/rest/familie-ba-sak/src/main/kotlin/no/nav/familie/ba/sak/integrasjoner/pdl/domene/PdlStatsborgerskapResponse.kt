package no.nav.familie.ba.sak.integrasjoner.pdl.domene

import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap

class PdlStatsborgerskapResponse(val person: PdlStatsborgerskapPerson?)
class PdlStatsborgerskapPerson(val statsborgerskap: List<Statsborgerskap>)
