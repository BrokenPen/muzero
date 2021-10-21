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

package ai.enpasos.muzero.platform.agent.fast.model.djl.blocks.dlowerlevel;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.AbstractBlock;
import ai.djl.training.ParameterStore;
import ai.djl.util.PairList;
import org.jetbrains.annotations.NotNull;

public class RescaleBlock extends AbstractBlock {

    private static final byte VERSION = 2;

    public RescaleBlock() {
        super(VERSION);
    }


    @Override
    protected @NotNull NDList forwardInternal(ParameterStore parameterStore, @NotNull NDList inputs, boolean training, PairList<String, Object> params) {
        NDArray current = inputs.head();
        // Scale to the range [0, 1]  (same range as the action input)


        // scale on each  ... a pytorch view or multi-dim support would be better
        Shape origShape = current.getShape();
        Shape shape2 = new Shape(origShape.get(0), origShape.get(1), origShape.get(2) * origShape.get(3));
        NDArray current2 = current.reshape(shape2);
        Shape shape3 = new Shape(origShape.get(0), origShape.get(1), 1, 1);
        NDArray min2 = current2.min(new int[]{2}, true).reshape(shape3);
        NDArray max2 = current2.max(new int[]{2}, true).reshape(shape3);


        // The following works on MXNet but is not implemented on PyTorch
//        NDArray min2 = current.min(new int[]{2, 3}, true);
//        NDArray max2 = current.max(new int[]{2, 3}, true);


        NDArray d = max2.sub(min2).maximum(1e-5);

        NDArray a = current.sub(min2);
        return new NDList(a.div(d));


    }

    @Override
    public Shape[] getOutputShapes(Shape[] inputs) {
        return inputs;
    }


}