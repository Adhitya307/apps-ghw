package com.apps.bubbletilt;

public class BaseResponse {
    private boolean success;
    private String message;

    public BaseResponse() {}

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}