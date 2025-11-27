package com.example.app_rightpiezo;

import java.util.List;

public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private List<T> dataList;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public List<T> getDataList() {
        return dataList;
    }

    // Helper method to get data in either single or list form
    public List<T> getDataAsList() {
        if (dataList != null) {
            return dataList;
        } else if (data != null) {
            return java.util.Arrays.asList(data);
        } else {
            return java.util.Collections.emptyList();
        }
    }
}