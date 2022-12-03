package com.ling1.springmvc.match;


import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.ling1.springmvc.lobby.LobbyService;
import com.ling1.springmvc.ocatile.OcaTile;
import com.ling1.springmvc.ocatile.OcaTileService;
import com.ling1.springmvc.player.PlayerColor;
import com.ling1.springmvc.player.PlayerService;
import com.ling1.springmvc.player.PlayerStats;
import com.ling1.springmvc.user.User;
import com.ling1.springmvc.user.UserService;

@Controller
@RequestMapping("/matches")
public class MatchController {

    public static int NUM_DICES_SIDES = 6;

    public static final String INSIDE_MATCH = "Matches/InsideMatch";
    public static final String FINISH_MATCH = "Matches/FinishedMatch";

    @Autowired
    LobbyService lobbyService;
    @Autowired
    MatchService matchService;
    @Autowired
    UserService userService;
    @Autowired
    PlayerService playerService;
    @Autowired
    OcaTileService ocaTileService;

    @GetMapping("/{matchId}")
    public ModelAndView matchInside(
            @PathVariable("matchId") Integer matchId, HttpServletResponse response) {
        Match currentMatch = matchService.getMatchById(matchId);
        if (currentMatch.getWinner() == null) {
            response.addHeader("Refresh", "2");
        }
        ModelAndView result = null;
        if (currentMatch.getWinner() != null) {
            result = new ModelAndView(FINISH_MATCH);
        } else {
            result = new ModelAndView(INSIDE_MATCH);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User loggedUser = userService.findUsername(authentication.getName());

        PlayerStats previousPlayer = null;
        Boolean prevPChosen = false;
        // If the previous player landed in an "extra roll tile" there is no need to
        // find the previous player, as it will be himself again.
        // The info related to the jump between tiles is added as well.
        if (currentMatch.getPlayerToPlay().getPosition() != 0) {
            OcaTile currentTile = ocaTileService.findTileTypeByPosition(currentMatch.getPlayerToPlay().getPosition());
            if (currentTile.getType()
                    .getName().equals("OCA")
                    || currentTile.getType()
                            .getName().equals("BRIDGE")
                    || currentTile.getType()
                            .getName().equals("DICE")) {
                previousPlayer = currentMatch.getPlayerToPlay();
                if (currentTile.getType()
                        .getName().equals("OCA") && currentTile.getId() != 1) {
                    result.addObject("prevOca", ocaTileService.allOcas().get(ocaTileService.allOcas()
                            .indexOf(currentTile) - 1));
                }
                if (currentTile.getType()
                        .getName().equals("BRIDGE")) {
                    result.addObject("otherBridge", ocaTileService.otherBridge(currentTile.getId()));
                }
                if (currentTile.getType()
                        .getName().equals("DICE")) {
                    result.addObject("otherDice", ocaTileService.otherDice(currentTile.getId()));
                }
                prevPChosen = true;
            }
        }
        // As the color order is always the same, from the color of the player that has
        // to play we can find the previous color in the match and therefore the
        // previous player. This process is skipped if the player landed on a "extra
        // roll tile".
        Integer ColorPosition = playerService.findColors().indexOf(currentMatch.getPlayerToPlay().getPlayerColor());
        while (!prevPChosen) {
            if (ColorPosition == 0) {
                ColorPosition = 3;
            } else
                ColorPosition--;
            PlayerColor colorToTry = playerService.findColors().get((ColorPosition));
            for (PlayerStats ps : currentMatch.getPlayerStats()) {

                if (ps.getPlayerColor() == colorToTry) {
                    previousPlayer = ps;
                    prevPChosen = true;
                }

            }
        }
        result.addObject("loggedUser", loggedUser);
        result.addObject("match", currentMatch);
        result.addObject("prevPlayer", previousPlayer);
        result.addObject("allTiles", ocaTileService.getAllTiles());
        result.addObject("allOcas", ocaTileService.allOcas());
        return result;
    }

    @GetMapping("/{matchId}/advance")
    public ModelAndView matchAdvance(
            @PathVariable("matchId") Integer matchId) {
        Match matchToUpdate = matchService.getMatchById(matchId);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User loggedUser = userService.findUsername(authentication.getName());

        if (loggedUser == matchToUpdate.getPlayerToPlay().getUser() && matchToUpdate.getWinner() == null) {
            // If the current player is stuck a turn is removed from the counter, the last
            // roll is set to -1 for jsp visualization.
            if (matchToUpdate.getPlayerToPlay().getTurnsStuck() != 0) {
                matchToUpdate.getPlayerToPlay().setTurnsStuck(matchToUpdate.getPlayerToPlay().getTurnsStuck() - 1);
                matchToUpdate.setLastRoll(-1);
                playerService.save(matchToUpdate.getPlayerToPlay());
                // Finding next player, similar to finding previous player.
                Integer ColorPosition = playerService.findColors()
                        .indexOf(matchToUpdate.getPlayerToPlay().getPlayerColor());
                Boolean assignedNextTurn = false;
                while (!assignedNextTurn) {
                    if (ColorPosition == 3) {
                        matchToUpdate.setNumTurns(matchToUpdate.getNumTurns() + 1);
                        ColorPosition = 0;
                    } else
                        ColorPosition++;
                    PlayerColor colorToTry = playerService.findColors().get((ColorPosition));
                    for (PlayerStats ps : matchToUpdate.getPlayerStats()) {
                        if (ps.getPlayerColor() == colorToTry) {
                            assignedNextTurn = true;
                            matchToUpdate.setPlayerToPlay(ps);
                        }
                    }
                }
                // If the current player is not stuck, normal dice rolling is done.
            } else {
                Integer rolledNumber = 1 + (int) Math.floor(Math.random() * NUM_DICES_SIDES);
                Integer newPos = matchToUpdate.getPlayerToPlay().getPosition() + rolledNumber;
                // If you go over the final tile, go back as the same extra tiles.
                if (newPos > ocaTileService.getAllTiles().size()) {
                    newPos = ocaTileService.getAllTiles().size() - (newPos - ocaTileService.getAllTiles().size());
                }
                matchToUpdate.getPlayerToPlay()
                        .setNumDiceRolls(matchToUpdate.getPlayerToPlay().getNumDiceRolls() + 1);
                Boolean rollAgain = false;

                switch (ocaTileService.findTileTypeByPosition(newPos).getType().getName()) {
                    case "NORMAL":
                        break;
                    case "OCA":
                        newPos = ocaTileService.nextOca(newPos);
                        // As the final tile is also considered an OCA, if you land on the previous OCA
                        // you should win.
                        if (ocaTileService.findTileTypeByPosition(newPos).getType().getName().equals("END")) {
                            matchToUpdate.getPlayerToPlay()
                                    .setNumDiceRolls(matchToUpdate.getPlayerToPlay().getNumDiceRolls() + 1);
                            matchToUpdate.setWinner(matchToUpdate.getPlayerToPlay());
                            matchService.save(matchToUpdate);
                            playerService.save(matchToUpdate.getPlayerToPlay());
                            return new ModelAndView("redirect:/matches/" + matchId);
                        }
                        rollAgain = true;
                        matchToUpdate.getPlayerToPlay()
                                .setNumberOfGooses(matchToUpdate.getPlayerToPlay().getNumberOfGooses() + 1);
                        break;
                    case "BRIDGE":
                        newPos = ocaTileService.otherBridge(newPos);
                        rollAgain = true;
                        break;
                    case "INN":
                        matchToUpdate.getPlayerToPlay()
                                .setTurnsStuck(2);
                        matchToUpdate.getPlayerToPlay()
                                .setNumberOfInns(matchToUpdate.getPlayerToPlay().getNumberOfInns() + 1);
                        break;
                    case "WELL":
                        matchToUpdate.getPlayerToPlay()
                                .setTurnsStuck(3);
                        matchToUpdate.getPlayerToPlay()
                                .setNumberOfPlayerWells(matchToUpdate.getPlayerToPlay().getNumberOfPlayerWells() + 1);
                        break;
                    case "DICE":
                        newPos = ocaTileService.otherDice(newPos);
                        rollAgain = true;
                        break;
                    case "LABYRINTH":
                        newPos = 30;
                        matchToUpdate.getPlayerToPlay()
                                .setNumberOfLabyrinths(matchToUpdate.getPlayerToPlay().getNumberOfLabyrinths() + 1);
                        break;
                    case "PRISON":
                        matchToUpdate.getPlayerToPlay()
                                .setTurnsStuck(4);
                        matchToUpdate.getPlayerToPlay()
                                .setNumberOfPlayerPrisons(
                                        matchToUpdate.getPlayerToPlay().getNumberOfPlayerPrisons() + 1);
                        break;
                    case "DEATH":
                        newPos = 1;
                        matchToUpdate.getPlayerToPlay()
                                .setNumberOfPlayerDeaths(matchToUpdate.getPlayerToPlay().getNumberOfPlayerDeaths() + 1);
                        break;
                    case "END":
                        matchToUpdate.getPlayerToPlay().setPosition(newPos);
                        matchToUpdate.getPlayerToPlay()
                                .setNumDiceRolls(matchToUpdate.getPlayerToPlay().getNumDiceRolls() + 1);
                        matchToUpdate.setWinner(matchToUpdate.getPlayerToPlay());
                        matchService.save(matchToUpdate);
                        playerService.save(matchToUpdate.getPlayerToPlay());
                        return new ModelAndView("redirect:/matches/" + matchId);
                }
                matchToUpdate.setLastRoll(rolledNumber);
                matchToUpdate.getPlayerToPlay().setPosition(newPos);

                Integer ColorPosition = playerService.findColors()
                        .indexOf(matchToUpdate.getPlayerToPlay().getPlayerColor());
                Boolean assignedNextTurn = false;
                // Finding next player, if the player landed on a oca this process is skipped
                // (he is the playerToPlay again).
                if (!rollAgain) {
                    while (!assignedNextTurn) {
                        if (ColorPosition == 3) {
                            matchToUpdate.setNumTurns(matchToUpdate.getNumTurns() + 1);
                            ColorPosition = 0;
                        } else
                            ColorPosition++; // this code could be done way cleaner with modulus ((ColorPosition+1)%3);
                                             // yet to discover why it doesn't work
                        PlayerColor colorToTry = playerService.findColors().get((ColorPosition));
                        for (PlayerStats ps : matchToUpdate.getPlayerStats()) {
                            if (ps.getPlayerColor() == colorToTry) {
                                assignedNextTurn = true;
                                matchToUpdate.setPlayerToPlay(ps);
                            }
                        }
                    }
                }
            }

            matchService.save(matchToUpdate);
            playerService.save(matchToUpdate.getPlayerToPlay());
            ModelAndView result = new ModelAndView("redirect:/matches/" + matchId);
            return result;
        }
        ModelAndView result = new ModelAndView("redirect:/matches/" + matchId);
        result.addObject("message", "It's not your turn");
        return result;

    }

}
