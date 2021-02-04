package gcm.core.epi.plugin.vaccine.resourcebased;

import gcm.core.epi.propertytypes.FipsCode;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class VaccineDoseFipsContainer {

    private final Map<FipsCode, Long> dosesByFipsCode = new HashMap<>();
    private final Map<FipsCode, Optional<FipsCode>> fipsCodeNextHierarchyMap = new HashMap<>();

    private Optional<FipsCode> getNextFipsCodeInHierarchy(FipsCode fipsCode) {
        return fipsCodeNextHierarchyMap.computeIfAbsent(fipsCode, FipsCode::getNextFipsCodeInHierarchy);
    }

    public long getDosesAvailableTo(FipsCode fipsCode) {
        long doses = dosesByFipsCode.getOrDefault(fipsCode, 0L);
        Optional<FipsCode> nextFipsCodeInHierarchy = getNextFipsCodeInHierarchy(fipsCode);
        while(nextFipsCodeInHierarchy.isPresent()) {
            doses += dosesByFipsCode.getOrDefault(nextFipsCodeInHierarchy.get(), 0L);
            nextFipsCodeInHierarchy = getNextFipsCodeInHierarchy(nextFipsCodeInHierarchy.get());
        }
        return doses;
    }

    public void deliverDosesTo(FipsCode fipsCode, long doses) {
        long currentDoses = dosesByFipsCode.getOrDefault(fipsCode,0L);
        dosesByFipsCode.put(fipsCode, currentDoses + doses);
    }

    public void removeDoseFrom(FipsCode fipsCode) {
        long doses = dosesByFipsCode.getOrDefault(fipsCode, 0L);
        while (doses == 0L) {
            Optional<FipsCode> nextFipsCodeInHierarchy = getNextFipsCodeInHierarchy(fipsCode);
            doses = dosesByFipsCode.getOrDefault(nextFipsCodeInHierarchy.get(), 0L);
            fipsCode = nextFipsCodeInHierarchy.get();
        }
        if (doses == 0L) {
            throw new RuntimeException("No doses available in FIPS hierarchy");
        }
        dosesByFipsCode.put(fipsCode, dosesByFipsCode.get(fipsCode) - 1);
    }

}
