package cnn.zoo.impl;

import cnn.graphs.Graph;
import cnn.graphs.impl.NeuralNetwork;
import cnn.graphs.impl.ResidualBlock;
import cnn.nodes.conf.Conv2dConf;
import cnn.nodes.conf.DenseConf;
import cnn.nodes.conf.Pooling2dConf;
import cnn.nodes.enumerations.ActivationType;

import static cnn.graphs.impl.ResidualBlock.ShortcutMethod.CONVOLUTION;
import static cnn.graphs.impl.ResidualBlock.ShortcutMethod.SAME_SIZE;

public class ResNet18 {
    /**
     * Create the ResNet18 model.
     * @param nbClasses the number of classes, i.e. number of outputs.
     * @return the computational graph.
     */
    public static Graph create(int nbClasses) {
        // Network's begin.
        NeuralNetwork neuralNetwork = new NeuralNetwork()
                .addLayer("Conv2d", new Conv2dConf(new int[]{64, 7, 7}, new int[]{2, 2}))
                .addLayer("MaxPooling2d", new Pooling2dConf(new int[]{3, 3}));
        // Residual blocks.
        for (int i = 0; i < 2; i++)
            neuralNetwork.addLayer(new ResidualBlock(
                    new int[][]{{64, 3, 3}, {64, 3, 3}},
                    new int[][]{{1, 1}, {1, 1}},
                    SAME_SIZE
            ));
        for (int i = 0; i < 2; i++)
            neuralNetwork.addLayer(new ResidualBlock(
                    new int[][]{{128, 3, 3}, {128, 3, 3}},
                    i == 0 ? new int[][]{{2, 2}, {1, 1}} : new int[][]{{1, 1}, {1, 1}},
                    i == 0 ? CONVOLUTION : SAME_SIZE
            ));
        for (int i = 0; i < 2; i++)
            neuralNetwork.addLayer(new ResidualBlock(
                    new int[][]{{256, 3, 3}, {256, 3, 3}},
                    i == 0 ? new int[][]{{2, 2}, {1, 1}} : new int[][]{{1, 1}, {1, 1}},
                    i == 0 ? CONVOLUTION : SAME_SIZE
            ));
        for (int i = 0; i < 2; i++)
            neuralNetwork.addLayer(new ResidualBlock(
                    new int[][]{{512, 3, 3}, {512, 3, 3}},
                    i == 0 ? new int[][]{{2, 2}, {1, 1}} : new int[][]{{1, 1}, {1, 1}},
                    i == 0 ? CONVOLUTION : SAME_SIZE
            ));
        // Network's end.
        neuralNetwork
                .addLayer("AvgPooling2d")
                .addLayer("Flatten")
                .addLayer("Dense", new DenseConf(nbClasses).setAf(ActivationType.SOFTMAX));
        return neuralNetwork;
    }
}