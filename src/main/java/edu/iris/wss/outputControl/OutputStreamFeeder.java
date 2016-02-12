/*******************************************************************************
 * Copyright (c) 2015 IRIS DMC supported by the National Science Foundation.
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

package edu.iris.wss.outputControl;

import java.io.IOException;
import java.io.OutputStream;
import org.apache.log4j.Logger;

/**
 * Sends data to an OutputStream on a separate thread.
 * The point of this class is to (1) Decouple reading and writing
 * so that they may happen simultaneously and (2) maintain a drip rate
 * to the OutputStream to maintain an HTTP/TCP connection. (2) Can happen
 * if the input read stalls for whatever reason.
 */
public class OutputStreamFeeder {

    private final OutputStream outputStream;
    private final int dripPeriod;
    private final Worker worker;
    private final Thread workerThread;

    private TransferBuffer transferBuffer;

    private boolean close = false;
    private boolean closed = false;

    private final Object syncObjStart = new Object();
    private final Object syncObj = new Object();
    private final Object writeAck = new Object();

    private Exception exception = null;

    private boolean started = false;

    Logger logger = Logger.getLogger(OutputStreamFeeder.class);
    

    public OutputStreamFeeder(OutputStream outputStream, int bufferSize, int retainSize, int dripSize, int dripPeriod) {

        this.outputStream = outputStream;

        transferBuffer = new TransferBuffer(bufferSize, retainSize, dripSize);

        this.dripPeriod = dripPeriod;

        // make the worker thread and start it.
        this.worker = new Worker();
        this.workerThread = new Thread(worker);
    }

    public void start() {

        if( started ) {
            logger.error("start called more than once??");
            return;
        }

        logger.debug("starting the worker thread");

        this.workerThread.start();

        // this block makes sure the thread has started execution before returning.
        synchronized(syncObjStart) {
            if( !started ) {
                try {syncObjStart.wait();} catch (InterruptedException e) {	}
            }
        }
    }

    public void write(byte[] srcBuffer, int srcLength) throws IOException {

        if( !started ) {
            logger.error("start was never called!");
            throw new IllegalStateException("start() has not been called!");
        }

        throwException();

        int srcPos = 0;

        int bytesRemaining = srcLength;

        while(true) {

            throwException();

            int nPushed = transferBuffer.push(srcBuffer, srcPos, bytesRemaining);

            bytesRemaining -= nPushed;

            srcPos += nPushed;

            synchronized(syncObj) {
                syncObj.notify();
            }

            if( bytesRemaining == 0 ) {
                break;
            }

            synchronized (writeAck) {
                try {
                    // loop waiting for room in the buffer
                    writeAck.wait(500);
                } catch (InterruptedException ex) {
                    exception = ex;
                    throw new RuntimeException("??", ex);
                }
            }
        }
    }

    public void close() throws IOException {

        logger.debug("close() called on OutputStreamFeeder");

        if( !started ) {
            logger.error("start was never called!");
            throw new IllegalStateException("start() has not been called!");
        }

        synchronized(syncObj) {
            close = true;
            syncObj.notify();
        }
        long giveupTime = System.currentTimeMillis() + 600000l;

        synchronized(writeAck) {
            while(!closed) {
                try {
                    writeAck.wait(5000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException("??", ex);
                }
            }
            if( System.currentTimeMillis() > giveupTime ) {
                logger.error("taking more than 10 minutes to close. Giving up!");
                throw new RuntimeException("taking more than 10 minutes to close. Giving up!");
            }
        }

        throwException();

    }
    
    
    public long getBytesWritten() {
        if( worker == null ) {
            return -1;
        } else {
            return worker.bytesWritten;
        }
    }

    private void throwException() throws IOException {
        if( exception == null ) {
            return;
        } else if( exception instanceof IOException ) {
            throw (IOException)exception;
        } else {
            throw new IOException(exception);
        }

    }


    private class Worker implements Runnable {
        
        long bytesWritten = 0;

        public void run() {

            // this block lets the start() method know that we're running.
            synchronized(syncObjStart) {
                started = true;
                syncObjStart.notifyAll();
            }

            try{
                logger.debug("OutputStreamFeeder worker thread run() called");
                _run();
            } catch(Exception e) {
                logger.error("run() barfed! This should have been caught elsewhere.", e);
                if( exception != null ) {
                    exception = e;
                }
            }
            
        }

        public void _run() {

            byte[] buff = new byte[transferBuffer.getBuffSize()];

            long nextFlushTime = 0;
            boolean dataSent = false;
            
            try {

                // loop until close and buffer is empty
                while (!(transferBuffer.isEmpty() && close)) {

                    if( !close && !transferBuffer.moreThanRetain()) {
                        synchronized (syncObj) {
                            if( !close ) { // just to be on the safe side, check again.
                                syncObj.wait(dripPeriod);
                            }
                        }
                    }

                    int writeLen = 0;

                    if( close ) {
                        writeLen = transferBuffer.popAll(buff);
                    } else {
                        writeLen = transferBuffer.popSome(buff);
                    }

                    if( writeLen > 0 ) {

//                        SleepTest.sleep("/tmp/sleep2", "write");

                        outputStream.write(buff, 0, writeLen);
                        
                        bytesWritten += writeLen;

                        dataSent = true;

                        // let the potentiall waiting write know that a write happened.

                        synchronized(writeAck) {
                            writeAck.notify();
                        }
                    }

// Thread.sleep(2000);

                    // Flush if we haven't done so for a while
                    if (System.currentTimeMillis() >= nextFlushTime && dataSent) {
                        logger.debug("outputStream.flush()");
                        outputStream.flush();
                        nextFlushTime = System.currentTimeMillis() + dripPeriod;
                    }

                } // end while
                
            } catch (InterruptedException ex) {
                logger.info(ex);
                exception = ex;
            } catch (IOException ex) {
                logger.info(ex);
                exception = ex;
            } finally {
                try{
                    // only close if data has been sent.
                    if( dataSent ) {
                        outputStream.close();
                    }
                } catch(Exception e) {
                }
                closed = true;
                try {
                    synchronized (writeAck) {
                        writeAck.notify();
                    }
                } catch (Exception e) {
                }
            }
        }
    }

}


