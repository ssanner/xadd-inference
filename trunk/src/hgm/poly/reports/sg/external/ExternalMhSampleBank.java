package hgm.poly.reports.sg.external;

import hgm.asve.Pair;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 25/11/14
 * Time: 6:07 PM
 */
public class ExternalMhSampleBank implements Iterator<Pair<Double[], Long /*sample taking time*/>> {
    private String[] vars;

    private List<ParticleAndCountAndTime> particleAndCountList = new ArrayList<ParticleAndCountAndTime>();

    public ExternalMhSampleBank(String[] vars) {
        this.vars = vars;
    }

    public ExternalMhSampleBank() {
        this(null);
    }

    public void addNewParticle(Map<String, Double> sampleAssignment, long time) {
        // if vars are not set, take them from the first sample assignment and sort them in the alphabetic order:
        if (vars == null) {
            SortedSet<String> varsSet = new TreeSet<String>(sampleAssignment.keySet());
            vars = varsSet.toArray(new String[varsSet.size()]);
        }

        if (sampleAssignment.size() != vars.length)
            throw new RuntimeException("cannot add assignment: " + sampleAssignment + "\n to a bank with vars: " + Arrays.toString(vars));

        Double[] sample = new Double[vars.length];

        for (int i = 0; i < vars.length; i++) {
            String var = vars[i];
            Double value = sampleAssignment.get(var);
            if (value == null)
                throw new RuntimeException(var + " cannot be instantiated given: " + sampleAssignment);

            sample[i] = value;
        }

//        if (sampleAndCountList.isEmpty()) {
            particleAndCountList.add(new ParticleAndCountAndTime(sample, 1, time));
//        } else {
//            SampleAndCountAndTime lastSampleAndCount = sampleAndCountList.get(sampleAndCountList.size() - 1);
//            if (Arrays.equals(lastSampleAndCount.getSample(), sample)) {
//                lastSampleAndCount.increaseCounter();
//            } else {
//                sampleAndCountList.add(new SampleAndCountAndTime(sample, 1));
//            }
//        }

    }

    @Override
    public String toString() {
        return "ExternalMhSampler{" +
                "\n vars=" + Arrays.toString(vars) +
                ",\n sampleAndCountList=" + particleAndCountList +
                '}';
    }

    public void addInstanceOfPreviousParticle(long extraTime) {
        particleAndCountList.get(particleAndCountList.size()-1).increaseCountAndTime(extraTime);
    }



    int currentParticleIndex = 0;
//    SampleAndCountAndTime currentSampleAndCount = null;
    @Override
    public boolean hasNext() {
        return currentParticleIndex <this.particleAndCountList.size();
/*
//        currentSampleAndCount = this.sampleAndCountList.get(currentParticleIndex);
//        if (currentSampleAndCount.getCount())
        if (counterOfCurrentParticle == null) {
            counterOfCurrentParticle = this.particleAndCountList.get(currentParticleIndex).getCount();
        }
*/

    }

    Integer counterOfCurrentParticle = null;
    @Override
    public Pair<Double[], Long> next() {
        ParticleAndCountAndTime currentParticleAndTime = this.particleAndCountList.get(currentParticleIndex);
        if (counterOfCurrentParticle == null) {
            counterOfCurrentParticle = currentParticleAndTime.getCount();
        }

        Pair<Double[], Long> output = new Pair<Double[], Long>(currentParticleAndTime.getParticle(), currentParticleAndTime.getIndividualSamplingTime());

        counterOfCurrentParticle--;

        if (counterOfCurrentParticle==0) {
            currentParticleIndex++;
            counterOfCurrentParticle = null;
        }


        return output;
    }


    @Override
    public void remove() {
        throw new RuntimeException("not implemented");
    }
}

