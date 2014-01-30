package tskill.jskills.trueskill;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TwoPlayerTrueSkillCalculatorTest {

    private TwoPlayerTrueSkillCalculator calculator;

    @BeforeMethod
    public void setup() {
        calculator = new TwoPlayerTrueSkillCalculator();
    }

    @Test
    public void TestAllTwoPlayerScenarios() {
        // We only support two players
        TrueSkillCalculatorTests.TestAllTwoPlayerScenarios(calculator);
    }
    
    // TODO: Assert failures for larger teams
}