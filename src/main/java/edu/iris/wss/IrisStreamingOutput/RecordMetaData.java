package edu.iris.wss.IrisStreamingOutput;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RecordMetaData {

	private Long size;
	private Date start;
	private Date end;
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy,DDD,HH:mm:ss.SSSS");

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

	public void setStart(String start) throws ParseException {
		Date d = sdf.parse(start);
		if (this.start != null) {
			if (d.after(this.start)) {
				return;
			}
		}
		this.start = d;
	}

	public Date getEnd() {
		return end;
	}

	public void setEnd(Date end) {
		this.end = end;
	}

	public void setEnd(String end) throws ParseException {
		Date d = sdf.parse(end);
		if (this.end != null) {
			if (d.before(this.end)) {
				return;
			}
		}
		this.end = d;
	}
}
