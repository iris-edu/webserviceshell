/*******************************************************************************
 * Copyright (c) 2017 IRIS DMC supported by the National Science Foundation.
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

import org.apache.log4j.Logger;

public class TransferBuffer {

    private byte[] buffer;
    private int retainSize;
    private int dripSize;
    private int end;

    Logger logger = Logger.getLogger(TransferBuffer.class);

    /**
     * For debugging only.
     * @return
     */
    public synchronized String toString() {
        return "" + buffer.length + ", " + end + ", " + retainSize + ", " + dripSize ;
    }

    /**
     *
     * @param buffSize Size of the buffer
     * @param retainSize used by readSome
     * @param dripSize used by readSome
     */
    public TransferBuffer(int buffSize, int retainSize, int dripSize) {
        buffer = new byte[buffSize];
        this.retainSize = retainSize;
        this.dripSize = dripSize;
        end = 0;

    }

    public int getBuffSize() {
        return buffer.length;
    }

    public synchronized boolean isEmpty() {
        return end == 0;
    }

    /**
     * Returns true if bytes in buffer is greater than retain-size.
     * @return
     */
    public synchronized boolean moreThanRetain() {
        return end > retainSize;
    }

    public int getEnd() {
        return end;
    }


    /**
     * Appends bytes on to the buffer. Returns the number
     * of bytes appended.
     * @param srcBuffer where bytes come from
     * @param srcPos where in the srcBuffer to start append
     * @param len number of bytes to copy
     * @return number of bytes copied.
     */
    public synchronized int push(byte[] srcBuffer, int srcPos, int len) {


        // detect badness
        if( srcPos > srcBuffer.length ) {
            throw new RuntimeException( "assert failed: srcPos > srcBuffer.length: " + srcPos + ", " + srcBuffer.length);
        }
        if( len > (srcBuffer.length - srcPos)) {
            throw new RuntimeException( "assert failed: len > (srcBuffer.length - srcPos): " + len + ", "  + srcBuffer.length + ", " + srcPos);
        }

        int available =  buffer.length - end;

        if( available == 0 ) {
            return 0;
        }

        if( available < 0 ) {
             throw new RuntimeException("assert failed: available < 0: " + available + ", " + end + ", " + buffer.length);
        }

        int lenUse = len;
        if( lenUse > available ) {
            lenUse = available;
        }

//        System.arraycopy(src, srcPos, dest, destPos, length);

        System.arraycopy(srcBuffer, srcPos, buffer, end, lenUse);

        end += lenUse;
        if( end > buffer.length ) {
            throw new RuntimeException("assert failed: end > buffer.length: " + end + ", " + buffer.length);
        }

        return lenUse;
    }

    /**
     * Pops the head into the destBuffer.
     * If the buffer has more than the retain size,
     * it pops up to the retain size. If the buffer has
     * less than the retain size, it pops dripSize.
     * @param destBuffer
     * @return number of bytes offered into destBuffer.
     */
    public synchronized int popSome(byte[] destBuffer) {

        if( end < 0 ) {
            throw new RuntimeException("assert failed. end < 0: " +  end);
        }

        if( end > buffer.length ) {
            throw new RuntimeException("assert failed. end < buffer.length: " +  end + ", " + buffer.length);
        }

        int offer = 0;
        if (end > retainSize) {
            offer = end - retainSize;
        } else if (end > dripSize) {
            offer = dripSize;
            logger.debug("popSome(), dripping buffer: " + offer + " of " + end);
        } else if( end > 0 ){
            offer = end;
            logger.debug("popSome(), dripping end of buffer: " + offer + " of " + end);
        } else if( end == 0 ){
            logger.debug("popSome(), buffer empty");
            return 0;
        } else {
            throw new RuntimeException("assert failed. Should not have gotten here!");
        }

        if( offer > destBuffer.length ) {
            offer = destBuffer.length;
        }

        // System.arraycopy(src, srcPos, dest, destPos, length);

        // copy the offer amount into the destination buffer.
        System.arraycopy(buffer, 0, destBuffer, 0, offer);

        shiftBufferLeft(offer);

//        // now shift the buffer to the left by offer amount, unless at the end
//        if( offer < end ) {
//            System.arraycopy(buffer, offer, buffer, 0, end - offer);
////            this.shiftLeft(offer);
//        }
//        end -= offer;
//
//        if( end < 0 ) {
//            throw new RuntimeException("assert failed. end < 0: " +  end);
//        }


        return offer;
    }

    /**
     * Pops as many bytes as possible into the destBuffer.
     * @param destBuffer
     * @return returns the number of bytes put into the destBuffer.
     */
    public synchronized int popAll(byte[] destBuffer) {

        if( end < 0 ) {
            throw new RuntimeException("assert failed. end < 0: " +  end);
        }

        if( end > buffer.length ) {
            throw new RuntimeException("assert failed. end < buffer.length: " +  end + ", " + buffer.length);
        }

        if( end == 0 ) {
            return 0;
        }

        int offer = end;
        if( offer > destBuffer.length ) {
            offer = destBuffer.length;
        }

        // System.arraycopy(src, srcPos, dest, destPos, length);

        // copy the offer amount into the destination buffer.
        System.arraycopy(buffer, 0, destBuffer, 0, offer);


        shiftBufferLeft(offer);

//        // now shift the buffer to the left by offer amount, unless at the end
//        if( offer < end ) {
//            System.arraycopy(buffer, offer, buffer, 0, end - offer);
////            this.shiftLeft(offer);
//        }
//        end -= offer;
//
//        if( end < 0 ) {
//            throw new RuntimeException("assert failed. end < 0: " +  end);
//        }
        return offer;
    }

    private void shiftBufferLeft(int shift) {

        if( end < 0 ) {
            throw new RuntimeException("assert failed. end < 0: " +  end);
        } else if( shift > end ) {
            throw new RuntimeException("assert failed. shift > end: " + shift + ", " + end);
        } else if( shift > buffer.length ) {
            throw new RuntimeException("assert failed. shift > buffer.length: " + shift + ", " + buffer.length);
        } else if( end == shift ) {
            end = 0;
        } else {
            // System.arraycopy(src, srcPos, dest, destPos, length);
            System.arraycopy(buffer, shift, buffer, 0, end - shift);
            end -= shift;
        }

        if( end < 0 ) {
            throw new RuntimeException("assert failed. end < 0: " +  end);
        }
    }

}
