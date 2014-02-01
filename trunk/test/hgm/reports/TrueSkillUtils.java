package hgm.reports;

import hgm.preference.Choice;
import hgm.preference.Preference;
import hgm.preference.db.PreferenceDatabase;
import tskill.jskills.*;
import tskill.ranking.AttribChoicePlayer;
import tskill.ranking.TrueSkillModelVersion1;

import java.util.Collection;
import java.util.Map;

/**
 * Created by Hadi Afshar.
 * Date: 30/01/14
 * Time: 2:28 AM
 */
public class TrueSkillUtils {
    public static Choice predictPreferenceChoice(int itemId1, int itemId2,
                                                      PreferenceDatabase db,
                                                      TrueSkillModelVersion1 model, double epsilon) {
        Double[] item1AttribValues = db.getItemAttributeValues(itemId1);
        Double[] item2AttribValues = db.getItemAttributeValues(itemId2);

        //each item is a team and each attribute is a player
        Integer dim = item1AttribValues.length;
        double util1 = 0d;
        double util2 = 0d;
        for (Integer attribId = 0; attribId < dim; attribId++) {
            AttribChoicePlayer item1attribValuePlayer = model.getPlayer(attribId, item1AttribValues[attribId]);
            AttribChoicePlayer item2attribValuePlayer = model.getPlayer(attribId, item2AttribValues[attribId]);
            util1 += model.getRating(item1attribValuePlayer).getMean(); //todo it is OK?
            util2 += model.getRating(item2attribValuePlayer).getMean(); //todo it is OK?

        }

        if (util1 - util2 > epsilon) return Choice.FIRST;
        if (util2 - util1 > epsilon) return Choice.SECOND;
        return Choice.EQUAL;
    }

    public static Map<IPlayer, Rating> calculateNewRankingsGiven(Preference pref,
                                                           PreferenceDatabase db,
                                                           TrueSkillModelVersion1<Integer, Double> model,
                                                           SkillCalculator calculator, GameInfo gameInfo) {
        Team team1 = createTeam(db.getItemAttributeValues(pref.getItemId1()), model);
        Team team2 = createTeam(db.getItemAttributeValues(pref.getItemId2()), model);

        Collection<ITeam> teams = model.generatePrunedTeamsForComparison(team1, team2);//Team.concat(team1, team2);

        int team1Rank, team2Rank;
        switch (pref.getPreferenceChoice()) {
            case EQUAL:
                team1Rank = 1;
                team2Rank = 1; //todo check that this works...
                break;
            case FIRST:
                team1Rank = 1;
                team2Rank = 2;
                break;
            case SECOND:
                team1Rank = 2;
                team2Rank = 1;
                break;
            default:
                throw new RuntimeException("unexpected choice...");
        }

        Map<IPlayer, Rating> newRatings = calculator.calculateNewRatings(gameInfo, teams, team1Rank, team2Rank);
        return newRatings;
    }

    public static Team createTeam(Double[] itemAttributeValues, TrueSkillModelVersion1<Integer, Double> model) {
//        System.out.println("item1AttribValues = " + Arrays.toString(itemAttributeValues));
        //making a team out of item1:
        Team team = new Team();
        for (Integer attribId = 0; attribId < itemAttributeValues.length; attribId++) {
            AttribChoicePlayer player1 = model.getPlayer(attribId, itemAttributeValues[attribId]);
//            System.out.println("player in Team1 = " + player1);
            team.addPlayer(player1, model.getRating(player1));
        }
        return team;
    }
}
