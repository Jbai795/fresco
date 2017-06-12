/*
 * Copyright (c) 2015, 2016 FRESCO (http://github.com/aicis/fresco).
 *
 * This file is part of the FRESCO project.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * FRESCO uses SCAPI - http://crypto.biu.ac.il/SCAPI, Crypto++, Miracl, NTL,
 * and Bouncy Castle. Please see these projects for any further licensing issues.
 *******************************************************************************/
package dk.alexandra.fresco.framework.sce;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.MPCException;
import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.ProtocolEvaluator;
import dk.alexandra.fresco.framework.ProtocolFactory;
import dk.alexandra.fresco.framework.ProtocolProducer;
import dk.alexandra.fresco.framework.Reporter;
import dk.alexandra.fresco.framework.configuration.ConfigurationException;
import dk.alexandra.fresco.framework.configuration.NetworkConfiguration;
import dk.alexandra.fresco.framework.configuration.NetworkConfigurationImpl;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.network.NetworkingStrategy;
import dk.alexandra.fresco.framework.network.ScapiNetworkImpl;
import dk.alexandra.fresco.framework.sce.configuration.ProtocolSuiteConfiguration;
import dk.alexandra.fresco.framework.sce.configuration.SCEConfiguration;
import dk.alexandra.fresco.framework.sce.resources.ResourcePool;
import dk.alexandra.fresco.suite.ProtocolSuite;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * Secure Computation Engine - responsible for having the overview of things and
 * setting everything up, e.g., based on properties.
 *
 * @author Kasper Damgaard (.. and others)
 */
public class SecureComputationEngineImpl<ResourcePoolT extends ResourcePool> implements
    SecureComputationEngine<ResourcePoolT> {

  private final int myId;
  private ProtocolEvaluator<ResourcePoolT> evaluator;
  private ProtocolSuiteConfiguration<ResourcePoolT> protocolSuiteConfiguration;
  private ExecutorService executorService;
  private boolean setup;
  private ProtocolSuite<ResourcePoolT> protocolSuite;
  private static final AtomicInteger threadCounter = new AtomicInteger(1);

  public SecureComputationEngineImpl(
      ProtocolSuiteConfiguration<ResourcePoolT> protocolSuite,
      ProtocolEvaluator<ResourcePoolT> evaluator,
      Level logLevel,
      int myId) {
    this.protocolSuiteConfiguration = protocolSuite;

    this.setup = false;

    //setup the basic stuff, but do not initialize anything yet
    Reporter.init(logLevel);
    this.evaluator = evaluator;
    this.myId = myId;
  }

  private static Network getNetworkFromConfiguration(SCEConfiguration sceConf,
      int myId, Map<Integer, Party> parties) {
    int channelAmount = 1;
    NetworkConfiguration conf = new NetworkConfigurationImpl(myId, parties);
    return buildNetwork(conf, channelAmount, sceConf.getNetworkStrategy());
  }

  private static Network buildNetwork(NetworkConfiguration conf,
      int channelAmount, NetworkingStrategy networkStrat) {
    Network network;
    switch (networkStrat) {
      case KRYONET:
        // TODO[PSN]
        // This might work on mac?
//          network = new KryoNetNetwork();
        network = new ScapiNetworkImpl();
        break;
      case SCAPI:
        network = new ScapiNetworkImpl();
        break;
      default:
        throw new ConfigurationException("Unknown networking strategy " + networkStrat);
    }
    network.init(conf, channelAmount);
    return network;
  }

  public static <ResourcePoolT extends ResourcePool> ResourcePoolT createResourcePool(
      SCEConfiguration<ResourcePoolT> sceConf,
      ProtocolSuiteConfiguration<ResourcePoolT> protocolSuiteConfiguration) throws IOException {

    int myId = sceConf.getMyId();
    Map<Integer, Party> parties = sceConf.getParties();

    // Secure random by default.
    Random rand = new Random(0);
    SecureRandom secRand = new SecureRandom();

    Network network = getNetworkFromConfiguration(sceConf, myId, parties);
    network.connect(10000);

    return protocolSuiteConfiguration.createResourcePool(
        myId, parties.size(),
        network, rand, secRand);
  }

  @Override
  public void runApplication(Application application, ResourcePoolT sceNetwork) {
    try {
      startApplication(application, sceNetwork).get(10, TimeUnit.MINUTES);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException("Internal error in waiting", e);
    }
  }

  public Future<?> startApplication(Application application, ResourcePoolT resourcePool) {
    prepareEvaluator();
    return executorService.submit(() -> evalApplication(application, resourcePool));
  }

  private void prepareEvaluator() {
    try {
      setup();
      this.evaluator.setProtocolInvocation(this.protocolSuite);
    } catch (IOException e) {
      throw new MPCException(
          "Could not run application due to errors during setup: " + e.getMessage(), e);
    }
  }

  private void evalApplication(Application application,
      ResourcePoolT resourcePool) {
    Reporter.info("Running application: " + application + " using protocol suite: "
        + this.protocolSuite);
    try {
      ProtocolFactory protocolFactory = this.protocolSuite.init(resourcePool);
      ProtocolProducer prod = application.prepareApplication(protocolFactory);
      long then = System.currentTimeMillis();
      this.evaluator.eval(prod, resourcePool);
      long now = System.currentTimeMillis();
      long timeSpend = now - then;
      Reporter.info("Running the application " + application + " took " + timeSpend + " ms.");
    } catch (IOException e) {
      throw new MPCException(
          "Could not run application " + application + " due to errors", e);
    } finally {
      application.closeApplication();
    }
  }

  @Override
  public synchronized void setup() throws IOException {
    if (this.setup) {
      return;
    }
    this.executorService = Executors.newCachedThreadPool(r -> {
      Thread thread = new Thread(r, "SCE-" + threadCounter.getAndIncrement());
      thread.setDaemon(true);
      return thread;
    });
    this.protocolSuite = this.protocolSuiteConfiguration.createProtocolSuite(myId);
    this.setup = true;
  }

  @Override
  public void shutdownSCE() {
    this.executorService.shutdown();
    this.setup = false;
  }

}
