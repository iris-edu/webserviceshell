package edu.iris.wss.utils;

import org.apache.log4j.PatternLayout;


public class WssLog4JLayout extends PatternLayout {
    @Override  
    public String getHeader() {  
        return LoggerUtils.getUsageLogHeader() + "\n";
    } 
}
