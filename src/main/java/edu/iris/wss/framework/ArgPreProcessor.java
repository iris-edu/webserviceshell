package edu.iris.wss.framework;

import java.util.List;

public abstract class ArgPreProcessor {
	public abstract void process(RequestInfo ri, List<String> cmd) throws Exception ;

}
