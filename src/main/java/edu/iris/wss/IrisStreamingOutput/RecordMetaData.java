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