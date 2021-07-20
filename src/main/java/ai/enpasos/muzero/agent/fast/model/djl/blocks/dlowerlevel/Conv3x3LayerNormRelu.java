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

package ai.enpasos.muzero.agent.fast.model.djl.blocks.dlowerlevel;

import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Activation;
import ai.djl.nn.convolutional.Conv2d;
import ai.djl.nn.norm.BatchNorm;
import ai.djl.nn.norm.LayerNorm;
import lombok.Builder;
import org.jetbrains.annotations.NotNull;


public class Conv3x3LayerNormRelu extends MySequentialBlock {


    private Conv3x3LayerNormRelu() {
    }

    @Builder()
    public static @NotNull Conv3x3LayerNormRelu newConvLayerNormRelu(int channels) {
        Conv3x3LayerNormRelu instance = new Conv3x3LayerNormRelu();
        instance.add(
                Conv2d.builder()
                        .setFilters(channels)
                        .setKernelShape(new Shape(3, 3))
                        .optBias(false)
                        .optPadding(new Shape(1, 1))   // needed to keep in shape
                        .build())
                .add(LayerNorm.builder().build())
                .add(Activation::relu);
        return instance;
    }


}