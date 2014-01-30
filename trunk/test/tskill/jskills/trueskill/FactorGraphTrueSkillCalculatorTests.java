package tskill.jskills.trueskill;

import org.testng.annotations.*;

public class FactorGraphTrueSkillCalculatorTests {

    private FactorGraphTrueSkillCalculator calculator;
    
    @BeforeMethod
    public void setup() {
        calculator = new FactorGraphTrueSkillCalculator();
    }

    @Test
    public void TestAllTwoTeamScenarios() {
        TrueSkillCalculatorTests.TestAllTwoTeamScenarios(calculator);
    }
    
    @Test
    public void TestAllTwoPlayerScenarios() {
        TrueSkillCalculatorTests.TestAllTwoPlayerScenarios(calculator);
    }

    @Test
    public void TestAllMultipleTeamScenarios() {
        TrueSkillCalculatorTests.TestAllMultipleTeamScenarios(calculator);
    }

    @Test
    public void TestPartialPlayScenarios() {
        TrueSkillCalculatorTests.TestPartialPlayScenarios(calculator);
    }
}