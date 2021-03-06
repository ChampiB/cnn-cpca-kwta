package cnn.examples;

import cnn.data.ArrayPtrFactory;
import cnn.dataset.DataSet;
import cnn.dataset.DataSetsFactory;
import cnn.nodes.Node;
import cnn.nodes.conf.Conv2dConf;
import cnn.nodes.conf.DenseConf;
import cnn.nodes.NodesFactory;

import cnn.graphs.impl.NeuralNetwork;
import cnn.data.ArrayPtr;
import org.nd4j.linalg.factory.Nd4j;

import java.lang.reflect.Method;
import java.security.InvalidParameterException;
import java.util.concurrent.Callable;

class CPU_vs_GPU {

    private static double time(Callable c) {
        try {
            long start_time = System.nanoTime();
            c.call();
            long end_time = System.nanoTime();
            return (end_time - start_time) / 1e6;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return -1;
        }
    }

    static class Parameters {

        String testType;
        int testId;
        String deviceType;
        int iterations;

        private boolean t;
        private boolean d;
        private boolean n;

        Parameters(String[] args) {
            if (args.length != 3) throw new InvalidParameterException();
            for (String arg : args) parse(arg);
            if (!t || !d || !n) throw new InvalidParameterException();
        }

        private void parse(String arg) {
            String[] table = arg.split("=");
            if (table.length != 2) throw new InvalidParameterException();
            switch (table[0]) {
                case "-t":
                    t = true;
                    parseT(table[1]);
                    break;
                case "-d":
                    d = true;
                    parseD(table[1]);
                    break;
                case "-n":
                    n = true;
                    parseN(table[1]);
                    break;
                default:
                    throw new InvalidParameterException();
            }
        }

        void parseT(String arg) {
            String[] table = arg.split(":");
            if (table.length != 2)
                throw new InvalidParameterException();
            if (!table[0].equals("layer") && !table[0].equals("network"))
                throw new InvalidParameterException();
            testType = table[0];
            testId = Integer.valueOf(table[1]);
        }

        void parseD(String arg) {
            if (!arg.equals("cpu") && !arg.equals("gpu"))
                throw new InvalidParameterException();
            deviceType = arg;
        }

        void parseN(String arg) {
            iterations = Integer.valueOf(arg);
        }
    }

    private static void help() {
        System.out.println("NAME");
        System.out.println("\tCPU_vs_GPU - run benchmarks between CPU and GPU");
        System.out.println();
        System.out.println("SYNOPSIS");
        System.out.println("\t./CPU_vs_GPU.java -t=test_type:id -d=device_type -n=iterations");
        System.out.println();
        System.out.println("DESCRIPTION");
        System.out.println();
        System.out.println("Run the examples specified by the arguments.");
        System.out.println();
        System.out.println("All arguments are mandatory:");
        System.out.println();
        System.out.println("\t-t=test_type:id");
        System.out.println("\t\tspecify the type of test (i.e. layer or network) and the test id.");
        System.out.println();
        System.out.println("\t\t#---------#-----------------------#");
        System.out.println("\t\t| test id | layer type            |");
        System.out.println("\t\t#---------#-----------------------#");
        System.out.println("\t\t|       1 | Conv2d                |");
        System.out.println("\t\t|       2 | Conv2d + kWTA         |");
        System.out.println("\t\t|       3 | Conv2d + CPCA         |");
        System.out.println("\t\t|       4 | Conv2d + CPCA + kWTA  |");
        System.out.println("\t\t|       5 | Dense                 |");
        System.out.println("\t\t|       6 | MaxPooling2d          |");
        System.out.println("\t\t#---------#-----------------------#");
        System.out.println("\t\t Table 1: table of layer test id.");
        System.out.println();
        System.out.println("\t\t#---------#----------#-----------------------#");
        System.out.println("\t\t| test id | layer id | layer type            |");
        System.out.println("\t\t#---------#----------#-----------------------#");
        System.out.println("\t\t|       1 |        1 | Conv2d                |");
        System.out.println("\t\t|         |        2 | MaxPooling2d          |");
        System.out.println("\t\t|         |        3 | Flatten               |");
        System.out.println("\t\t|         |        4 | Dense                 |");
        System.out.println("\t\t#---------#----------#-----------------------#");
        System.out.println("\t\t|       2 |        1 | Conv2d + kWTA         |");
        System.out.println("\t\t|         |        2 | MaxPooling2d          |");
        System.out.println("\t\t|         |        3 | Flatten               |");
        System.out.println("\t\t|         |        4 | Dense                 |");
        System.out.println("\t\t#---------#----------#-----------------------#");
        System.out.println("\t\t|       3 |        1 | Conv2d + CPCA         |");
        System.out.println("\t\t|         |        2 | MaxPooling2d          |");
        System.out.println("\t\t|         |        3 | Flatten               |");
        System.out.println("\t\t|         |        4 | Dense                 |");
        System.out.println("\t\t#---------#----------#-----------------------#");
        System.out.println("\t\t|       4 |        1 | Conv2d + CPCA + kWTA  |");
        System.out.println("\t\t|         |        2 | MaxPooling2d          |");
        System.out.println("\t\t|         |        3 | Flatten               |");
        System.out.println("\t\t|         |        4 | Dense                 |");
        System.out.println("\t\t#---------#----------#-----------------------#");
        System.out.println("\t\t      Table 2: table of network test id.      ");
        System.out.println();
        System.out.println("\t-d=device_type");
        System.out.println("\t\tspecify the type of device, i.e. cpu or gpu.");
        System.out.println();
        System.out.println("\t-n=iterations");
        System.out.println("\t\tspecify the number of iterations to run, i.e. any positive integer.");
        System.out.println();
        System.out.println("EXAMPLE");
        System.out.println("\t./CPU_vs_GPU.java -t=layer:1 -d=cpu -n=100");
        System.out.println();
    }

    private static void benchmarkLayer(Node node, long[] shape, int nbIterations) {
        // Create input data.
        ArrayPtr x = ArrayPtrFactory.fromData(Nd4j.ones(shape));

        // Training phase.
        double t = time(() -> {
            for (int i = 0; i < nbIterations; i++) {
                ArrayPtr y = node.activation(true, x);
                node.update(0.01, y);
            }
            return true;
        });
        System.out.println("Training of node conducted in: " + t + "ms");
    }

    private static void layer1(Parameters param) {
        System.out.println("Node type: Conv2d.");
        Node node = NodesFactory.create("Conv2d", param.deviceType);
        benchmarkLayer(node, new long[]{10, 1, 28, 28}, param.iterations);
    }

    private static void layer2(Parameters param) {
        System.out.println("Node type: Conv2d with kWTA.");
        Node node = NodesFactory.create("Conv2d", param.deviceType, new Conv2dConf(3, 0));
        benchmarkLayer(node, new long[]{10, 1, 28, 28}, param.iterations);
    }

    private static void layer3(Parameters param) {
        System.out.println("Node type: Conv2d with CPCA.");
        Node node = NodesFactory.create("Conv2d", param.deviceType, new Conv2dConf(0, 0.01));
        benchmarkLayer(node, new long[]{10, 1, 28, 28}, param.iterations);
    }

    private static void layer4(Parameters param) {
        System.out.println("Node type: Conv2d with kWTA and CPCA.");
        Node node = NodesFactory.create("Conv2d", param.deviceType, new Conv2dConf(3, 0.01));
        benchmarkLayer(node, new long[]{10, 1, 28, 28}, param.iterations);
    }

    private static void layer5(Parameters param) {
        System.out.println("Node type: Dense.");
        Node node = NodesFactory.create("Dense", param.deviceType, new DenseConf(10));
        benchmarkLayer(node, new long[]{10, 784}, param.iterations);
    }

    private static void layer6(Parameters param) {
        System.out.println("Node type: MaxPooling2d.");
        Node node = NodesFactory.create("MaxPooling2d", param.deviceType);
        benchmarkLayer(node, new long[]{10, 1, 28, 28}, param.iterations);
    }

    private static void benchmarkNetwork(NeuralNetwork network, int nbIterations) {
        // Create data set and neural graphs.
        DataSet dataSet = DataSetsFactory.create("Mnist", 20);

        // Training phase.
        double t = time(() -> {
            network.fit(dataSet, 0.01, nbIterations, 100);
            return true;
        });
        System.out.println("Training of network conducted in: " + t + "ms");

        // Testing phase.
        network.evaluate(dataSet);
    }

    private static void network1(Parameters param) {
        System.out.println("Network: Conv2d -> MaxPooling2d -> Flatten -> Dense.");
        NodesFactory.forceImplementation(param.deviceType);
        NeuralNetwork network = new NeuralNetwork()
                .addLayer("Conv2d")
                .addLayer("MaxPooling2d")
                .addLayer("Flatten")
                .addLayer("Dense", new DenseConf(10));
        benchmarkNetwork(network, param.iterations);
    }

    private static void network2(Parameters param) {
        System.out.println("Network: Conv2d with kWTA -> MaxPooling2d -> Flatten -> Dense.");
        NodesFactory.forceImplementation(param.deviceType);
        NeuralNetwork network = new NeuralNetwork()
                .addLayer("Conv2d", new Conv2dConf(3, 0))
                .addLayer("MaxPooling2d")
                .addLayer("Flatten")
                .addLayer("Dense", new DenseConf(10));
        benchmarkNetwork(network, param.iterations);
    }

    private static void network3(Parameters param) {
        System.out.println("Network: Conv2d with CPCA -> MaxPooling2d -> Flatten -> Dense.");
        NodesFactory.forceImplementation(param.deviceType);
        NeuralNetwork network = new NeuralNetwork()
                .addLayer("Conv2d", new Conv2dConf(0, 0.01))
                .addLayer("MaxPooling2d")
                .addLayer("Flatten")
                .addLayer("Dense", new DenseConf(10));
        benchmarkNetwork(network, param.iterations);
    }

    private static void network4(Parameters param) {
        System.out.println("Network: Conv2d with kWTA and CPCA -> MaxPooling2d -> Flatten -> Dense.");
        NodesFactory.forceImplementation(param.deviceType);
        NeuralNetwork network = new NeuralNetwork()
                .addLayer("Conv2d", new Conv2dConf(3, 0.01))
                .addLayer("MaxPooling2d")
                .addLayer("Flatten")
                .addLayer("Dense", new DenseConf(10));
        benchmarkNetwork(network, param.iterations);
    }

    private static void run(Parameters param) {
        System.out.println("Start " + param.testType + ":" + param.testId + " on " + param.deviceType + " for " + param.iterations + " iterations.");
        try {
            String functionName = param.testType + param.testId;
            Method method = CPU_vs_GPU.class.getDeclaredMethod(functionName, Parameters.class);
            method.invoke(null, param);
        } catch (Exception e) {
            System.out.println("Benchmark failed.");
            System.exit(1);
        }
        System.out.println("Done.");
    }

    public static void main(String[] args) {
        try {
            Parameters param = new Parameters(args);
            run(param);
            System.exit(0);
        } catch (Exception e) {
            help();
            System.exit(1);
        }
    }
}
