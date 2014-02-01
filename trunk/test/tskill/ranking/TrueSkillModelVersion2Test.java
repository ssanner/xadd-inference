package tskill.ranking;

import hgm.preference.Choice;
import org.junit.Test;
import tskill.jskills.*;
import tskill.jskills.trueskill.FactorGraphTrueSkillCalculator;
import tskill.jskills.trueskill.TrueSkillCalculatorTests;
import tskill.jskills.trueskill.TwoTeamTrueSkillCalculator;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 30/01/14
 * Time: 10:43 PM
 */
public class TrueSkillModelVersion2Test {


    @Test
    public void test1() {

        SkillCalculator calculator = new
                FactorGraphTrueSkillCalculator();
//                TwoTeamTrueSkillCalculator();

        GameInfo gameInfo = GameInfo.getDefaultGameInfo();

        TrueSkillModelVersion2 model = new TrueSkillModelVersion2(gameInfo, calculator, 1);
        System.out.println("model = " + model);
        model.updateByMatch(new Double[]{10d}, new Double[]{100d}, Choice.FIRST);
        System.out.println("model = " + model);
    }

    @Test
    public void test2() {

        SkillCalculator calculator = new
                FactorGraphTrueSkillCalculator();
//                TwoTeamTrueSkillCalculator();

        GameInfo gameInfo = GameInfo.getDefaultGameInfo();

        TrueSkillModelVersion2 model = new TrueSkillModelVersion2(gameInfo, calculator, 3);
        System.out.println("model = " + model);
        model.updateByMatch(new Double[]{10d, 20d, 30d}, new Double[]{10d, 20d, 30d}, Choice.FIRST);
        System.out.println("model = " + model);
        model.updateByMatch(new Double[]{10d, 20d, 30d}, new Double[]{11d, 21d, 31d}, Choice.FIRST);
        System.out.println("model = " + model);
        model.updateByMatch(new Double[]{10d, 10d, 10d}, new Double[]{0d, 0d, 0d}, Choice.FIRST);
        System.out.println("model = " + model);
        model.updateByMatch(new Double[]{10d, 20d, 30d}, new Double[]{100d, 200d, 300d}, Choice.SECOND);
        System.out.println("model = " + model);
        model.updateByMatch(new Double[]{10d, 20d, 30d}, new Double[]{100d, 200d, 0d}, Choice.SECOND);
        System.out.println("model = " + model);
    }


}
