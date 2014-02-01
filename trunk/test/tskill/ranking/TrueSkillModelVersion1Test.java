package tskill.ranking;


import hgm.preference.Choice;
import hgm.preference.Preference;
import org.junit.Test;
import tskill.jskills.*;
import tskill.jskills.trueskill.FactorGraphTrueSkillCalculator;
import tskill.jskills.trueskill.TrueSkillCalculatorTests;
import tskill.jskills.trueskill.TwoTeamTrueSkillCalculator;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * Created by Hadi Afshar.
 * Date: 30/01/14
 * Time: 10:42 PM
 */
@Deprecated
public class TrueSkillModelVersion1Test {




    @Test
    public void TestAllTwoPlayerScenarios() {
        SkillCalculator calculator = new
//                FactorGraphTrueSkillCalculator();
                TwoTeamTrueSkillCalculator();


        // This calculator supports up to two teams with many players each
        TrueSkillCalculatorTests.TestAllTwoPlayerScenarios(calculator);
    }

    @Test
    public void TestAllTwoTeamScenarios() {
        SkillCalculator calculator = new
//                FactorGraphTrueSkillCalculator();
                TwoTeamTrueSkillCalculator();

        // This calculator supports up to two teams with many players each
        TrueSkillCalculatorTests.TestAllTwoTeamScenarios(calculator);
    }

    @Test
    public void test1() {
        SkillCalculator calculator = new
//                FactorGraphTrueSkillCalculator();
                TwoTeamTrueSkillCalculator();


        //D = 3
        TrueSkillModelVersion1 model = new TrueSkillModelVersion1();
        model.put("color", new String[]{"red", "green", "blue"});
        model.put("size", new String[]{"small", "large"});
        model.put("price", new String[]{"1", "2", "3", "4"});

//        Player<Integer> player1 = new Player<Integer>(1);
//        Player<Integer> player2 = new Player<Integer>(2);
        GameInfo gameInfo = GameInfo.getDefaultGameInfo();

        //make team: red, small, 1
        Team team1 = model.generateTeam(model.getPlayer("color", "red"), model.getPlayer("size", "small"));
        Team team2 = model.generateTeam(model.getPlayer("color", "green"), model.getPlayer("size", "small"));

//        Team team1 = new Team(player1, gameInfo.getDefaultRating());
//        Team team2 = new Team(player2, gameInfo.getDefaultRating());
        Collection<ITeam> teams = model.generatePrunedTeamsForComparison(team1, team2);//Team.concat(team1, team2);

        Map<IPlayer, Rating> newRatings = calculator.calculateNewRatings(gameInfo, teams, 1, 2);
        model.updateRatings(newRatings);
//        System.out.println("newRatings = " + newRatings);
        System.out.println("model = " + model);

        newRatings = calculator.calculateNewRatings(gameInfo, Arrays.asList(new ITeam[]{
                model.generateTeam(model.getPlayer("color", "red"), model.getPlayer("size", "small")),
                model.generateTeam(model.getPlayer("color", "blue"), model.getPlayer("size", "large"))}),1,2 );

        model.updateRatings(newRatings);

        System.out.println("model = " + model);
        /*Rating player1NewRating = newRatings.get(player1);
        assertRating(29.39583201999924, 7.171475587326186, player1NewRating);

        Rating player2NewRating = newRatings.get(player2);
        assertRating(20.60416798000076, 7.171475587326186, player2NewRating);

        assertMatchQuality(0.447, calculator.calculateMatchQuality(gameInfo, teams));
*/

    }

    @Test
    public void test2() {
        SkillCalculator calculator = new
                FactorGraphTrueSkillCalculator();
//                TwoTeamTrueSkillCalculator();


        //D = 3
        TrueSkillModelVersion1 model = new TrueSkillModelVersion1();
        model.put("color", new String[]{"red"});
        model.put("size", new String[]{"small"});

        System.out.println("model = " + model);

        GameInfo gameInfo = GameInfo.getDefaultGameInfo();

        Team team1 = model.generateTeam(model.getPlayer("color", "red"));
        Team team2 = model.generateTeam(model.getPlayer("color", "red"));
//        Team team2 = model.generateTeam(model.getPlayer("size", "small"));

//        Collection<ITeam> teams = model.generatePrunedTeamsForComparison(team1, team2);//Team.concat(team1, team2);

//        Map<IPlayer, Rating> newRatings = calculator.calculateNewRatings(gameInfo, teams, 1, 2);
        Map<IPlayer, Rating> newRatings = calculator.calculateNewRatings(gameInfo, Team.concat(team1, team2), 1, 2);
        model.updateRatings(newRatings);
        System.out.println("model = " + model);

        /*
        newRatings = calculator.calculateNewRatings(gameInfo, Arrays.asList(new ITeam[]{
                model.generateTeam(model.getPlayer("color", "red"), model.getPlayer("size", "small")),
                model.generateTeam(model.getPlayer("color", "blue"), model.getPlayer("size", "large"))}),1,2 );

        model.updateRatings(newRatings);

        System.out.println("model = " + model);
*/
    }


}
