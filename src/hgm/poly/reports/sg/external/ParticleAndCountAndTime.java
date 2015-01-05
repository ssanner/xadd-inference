package hgm.poly.reports.sg.external;

import java.util.Arrays;

public class ParticleAndCountAndTime {
    private Double[] particle;
    private int count; //since in MH each sample is repeated several times, we simply counting the repeated samples to compress data...
    private long ensembleSamplingTime; //time for taking 'count' samples.

    public ParticleAndCountAndTime(Double[] particle, int count, long ensembleSamplingTime) {
        this.particle = particle;
        this.count = count;
        if (count < 1) throw new RuntimeException();
        this.ensembleSamplingTime = ensembleSamplingTime;
    }

    public Double[] getParticle() {
        return particle;
    }

    public int getCount() {
        return count;
    }

    @Override
    public String toString() {
        return "ParticleAndCountAndTime{" +
                "particle=" + Arrays.toString(particle) +
                ", count=" + count +
                ", ensembleSamplingTime=" + ensembleSamplingTime +
                "}\n";
    }

    public void increaseCountAndTime(long extraTime) {
        count++;
        ensembleSamplingTime += extraTime;
    }

    public long getEnsembleSamplingTime() {
        return ensembleSamplingTime;
    }

    public Long getIndividualSamplingTime() {
        return ensembleSamplingTime/count;
    }

//    public void setEnsembleSamplingTime(long ensembleSamplingTime) {
//        this.ensembleSamplingTime = ensembleSamplingTime;
//    }
}
