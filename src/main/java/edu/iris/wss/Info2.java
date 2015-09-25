package edu.iris.wss;


import edu.iris.wss.IrisStreamingOutput.IrisStreamingOutput;
import edu.iris.wss.framework.FdsnStatus.Status;
import edu.iris.wss.framework.RequestInfo;
import edu.iris.wss.utils.WebUtils;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import org.apache.log4j.Logger;

public class Info2 extends IrisStreamingOutput {
	public static final Logger logger = Logger.getLogger(Info2.class);
	
    long totalBytesTransmitted = 0L;
    private Date startTime;
    
    private String errorString;
    private String myWssVersion;
    
	@Context 	ServletContext context;
    
	public Info2()  {
        System.out.println("***************&&& Info2 constr");
    }

    @Override
    public void write(OutputStream os) {
//    		OutputStreamWrapper outputStreamWrapper = null;
//
//		Writer writer = null;
//		try {
//			outputStreamWrapper = new OutputStreamWrapper(os);
//
//			if ("text".equalsIgnoreCase(this.outputType)) {
//				writer = new TextQueueStreamWriter(outputStreamWrapper,
//						this.ri.appConfig.getVersion());
System.out.println("***************&&& Info2 write wssVersion: " + myWssVersion);
        try {
            os.write(myWssVersion.getBytes());

            totalBytesTransmitted += myWssVersion.getBytes().length;
        } catch (IOException ex) {
            this.errorString = ex.toString();
            logAndThrowException(ri, Status.INTERNAL_SERVER_ERROR,
                  "error writing wssVersion" + ex.getMessage());
        } finally {
            long processingTime = (new Date()).getTime() - startTime.getTime();
            logger.info("Info2 write done:  Wrote " + totalBytesTransmitted + " bytes"
                    + "  processingTime: " + processingTime
                    + "  totalBytesTransmitted: " + totalBytesTransmitted
                    + "  usageLog: " + ri.appConfig.getUsageLog(WebUtils.getConfigFileBase(context)));

            logUsageMessage(ri, null, totalBytesTransmitted,
                  processingTime, null, Status.OK, null);
            try {
                os.flush();
                os.close();
            } catch (IOException ex) {
                logAndThrowException(ri, Status.INTERNAL_SERVER_ERROR,
                      "error flushing or closing outputstream" + ex.getMessage());
            }
		}

    
    }

    @Override
    public Status getResponse() {
        startTime = new Date();
        myWssVersion  = "Info2 dynamic string with wssversion: " + ri.appConfig.getWssVersion();
        System.out.println("***************&&& Info2 getResponse wssVersion: " + myWssVersion);
        return Status.OK;
    }

    @Override
    public String getErrorString() {
        errorString = "test error message";
        System.out.println("***************&&& Info2 getErrorString wssVersion: " + myWssVersion);
        return errorString;
    }

    @Override
    public void setRequestInfo(RequestInfo ri) {
        System.out.println("***************&&& Info2 setRequestInfo ri: " + ri);
		if (ri == null) {
			throw new NullPointerException("Invalid RequestInfo object: null");
		}
		this.ri = ri;
    }
}