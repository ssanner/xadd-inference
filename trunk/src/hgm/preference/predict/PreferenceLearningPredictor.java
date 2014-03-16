package hgm.preference.predict;

import hgm.asve.Pair;
import hgm.preference.Choice;
import hgm.preference.db.PreferenceDatabase;

import java.util.List;
import java.util.Map;

/**
 * Created by Hadi Afshar.
 * Date: 2/02/14
 * Time: 3:33 AM
 */
public interface PreferenceLearningPredictor {
    /**
     *
     * @param trainingDatabase training database
     * @return an info list (most of the time) indicating the time spent in each internal phase
     */
    public Info learnToPredict(PreferenceDatabase trainingDatabase);

    //Items are often not included in the training database
    public Choice predictPreferenceChoice(Double[] item1, Double[] item2);

    public double probabilityOfFirstItemBeingPreferredOverSecond(Double[] item1, Double[] item2);
}
