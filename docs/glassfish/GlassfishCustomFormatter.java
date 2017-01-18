/*
 * Copyright (c) 2010, Oracle. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of Oracle nor the names of its contributors
 *   may be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package edu.iris.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/*
  originally from http://stackoverflow.com/questions/9609380/glassfish-3-how-do-you-change-the-default-logging-format
*/

public class GlassfishCustomFormatter extends Formatter{
    public static final String ISO_8601_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    private final String nl = System.getProperty("line.separator");
    private final DateFormat sdf = new SimpleDateFormat(ISO_8601_FORMAT);

    @Override
    public String format(LogRecord lr) {
        StringBuffer sb = new StringBuffer();

        // date and time
        Date dt = new Date();
        dt.setTime(lr.getMillis());
        sb.append(sdf.format(dt));
        sb.append(" ");

        // level (longest is "WARNING" = 7 chars, space fill for level output)
        String level = lr.getLevel().getName();
        int numSpaces = 7 - level.length();
        sb.append(level);
        for ( int i = 0 ; i < numSpaces + 1 ; i++ )
        {
            sb.append(" ");
        }

        sb.append("tid: ");
        sb.append(lr.getThreadID());
        sb.append(" ");
        if (lr.getThreadID() < 10 ) {
            sb.append(" ");
        }


        // class
        int lastSegment = lr.getSourceClassName().lastIndexOf(".") + 1;
        sb.append(lr.getSourceClassName().substring(lastSegment));
        sb.append(" - ");

        // message
        sb.append(formatMessage(lr));

        sb.append(nl);

        // optional stack trace
        if ( lr.getThrown() != null )
        {
            try
            {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                lr.getThrown().printStackTrace(pw);
                pw.close();
                sb.append(sw.toString());
            }
            catch ( Exception e )
            {
            }
        }

        return sb.toString();
    }
}
