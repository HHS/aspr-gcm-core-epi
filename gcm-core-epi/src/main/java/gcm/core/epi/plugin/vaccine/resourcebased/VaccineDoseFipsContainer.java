package gcm.core.epi.plugin.vaccine.resourcebased;

import gcm.core.epi.propertytypes.FipsCode;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class VaccineDoseFipsContainer {

    private final Map<FipsCode, Long> dosesByFipsCode = new HashMap<>();
    private final Map<FipsCode, Long> doseReserve = new HashMap<>();
    private final Map<FipsCode, Optional<FipsCode>> fipsCodeNextHierarchyMap = new HashMap<>();

    private Optional<FipsCode> getNextFipsCodeInHierarchy(FipsCode fipsCode) {
        return fipsCodeNextHierarchyMap.computeIfAbsent(fipsCode, FipsCode::getNextFipsCodeInHierarchy);
    }

    public long getDosesAvailableTo(FipsCode fipsCode) {
        long doses = dosesByFipsCode.getOrDefault(fipsCode, 0L) +
                doseReserve.getOrDefault(fipsCode, 0L);
        Optional<FipsCode> nextFipsCodeInHierarchy = getNextFipsCodeInHierarchy(fipsCode);
        while(nextFipsCodeInHierarchy.isPresent()) {
            doses += dosesByFipsCode.getOrDefault(nextFipsCodeInHierarchy.get(), 0L);
            nextFipsCodeInHierarchy = getNextFipsCodeInHierarchy(nextFipsCodeInHierarchy.get());
        }
        return doses;
    }

    public long getReservedDosesFor(FipsCode fipsCode) {
        return doseReserve.getOrDefault(fipsCode, 0L);
    }

    public void deliverDosesTo(FipsCode fipsCode, long doses) {
        long currentDoses = dosesByFipsCode.getOrDefault(fipsCode,0L);
        dosesByFipsCode.put(fipsCode, currentDoses + doses);
    }

    private Optional<FipsCode> getHierarchyFipsCodeWithDose(FipsCode fipsCode) {
        long doses = dosesByFipsCode.getOrDefault(fipsCode, 0L);
        while (doses == 0L) {
            Optional<FipsCode> nextFipsCodeInHierarchy = getNextFipsCodeInHierarchy(fipsCode);
            if (!nextFipsCodeInHierarchy.isPresent()) {
                break;
            }
            fipsCode = nextFipsCodeInHierarchy.get();
            doses = dosesByFipsCode.getOrDefault(nextFipsCodeInHierarchy.get(), 0L);
        }
        if (doses == 0L) {
            return Optional.empty();
        } else {
            return Optional.of(fipsCode);
        }
    }

    public void reserveDoseIfPossible(FipsCode fipsCode) {
        Optional<FipsCode> fipsCodeWithDose = getHierarchyFipsCodeWithDose(fipsCode);
        if (fipsCodeWithDose.isPresent()) {
            dosesByFipsCode.put(fipsCodeWithDose.get(), dosesByFipsCode.get(fipsCodeWithDose.get()) - 1);
            doseReserve.put(fipsCode, doseReserve.getOrDefault(fipsCode, 0L) + 1);
        }
    }

    public void removeDoseFrom(FipsCode fipsCode, boolean useReserve) {
        if (useReserve && doseReserve.getOrDefault(fipsCode, 0L) > 0L) {
            // Doses are reserved at a given FipsCode level only
            doseReserve.put(fipsCode, doseReserve.get(fipsCode) - 1);
        } else {
            Optional<FipsCode> fipsCodeWithDose = getHierarchyFipsCodeWithDose(fipsCode);
            if (fipsCodeWithDose.isPresent()) {
                dosesByFipsCode.put(fipsCodeWithDose.get(), dosesByFipsCode.get(fipsCodeWithDose.get()) - 1);
            } else {
                throw new RuntimeException("No doses available in FIPS hierarchy");
            }
        }
    }

}
