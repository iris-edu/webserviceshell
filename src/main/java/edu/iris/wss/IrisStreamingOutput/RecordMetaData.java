package edu.iris.wss.IrisStreamingOutput;

import java.text.ParseException;
import java.util.Date;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class RecordMetaData {

	private Long size;
	private Date start;
	private Date end;
    
    // replaced with Joda data parsing because simple java parasing was adding
    // time related something like SSSS/1000 
    DateTimeFormatter jodaFmt = DateTimeFormat.forPattern("yyyy,DDD,HH:mm:ss.SSSS");

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
        LocalDateTime jodaDate = LocalDateTime.parse(start, jodaFmt);
        Date d = jodaDate.toDate();

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
        LocalDateTime jodaDate = LocalDateTime.parse(end, jodaFmt);
        Date d = jodaDate.toDate();
        
        if (this.end != null) {
            if (d.after(this.end)) {
            this.end = d;
            }
        } else {
            this.end = d;
        }
    }
}
