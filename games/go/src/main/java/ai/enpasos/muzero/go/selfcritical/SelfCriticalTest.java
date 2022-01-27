package ai.enpasos.muzero.go.selfcritical;


import ai.djl.Model;
import ai.djl.nn.Block;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Slf4j
@Component
public class SelfCriticalTest  {


    @Autowired
    private MuZeroConfig conf;

    public List<Integer> run(SelfCriticalDataSet dataSet ) {
        List<SelfCriticalGame> input = dataSet.data;
        String modelPath = "./mymodel";
        List<Integer> output = null;

        Block block = SelfCriticalBlock.newSelfCriticalBlock(dataSet.maxFullMoves);

        try (Model model = Model.newInstance("mlp")) {
            model.setBlock(block);
            model.load(Path.of(modelPath));
            var predictor = model.newPredictor(new SelfCriticalTranslator());
            output = predictor.predict(dataSet);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new MuZeroException(e);
        }
        return output;
    }
}