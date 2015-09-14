/*******************************************************************************
 * Copyright (c) 2013 IRIS DMC supported by the National Science Foundation.
 *  
 * This file is part of the Web Service Shell (WSS).
 *  
 * The WSS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * The WSS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * A copy of the GNU Lesser General Public License is available at
 * <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package edu.iris.wss.services;

import java.util.Timer;
import java.util.TimerTask;

public class SrvReschedulableTimer extends Timer {
	
	// Default delay for timeout timers: 30 seconds
	public final static int defaultDelayMsec = 30 * 1000;
	
	private Runnable task = null;
	private TimerTask timerTask;
	private int delayMsec = defaultDelayMsec;
	
	public SrvReschedulableTimer() {
		
	}
	
	public SrvReschedulableTimer(Integer timeout) {
		if (timeout != null) delayMsec = timeout;
	}
	
	public int getDelayMsec() {
		return delayMsec;
	}
	public void setDelayMsec(int i) {
		delayMsec = i;
	}
	
	public void schedule(Runnable runnable) {
		task = runnable;
		timerTask = new TimerTask() { public void run() { task.run(); }; };	        
	    this.schedule(timerTask, delayMsec);        
	  }

	  public void reschedule() throws Exception {
		  if (task == null) {
			  throw new Exception("Runnable not set.  Probable attempt to reschedule a task that has never been scheduled");
		  }
	    timerTask.cancel();
	    timerTask = new TimerTask() { public void run() { task.run(); }; };
	    this.schedule(timerTask, delayMsec);        
	  }

	  public void cancel() {
		  // Stop the timerTask and most importantly to avoid memory leaks,
		  // call cancel on the actual timer.
		  timerTask.cancel();
		  super.cancel();
		  super.purge();
	  }
}
