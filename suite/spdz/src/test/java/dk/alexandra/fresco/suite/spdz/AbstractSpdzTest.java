package dk.alexandra.fresco.suite.spdz;

import static dk.alexandra.fresco.suite.spdz.configuration.PreprocessingStrategy.DUMMY;

import dk.alexandra.fresco.framework.ProtocolEvaluator;
import dk.alexandra.fresco.framework.TestThreadRunner;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.configuration.NetworkConfiguration;
import dk.alexandra.fresco.framework.configuration.TestConfiguration;
import dk.alexandra.fresco.framework.network.KryoNetNetwork;
import dk.alexandra.fresco.framework.sce.SecureComputationEngine;
import dk.alexandra.fresco.framework.sce.SecureComputationEngineImpl;
import dk.alexandra.fresco.framework.sce.evaluator.BatchEvaluationStrategy;
import dk.alexandra.fresco.framework.sce.evaluator.BatchedProtocolEvaluator;
import dk.alexandra.fresco.framework.sce.evaluator.EvaluationStrategy;
import dk.alexandra.fresco.framework.sce.resources.storage.FilebasedStreamedStorageImpl;
import dk.alexandra.fresco.framework.sce.resources.storage.InMemoryStorage;
import dk.alexandra.fresco.framework.util.HmacDrbg;
import dk.alexandra.fresco.logging.BatchEvaluationLoggingDecorator;
import dk.alexandra.fresco.logging.EvaluatorLoggingDecorator;
import dk.alexandra.fresco.logging.NetworkLoggingDecorator;
import dk.alexandra.fresco.logging.PerformanceLogger;
import dk.alexandra.fresco.logging.PerformanceLogger.Flag;
import dk.alexandra.fresco.suite.spdz.configuration.PreprocessingStrategy;
import dk.alexandra.fresco.suite.spdz.storage.DataSupplier;
import dk.alexandra.fresco.suite.spdz.storage.DataSupplierImpl;
import dk.alexandra.fresco.suite.spdz.storage.DummyDataSupplierImpl;
import dk.alexandra.fresco.suite.spdz.storage.SpdzStorage;
import dk.alexandra.fresco.suite.spdz.storage.SpdzStorageConstants;
import dk.alexandra.fresco.suite.spdz.storage.SpdzStorageImpl;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Abstract class which handles a lot of boiler plate testing code. This makes running a single test
 * using different parameters quite easy.
 */
public abstract class AbstractSpdzTest {

  protected void runTest(
      TestThreadRunner.TestThreadFactory<SpdzResourcePool, ProtocolBuilderNumeric> f,
      EvaluationStrategy evalStrategy,
      PreprocessingStrategy preProStrat, int noOfParties) throws Exception {
    runTest(f, evalStrategy, preProStrat, noOfParties, null);
  }

  protected void runTest(
      TestThreadRunner.TestThreadFactory<SpdzResourcePool, ProtocolBuilderNumeric> f,
      EvaluationStrategy evalStrategy,
      PreprocessingStrategy preProStrat, int noOfParties, EnumSet<Flag> performanceloggerFlags)
      throws Exception {
    List<Integer> ports = new ArrayList<>(noOfParties);
    for (int i = 1; i <= noOfParties; i++) {
      ports.add(9000 + i * (noOfParties - 1));
    }

    Map<Integer, NetworkConfiguration> netConf =
        TestConfiguration.getNetworkConfigurations(noOfParties, ports);
    Map<Integer, TestThreadRunner.TestThreadConfiguration<SpdzResourcePool, ProtocolBuilderNumeric>> conf =
        new HashMap<>();
    Map<Integer, List<PerformanceLogger>> pls = new HashMap<>();
    for (int playerId : netConf.keySet()) {
      pls.put(playerId, new ArrayList<>());
      SpdzProtocolSuite protocolSuite = new SpdzProtocolSuite(150);

      BatchEvaluationStrategy<SpdzResourcePool> batchEvalStrat = evalStrategy.getStrategy();

      if (performanceloggerFlags != null && performanceloggerFlags
          .contains(Flag.LOG_NATIVE_BATCH)) {
        batchEvalStrat = new BatchEvaluationLoggingDecorator<>(batchEvalStrat);
        pls.get(playerId).add((PerformanceLogger) batchEvalStrat);
      }
      ProtocolEvaluator<SpdzResourcePool, ProtocolBuilderNumeric> evaluator =
          new BatchedProtocolEvaluator<>(batchEvalStrat, protocolSuite);
      if (performanceloggerFlags != null && performanceloggerFlags.contains(Flag.LOG_EVALUATOR)) {
        evaluator = new EvaluatorLoggingDecorator<>(evaluator);
        pls.get(playerId).add((PerformanceLogger) evaluator);
      }
      
      SecureComputationEngine<SpdzResourcePool, ProtocolBuilderNumeric> sce =
          new SecureComputationEngineImpl<>(protocolSuite, evaluator);
      
      TestThreadRunner.TestThreadConfiguration<SpdzResourcePool, ProtocolBuilderNumeric> ttc =
          new TestThreadRunner.TestThreadConfiguration<>(
              sce,
              () -> createResourcePool(playerId, noOfParties, new Random(),
                  new SecureRandom(), preProStrat),
              () -> {
                KryoNetNetwork kryoNetwork = new KryoNetNetwork(netConf.get(playerId));
                if (performanceloggerFlags != null
                    && performanceloggerFlags.contains(Flag.LOG_NETWORK)) {
                  NetworkLoggingDecorator network = new NetworkLoggingDecorator(kryoNetwork);
                  pls.get(playerId).add(network);
                  return network;
                } else {
                  return kryoNetwork;
                }
              });
      conf.put(playerId, ttc);
    }
    TestThreadRunner.run(f, conf);
    for (Integer pId : pls.keySet()) {
      for (PerformanceLogger pl : pls.get(pId)) {
        pl.printPerformanceLog(pId);
      }
    }
  }

  private SpdzResourcePool createResourcePool(int myId, int size, Random rand,
      SecureRandom secRand, PreprocessingStrategy preproStrat) {
    DataSupplier supplier;
    if (preproStrat == DUMMY) {      
      supplier = new DummyDataSupplierImpl(myId, size);
    } else {
      // case STATIC:
      int noOfThreadsUsed = 1;        
      String storageName =
          SpdzStorageConstants.STORAGE_NAME_PREFIX + noOfThreadsUsed + "_" + myId + "_" + 0
          + "_";
      supplier = new DataSupplierImpl(new FilebasedStreamedStorageImpl(new InMemoryStorage()), storageName, size);                
    }
    SpdzStorage store = new SpdzStorageImpl(supplier);
    try {
      return new SpdzResourcePoolImpl(myId, size, new HmacDrbg(), store);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Your system does not have the necessary hash function avaiable.", e);
    }
  }
}
