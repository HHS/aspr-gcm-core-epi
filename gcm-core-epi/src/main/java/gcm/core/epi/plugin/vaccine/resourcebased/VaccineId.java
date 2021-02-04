package gcm.core.epi.plugin.vaccine.resourcebased;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.scenario.ResourceId;

import java.util.*;

@JsonDeserialize(as = VaccineId.class)
public class VaccineId implements ResourceId {

    private static final List<String> strings = new ArrayList<>();
    private static final Map<String, Integer> ordinalMap = new HashMap<>();
    private final String id;

    private VaccineId(String id) {
        this.id = id;
        if (!ordinalMap.containsKey(id)) {
            ordinalMap.put(id, strings.size());
            strings.add(id);
        }
    }

    public static VaccineId of(String id) {
        return new VaccineId(id);
    }

    public static int ordinal(String id) {
        return ordinalMap.get(id);
    }

    public int ordinal() {
        return ordinalMap.get(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VaccineId vaccineId = (VaccineId) o;
        return id.equals(vaccineId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
