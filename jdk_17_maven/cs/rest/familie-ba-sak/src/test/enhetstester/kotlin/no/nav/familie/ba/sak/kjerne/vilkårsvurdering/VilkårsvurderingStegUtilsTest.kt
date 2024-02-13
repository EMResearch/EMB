package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.common.toPeriode
import no.nav.familie.ba.sak.ekstern.restDomene.RestVilkårResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat.Companion.VilkårResultatComparator
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class VilkårsvurderingStegUtilsTest {

    lateinit var vilkårResultat1: VilkårResultat
    lateinit var vilkårResultat2: VilkårResultat
    lateinit var vilkårResultat3: VilkårResultat
    lateinit var vilkårsvurdering: Vilkårsvurdering
    lateinit var personResultat: PersonResultat
    lateinit var vilkår: Vilkår
    lateinit var resultat: Resultat
    lateinit var behandling: Behandling

    @BeforeEach
    fun init() {
        val personAktørId = randomAktør()

        behandling = lagBehandling()

        vilkår = Vilkår.BOR_MED_SØKER
        resultat = Resultat.OPPFYLT

        vilkårsvurdering = lagVilkårsvurdering(personAktørId, behandling, resultat)

        personResultat = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = personAktørId,
        )

        vilkårResultat1 = VilkårResultat(
            id = 1,
            personResultat = personResultat,
            vilkårType = vilkår,
            resultat = resultat,
            resultatBegrunnelse = null,
            periodeFom = LocalDate.of(2010, 1, 1),
            periodeTom = LocalDate.of(2010, 6, 1),
            begrunnelse = "",
            sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
        )
        vilkårResultat2 = VilkårResultat(
            id = 2,
            personResultat = personResultat,
            vilkårType = vilkår,
            resultat = resultat,
            resultatBegrunnelse = null,
            periodeFom = LocalDate.of(2010, 6, 2),
            periodeTom = LocalDate.of(2010, 8, 1),
            begrunnelse = "",
            sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
        )
        vilkårResultat3 = VilkårResultat(
            id = 3,
            personResultat = personResultat,
            vilkårType = vilkår,
            resultat = resultat,
            resultatBegrunnelse = null,
            periodeFom = LocalDate.of(2010, 8, 2),
            periodeTom = LocalDate.of(2010, 12, 1),
            begrunnelse = "",
            sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
        )
        personResultat.setSortedVilkårResultater(
            setOf(
                vilkårResultat1,
                vilkårResultat2,
                vilkårResultat3,
            ).toSortedSet(VilkårResultatComparator),
        )
    }

    private fun assertPeriode(expected: Periode, actual: Periode) {
        assertEquals(expected.fom, actual.fom)
        assertEquals(expected.tom, actual.tom)
    }

    @Test
    fun `periode erstattes dersom en periode med overlappende tidsintervall legges til`() {
        val restVilkårResultat = RestVilkårResultat(
            2, vilkår, resultat,
            LocalDate.of(2010, 6, 2), LocalDate.of(2011, 9, 1),
            "",
            "",
            LocalDateTime.now(),
            behandling.id,
        )
        VilkårsvurderingUtils.muterPersonVilkårResultaterPut(
            personResultat,
            restVilkårResultat,
        )

        assertEquals(2, personResultat.vilkårResultater.size)
        assertPeriode(
            Periode(
                LocalDate.of(2010, 1, 1),
                LocalDate.of(2010, 6, 1),
            ),
            personResultat.getSortedVilkårResultat(0)!!.toPeriode(),
        )

        assertPeriode(
            Periode(
                LocalDate.of(2010, 6, 2),
                LocalDate.of(2011, 9, 1),
            ),
            personResultat.getSortedVilkårResultat(1)!!.toPeriode(),
        )
    }

    @Test
    fun `periode splittes dersom en periode med inneklemt tidsintervall legges til`() {
        val restVilkårResultat = RestVilkårResultat(
            2, vilkår, resultat,
            LocalDate.of(2010, 3, 5), LocalDate.of(2010, 5, 20),
            "",
            "",
            LocalDateTime.now(),
            behandling.id,
        )

        VilkårsvurderingUtils.muterPersonVilkårResultaterPut(
            personResultat,
            restVilkårResultat,
        )

        assertEquals(4, personResultat.vilkårResultater.size)
        assertPeriode(
            Periode(
                LocalDate.of(2010, 1, 1),
                LocalDate.of(2010, 3, 4),
            ),
            personResultat.getSortedVilkårResultat(0)!!.toPeriode(),
        )
        assertPeriode(
            Periode(
                LocalDate.of(2010, 3, 5),
                LocalDate.of(2010, 5, 20),
            ),
            personResultat.getSortedVilkårResultat(1)!!.toPeriode(),
        )
        assertPeriode(
            Periode(
                LocalDate.of(2010, 5, 21),
                LocalDate.of(2010, 6, 1),
            ),
            personResultat.getSortedVilkårResultat(2)!!.toPeriode(),
        )
        assertPeriode(
            Periode(
                LocalDate.of(2010, 8, 2),
                LocalDate.of(2010, 12, 1),
            ),
            personResultat.getSortedVilkårResultat(3)!!.toPeriode(),
        )
    }

    @Test
    fun `fom-dato flyttes korrekt`() {
        val restVilkårResultat = RestVilkårResultat(
            2, vilkår, resultat,
            LocalDate.of(2010, 4, 2), LocalDate.of(2010, 8, 1),
            "",
            "",
            LocalDateTime.now(),
            behandling.id,
        )

        VilkårsvurderingUtils.muterPersonVilkårResultaterPut(
            personResultat,
            restVilkårResultat,
        )

        assertEquals(3, personResultat.vilkårResultater.size)
        assertPeriode(
            Periode(
                LocalDate.of(2010, 1, 1),
                LocalDate.of(2010, 4, 1),
            ),
            personResultat.getSortedVilkårResultat(0)!!.toPeriode(),
        )
        assertPeriode(
            Periode(
                LocalDate.of(2010, 4, 2),
                LocalDate.of(2010, 8, 1),
            ),
            personResultat.getSortedVilkårResultat(1)!!.toPeriode(),
        )
        assertPeriode(
            Periode(
                LocalDate.of(2010, 8, 2),
                LocalDate.of(2010, 12, 1),
            ),
            personResultat.getSortedVilkårResultat(2)!!.toPeriode(),
        )
    }

    @Test
    fun `tom-dato flyttes korrekt`() {
        val restVilkårResultat = RestVilkårResultat(
            2, vilkår, resultat,
            LocalDate.of(2010, 6, 2), LocalDate.of(2010, 9, 1),
            "",
            "",
            LocalDateTime.now(),
            behandling.id,
        )

        VilkårsvurderingUtils.muterPersonVilkårResultaterPut(
            personResultat,
            restVilkårResultat,
        )

        assertEquals(3, personResultat.vilkårResultater.size)
        assertPeriode(
            Periode(
                LocalDate.of(2010, 1, 1),
                LocalDate.of(2010, 6, 1),
            ),
            personResultat.getSortedVilkårResultat(0)!!.toPeriode(),
        )
        assertPeriode(
            Periode(
                LocalDate.of(2010, 6, 2),
                LocalDate.of(2010, 9, 1),
            ),
            personResultat.getSortedVilkårResultat(1)!!.toPeriode(),
        )
        assertPeriode(
            Periode(
                LocalDate.of(2010, 9, 2),
                LocalDate.of(2010, 12, 1),
            ),
            personResultat.getSortedVilkårResultat(2)!!.toPeriode(),
        )
    }

    @Test
    fun `Skal fjerne og ikke fylle inn tom periode i midten`() {
        VilkårsvurderingUtils.muterPersonResultatDelete(personResultat, 2)

        assertEquals(2, personResultat.vilkårResultater.size)
        assertPeriode(
            Periode(
                LocalDate.of(2010, 1, 1),
                LocalDate.of(2010, 6, 1),
            ),
            personResultat.getSortedVilkårResultat(0)!!.toPeriode(),
        )
        assertPeriode(
            Periode(
                LocalDate.of(2010, 8, 2),
                LocalDate.of(2010, 12, 1),
            ),
            personResultat.getSortedVilkårResultat(1)!!.toPeriode(),
        )
    }

    @Test
    fun `Skal fjerne første periode`() {
        VilkårsvurderingUtils.muterPersonResultatDelete(
            personResultat,
            1,
        )

        assertEquals(2, personResultat.vilkårResultater.size)
        assertPeriode(
            Periode(
                LocalDate.of(2010, 6, 2),
                LocalDate.of(2010, 8, 1),
            ),
            personResultat.getSortedVilkårResultat(0)!!.toPeriode(),
        )
        assertPeriode(
            Periode(
                LocalDate.of(2010, 8, 2),
                LocalDate.of(2010, 12, 1),
            ),
            personResultat.getSortedVilkårResultat(1)!!.toPeriode(),
        )
    }

    @Test
    fun `Skal fjerne siste periode`() {
        VilkårsvurderingUtils.muterPersonResultatDelete(
            personResultat,
            3,
        )

        assertEquals(2, personResultat.vilkårResultater.size)
        assertPeriode(
            Periode(
                LocalDate.of(2010, 1, 1),
                LocalDate.of(2010, 6, 1),
            ),
            personResultat.getSortedVilkårResultat(0)!!.toPeriode(),
        )
        assertPeriode(
            Periode(
                LocalDate.of(2010, 6, 2),
                LocalDate.of(2010, 8, 1),
            ),
            personResultat.getSortedVilkårResultat(1)!!.toPeriode(),
        )
    }

    @Test
    fun `Skal nullstille periode hvis det kun finnes en periode`() {
        val mockPersonResultat = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = randomAktør(),
        )

        val mockVilkårResultat = VilkårResultat(
            id = 1,
            personResultat = mockPersonResultat,
            vilkårType = vilkår,
            resultat = resultat,
            resultatBegrunnelse = null,
            periodeFom = LocalDate.of(2010, 1, 1),
            periodeTom = LocalDate.of(2010, 6, 1),
            begrunnelse = "",
            sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
        )
        mockPersonResultat.setSortedVilkårResultater(setOf(mockVilkårResultat))

        VilkårsvurderingUtils.muterPersonResultatDelete(
            mockPersonResultat,
            1,
        )

        assertEquals(1, mockPersonResultat.vilkårResultater.size)
        assertEquals(Resultat.IKKE_VURDERT, mockPersonResultat.getSortedVilkårResultat(0)!!.resultat)
    }

    @Test
    fun `Skal legge til periode`() {
        assertEquals(3, personResultat.vilkårResultater.size)
        VilkårsvurderingUtils.muterPersonResultatPost(personResultat, Vilkår.BOR_MED_SØKER)
        assertEquals(4, personResultat.vilkårResultater.size)
    }

    @Test
    fun `Skal kaste feil når det legges til periode i en vilkårtype der det allerede finnes en uvurdert periode`() {
        VilkårsvurderingUtils.muterPersonResultatPost(personResultat, Vilkår.BOR_MED_SØKER)
        assertThrows(FunksjonellFeil::class.java) {
            VilkårsvurderingUtils.muterPersonResultatPost(personResultat, Vilkår.BOR_MED_SØKER)
        }
    }

    @Test
    fun `Skal tilpasse vilkår for endret vilkår når begge mangler tom-dato`() {
        val vilkårResultat = VilkårResultat(
            personResultat = personResultat,
            vilkårType = vilkår,
            resultat = resultat,
            periodeFom = LocalDate.of(2020, 1, 1),
            begrunnelse = "",
            sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
        )
        val restVilkårResultat = RestVilkårResultat(
            id = 1,
            vilkårType = vilkår,
            resultat = resultat,
            periodeFom = LocalDate.of(2020, 6, 1),
            periodeTom = null,
            begrunnelse = "",
            endretAv = "",
            endretTidspunkt = LocalDateTime.now(),
            behandlingId = behandling.id,
        )

        VilkårsvurderingUtils.tilpassVilkårForEndretVilkår(personResultat, vilkårResultat, restVilkårResultat)

        assertEquals(LocalDate.of(2020, 1, 1), vilkårResultat.periodeFom)
        assertEquals(LocalDate.of(2020, 5, 31), vilkårResultat.periodeTom)
    }

    @Test
    fun `flyttResultaterTilInitielt filtrer ikke bort ikke oppfylte perioder når det gjelder samme behandling`() {
        val søkerAktørId = randomAktør()
        val behandling = lagBehandling()

        val initiellVilkårvurdering =
            lagVilkårsvurderingMedForskelligeResultat(søkerAktørId, behandling, listOf(Resultat.OPPFYLT))
        val aktivVilkårsvurdering =
            lagVilkårsvurderingMedForskelligeResultat(
                søkerAktørId,
                behandling,
                listOf(Resultat.IKKE_OPPFYLT, Resultat.OPPFYLT),
            )

        val (initiell, _) = VilkårsvurderingUtils.flyttResultaterTilInitielt(
            initiellVilkårsvurdering = initiellVilkårvurdering,
            aktivVilkårsvurdering = aktivVilkårsvurdering,
        )

        val opprettetBosattIRiket =
            initiell.personResultater.flatMap { it.vilkårResultater }.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }

        assertEquals(2, opprettetBosattIRiket.size)
        assertEquals(
            listOf(Resultat.IKKE_OPPFYLT, Resultat.OPPFYLT).sorted(),
            opprettetBosattIRiket.map { it.resultat }.sorted(),
        )
    }

    @Test
    fun `flyttResultaterTilInitielt filtrer ikke oppfylt om oppfylt finnes ved kopiering fra forrige behandling`() {
        val søkerAktørId = randomAktør()
        val behandling = lagBehandling()
        val behandling2 = lagBehandling()

        val initiellVilkårvurdering =
            lagVilkårsvurderingMedForskelligeResultat(søkerAktørId, behandling, listOf(Resultat.OPPFYLT))
        val aktivVilkårsvurdering =
            lagVilkårsvurderingMedForskelligeResultat(
                søkerAktørId,
                behandling2,
                listOf(Resultat.IKKE_OPPFYLT, Resultat.OPPFYLT),
            )

        val (initiell, _) = VilkårsvurderingUtils.flyttResultaterTilInitielt(
            initiellVilkårsvurdering = initiellVilkårvurdering,
            aktivVilkårsvurdering = aktivVilkårsvurdering,
        )

        val opprettetBosattIRiket =
            initiell.personResultater.flatMap { it.vilkårResultater }.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }

        assertEquals(1, opprettetBosattIRiket.size)
        assertEquals(Resultat.OPPFYLT, opprettetBosattIRiket.first().resultat)
    }

    @Test
    fun `flyttResultaterTilInitielt filtrer ikke ikke oppfylt om oppfylt ikke finnes`() {
        val søkerAktørId = randomAktør()
        val behandling = lagBehandling()

        val initiellVilkårsvurdering =
            lagVilkårsvurderingMedForskelligeResultat(søkerAktørId, behandling, listOf(Resultat.OPPFYLT))
        val activeVilkårvurdering =
            lagVilkårsvurderingMedForskelligeResultat(
                søkerAktørId,
                behandling,
                listOf(Resultat.IKKE_OPPFYLT, Resultat.IKKE_OPPFYLT),
            )

        val (initiell, _) = VilkårsvurderingUtils.flyttResultaterTilInitielt(
            initiellVilkårsvurdering = initiellVilkårsvurdering,
            aktivVilkårsvurdering = activeVilkårvurdering,
        )

        val opprettetBosattIRiket =
            initiell.personResultater.flatMap { it.vilkårResultater }.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }

        assertEquals(2, opprettetBosattIRiket.size)
        assertTrue(opprettetBosattIRiket.none { it.resultat == Resultat.OPPFYLT })
    }

    fun lagVilkårsvurderingMedForskelligeResultat(
        søkerAktør: Aktør,
        behandling: Behandling,
        resultater: List<Resultat>,
    ): Vilkårsvurdering {
        val vilkårsvurdering = Vilkårsvurdering(
            behandling = behandling,
        )
        var månedsteller = 0L
        val personResultat = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = søkerAktør,
        )
        personResultat.setSortedVilkårResultater(
            resultater.map {
                VilkårResultat(
                    personResultat = personResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = it,
                    periodeFom = LocalDate.now().plusMonths(månedsteller++),
                    periodeTom = LocalDate.now().plusMonths(månedsteller++),
                    begrunnelse = "",
                    sistEndretIBehandlingId = behandling.id,
                )
            }.toSet(),
        )
        vilkårsvurdering.personResultater = setOf(personResultat)
        return vilkårsvurdering
    }
}
