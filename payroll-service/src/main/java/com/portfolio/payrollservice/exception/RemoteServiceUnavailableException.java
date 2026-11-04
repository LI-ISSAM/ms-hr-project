package com.portfolio.payrollservice.exception;

public class RemoteServiceUnavailableException extends RuntimeException {

    public RemoteServiceUnavailableException(String serviceName, Throwable cause) {
        super("Service unavailable: " + serviceName, cause);
    }
}