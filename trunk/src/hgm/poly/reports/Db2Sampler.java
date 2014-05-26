package hgm.poly.reports;

import hgm.BayesianDataGenerator;
import hgm.poly.sampling.SamplerInterface;

public interface Db2Sampler {
    String getName();

    SamplerInterface createSampler(BayesianDataGenerator db);
}
