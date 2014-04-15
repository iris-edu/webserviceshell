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

package edu.iris.wss.IrisStreamingOutput;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class RecordMetaData {

	private Long size;
	private Date start;
	private Date end;

	static SimpleDateFormat fmt = new SimpleDateFormat("yyyy,DDD,HH:mm:ss.SSS");

	public Long getSize() {
		return size;
	}

	public void setSize(Long size) {
		this.size = size;
	}

	public Date getStart() {
		return start;
	}

	public void setStart(Date start) {
		this.start = start;
	}

	public void setIfEarlier(String start) throws ParseException {
		if (start == null) {
			return;// for now
		}
		start = start.substring(0, 21);
		//System.out.println("Before: "+start);
		Date d = fmt.parse(start);
		if (this.start != null) {
			if (d.before(this.start)) {
				this.start = d;
			}
		} else {
			this.start = d;
		}
		//System.out.println("After: "+this.start);
	}

	public Date getEnd() {
		return end;
	}

	public void setEnd(Date end) {
		this.end = end;
	}

	public void setIfLater(String end) throws ParseException {
		if (end == null) {
			return;// for now
		}
		end = end.substring(0, 21);
		//System.out.println("Before: "+end);
		Date d = fmt.parse(end);

		if (this.end != null) {
			if (d.after(this.end)) {
				this.end = d;
			}
		} else {
			this.end = d;
		}
		//System.out.println("After: "+this.end);
	}

	public static void main(String[] args) {
		RecordMetaData rmd = new RecordMetaData();

		try {
            String input = "2011,036,17:24:50.9999";
			rmd.setIfEarlier(input);
            System.out.println("*** input: " + input + "  as Date obj, start: "
                + RecordMetaData.fmt.format(rmd.getStart()));
            
			rmd.setIfLater(input);
            System.out.println("*** input: " + input + "    as Date obj, end: "
                + RecordMetaData.fmt.format(rmd.getEnd()));

        } catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}