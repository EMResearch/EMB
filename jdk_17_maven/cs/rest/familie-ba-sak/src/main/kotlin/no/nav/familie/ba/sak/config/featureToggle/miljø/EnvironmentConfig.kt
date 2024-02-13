package no.nav.familie.ba.sak.config.featureToggle.miljø

import org.springframework.core.env.Environment

fun Environment.erAktiv(profil: Profil) = activeProfiles.any { it == profil.navn.trim() }
