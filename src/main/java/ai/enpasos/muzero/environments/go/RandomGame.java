package ai.enpasos.muzero.environments.go;

import ai.enpasos.muzero.environments.go.environment.*;
import ai.enpasos.muzero.environments.go.environment.scoring.GameResult;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

@Slf4j
public class RandomGame {
    public static void main(String[] args) {
        var state = GameState.newGame(5);
        log.info(state.getBoard().toString());

        List<Move> validMoves = state.getValidMoves();

        while (!validMoves.isEmpty()) {
            Collections.shuffle(validMoves);
            Move selectedMove = validMoves.get(0);


            state = state.applyMove(selectedMove);


            if (selectedMove instanceof Play) {
                log.info("\n"+ state.getBoard().toString());
            } else if (selectedMove instanceof Pass) {
                log.info("Pass");
            } else if (selectedMove instanceof Resign) {
                log.info("Resign");
            }

            validMoves = state.getValidMoves();
        }

        log.info("*** G A M E   O V E R ***");

        var result = GameResult.apply(state.getBoard());
        log.info("result = " +  result + "\n" + result.toDebugString());






    }


}