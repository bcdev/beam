package org.esa.beam.dataio.netcdf.metadata.profiles.cf;

import java.util.HashMap;
import java.util.Map;

/**
 * Class responsible for mapping unit strings to CF compliant unit strings.
 *
 * @author Marco Peters
 */
class CfCompliantUnitMapper {

    private static Map<String, String> unitMap = new HashMap<String, String>();

    static {
        unitMap.put("deg", "degree");
    }

    /**
     * Tries to find a CF compliant unit string for the given one. If none is found the original unit string is returned.
     *
     *
     * @param unit The unit string to find a CF compliant unit string for.
     *
     * @return A CF compliant unit string. If none is found the original unit string is returned.
     */
    public static String tryFindUnitString(String unit) {
        if (unitMap.containsKey(unit)) {
            return unitMap.get(unit);
        }
        return unit;
    }
}
