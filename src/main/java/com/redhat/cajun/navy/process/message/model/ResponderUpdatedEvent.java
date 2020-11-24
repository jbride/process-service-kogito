package com.redhat.cajun.navy.process.message.model;

public class ResponderUpdatedEvent {

    private String status;

    private String statusMessage;

    private Responder responder;

    public String getStatus() {
        return status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public Responder getResponder() {
        return responder;
    }
}
