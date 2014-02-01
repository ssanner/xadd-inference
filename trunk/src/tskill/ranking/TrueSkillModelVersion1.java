package tskill.ranking;

import tskill.jskills.IPlayer;
import tskill.jskills.ITeam;
import tskill.jskills.Rating;
import tskill.jskills.Team;

import java.util.*;

/**
 * Created by Hadi Afshar.
 * Date: 25/01/14
 * Time: 8:20 PM
 */
@Deprecated
public class TrueSkillModelVersion1<A, C> /*A: attrib type, C: choice type*/{

    public static final double INIT_MEAN = 0.0;
    public static final double INIT_STANDARD_DEVIATION = 10;//Double.POSITIVE_INFINITY;

    private Map<A, C[]> attribToAttribChoicesMap = new HashMap<A, C[]>(); //todo is this needed?
//    private List<String> attributes = new ArrayList<>();
    private Map<AttribChoicePlayer, Rating> playerToRatingMap = new HashMap<AttribChoicePlayer, Rating>();

    public void put(A attribute, C[] attributeChoices) {

        if (attribToAttribChoicesMap.keySet().contains(attribute)) throw new RankingException("already exists");

//        attributes.add(attribute);
        attribToAttribChoicesMap.put(attribute, attributeChoices);
        for (C choice : attributeChoices) {
            playerToRatingMap.put(new AttribChoicePlayer(attribute, choice), generateInitRating());
        }
    }

    public AttribChoicePlayer getPlayer(A attribute, C attribValue) {
        for (AttribChoicePlayer player : playerToRatingMap.keySet()) {
            if (player.attribute.equals(attribute) && player.choice.equals(attribValue)) return player;
        }
        throw new RankingException("no such player exists");

    }

    public Rating generateInitRating() {
        return new Rating(INIT_MEAN, INIT_STANDARD_DEVIATION);
    }

    public Rating getRating(AttribChoicePlayer player) {
        Rating rating = playerToRatingMap.get(player);
        if (rating==null) throw new RankingException("no such player");
        return rating;
    }

    public Team generateTeam(AttribChoicePlayer... players) {
        Team team = new Team();
        for (AttribChoicePlayer p : players) {
            team.addPlayer(p, getRating(p));
        }
        return team;
    }

    public void updateRatings(Map<IPlayer, Rating> newRatings) {
        for (IPlayer iPlayer : newRatings.keySet()) {
            if (!(iPlayer instanceof AttribChoicePlayer)) throw new RankingException("type mismatch");
            AttribChoicePlayer player = (AttribChoicePlayer)iPlayer;
            playerToRatingMap.put(player, newRatings.get(iPlayer));
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Model{");
        for (Map.Entry<AttribChoicePlayer, Rating> playerRatingEntry : playerToRatingMap.entrySet()) {
            sb.append(playerRatingEntry.toString() + "\n");
        }
        sb.append("}");

        return sb.toString();
    }

    //omits shared players from the teams
    public Collection<ITeam> generatePrunedTeamsForComparison(Team team1, Team team2) {
        //List<ITeam> prunedTeams = new ArrayList<>();
        Team newTeam1 = new Team();
        for (Map.Entry<IPlayer, Rating> playerRatingEntry : team1.entrySet()) {
              if (!team2.keySet().contains(playerRatingEntry.getKey())) newTeam1.addPlayer(playerRatingEntry.getKey(), playerRatingEntry.getValue());
        }

        Team newTeam2 = new Team();
        for (Map.Entry<IPlayer, Rating> playerRatingEntry : team2.entrySet()) {
              if (!team1.keySet().contains(playerRatingEntry.getKey())) newTeam2.addPlayer(playerRatingEntry.getKey(), playerRatingEntry.getValue());
        }

        List<ITeam> result = new ArrayList<ITeam>(2);
        result.add(newTeam1);
        result.add(newTeam2);

        return result;

    }
}
