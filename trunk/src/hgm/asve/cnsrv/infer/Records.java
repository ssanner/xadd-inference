package hgm.asve.cnsrv.infer;

import hgm.asve.cnsrv.FactorSet;
import hgm.asve.cnsrv.factor.Factor;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 7/10/13
 * Time: 9:18 PM
 */
public class Records {
    public String title;
    public List<String> desiredVariableEliminationOrder = new ArrayList<String>();
    public List<String> variablesActuallyMarginalized = new ArrayList<String>();
    public Map<String, String> featureValueMap = new HashMap<String, String>();
    public List<String> recordedFactorSets = new ArrayList<String>();

    public int maxFactorNodeCount = 0;
    public int maxFactorLeafCount = 0;
    public int maxFactorBranchCount = 0;
    private String finalResultRecordString;

    public Records(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("Records of: '" + title + "' \n" +
                "desired.variable-elimination.order = " + desiredVariableEliminationOrder +
                "\nvariables.actually.marginalized = " + variablesActuallyMarginalized + "\n");

        for (Map.Entry<String, String> entry : featureValueMap.entrySet()) {
            str.append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
        }


        for (String record : recordedFactorSets) {
            str.append(record);
        }

        str.append("\n").append(title).append(".#maxFactorNode = ").append(maxFactorNodeCount);
        str.append("\n").append(title).append(".#maxFactorLeaf = ").append(maxFactorLeafCount);
        str.append("\n").append(title).append(".#maxFactorBranchCount = ").append(maxFactorBranchCount);

        return str.toString();
    }

    public void set(String key, String value) {
        if (featureValueMap.containsKey(key)) throw new RuntimeException("Key: " + key + " already exists in the map");
        featureValueMap.put(key, value);
    }

    public String factorSetRecordStr(FactorSet factorSet, boolean recordCounts) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Factor f : factorSet) {
            int nodeCount = f.getNodeCount();
            int leafCount = f.getLeafCount();
            int branchCount = f.getBranchCount();
            stringBuilder.append("\t[Factor: ").append(f.getHelpingText()).append(
                    "][#Node: ").append(nodeCount).append(
                    "][#Leaf: ").append(leafCount).append(
                    "][#path: ").append(branchCount).append("]\n");

            if (recordCounts) {
                maxFactorBranchCount = Math.max(maxFactorBranchCount, branchCount);
                maxFactorLeafCount = Math.max(maxFactorLeafCount, leafCount);
                maxFactorNodeCount = Math.max(maxFactorNodeCount, nodeCount);
            }

        }

        if (factorSet.size() > 1) {
            stringBuilder.append(" [#Estim. joint path = ").append(factorSet.predictedJointPathCount()).append("]");
        }
        return stringBuilder.toString();
    }

    public void recordFactorSetApproximation(String preApproximation, String postApproximationInCaseAny) {
        if (postApproximationInCaseAny == null) {
            recordedFactorSets.add(preApproximation);
        } else {
            recordedFactorSets.add("\t PreApprox: " + preApproximation + " --> PostApprox: " + postApproximationInCaseAny);
        }
    }

    public void recordFactor(Factor nonApproximatedFactor) {
        String rec = factorSetRecordStr(new FactorSet(Arrays.asList(nonApproximatedFactor)), true);
        recordFactorSetApproximation(rec, null);
    }

    public void recordFinalResult(Factor finalResult) {
        finalResultRecordString = factorSetRecordStr(new FactorSet(Arrays.asList(finalResult)), true);
        recordFactorSetApproximation(finalResultRecordString, null);
    }

    public String getFinalResultRecord() {
        return finalResultRecordString;
    }
}
