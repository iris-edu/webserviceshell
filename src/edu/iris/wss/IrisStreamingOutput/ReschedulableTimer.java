package edu.iris.wss.IrisStreamingOutput;

import java.util.Timer;
import java.util.TimerTask;

public class ReschedulableTimer extends Timer {
	
	// Default delay for timeout timers: 30 seconds
	public final static int defaultDelayMsec = 30 * 1000;
	
	private Runnable task = null;
	private TimerTask timerTask;
	private int delayMsec = defaultDelayMsec;
	
	public ReschedulableTimer() {
		
	}
	
	public ReschedulableTimer(Integer timeout) {
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
