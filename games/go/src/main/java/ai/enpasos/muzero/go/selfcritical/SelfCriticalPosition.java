package ai.enpasos.muzero.go.selfcritical;

import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
@Builder
public class SelfCriticalPosition implements Comparable<SelfCriticalPosition> {
    int fullMove;
    OneOfTwoPlayer player;

    @Override
    public int compareTo(@NotNull SelfCriticalPosition o) {
        return Integer.compare(this.fullMove, getFullMove());
    }
}