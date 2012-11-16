package edu.iris.wss.IrisStreamingOutput;

import org.apache.log4j.Logger;

public class TestSingleton implements IrisSingleton {
	public static final Logger logger = Logger.getLogger(TestSingleton.class);	

	public void init() {
		logger.info("initted");
	}
}
