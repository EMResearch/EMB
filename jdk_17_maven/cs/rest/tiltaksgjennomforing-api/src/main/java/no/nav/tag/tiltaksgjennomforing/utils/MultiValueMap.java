package no.nav.tag.tiltaksgjennomforing.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Enkel implementasjon av en multivaluemap som gjør det enkelt å legge
 * til flere verdier på en og samme nøkkel.
 * <p>
 * Erstatter en google commons-implementasjon.
 */
public class MultiValueMap<K, V> {

    private final HashMap<K, ArrayList<V>> multivalMap;

    public MultiValueMap() {
        this.multivalMap = new HashMap<>();
    }

    public void put(K key, V val) {
        if (multivalMap.containsKey(key)) {
            var multival = multivalMap.get(key);
            multival.add(val);
        } else {
            var newList = new ArrayList<V>();
            newList.add(val);
            multivalMap.put(key, newList);
        }
    }

    public Collection<V> get(K key) {
        return multivalMap.get(key);
    }

    public Map<K, Collection<V>> toMap() {
        return new HashMap<>(multivalMap);
    }

    public static <K, V> MultiValueMap<K, V> empty() {
        return new MultiValueMap<>();
    }
}
