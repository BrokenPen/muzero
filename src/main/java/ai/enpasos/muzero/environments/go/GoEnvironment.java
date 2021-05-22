/*
 *  Copyright (c) 2021 enpasos GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package ai.enpasos.muzero.environments.go;

import ai.enpasos.muzero.MuZeroConfig;
import ai.enpasos.muzero.environments.EnvironmentBaseBoardGames;
import ai.enpasos.muzero.environments.OneOfTwoPlayer;
import ai.enpasos.muzero.environments.go.environment.*;
import ai.enpasos.muzero.environments.go.environment.scoring.GameResult;
import ai.enpasos.muzero.play.Action;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
public class GoEnvironment extends EnvironmentBaseBoardGames {

    private List<GameState> history; // TODO refactor
    private GameState state;
    private GameResult result;

    public GoEnvironment(@NotNull MuZeroConfig config) {
        super(config);
        history = new ArrayList<>();
        state = GameState.newGame(config.getBoardWidth());
        history.add(state);
    }

    public @NotNull List<Action> legalActions() {
        return state.getValidMoves().stream()
                .filter(m -> !(m instanceof Resign))  // muzero is not resigning :-)
                .map(move -> GoAdapter.translate(this.config, move)).collect(Collectors.toList());
    }


    List<GameState> getHistory() {
        return this.history;
    }


    @Override
    public float step(@NotNull Action action) {

        Player thisPlayer = state.getNextPlayer();

        state = state.applyMove(GoAdapter.translate(this.config, action));
        history.add(state);

        float reward = 0f;

        if (terminal()) {
            result = GameResult.apply(state.getBoard());
            log.debug(result.toString());
            reward = (thisPlayer == result.winner()) ? 1f : -1f;
        }

        return reward;
    }


    public void swapPlayer() {
        throw new NotImplementedException("swapPlayer() is not implemented here");
    }


//    private boolean isLegalAction(Action action_) {
//        return legalActions().contains(action_);
//    }

    @Override
    public void setPlayerToMove(OneOfTwoPlayer player) {
        throw new NotImplementedException("setPlayerToMove is not implemented");
    }

    @Override
    public OneOfTwoPlayer getPlayerToMove() {
        return GoAdapter.translate(this.state.getNextPlayer());
    }

    @Override
    public int[][] currentImage() {

        // TODO
        throw new NotImplementedException("swapPlayer() is not implemented");
      //  return this.board;
    }

    public boolean terminal() {
        return this.state.isOver();
    }



    public @NotNull String render() {

        String lastMove = "NONE YET";
        if (state.getLastMove().isPresent()) {
            if (state.getLastMove().get() instanceof Pass)  {
                lastMove = "PASS";
            } else if (state.getLastMove().get() instanceof Resign)  {
                lastMove = "RESIGN";
            } else if (state.getLastMove().get() instanceof Play)  {
                Play play = (Play)state.getLastMove().get();
                Point p = play.getPoint();
                lastMove = "PLAY(" + String.valueOf((char)(64 + p.getCol()))  + ", " + (config.getBoardHeight()-p.getRow()+1) + ")";
            }
        }

        String lastMoveStr =  (state.getNextPlayer().other()== Player.BlackPlayer?"x":"o")
                + " move: " + lastMove;

        String status = "GAME RUNNING";

        if (result != null) {
            status = "GAME OVER\n" + result.toString();
        }

         return lastMoveStr + "\n" + state.getBoard().toString() +  "\n" + status ;

    }



}