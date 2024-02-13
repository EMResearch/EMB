package no.nav.familie.ba.sak.integrasjoner.pdl.domene

class PdlVergeResponse(val person: PdlVergePerson?)
class PdlVergePerson(val vergemaalEllerFremtidsfullmakt: List<VergemaalEllerFremtidsfullmakt>)

class VergemaalEllerFremtidsfullmakt(val type: String?)
