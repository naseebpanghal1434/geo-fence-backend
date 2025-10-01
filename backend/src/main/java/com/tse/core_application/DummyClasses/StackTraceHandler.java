package com.tse.core_application.DummyClasses;

import java.io.PrintWriter;
import java.io.StringWriter;

public class StackTraceHandler {

    public static String getAllStackTraces(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
