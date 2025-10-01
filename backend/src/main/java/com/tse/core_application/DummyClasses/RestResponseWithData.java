package com.tse.core_application.DummyClasses;

public class RestResponseWithData {
    private int status;
    private String message;
    private String timestamp;
    private Object data;

    public RestResponseWithData() {
    }

    public RestResponseWithData(int status, String message, String timestamp, Object data) {
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
        this.data = data;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
