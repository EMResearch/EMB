package no.nav.tag.tiltaksgjennomforing.avtale;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import no.nav.tag.tiltaksgjennomforing.utils.Utils;

import javax.persistence.*;
import java.util.UUID;

@Data
@Entity
@Accessors(chain = true)
@FieldNameConstants
public class Maal {
    @Id
    @GeneratedValue
    private UUID id;
    @Enumerated(EnumType.STRING)
    private MaalKategori kategori;
    private String beskrivelse;
    @ManyToOne
    @JoinColumn(name = "avtale_innhold")
    @JsonIgnore
    @ToString.Exclude
    private AvtaleInnhold avtaleInnhold;

    public Maal() {}

    public Maal(Maal fra) {
        id = UUID.randomUUID();
        kategori = fra.kategori;
        beskrivelse = fra.beskrivelse;
    }


    public void sjekkMaalLengde() {
        Utils.sjekkAtTekstIkkeOverskrider1000Tegn(this.getBeskrivelse(), "Maks lengde for mål er 1000 tegn");
    }
}
