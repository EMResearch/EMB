package no.nav.familie.ba.sak.integrasjoner.pdl.domene

class PdlDødsfallResponse(val person: PdlDødsfallPerson?)
class PdlDødsfallPerson(val doedsfall: List<Doedsfall>)

class Doedsfall(val doedsdato: String?)
