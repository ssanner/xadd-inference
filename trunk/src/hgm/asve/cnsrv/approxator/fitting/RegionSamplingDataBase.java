package hgm.asve.cnsrv.approxator.fitting;

import hgm.asve.Pair;
import hgm.asve.XaddPath;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 27/10/13
 * Time: 6:05 PM
 */
// Map<XaddPath, Pair<List<Map<String, Double>>, List<Double>>>
public class RegionSamplingDataBase {
    private Map<XaddPath, SamplingDB> regionInfoMap = new HashMap<XaddPath, SamplingDB>();// db Pair<List<Map<String, Double>>, List<Double>>>

    public void addSamplingInfo(XaddPath activatedRegion, HashMap<String, Double> continuousAssignment, Double target) {
        SamplingDB regionInfoDB = regionInfoMap.get(activatedRegion);
        if (regionInfoDB == null) {
            regionInfoDB = new SamplingDB();
            regionInfoMap.put(activatedRegion, regionInfoDB);
        }

        regionInfoDB.addSamplingInfo(continuousAssignment, target);
    }

    public int size() {
        return regionInfoMap.size();
    }

    public Set<Map.Entry<XaddPath, SamplingDB>> entrySet() {
        return regionInfoMap.entrySet();
    }

    public Collection<SamplingDB> allSamplingDBs() {
        return regionInfoMap.values();
    }

    public Set<XaddPath> allRegions() {
        return regionInfoMap.keySet();
    }

    /**
     * @param path a (not necessarily complete) path
     * @return the sampling info of any path that starts with the input path
     */
    public SamplingDB getAccumulatedSamplingInfo(XaddPath path) {
        SamplingDB totalSamplingDB = new SamplingDB();
        Set<XaddPath> activatedPaths = getAllPathsStartingWith(path);
//        System.out.println("All paths starting with: " + path + "\t are the following " + activatedPaths.size() +  " paths: " + activatedPaths);
        for (XaddPath activatedPath : activatedPaths) {
            totalSamplingDB.addAllSamplingInfo(regionInfoMap.get(activatedPath));
        }
        return totalSamplingDB;
    }

    private Set<XaddPath> getAllPathsStartingWith(XaddPath path) {
        Set<XaddPath> results = new HashSet<XaddPath>();

        for (XaddPath region : regionInfoMap.keySet()) {
            if (region.startsWith(path)) {
                results.add(region);
            }
        }

        return results;
    }
}
