package edu.iris.wss.IrisStreamingOutput;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import edu.sc.seis.seisFile.mseed.Btime;

public class RecordMetaData {

	private Long size;
	private Date start;
	private Date end;

	SimpleDateFormat fmt = new SimpleDateFormat("yyyy,DDD,HH:mm:ss.SSSS");

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
		start = start.replaceAll("(\\.[0-9]{3})[0-9]*( [AP]M)", "$1$2");
		Date d = fmt.parse(start);
		if (this.start != null) {
			if (d.before(this.start)) {
				this.start = d;
			}
		} else {
			this.start = d;
		}
	}

	public Date getEnd() {
		return end;
	}

	public void setEnd(Date end) {
		this.end = end;
	}

	public void setIfLater(String end) throws ParseException {
		end = end.replaceAll("(\\.[0-9]{3})[0-9]*( [AP]M)", "$1$2");
		Date d = fmt.parse(end);

		if (this.end != null) {
			if (d.after(this.end)) {
				this.end = d;
			}
		} else {
			this.end = d;
		}
	}
}
