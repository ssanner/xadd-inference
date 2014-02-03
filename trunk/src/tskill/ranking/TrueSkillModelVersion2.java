package tskill.ranking;

import hgm.preference.Choice;
import tskill.jskills.*;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 25/01/14
 * Time: 8:20 PM
 */
public class TrueSkillModelVersion2 {
    public static final double INIT_MEAN = 0;  //TODO WHAT IF 0?
    public static final double INIT_STANDARD_DEVIATION = 10;//Double.POSITIVE_INFINITY;

    private GameInfo gameInfo;
    private SkillCalculator calculator;
    private List<IdPlayer> players;

    public TrueSkillModelVersion2(GameInfo gameInfo, SkillCalculator calculator, int numberOfFeatures) {
        this.gameInfo = gameInfo;
        this.calculator = calculator;
        players = new ArrayList<IdPlayer>(numberOfFeatures);
        for (int id = 0; id<numberOfFeatures; id++) {
            players.add(new IdPlayer(id, new Rating(INIT_MEAN, INIT_STANDARD_DEVIATION)));
        }

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ModelV2{\n");
        for (IdPlayer p : players) {
            sb.append("\t" + p.toString() + "\n");
        }
        sb.append("}");

        return sb.toString();
    }


    public void updateByMatch(Double[] values1, Double[] values2, Choice choice) {
        if (values1.length != values2.length) throw new RuntimeException("type mismatch");

        Team team1 = generatePositiveDifferentialTeam(values1, values2);
        Team team2 = generatePositiveDifferentialTeam(values2, values1);

        int teamRank1;
        int teamRank2;
        switch (choice){
            case FIRST:
                teamRank1 = 1;
                teamRank2 = 2;
                break;
            case SECOND:
                teamRank1 = 2;
                teamRank2 = 1;
                break;
            case EQUAL:
                teamRank1 = 1; //todo: is this ok?
                teamRank2 = 1;
                break;
            default:
                throw new RuntimeException("unknown choice");
        }

        Map<IPlayer, Rating> newRatings = calculator.calculateNewRatings(gameInfo, Team.concat(team1, team2), teamRank1, teamRank2);

        //updating the rates:
        for (Map.Entry<IPlayer, Rating> playerAndRating : newRatings.entrySet()) {
            IdPlayer player = (IdPlayer)playerAndRating.getKey();
            Rating newUnNormRanking = playerAndRating.getValue();
            Integer id = player.getId();
            if (id != -1) {
                double v = Math.abs(values1[id] - values2[id]); //absolute since it does not matter, in which side the player has played...
                if (v == 0.0) throw new RuntimeException("unexpected v == 0");

                player.updateRating(new Rating(newUnNormRanking.getMean() / v, newUnNormRanking.getStandardDeviation() / v)); //mind the div...
            }
        }
    }

    private IPlayer generateNewVoidPlayer() {
        return new IdPlayer(-1, null /*since the rating is never updated for void players*/);
    }

    private Team generatePositiveDifferentialTeam(Double[] vals1, Double[] vals2) {

        Team team = new Team();

        //Note: I add the following to prevent error when one of the competing teams is empty. //todo talk with Scott about it...
        team.addPlayer(generateNewVoidPlayer(), new Rating(INIT_MEAN, INIT_STANDARD_DEVIATION));

        for (int i = 0; i < vals1.length; i++) {
            double v = vals1[i] - vals2[i];
            if (v > 0.0) {
                IdPlayer player = players.get(i);
                team.addPlayer(player,
                        new Rating(player.getRating().getMean() * v, player.getRating().getStandardDeviation() * v)); //mind the multiplication....
            }
        }

        return team;
    }

    public Rating getRating(int playerId) {
        return players.get(playerId).getRating();
    }
}
