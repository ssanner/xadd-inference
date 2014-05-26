package hgm.preference.predict;

import hgm.preference.Choice;
import hgm.preference.Preference;
import hgm.preference.db.PreferenceDatabase;
import tskill.jskills.GameInfo;
import tskill.jskills.SkillCalculator;
import tskill.jskills.trueskill.FactorGraphTrueSkillCalculator;
import tskill.ranking.TrueSkillModelVersion2;

/**
 * Created by Hadi Afshar.
 * Date: 2/02/14
 * Time: 6:39 AM
 */
public class TrueSkillPrefLearningPredictor implements PreferenceLearningPredictor {
    private TrueSkillModelVersion2 model;
    private double epsilon;

    public TrueSkillPrefLearningPredictor(double epsilon) {
        this.epsilon = epsilon;
    }

    @Override
    public Info learnToPredict(PreferenceDatabase trainingDatabase) {
        SkillCalculator calculator = new
                FactorGraphTrueSkillCalculator();
//                TwoTeamTrueSkillCalculator();

        GameInfo gameInfo = GameInfo.getDefaultGameInfo();  //todo what are good parameters?

        long time1 = System.currentTimeMillis();
        model = new TrueSkillModelVersion2(gameInfo, calculator, trainingDatabase.getNumberOfParameters());
        for (Preference preference : trainingDatabase.getObservedDataPoints()) {
            model.updateByMatch(trainingDatabase.getItemAttributeValues(preference.getItemId1()),
                    trainingDatabase.getItemAttributeValues(preference.getItemId2()), preference.getPreferenceChoice());

        }

        long time2 = System.currentTimeMillis();
        Info info = new Info();
        info.add("Elapsed Time:", (double) (time2 - time1));
        return info;
    }

    @Override
    public Choice predictPreferenceChoice(Double[] item1, Double[] item2) {
        //each item is a team and each attribute is a player

        Integer dim = item1.length;
        double util1 = 0d;
        double util2 = 0d;
        for (int attribId = 0; attribId < dim; attribId++) {
            util1 += item1[attribId] * model.getRating(attribId).getMean(); //todo it is OK?
            util2 += item2[attribId] * model.getRating(attribId).getMean(); //todo it is OK?
        }

        if (util1 - util2 > epsilon) return Choice.FIRST;
        if (util2 - util1 > epsilon) return Choice.SECOND;
        return Choice.EQUAL;
    }

    @Override
    public double probabilityOfFirstItemBeingPreferredOverSecond(Double[] item1, Double[] item2) {
        //each item is a team and each attribute is a player
        Integer dim = item1.length;
        double util1 = 0d;
        double util2 = 0d;
        for (int attribId = 0; attribId < dim; attribId++) {
            util1 += item1[attribId] * model.getRating(attribId).getMean(); //todo it is OK?
            util2 += item2[attribId] * model.getRating(attribId).getMean(); //todo it is OK?
        }

        if (util1 == 0 && util2 == 0) {
            System.err.println("both utils 0! in true.skill");
            return  0.5;
        }

        return util1/(util1 + util2);
    }
}
