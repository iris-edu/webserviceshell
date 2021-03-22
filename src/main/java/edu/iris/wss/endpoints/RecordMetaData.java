/*******************************************************************************
 * Copyright (c) 2018 IRIS DMC supported by the National Science Foundation.
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

package edu.iris.wss.endpoints;

import edu.iris.wss.framework.Util;
import edu.sc.seis.seisFile.mseed.Btime;
import java.text.SimpleDateFormat;


public class RecordMetaData {

	private Long size;
	private Btime start;
	private Btime end;

    public static final String SeisFileDataFormat = "yyyy,DDD,HH:mm:ss.SSS";

	public Long getSize() {
		return size;
	}

	public void setSize(Long size) {
		this.size = size;
	}

	public Btime getStart() {
		return start;
	}

	public void setStart(Btime start) {
		this.start = start;
	}

	public void setIfEarlier(Btime start) {
		if (start == null) {
			return;// for now
		}

		if (this.start != null) {
			if (start.before(this.start)) {
				this.start = start;
			}
		} else {
			this.start = start;
		}
	}

	public Btime getEnd() {
		return end;
	}

	public void setEnd(Btime end) {
		this.end = end;
	}

	public void setIfLater(Btime end) {
		if (end == null) {
			return;// for now
		}

		if (this.end != null) {
			if (end.after(this.end)) {
				this.end = end;
			}
		} else {
			this.end = end;
		}
	}

}
