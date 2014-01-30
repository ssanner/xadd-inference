package tskill.jskills.trueskill;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TwoTeamTrueSkillCalculatorTest {

    private TwoTeamTrueSkillCalculator calculator;

    @BeforeMethod
    public void setup() {
        calculator = new TwoTeamTrueSkillCalculator();
    }

    @Test
    public void TestAllTwoPlayerScenarios() {
        // This calculator supports up to two teams with many players each
        TrueSkillCalculatorTests.TestAllTwoPlayerScenarios(calculator);
    }

    @Test
    public void TestAllTwoTeamScenarios() {
        // This calculator supports up to two teams with many players each
        TrueSkillCalculatorTests.TestAllTwoTeamScenarios(calculator);
    }
}