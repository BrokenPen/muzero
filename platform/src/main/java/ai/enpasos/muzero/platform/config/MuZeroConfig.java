package ai.enpasos.muzero.platform.config;

import ai.djl.Device;
import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.agent.rational.Action;
import ai.enpasos.muzero.platform.agent.rational.KnownBounds;
import ai.enpasos.muzero.platform.common.MuZeroException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Set;

import static ai.enpasos.muzero.platform.config.KnownBoundsType.FROM_VALUES;
import static ai.enpasos.muzero.platform.config.KnownBoundsType.MINUSONE_ONE;

@Component
@ConfigurationProperties("muzero")
@Data
@Slf4j
@SuppressWarnings("squid:S1104")
public class MuZeroConfig {
    public static final boolean HIDDEN_STATE_REMAIN_ON_GPU = false;
    public Map<GameType, Conf> games;
    private RunType run = RunType.NONE;
    private GameType activeGame;

    public Class<? extends Game> getGameClass() {
        try {
            return (Class<? extends Game>) Class.forName(getGameClassName());
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage());
            throw new MuZeroException(e);
        }
    }

    public Class<? extends Action> getActionClass() {
        try {
            return (Class<? extends Action>) Class.forName(getActionClassName());
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage());
            throw new MuZeroException(e);
        }
    }


    public KnownBounds getKnownBounds() {
        if (this.getKnownBoundsType() == MINUSONE_ONE) {
            return new KnownBounds(-1d, 1d);
        }
        if (this.getKnownBoundsType() == FROM_VALUES) {
            return new KnownBounds(this.getValues()[0], this.getValues()[this.getValues().length - 1]);
        }
        return new KnownBounds();
    }

    public Game newGame() {
        try {
            Constructor<?> constructor = this.getGameClass().getConstructor(MuZeroConfig.class);
            return (Game) constructor.newInstance(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Action newAction(int index) {
        Action action = this.newAction();
        action.setIndex(index);
        return action;
    }

    public Action newAction() {
        try {
            Constructor<?> constructor = this.getActionClass().getConstructor(MuZeroConfig.class);
            return (Action) constructor.newInstance(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public @NotNull String getGamesBasedir() {
        return getOutputDir() + "games";
    }


    public Device getInferenceDevice() {
        switch (this.getInferenceDeviceType()) {
            case CPU:
                return Device.cpu();
            case GPU:
            default:
                return Device.gpu();
        }
    }

    private Conf getConf() {
        return this.games.get(this.activeGame);
    }

    public double getKomi() {
        return getConf().komi;
    }

    public void setKomi(double komi) {
        getConf().setKomi(komi);
    }

    public double getMaxKomi() {
        return getConf().maxKomi;
    }

    public String getModelName() {
        return getConf().modelName;
    }

    public String getGameClassName() {
        return getConf().gameClassName;
    }

    public String getActionClassName() {
        return getConf().actionClassName;
    }

    public PlayerMode getPlayerMode() {
        return getConf().playerMode;
    }

    public ValueHeadType getValueHeadType() {
        return getConf().valueHeadType;
    }


    public boolean isNetworkWithRewardHead() {
        return getConf().networkWithRewardHead;
    }

    public SymmetryType getSymmetryType() {
        return getConf().symmetryType;
    }

    public String getNetworkBaseDir() {
        if (getConf().networkBaseDir != null) return getConf().networkBaseDir;
        return getConf().getOutputDir() + "networks";
    }

    public void setNetworkBaseDir(String networkBaseDir) {
        getConf().setNetworkBaseDir(networkBaseDir);
    }

    public boolean isWithRewardHead() {
        return getConf().withRewardHead;
    }

    public int getNumObservationLayers() {
        return getConf().numObservationLayers;
    }

    public int getNumActionLayers() {
        return getConf().numActionLayers;
    }

    public int getNumChannels() {
        return getConf().numChannels;
    }

    public int getNumBottleneckChannels() {
        return getConf().numBottleneckChannels;
    }

    public int getNumHiddenStateChannels() {
        return getConf().numHiddenStateChannels;
    }

    public int getNumResiduals() {
        return getConf().numResiduals;
    }

    public double[] getValues() {
        return getConf().values;
    }

    public double getValueSpan() {
        double[] vs = getConf().getValues();
        return vs[vs.length - 1] - vs[0];
    }

    public int getNumberOfTrainingSteps() {
        return getConf().numberOfTrainingSteps;
    }

    public int getNumberOfTrainingStepsPerEpoch() {
        return getConf().numberOfTrainingStepsPerEpoch;
    }

    public int getWindowValueSelfconsistencySize() {
        return getConf().windowValueSelfconsistencySize;
    }


    public int getBatchSize() {
        return getConf().batchSize;
    }

    public int getNumUnrollSteps() {
        return getConf().numUnrollSteps;
    }

    public int getTdSteps( ) {
        return getConf().getTrainingTypes().get(getConf().getTrainingTypeKey() ).getTdSteps();
    }

    public float getDiscount() {
        return getConf().discount;
    }


    public float getWeightDecay() {
        return getConf().weightDecay;
    }

    public float getValueLossWeight() {
        return getConf().valueLossWeight;
    }

    public float getLrInit() {
        return getConf().lrInit;
    }

//    public float getAlternativeActionsWeight() {
//        return getConf().alternativeActionsWeight;
//    }

    public boolean isAbsorbingStateDropToZero() {
        return getConf().absorbingStateDropToZero;
    }

    public int getSize() {
        return getConf().size;
    }

    public int getMaxMoves() {
        return getConf().maxMoves;
    }

    public int getBoardHeight() {
        return getConf().boardHeight;
    }

    public int getBoardWidth() {
        return getConf().boardWidth;
    }

    public int getActionSpaceSize() {
        return getConf().actionSpaceSize;
    }

    public int getNumberTrainingStepsOnStart() {
        return getConf().numberTrainingStepsOnStart;
    }

    public int getSurpriseCheckInterval() {
        return getConf().surpriseCheckInterval;
    }

    public double getRootDirichletAlpha( ) {
        return getConf().getTrainingTypes().get(getConf().getTrainingTypeKey()).rootDirichletAlpha;
    }

    public double getRootExplorationFraction( ) {
        return getConf().getTrainingTypes().get(getConf().getTrainingTypeKey()).rootExplorationFraction;
    }

    public double getVariableStartFraction() {
        return getConf().variableStartFraction;
    }

    public KnownBoundsType getKnownBoundsType() {
        return getConf().knownBoundsType;
    }

    public double getGumbelScale() {
        return getConf().gumbelScale;
    }

    public DeviceType getInferenceDeviceType() {
        return getConf().inferenceDeviceType;
    }

    // https://arxiv.org/pdf/1611.01144.pdf

    public void setInferenceDeviceType(DeviceType deviceType) {
        getConf().setInferenceDeviceType(deviceType);
    }

    public double getTemperatureRoot( ) {
        return getConf().getTrainingTypes().get(getConf().getTrainingTypeKey()).temperatureRoot;
    }

    public void setTemperatureRoot( double temperature) {
        getConf().getTrainingTypes().get(getConf().getTrainingTypeKey()).temperatureRoot = temperature;
    }

    public double getFractionOfAlternativeActionGames() {
        return getConf().fractionOfAlternativeActionGames;
    }

    public String getOutputDir() {
        return getConf().outputDir;
    }

    public void setOutputDir(String outputDir) {
        getConf().setOutputDir(outputDir);
    }

    public int getNumEpisodes() {
        return getConf().getTrainingTypes().get(getConf().getTrainingTypeKey()).numEpisodes;
    }

    public int getInitialGumbelM() {
        return getConf().initialGumbelM;
    }

    public int getNumSimulations( ) {
        return getConf().getTrainingTypes().get(getConf().getTrainingTypeKey()).numSimulations;
    }

    public void setNumSimulations( int numSimulations) {
        getConf().getTrainingTypes().get(getConf().getTrainingTypeKey()).setNumSimulations(numSimulations);
    }

    public int getNumParallelGamesPlayed() {
        return getConf().getTrainingTypes().get(getConf().getTrainingTypeKey()).numParallelGamesPlayed;
    }

    public int getBroadcastEveryN() {
        return getConf().broadcastEveryN;
    }


    public boolean isExtraValueTrainingOn() {
        return getConf().isExtraValueTrainingOn();
    }

    public boolean isSurpriseHandlingOn() {
        return getConf().isSurpriseHandlingOn();
    }

    public int getCVisit() {
        return getConf().cVisit;
    }

    public void setCVisit(int cVisit) {
        getConf().setCVisit(cVisit);
    }

    public long getMaxGameLiveTime() {
        return getConf().maxGameLiveTime;
    }

    public double getCScale() {
        return getConf().cScale;
    }


    public int getNumPurePolicyPlays() {
        return getConf().numPurePolicyPlays;
    }

    public FileType getGameBufferWritingFormat() {
        return getConf().gameBufferWritingFormat;
    }

    public void setGameBufferWritingFormat(FileType fileType) {
        getConf().setGameBufferWritingFormat(fileType);
    }



    public TrainingTypeKey getTrainingTypeKey() {
        return getConf().trainingTypeKey;
    }

    public void setTrainingTypeKey(TrainingTypeKey trainingTypeKey) {
        getConf().setTrainingTypeKey(trainingTypeKey);
    }


    public int getWindowSize(long gamesPlayed) {
        int w0 = getConf().windowSizeStart;
        if (!getConf().windowSizeIsDynamic) return w0;
        double a = getConf().windowSizeExponent;
        double b = getConf().windowSizeSlope;

        int w = (int) (w0 * (1.0 + b * (Math.pow((double)gamesPlayed / w0, a) - 1) / a));

        return Math.max(w0, w);

    }

    public Set<TrainingTypeKey> getTrainingTypeKeys() {
        return getConf().getTrainingTypes().keySet();
    }


    @Data
    public static class Conf {
        protected boolean surpriseHandlingOn;
        protected boolean extraValueTrainingOn;
        protected double komi;
        protected double maxKomi = Double.MAX_VALUE;
        protected String modelName;
        protected String gameClassName;
        protected String actionClassName;
        protected PlayerMode playerMode;
        protected ValueHeadType valueHeadType;
        protected boolean networkWithRewardHead;
        protected SymmetryType symmetryType;
        protected String networkBaseDir;
        protected boolean withRewardHead;
        protected int numObservationLayers;
        protected int numActionLayers;
        protected int numChannels;
        protected int broadcastEveryN;
        protected int numBottleneckChannels;
        protected int numHiddenStateChannels;
        protected int numResiduals;
        protected int numberOfTrainingSteps;
        protected int numberOfTrainingStepsPerEpoch;


        protected int windowSizeStart;
        protected boolean windowSizeIsDynamic = false;
        protected double windowSizeExponent;
        protected double windowSizeSlope;


        protected double fractionOfAlternativeActionGames;

        protected int windowValueSelfconsistencySize;
        protected int batchSize;
        protected int numUnrollSteps;

        protected float discount;
        protected float weightDecay;
        protected float valueLossWeight;
        protected float lrInit;

      //  protected float alternativeActionsWeight;
        protected boolean absorbingStateDropToZero;
        protected int size;
        protected int maxMoves;
        protected int boardHeight;
        protected int boardWidth;
        protected int actionSpaceSize;
        protected int numberTrainingStepsOnStart;

        protected int surpriseCheckInterval;
        protected KnownBoundsType knownBoundsType;


        protected double variableStartFraction;
        protected DeviceType inferenceDeviceType;
        protected String outputDir;


        protected FileType gameBufferWritingFormat = FileType.ZIPPED_PROTOCOL_BUFFERS;
        protected long maxGameLiveTime;

        int initialGumbelM;
        double gumbelScale = 1;
        int cVisit;
        double cScale;
        int numPurePolicyPlays;

        double[] values;


        public Map<TrainingTypeKey, TrainingType> trainingTypes;

        protected TrainingTypeKey trainingTypeKey;


        @Data
        public static class TrainingType {
            protected int numEpisodes;
            protected int numParallelGamesPlayed;

            protected int tdSteps;
            protected int numSimulations;
            protected double rootDirichletAlpha;
            protected double rootExplorationFraction;
            double temperatureRoot = 0.0;
        }
    }

}
