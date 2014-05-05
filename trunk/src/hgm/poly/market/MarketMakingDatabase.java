package hgm.poly.market;

import hgm.ModelDatabase;

import java.util.List;

/**
 * Created by Hadi Afshar. Date: 19/12/13 Time: 8:33 PM
 */
public interface MarketMakingDatabase extends ModelDatabase {

	int getInstrumentTypeCount(); //determines the dimension of value vector... (like getNumberOfAttributes() in BPPL)

//	int getNumberOfItems();

	List<TradingResponse> getTradingResponses();

//	Double[] getItemAttributeValues(int itemId);

	/**
	 * 
	 * @return auxiliary weigh weight vector is a weight vector representing a point which is satisfied by (almost) all constraint therefore can be
	 *         used as the initial weight sample (for the posterior). If this method is not implemented NULL should be returned.
	 */
//	double[] getAuxiliaryWeightVector();

}
