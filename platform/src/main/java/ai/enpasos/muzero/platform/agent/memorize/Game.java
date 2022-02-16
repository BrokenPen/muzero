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

package ai.enpasos.muzero.platform.agent.memorize;

import ai.djl.ndarray.NDManager;
import ai.enpasos.muzero.platform.agent.intuitive.NetworkIO;
import ai.enpasos.muzero.platform.agent.intuitive.Observation;
import ai.enpasos.muzero.platform.agent.rational.Action;
import ai.enpasos.muzero.platform.agent.rational.ActionHistory;
import ai.enpasos.muzero.platform.agent.rational.Node;
import ai.enpasos.muzero.platform.agent.rational.Player;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayerMode;
import ai.enpasos.muzero.platform.environment.Environment;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.stream.IntStream;

/**
 * A single episode of interaction with the environment.
 */
@Data
public abstract class Game {

    protected boolean purelyRandom;

    protected GameDTO gameDTO;

    protected MuZeroConfig config;

    protected int actionSpaceSize;
    protected double discount;
    protected Environment environment;
    private Random r;

    private double[] surprises;
    double[] values;
    double[] entropies;
    boolean done;
    int tSurprise;
    int tTrainingStart;

    private float error;


    //protected List<Float> valuesFromCurrentInitialInference;
    protected GameDTO originalGameDTO;


    protected Game(@NotNull MuZeroConfig config) {
        this.config = config;
    //    valuesFromCurrentInitialInference = new ArrayList<>();
        this.gameDTO = new GameDTO();
        this.actionSpaceSize = config.getActionSpaceSize();
        this.discount = config.getDiscount();
        r = new Random();
    }

    protected Game(@NotNull MuZeroConfig config, GameDTO gameDTO) {
        this.config = config;
     //   valuesFromCurrentInitialInference = new ArrayList<>();
        this.gameDTO = gameDTO;
        this.actionSpaceSize = config.getActionSpaceSize();
        this.discount = config.getDiscount();
    }

    public static Game decode(@NotNull MuZeroConfig config, byte @NotNull [] bytes) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        try (ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
            GameDTO dto = (GameDTO) objectInputStream.readObject();
            Game game = config.newGame();
            Objects.requireNonNull(game).setGameDTO(dto);
            return game;
        } catch (Exception e) {
            throw new MuZeroException(e);
        }
    }

    public float calculateSquaredDistanceBetweenOriginalAndCurrentValue() {
        this.error = 0;
        for (int i = 0; i < this.originalGameDTO.getRootValuesFromInitialInference().size(); i++) {
            double d = this.originalGameDTO.getRootValuesFromInitialInference().get(i)
                     - this.getGameDTO().getRootValuesFromInitialInference().get(i);
            this.error += d * d;
        }
        return this.error;
    }

    public @NotNull Game copy() {
        Game copy = getConfig().newGame();
        copy.setGameDTO(this.gameDTO.copy());
        copy.setConfig(this.getConfig());
        copy.setDiscount(this.getDiscount());
        copy.setEnvironment(this.getEnvironment());
        copy.setActionSpaceSize(this.getActionSpaceSize());
        copy.replayToPosition(copy.getGameDTO().getActions().size());
        return copy;
    }


    public @Nullable Float getLastReward() {
        if (getGameDTO().getRewards().size() == 0) return null;
        return getGameDTO().getRewards().get(getGameDTO().getRewards().size() - 1);
    }

    public abstract boolean terminal();

    public abstract List<Action> legalActions();


    public abstract List<Action> allActionsInActionSpace();

    public void apply(int @NotNull ... actionIndex) {
        Arrays.stream(actionIndex).forEach(
                i -> apply(config.newAction(i))
        );
    }

    public void apply(@NotNull Action action) {
        float reward = this.environment.step(action);

        this.getGameDTO().getRewards().add(reward);
        this.getGameDTO().getActions().add(action.getIndex());
    }


    public List<Target> makeTarget(int stateIndex, int numUnrollSteps, int tdSteps) {
        List<Target> targets = new ArrayList<>();

        IntStream.range(stateIndex, stateIndex + numUnrollSteps + 1).forEach(currentIndex -> {
            Target target = new Target();
            fillTarget(stateIndex, tdSteps, currentIndex, target);
            targets.add(target);
        });
        return targets;
    }

    private void fillTarget(int stateIndex, int tdSteps, int currentIndex, Target target) {
        int perspective = getPerspective(stateIndex, currentIndex);

        int bootstrapIndex = currentIndex + tdSteps;

        double value = getBootstrapValue(tdSteps, bootstrapIndex);
        value = finalizeValue(currentIndex, perspective, bootstrapIndex, value);

        float lastReward = getLastReward(currentIndex);

        if (currentIndex < this.getGameDTO().getRootValues().size()) {
            target.setValue((float) value);
            target.setReward(lastReward);
            target.setPolicy(this.getGameDTO().getPolicyTargets().get(currentIndex));
        } else if (!config.isNetworkWithRewardHead() && currentIndex == this.getGameDTO().getRootValues().size()) {
            // If we do not train the reward (as only boardgames are treated here)
            // the value has to take the role of the reward on this node (needed in MCTS)
            // if we were running the network with reward head
            // the value would be 0 here
            // but as we do not get the expected reward from the network
            // we need use this node to keep the reward value
            // therefore target.value is not 0f
            // To make the whole thing clear. The cases with and without a reward head should be treated in a clearer separation
            target.setValue((float) value);  // this is not really the value, it is taking the role of the reward here
            target.setReward(lastReward);
            target.setPolicy(new float[this.actionSpaceSize]);
            // the idea is not to put any force on the network to learn a particular action where it is not necessary
            Arrays.fill(target.getPolicy(), 0f);
        } else {
            target.setValue(config.isAbsorbingStateDropToZero() ? 0f : (float) value);
            target.setReward(lastReward);
            target.setPolicy(new float[this.actionSpaceSize]);
            // the idea is not to put any force on the network to learn a particular action where it is not necessary
            Arrays.fill(target.getPolicy(), 0f);
        }
    }

    private float getLastReward(int currentIndex) {
        float lastReward;
        if (currentIndex > 0 && currentIndex <= this.getGameDTO().getRewards().size()) {
            lastReward = this.getGameDTO().getRewards().get(currentIndex - 1);
        } else {
            lastReward = 0f;
        }
        return lastReward;
    }

    private double finalizeValue(int currentIndex, int perspective, int bootstrapIndex, double value) {
        int startIndex;
        if (config.isNetworkWithRewardHead()) {
            startIndex = currentIndex;
        } else {
            startIndex = Math.min(currentIndex, this.getGameDTO().getRewards().size() - 1);
        }
        for (int i = startIndex; i < this.getGameDTO().getRewards().size() && i < bootstrapIndex; i++) {
            value += (double) this.getGameDTO().getRewards().get(i) * Math.pow(this.discount, i) * perspective;
        }
        return value;
    }

    private double getBootstrapValue(int tdSteps, int bootstrapIndex) {
        double value;
        if (bootstrapIndex < this.getGameDTO().getRootValues().size()) {
            value = this.getGameDTO().getRootValues().get(bootstrapIndex) * Math.pow(this.discount, tdSteps);
        } else {
            value = 0;
        }
        return value;
    }

    private int getPerspective(int stateIndex, int currentIndex) {
        int perspective;
        boolean perspectiveChange = config.getPlayerMode() == PlayerMode.TWO_PLAYERS;
        int currentIndexPerspective;
        int winnerPerspective;
        if (perspectiveChange) {
            currentIndexPerspective = toPlay() == OneOfTwoPlayer.PLAYER_A ? 1 : -1;
            currentIndexPerspective *= Math.pow(-1d, (double) currentIndex - stateIndex);
            winnerPerspective = this.getGameDTO().getRewards().size() % 2 == 1 ? 1 : -1;
        } else {
            currentIndexPerspective = 1;
            winnerPerspective = 1;
        }
        perspective = winnerPerspective * currentIndexPerspective;
        return perspective;
    }


    public abstract Player toPlay();


    public @NotNull ActionHistory actionHistory() {
        return new ActionHistory(config, this.gameDTO.getActions(), actionSpaceSize);
    }


    public abstract String render();


    public abstract Observation getObservation(NDManager ndManager);

    public abstract void replayToPosition(int stateIndex);

    public @NotNull List<Integer> getRandomActionsIndices(int i) {

        List<Integer> actionList = new ArrayList<>();
        for (int j = 0; j < i; j++) {
            actionList.add(r.nextInt(this.config.getActionSpaceSize()));
        }
        return actionList;
    }


    public boolean equals(Object other) {
        if (!(other instanceof Game)) return false;
        Game otherGame = (Game) other;
        return this.getGameDTO().getActions().equals(otherGame.getGameDTO().getActions());
    }

    public int hashCode() {
        return this.getGameDTO().getActions().hashCode();
    }


    public abstract void renderNetworkGuess(MuZeroConfig config, Player toPlay, NetworkIO networkIO, boolean b);

    public abstract void renderSuggestionFromPriors(MuZeroConfig config, Node node);

    public abstract void renderMCTSSuggestion(MuZeroConfig config, float[] childVisits);

    public void beforeReplay() {
        this.originalGameDTO = this.gameDTO;
        this.gameDTO = new GameDTO();
        this.initEnvironment();
    }

    public void afterReplay() {
        this.gameDTO = this.originalGameDTO;
    }

    public abstract void initEnvironment();
}