/*
 * Copyright (c) 2016 FRESCO (http://github.com/aicis/fresco).
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
package dk.alexandra.fresco.suite.tinytables.online;

import dk.alexandra.fresco.framework.ProtocolFactory;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.sce.configuration.ProtocolSuiteConfiguration;
import dk.alexandra.fresco.framework.sce.configuration.SCEConfiguration;
import dk.alexandra.fresco.framework.sce.resources.ResourcePool;
import dk.alexandra.fresco.framework.sce.resources.ResourcePoolImpl;
import dk.alexandra.fresco.suite.ProtocolSuite;
import java.io.File;
import java.security.SecureRandom;
import java.util.Properties;
import java.util.Random;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class TinyTablesConfiguration implements ProtocolSuiteConfiguration{

	private ProtocolFactory tinyTablesFactory;
	private File tinytablesfile;
	
	public TinyTablesConfiguration() {
		tinyTablesFactory = new TinyTablesFactory();
	}
	
	public static ProtocolSuiteConfiguration fromCmdLine(SCEConfiguration sceConf,
			CommandLine cmd) throws ParseException, IllegalArgumentException {
		
		Options options = new Options();
		
		TinyTablesConfiguration configuration = new TinyTablesConfiguration();
		
		String tinyTablesFileOption = "tinytables.file";
		
		options.addOption(Option
				.builder("D")
				.desc("The file where the generated TinyTables is leaded from.")
				.longOpt(tinyTablesFileOption).required(false).hasArgs().build());
		
		Properties p = cmd.getOptionProperties("D");
		
		String tinyTablesFilePath = p.getProperty(tinyTablesFileOption, "tinytables");
		File tinyTablesFile = new File(tinyTablesFilePath);
		configuration.setTinyTablesFile(tinyTablesFile);
		
		System.out.println("FromCmdArgs: " + configuration.getTinyTablesFile());
		
		return configuration;
	}
	
	public void setTinyTablesFile(File file) {
		this.tinytablesfile = file;
	}

	File getTinyTablesFile() {
		return this.tinytablesfile;
	}

  @Override
  public ProtocolSuite createProtocolSuite(int myPlayerId) {
    return new TinyTablesProtocolSuite(myPlayerId, this);
  }

	@Override
	public ResourcePool createResourcePool(int myId, int size, Network network,
			Random rand, SecureRandom secRand) {
		return new ResourcePoolImpl(myId, size, network, rand, secRand);
	}
}
