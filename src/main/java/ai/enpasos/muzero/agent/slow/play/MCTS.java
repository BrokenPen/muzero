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

package ai.enpasos.muzero.agent.slow.play;

import ai.djl.ndarray.NDArray;
import ai.enpasos.muzero.MuZeroConfig;
import ai.enpasos.muzero.agent.fast.model.Network;
import ai.enpasos.muzero.agent.fast.model.NetworkIO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.analysis.function.Min;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.environments.EnvironmentBaseBoardGames.render;

@Slf4j
public class MCTS {
    private final MuZeroConfig config;

    private final @NotNull RandomGenerator rng;

    public MCTS(MuZeroConfig config) {
        this.config = config;
        this.rng = new Well19937c();
    }


    public static void backpropagate(@NotNull List<Node> searchPath, double value, Player toPlay, double discount, @NotNull MinMaxStats minMaxStats, MuZeroConfig config) {
        for (int i = searchPath.size() - 1; i >= 0; i--) {
            Node node = searchPath.get(i);
            if (node.getToPlay() == toPlay) {
                node.setValueSum(node.getValueSum() + value);
            } else {
                node.setValueSum(node.getValueSum() - value);
            }
            node.setVisitCount(node.getVisitCount() + 1);
            minMaxStats.update(node.valueScore(minMaxStats, config));
            value = node.reward + discount * value;
        }
    }


//    private void printUCBScores(@NotNull MuZeroConfig config, @NotNull List<Node> searchPath, @NotNull MinMaxStats minMaxStats) {
//        for (Node node : searchPath) {
//            printUCBScores(config, node, minMaxStats);
//        }
//    }

//    private void printUCBScores(@NotNull MuZeroConfig config, @NotNull Node node, @NotNull MinMaxStats minMaxStats) {
//        String[][] values = new String[config.getBoardHeight()][config.getBoardWidth()];
//        for (int i = 0; i < config.getActionSpaceSize(); i++) {
//            Action action = new Action(config, i);
//            Node child = node.getChildren().get(action);
//            double score;
//            if (child != null) {
//                score = ucbScore(node, child, minMaxStats);
//
//                values[Action.getRow(config, i)][Action.getCol(config, i)] =
//                        String.format("%3d", Math.round(100.0 * score));
//            }
//        }
//
//        System.out.println(render(config, values));
//    }

    public void run(@NotNull Node root, @NotNull ActionHistory actionHistory, @NotNull Network network,
                    Duration inferenceDuration, @NotNull List<NDArray> actionSpaceOnDevice) {


        runParallel(List.of(root), List.of(actionHistory), network,
                inferenceDuration, actionSpaceOnDevice);
    }

    public void runParallel(@NotNull List<Node> rootList, @NotNull List<ActionHistory> actionHistoryList, @NotNull Network network,
                            @Nullable Duration inferenceDuration, @NotNull List<NDArray> actionSpaceOnDevice) {


        // TODO better would be a simulation object that encapsulates the info that is spread over many Lists
        List<MinMaxStats> minMaxStatsList = IntStream.rangeClosed(1, rootList.size())
                .mapToObj(i -> new MinMaxStats(config.getKnownBounds()))
                .collect(Collectors.toList());


        for (int i = 0; i < config.getNumSimulations(); i++) { // in range(config.num_simulations):

            List<ActionHistory> historyList = actionHistoryList.stream().map(ActionHistory::clone).collect(Collectors.toList());

            List<Node> nodeList = new ArrayList<>(rootList);


            List<List<Node>> searchPathList = IntStream.range(0, rootList.size())
                    .mapToObj(nL -> new ArrayList<Node>())
                    .collect(Collectors.toList());
            for (int g = 0; g < nodeList.size(); g++) {
                Node node = nodeList.get(g);
                List<Node> searchPath = searchPathList.get(g);
                MinMaxStats minMaxStats = minMaxStatsList.get(g);
                ActionHistory history = historyList.get(g);
                searchPath.add(node);
                while (node.expanded()) {
                    Map.Entry<Action, Node> actionNodeEntry = selectChild(node, minMaxStats);
                    Action action = actionNodeEntry.getKey();
                    history.addAction(action);
                    node = actionNodeEntry.getValue();
                    nodeList.set(g, node);
                    node.setAction(action);// for debugging
                    searchPath.add(node);
                }
            }


            List<NDArray> hiddenStateList = searchPathList.stream().map(searchPath -> {
                Node parent = searchPath.get(searchPath.size() - 2);
                return parent.hiddenState;
            }).collect(Collectors.toList());


            List<Action> lastActions = historyList.stream().map(ActionHistory::lastAction).collect(Collectors.toList());
            List<NDArray> actionList = lastActions.stream().map(action -> {
                return actionSpaceOnDevice.get(action.getIndex());
                //   return a.duplicate();
            }).collect(Collectors.toList());

//                // Inside the search tree we use the dynamics function to obtain the next
//                // hidden state given an action and the previous hidden state.
//                Node parent = searchPath.get(searchPath.size() - 2);
//
//
            if (inferenceDuration != null) inferenceDuration.value -= System.currentTimeMillis();
            List<NetworkIO> networkOutputList = network.recurrentInferenceListDirect(hiddenStateList,
                    actionList
            );
            if (inferenceDuration != null) inferenceDuration.value += System.currentTimeMillis();
//
            for (int g = 0; g < nodeList.size(); g++) {
                ActionHistory history = historyList.get(g);
                Node node = nodeList.get(g);
                NetworkIO networkOutput = Objects.requireNonNull(networkOutputList).get(g);
                List<Node> searchPath = searchPathList.get(g);
                MinMaxStats minMaxStats = minMaxStatsList.get(g);

                expandNode(node, history.toPlay(), history.actionSpace(config), networkOutput, false);

                backpropagate(searchPath, networkOutput.getValue(), history.toPlay(), config.getDiscount(), minMaxStats, config);

                // System.out.println(i + ": " + searchPathToString(searchPath, false));

            }


        }


        clean(rootList);
    }

    private void clean(@NotNull List<Node> rootList) {
        rootList.forEach(
                this::clean
        );
    }

    private void clean(@NotNull Node node) {
        if (node.getHiddenState() != null) {
            node.getHiddenState().close();
            node.setHiddenState(null);
        }
        node.getChildren().entrySet().forEach(
                e -> {
                    Node child = e.getValue();
                    clean(child);
                }
        );
    }

    private @NotNull String searchPathToString(@NotNull List<Node> searchPath, boolean withValue, MinMaxStats minMaxStats) {
        StringBuffer buf = new StringBuffer();
        searchPath.forEach(
                n -> {
                    if (n.getAction() != null) {
                        buf.append(", ")
                                .append(n.getAction().getIndex());
                        if (withValue) {
                            buf.append("(")
                                    .append(n.valueScore(minMaxStats, config))
                                    .append(")");
                        }
                    } else {
                        buf.append("root");
                    }
                }
        );
        return buf.toString();
    }


    public void expandNode(@NotNull Node node, Player toPlay, @NotNull List<Action> actions, NetworkIO networkOutput,
                           boolean fastRuleLearning) {
        node.toPlay = toPlay;
        if (!fastRuleLearning) {
            node.hiddenState = networkOutput.getHiddenState();
            node.reward = networkOutput.getReward();
        }
        if (fastRuleLearning) {
            double p = 1d / actions.size();
            for (Action action : actions) {
                node.children.put(action, new Node(p));  // p/policySum = probability that this action is chosen
            }
        } else {
//            actions.stream().forEach(
//                    a -> {
//                        if (a.getIndex() == 82) {
//                            int i = 42;
//                        }
//                    }
//            );
            Map<Action, Float> policy = actions.stream()
                    .collect(Collectors.toMap(a -> a, a -> networkOutput.getPolicyValues()[a.getIndex()]));

            double policySum = policy.values().stream()
                    .mapToDouble(Double::valueOf)
                    .sum();
            for (Map.Entry<Action, Float> e : policy.entrySet()) {
                Action action = e.getKey();
                Float p = e.getValue();
                node.children.put(action, new Node(p / policySum));  // p/policySum = probability that this action is chosen
            }








        }
    }
//    public void expandNodeParallel (List<Node> nodeList, List<Player> toPlayList, List<List<Action>> actionsList, NetworkIO networkOutput,
//                            boolean fastRuleLearning){
//
//        for (int i = 0; i < nodeList.size(); i++) {
//            nodeList.get(i).toPlay = toPlayList
//        }
//        node.toPlay = toPlay;
//        if (!fastRuleLearning) {
//            node.hiddenState = networkOutput.getHiddenState();
//            node.reward = networkOutput.getReward();
//        }
//
//
//
//        if (fastRuleLearning) {
//            double p = 1d / actions.size();
//            for (Action action : actions) {
//                node.children.put(action, new Node(p));  // p/policySum = probability that this action is chosen
//            }
//        } else {
//            Map<Action, Float> policy = actions.stream()
//                    .collect(Collectors.toMap(a -> a, a -> networkOutput.getPolicyValues()[a.getIndex()]));
//
//
//            //
//            //
//
//            double policySum = policy.values().stream()
//                    .mapToDouble(Double::valueOf)
//                    .sum();
//            for (Map.Entry<Action, Float> e : policy.entrySet()) {
//                Action action = e.getKey();
//                Float p = e.getValue();
//                node.children.put(action, new Node(p / policySum));  // p/policySum = probability that this action is chosen
//            }
//        }
//    }


    public Map.@NotNull Entry<Action, Node> selectChild(@NotNull Node node, MinMaxStats minMaxStats) {

        Action action = selectAction(node, minMaxStats);

        return Map.entry(action, node.children.get(action));

    }

    public  Action selectAction(@NotNull Node node, MinMaxStats minMaxStats) {
        List<Pair<Action, Double>> distributionInput = getDistributionInput(node, config, minMaxStats);

        Action action = selectActionByDrawingFromDistribution(distributionInput);
        return action;
    }

    public static List<Pair<Action, Double>> getDistributionInput(@NotNull Node node, MuZeroConfig config, MinMaxStats minMaxStats) {

        List<Map.Entry<Action, Node>> list = new ArrayList<>(node.children.entrySet());
        List<Pair<Action, Double>> distributionInput;
        if (node.getVisitCount() != 0) {
            double multiplierLambda = multiplierLambda(node, config);

            double alphaMin = list.stream()
                    .mapToDouble(an -> {
                        Node child = an.getValue();
                        return child.valueScore(minMaxStats, config) + multiplierLambda * child.getPrior();
                    })
                    .max().getAsDouble();
            double alphaMax = list.stream()
                    .mapToDouble(an -> {
                        Node child = an.getValue();
                        return child.valueScore(minMaxStats, config);
                    })
                    .max().getAsDouble() + multiplierLambda;

            double alpha = calcAlpha(node, list, multiplierLambda, alphaMin, alphaMax, config, minMaxStats);


           distributionInput =
                    list.stream()
                            .map(e -> Pair.create(e.getKey(), optPolicy(multiplierLambda, alpha, e.getValue(), minMaxStats, config)))
                            .collect(Collectors.toList());

        } else {

            double sum = list.stream()
                    .mapToDouble(e -> e.getValue().getPrior())
                    .sum();
            distributionInput =
                    list.stream()
                            .map(e -> Pair.create(e.getKey(), e.getValue().getPrior() / sum))
                            .collect(Collectors.toList());

        }
        return distributionInput;
    }

    private static double calcAlpha(Node node, List<Map.Entry<Action, Node>> list, double multiplierLambda, double alphaMin, double alphaMax, MuZeroConfig config, MinMaxStats minMaxStats) {
        // dichotomic search
        double optPolicySum = 0d;
        double alpha = 0d;
        double epsilon = 0.000000001d;
        do {
            alpha = (alphaMax + alphaMin) / 2d;
            optPolicySum = optPolicySum(list, multiplierLambda, alpha, minMaxStats, config);

            if (optPolicySum > 1d) {
                alphaMin = alpha;
            } else {
                alphaMax = alpha;
            }

          //  System.out.println(optPolicySum);
        } while ( FastMath.abs(optPolicySum - 1d) > epsilon);

        return alpha;
    }

    private static double optPolicySum(List<Map.Entry<Action, Node>> list, double multiplierLambda, double alpha, MinMaxStats minMaxStats, MuZeroConfig config) {
        return list.stream()
                   .mapToDouble(e -> {
                       Node child = e.getValue();
                       return optPolicy(multiplierLambda, alpha, child, minMaxStats, config);
                   })
                   .sum();
    }

    private static double optPolicy(double multiplierLambda, double alpha, Node child, MinMaxStats minMaxStats, MuZeroConfig config) {
        double optPolicy;
        optPolicy = multiplierLambda * child.prior / (alpha - child.valueScore(minMaxStats, config));
        return optPolicy;
    }


    // from "MCTS as regularized policy optimization", equation 4
    public static double multiplierLambda(@NotNull Node parent, MuZeroConfig config) {
        return c(parent, config) * Math.sqrt(parent.getVisitCount()) / (parent.getVisitCount() + config.getActionSpaceSize());
    }


    private static double c(@NotNull Node parent, MuZeroConfig config) {
        double pbC;
        pbC = Math.log((parent.getVisitCount() + config.getPbCBase() + 1d) / config.getPbCBase()) + config.getPbCInit();
        return pbC;
    }


//    public Action selectActionByDrawingFromDistribution(int numMoves, @NotNull Node node, Network network) {
//
//        List<Pair<Action, Double>> distributionInput = getActionDistributionInput(numMoves, node, network);
//
//        return selectActionByDrawingFromDistribution(distributionInput);
//    }

    public Action selectActionByDrawingFromDistribution(List<Pair<Action, Double>> distributionInput) {
        EnumeratedDistribution<Action> distribution = null;
        try {
            distribution = new EnumeratedDistribution<>(rng, distributionInput);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return distribution.sample();
    }

//    public Action selectActionByMaxFromDistribution(int numMoves, @NotNull Node node, Network network) {
//
//        List<Pair<Action, Double>> distributionInput = getActionDistributionInput(numMoves, node, network);
//        return distributionInput.stream().max(Comparator.comparing(Pair::getValue)).get().getKey();
//
//    }

//    private List<Pair<Action, Double>> getActionDistributionInput(int numMoves, @NotNull Node node, @Nullable Network network) {
//        int numTrainingSteps = network != null ? network.trainingSteps() : 1;
//        double t = config.getVisitSoftmaxTemperatureFn().apply(numMoves, numTrainingSteps);
//        double td = 1.0 / t;
//
//        double sum = node.getChildren().values().stream().mapToDouble(v -> Math.pow(v.getVisitCount(), td)).sum();
//
//        return node.getChildren().entrySet().stream()
//                .map(e -> {
//                    Action action = e.getKey();
//                    double v = Math.pow(e.getValue().getVisitCount(), td) / sum;
//                    return new Pair<Action, Double>(action, v);
//                })
//                .collect(Collectors.toList());
//    }

    private double visitSoftmaxTemperature(int numMoves, int numTrainingSteps) {
        return config.getVisitSoftmaxTemperatureFn().apply(numMoves, numTrainingSteps);
    }

}
