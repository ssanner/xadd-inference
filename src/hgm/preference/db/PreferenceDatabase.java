package hgm.preference.db;

import hgm.BayesianDataGenerator;
import hgm.poly.bayesian.PriorHandler;
import hgm.preference.Preference;

/**
 * Created by Hadi Afshar. Date: 19/12/13 Time: 8:33 PM
 */
public abstract class PreferenceDatabase extends BayesianDataGenerator<Preference> {
    protected PreferenceDatabase(PriorHandler prior) {
        super(prior);
    }  //todo should implement ModelDatabase<Preference>

	public abstract int getNumberOfItems();

	public abstract Double[] getItemAttributeValues(int itemId);

	/**
	 * 
	 * @return auxiliary weigh weight vector is a weight vector representing a point which is satisfied by (almost) all constraint therefore can be
	 *         used as the initial weight sample (for the posterior). If this method is not implemented NULL should be returned.
	 */
	public abstract double[] getAuxiliaryWeightVector();

}
