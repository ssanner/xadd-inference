package hgm.asve.cnsrv.approxator.regression;

import hgm.asve.XaddPath;
import hgm.asve.cnsrv.approxator.regression.measures.DivergenceMeasure;
import xadd.XADD;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 14/11/13
 * Time: 12:00 AM
 */
public class AgglomerativeRegressionBasedXaddApproximator extends RegressionBasedXaddApproximator {
    private int maxNumberOfRegions;

    public AgglomerativeRegressionBasedXaddApproximator(XADD context,
                                                        DivergenceMeasure divergenceMeasure,
                                                        int maxPower,
                                                        int sampleNumPerContinuousVar,
                                                        double regularizationCoefficient,
                                                        int maxNumberOfRegions) {
        super(context, divergenceMeasure, maxPower, sampleNumPerContinuousVar, regularizationCoefficient);

        this.maxNumberOfRegions = maxNumberOfRegions;
    }

    public AgglomerativeRegressionBasedXaddApproximator(DivergenceMeasure divergenceMeasure,
                                                        int maxPower, int sampleNumPerContinuousVar,
                                                        double regularizationCoefficient,
                                                        int maxNumberOfRegions) {
        super(divergenceMeasure, maxPower, sampleNumPerContinuousVar, regularizationCoefficient);

        this.maxNumberOfRegions = maxNumberOfRegions;
    }

    @Override
    public XADD.XADDNode approximateXadd(XADD.XADDNode root) {
        //todo apparently substitution does not remove empty regions? 80 leaves, 51 regions!
//        System.out.println("context.getBranchCount(context._hmNode2Int.get(root)) = " + context.getBranchCount(context._hmNode2Int.get(root)));
//        System.out.println("context.getLeafCount(context._hmNode2Int.get(root)) = " + context.getLeafCount(context._hmNode2Int.get(root)));
        if (context.getLeafCount(context._hmNode2Int.get(root)) < maxNumberOfRegions) return root;

        RegionSamplingDataBase mappingFromRegionsToSamplesAndTargets =
                generatePathMappedToSamplesAndTargets(root, sampleNumPerContinuousVar);

        Set<XaddPath> regions = mappingFromRegionsToSamplesAndTargets.allRegions();
        if (regions.size() < maxNumberOfRegions) return root; //no region to merge...
        List<RegionGroup> groups = new ArrayList<RegionGroup>(regions.size());
        for (XaddPath region : regions) {
            groups.add(new RegionGroup(region, mappingFromRegionsToSamplesAndTargets.getSamplingInfo(region))); // initially each region group contains a single distinct region
        }

        int regionCount = regions.size();

//        System.out.println("._._._._regionCount = " + regionCount);

        List<InterGroupInfo> pairInfos = new ArrayList<InterGroupInfo>();
        //for each distinct couple of groups make pair-info:
        for (int i = 0; i < groups.size() - 1; i++) {
            for (int j = i + 1; j < groups.size(); j++) {
                RegionGroup group1 = groups.get(i);
                RegionGroup group2 = groups.get(j);
                pairInfos.add(makeInterGroupInfo(group1, group2));
            }
        }

//        System.out.println("pairInfos = " + pairInfos);

        while (regionCount > maxNumberOfRegions) {
            Collections.sort(pairInfos);
            InterGroupInfo pairToBeMerged = pairInfos.remove(0);
//            System.out.println("pairToBeMerged = " + pairToBeMerged + "\n");
            regionCount--; //since as a result of merging, number of distinct groups will be reduced by one
            RegionGroup firstGroupToBeMerged = pairToBeMerged.getGroup1();
            RegionGroup secondGroupToBeMerged = pairToBeMerged.getGroup2();
            RegionGroup resultOfMerging = new RegionGroup(firstGroupToBeMerged, secondGroupToBeMerged, pairToBeMerged.getPairApproximation());
//            System.out.println("resultOfMerging = " + resultOfMerging + "\n...\n" );
            //replace the union with any of the regions and update info
            for (int i = 0; i < pairInfos.size(); i++) {
                InterGroupInfo groupPair = pairInfos.get(i);
                if (groupPair.aMemberEqualsOneOf(firstGroupToBeMerged, secondGroupToBeMerged)) {
                    RegionGroup otherMember = groupPair.aMemberNotEqualToAnyOf(firstGroupToBeMerged, secondGroupToBeMerged);
//                    System.out.println("otherMember = " + otherMember);
                    pairInfos.set(i, makeInterGroupInfo(resultOfMerging, otherMember));
                }
            }
            // remove identical entries i.e. if A anb B are merged <A,C> and <B,C> both become <A&B, C>
            pairInfos = new ArrayList(new HashSet<InterGroupInfo>(pairInfos));
        }

        Set<RegionGroup> finalGroups = new HashSet<RegionGroup>(maxNumberOfRegions);
        for (InterGroupInfo pairInfo : pairInfos) {
            finalGroups.add(pairInfo.getGroup1());
            finalGroups.add(pairInfo.getGroup2());
        }

        Map<XaddPath, XADD.XADDTNode> regionNewLeafMap = restructureToMap(finalGroups);
        return super.substitute(root, regionNewLeafMap, true);
    }

    private Map<XaddPath, XADD.XADDTNode> restructureToMap(Set<RegionGroup> groups) {
        Map<XaddPath, XADD.XADDTNode> map = new HashMap<XaddPath, XADD.XADDTNode>();
        for (RegionGroup group : groups) {
            XADD.XADDTNode associatedLeaf = group.getAssociatedLeaf();
            for (XaddPath region : group.regions) {
                map.put(region, associatedLeaf);
            }
        }
        return map;
    }

    private InterGroupInfo makeInterGroupInfo(RegionGroup group1, RegionGroup group2) {
        HashSet<String> localVars = new HashSet<String>();

        group1.getAssociatedLeaf().collectVars(localVars);
        group2.getAssociatedLeaf().collectVars(localVars);

        SamplingDB db1 = group1.getSamples();
        SamplingDB db2 = group2.getSamples();
        SamplingDB unionRegionSamples = SamplingDB.unionOfSamplingDBs(db1, db2);

//                if (localVars.isEmpty()) {
//                    throw new RuntimeException("not handled yet.."); //todo weighted average should be returned..
//                }

        XADD.XADDTNode approx = super.approximateByRegression(localVars, unionRegionSamples, maxPower, regularizationCoefficient);
        double error = divergenceMeasure.calcDivergenceBetweenApproximatingNodeAndSamples(context, approx, unionRegionSamples);

        return new InterGroupInfo(group1, group2, approx, error);
    }


    class RegionGroup {
        private List<XaddPath> regions = new ArrayList<XaddPath>();
        /**
         * All members of the group are associated to this leaf
         */
        private XADD.XADDTNode leaf;

        /**
         * a data base containing samples of any region in the group
         */
        private SamplingDB samples;

        /**
         * a group with a single member
         */
        RegionGroup(XaddPath path, SamplingDB sampleDB) {
            regions.add(path);
            this.leaf = path.getLeaf();
            this.samples = sampleDB;
        }

        public RegionGroup(RegionGroup g1, RegionGroup g2, XADD.XADDTNode commonAssociatedLeaf) {
            regions.addAll(union(g1.regions, g2.regions));
            this.leaf = commonAssociatedLeaf;
            this.samples = SamplingDB.unionOfSamplingDBs(g1.getSamples(), g2.getSamples());
        }

        XADD.XADDTNode getAssociatedLeaf() {
            return leaf;
        }

        SamplingDB getSamples() {
            return samples;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("RegionGroup{" +
                    "regions hashes=");
            for (XaddPath r : regions) {
                sb.append(r.hashCode()).append(", ");
            }
            sb/*.append("  leaf=").append(leaf)*/.append(", #samples=").append(samples.size()).append("}");
            return sb.toString();
        }
    }

    public static List<XaddPath> union(List<XaddPath> paths1, List<XaddPath> paths2) {
        List<XaddPath> union = new ArrayList<XaddPath>(paths1);
        union.addAll(paths2);
        return union;
    }

    class InterGroupInfo implements Comparable<InterGroupInfo> {
        private RegionGroup group1;
        private RegionGroup group2;
        private XADD.XADDTNode pairApproximation;
        private Double error;

        InterGroupInfo(RegionGroup group1, RegionGroup group2, XADD.XADDTNode pairApproximation, Double error) {
            this.group1 = group1;
            this.group2 = group2;
            this.pairApproximation = pairApproximation;
            this.error = error;
        }

        RegionGroup getGroup1() {
            return group1;
        }

        RegionGroup getGroup2() {
            return group2;
        }

        XADD.XADDTNode getPairApproximation() {
            return pairApproximation;
        }

        Double getError() {
            return error;
        }

        @Override
        public int compareTo(InterGroupInfo that) {
            if (this.error < that.error) return -1;
            if (this.error > that.error) return 1;
            if (this.hashCode() < that.hashCode()) return -1;
            if (this.hashCode() > that.hashCode()) return 1;
            return 0;
        }

        /**
         * @return true only if any group of this container (no matter which one) is equal to a group of the other container so order does not matter.
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            InterGroupInfo that = (InterGroupInfo) o;

            return ((group1.equals(that.group1) && group2.equals(that.group2)) ||
                    (group2.equals(that.group1) && group1.equals(that.group2)));
        }

        //symmetric hash code
        @Override
        public int hashCode() {
            return (group1.hashCode() + group2.hashCode()) * 31;
        }

        public boolean aMemberEqualsOneOf(RegionGroup... groups) {
            for (RegionGroup g : groups) {
                if (group1.equals(g) || group2.equals(g)) return true;
            }
            return false;
        }

        public RegionGroup aMemberNotEqualToAnyOf(RegionGroup g1, RegionGroup g2) {
            if (!g1.equals(group1) && !g2.equals(group1)) return group1;
            if (!g1.equals(group2) && !g2.equals(group2)) return group2;

            throw new RuntimeException("I do not know what happened!: group1= " + group1 + "\n group2= " + group2 + "\n g1= " + g1 + "\n g2= " + g2);
//            return null;
        }

        @Override
        public String toString() {
            return "<" +
                    "group1=" + group1 +
                    ", \ngroup2=" + group2 +
                    ", \npairApproximation=" + pairApproximation +
                    ", error=" + error +
                    "}";
        }
    }
}
