package com.monitor.dto;

import java.util.List;

/** Cuerpo opcional de POST /api/services/{service}/{operation}: {"params": [ ... ]}. */
public class ExecutionRequest {

    private List<Object> params;

    public List<Object> getParams() {
        return params;
    }

    public void setParams(List<Object> params) {
        this.params = params;
    }
}
