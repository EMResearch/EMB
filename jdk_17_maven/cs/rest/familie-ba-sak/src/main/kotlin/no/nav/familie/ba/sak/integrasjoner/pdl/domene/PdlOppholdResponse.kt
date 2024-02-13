package no.nav.familie.ba.sak.integrasjoner.pdl.domene

import no.nav.familie.kontrakter.felles.personopplysning.Opphold

class PdlOppholdResponse(val person: PdlOppholdPerson?)
class PdlOppholdPerson(val opphold: List<Opphold>?)
