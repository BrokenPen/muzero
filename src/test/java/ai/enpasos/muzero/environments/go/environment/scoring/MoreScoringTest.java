package ai.enpasos.muzero.environments.go.environment.scoring;


import ai.enpasos.muzero.environments.go.environment.GoBoard;
import ai.enpasos.muzero.environments.go.environment.Point;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static ai.enpasos.muzero.environments.go.environment.Player.BlackPlayer;
import static ai.enpasos.muzero.environments.go.environment.Player.WhitePlayer;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class MoreScoringTest {

    // 5  X  X  O  O  .
    // 4  .  X  O  O  O
    // 3  X  X  X  O  O
    // 2  .  X  O  O  .
    // 1  X  X  X  O  O
    //    A  B  C  D  E
    @Test
    void scoreAGiven5x5GameC() {
        var board = new GoBoard(5);

        board = board.placeStone(BlackPlayer, new Point(1, 1));
        board = board.placeStone(BlackPlayer, new Point(1, 2));
        board = board.placeStone(WhitePlayer, new Point(1, 3));
        board = board.placeStone(WhitePlayer, new Point(1, 4));

        board = board.placeStone(BlackPlayer, new Point(2, 2));
        board = board.placeStone(WhitePlayer, new Point(2, 3));
        board = board.placeStone(WhitePlayer, new Point(2, 4));
        board = board.placeStone(WhitePlayer, new Point(2, 5));

        board = board.placeStone(BlackPlayer, new Point(3, 1));
        board = board.placeStone(BlackPlayer, new Point(3, 2));
        board = board.placeStone(BlackPlayer, new Point(3, 3));
        board = board.placeStone(WhitePlayer, new Point(3, 4));
        board = board.placeStone(WhitePlayer, new Point(3, 5));

        board = board.placeStone(BlackPlayer, new Point(4, 2));
        board = board.placeStone(WhitePlayer, new Point(4, 3));
        board = board.placeStone(WhitePlayer, new Point(4, 4));

        board = board.placeStone(BlackPlayer, new Point(5, 1));
        board = board.placeStone(BlackPlayer, new Point(5, 2));
        board = board.placeStone(BlackPlayer, new Point(5, 3));
        board = board.placeStone(WhitePlayer, new Point(5, 4));
        board = board.placeStone(WhitePlayer, new Point(5, 5));

        log.debug("final board configuration: \n" + board);

        var result = GameResult.apply(board);
        log.debug("result = \n" + result.toString());
        log.debug("result = \n" + result.toDebugString());

    }


    // 5  O  X  .  O  X
    // 4  .  X  X  X  X
    // 3  X  X  X  .  X
    // 2  O  O  X  X  .
    // 1  O  O  .  O  .
    //    A  B  C  D  E
    @Test
    void scoreAGiven5x5GameB() {
        var board = new GoBoard(5);

        board = board.placeStone(WhitePlayer, new Point(1, 1));
        board = board.placeStone(BlackPlayer, new Point(1, 2));
        board = board.placeStone(WhitePlayer, new Point(1, 4));
        board = board.placeStone(BlackPlayer, new Point(1, 5));

        board = board.placeStone(BlackPlayer, new Point(2, 2));
        board = board.placeStone(BlackPlayer, new Point(2, 3));
        board = board.placeStone(BlackPlayer, new Point(2, 4));
        board = board.placeStone(BlackPlayer, new Point(2, 5));

        board = board.placeStone(BlackPlayer, new Point(3, 1));
        board = board.placeStone(BlackPlayer, new Point(3, 2));
        board = board.placeStone(BlackPlayer, new Point(3, 3));
        board = board.placeStone(BlackPlayer, new Point(3, 5));

        board = board.placeStone(WhitePlayer, new Point(4, 1));
        board = board.placeStone(WhitePlayer, new Point(4, 2));
        board = board.placeStone(BlackPlayer, new Point(4, 3));
        board = board.placeStone(BlackPlayer, new Point(4, 4));

        board = board.placeStone(WhitePlayer, new Point(5, 1));
        board = board.placeStone(WhitePlayer, new Point(5, 2));
        board = board.placeStone(WhitePlayer, new Point(5, 4));

        log.debug("final board configuration: \n" + board);

        var result = GameResult.apply(board);
        log.debug("result = \n" + result.toString());
        log.debug("result = \n" + result.toDebugString());

    }



    // bbbbb
    // b.bww
    // bbww.
    // wbww.
    // .bbww
    @Test
    void scoreAGiven5x5Game() {
        var board = new GoBoard(5);

        board = board.placeStone(BlackPlayer, new Point(5, 2));
        board = board.placeStone(BlackPlayer, new Point(5, 3));
        board = board.placeStone(WhitePlayer, new Point(5, 4));
        board = board.placeStone(WhitePlayer, new Point(5, 5));

        board = board.placeStone(WhitePlayer, new Point(4, 1));
        board = board.placeStone(BlackPlayer, new Point(4, 2));
        board = board.placeStone(WhitePlayer, new Point(4, 3));
        board = board.placeStone(WhitePlayer, new Point(4, 4));

        board = board.placeStone(BlackPlayer, new Point(3, 1));
        board = board.placeStone(BlackPlayer, new Point(3, 2));
        board = board.placeStone(WhitePlayer, new Point(3, 3));
        board = board.placeStone(WhitePlayer, new Point(3, 4));

        board = board.placeStone(BlackPlayer, new Point(2, 1));
        board = board.placeStone(BlackPlayer, new Point(2, 3));
        board = board.placeStone(WhitePlayer, new Point(2, 4));
        board = board.placeStone(WhitePlayer, new Point(2, 5));

        board = board.placeStone(BlackPlayer, new Point(1, 1));
        board = board.placeStone(BlackPlayer, new Point(1, 2));
        board = board.placeStone(BlackPlayer, new Point(1, 3));
        board = board.placeStone(BlackPlayer, new Point(1, 4));
        board = board.placeStone(BlackPlayer, new Point(1, 5));

        log.debug("final board configuration: \n" + board);

        var result = GameResult.apply(board);
        log.debug("result = \n" + result.toString());
        log.debug("result = \n" + result.toDebugString());

    }
}