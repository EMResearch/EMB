package no.nav.familie.tilbake.common

import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.config.PropertyName

fun Task.fagsystem(): String = this.metadata.getProperty(PropertyName.FAGSYSTEM, "UKJENT")
