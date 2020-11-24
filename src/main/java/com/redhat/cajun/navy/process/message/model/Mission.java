package com.redhat.cajun.navy.process.message.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class Mission implements Serializable {

	private static final long serialVersionUID = 1L;

	public enum Status {
		CREATED, STARTED, PICKEDUP, DROPPED, ABORTED
	}

	private String incidentId;
	
	private String responderId;
	
	private Status status = Status.CREATED;

	private BigDecimal responderStartLat;

	private BigDecimal responderStartLong;

	private BigDecimal incidentLat;

	private BigDecimal incidentLong;

	private BigDecimal destinationLat;

	private BigDecimal destinationLong;
	
	private Long lastUpdate;

	public String getIncidentId() {
		return incidentId;
	}

	public void setIncidentId(String incidentId) {
		this.incidentId = incidentId;
	}

	public String getResponderId() {
		return responderId;
	}

	public void setResponderId(String responderId) {
		this.responderId = responderId;
	}

	public BigDecimal getResponderStartLat() {
		return responderStartLat;
	}

	public void setResponderStartLat(BigDecimal responderStartLat) {
		this.responderStartLat = responderStartLat;
	}

	public BigDecimal getResponderStartLong() {
		return responderStartLong;
	}

	public void setResponderStartLong(BigDecimal responderStartLong) {
		this.responderStartLong = responderStartLong;
	}

	public BigDecimal getIncidentLat() {
		return incidentLat;
	}

	public void setIncidentLat(BigDecimal incidentLat) {
		this.incidentLat = incidentLat;
	}

	public BigDecimal getIncidentLong() {
		return incidentLong;
	}

	public void setIncidentLong(BigDecimal incidentLong) {
		this.incidentLong = incidentLong;
	}

	public BigDecimal getDestinationLat() {
		return destinationLat;
	}

	public void setDestinationLat(BigDecimal destinationLat) {
		this.destinationLat = destinationLat;
	}

	public BigDecimal getDestinationLong() {
		return destinationLong;
	}

	public void setDestinationLong(BigDecimal destinationLong) {
		this.destinationLong = destinationLong;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

    public Long getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate( Long lastUpdate ) {
        this.lastUpdate = lastUpdate;
    }

    @Override
	public String toString() {
		return "Mission [incidentId=" + incidentId + ", responderId=" + responderId + ", incidentLat=" + incidentLat
				+ ", incidentLong=" + incidentLong +  ", responderLat=" + responderStartLat
				+ ", responderLong=" + responderStartLong + ", destinationLat=" + destinationLat
				+ ", destinationLong=" + destinationLong + ", status=" + status + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((incidentId == null) ? 0 : incidentId.hashCode());
		result = prime * result + ((lastUpdate == null) ? 0 : lastUpdate.hashCode());
		result = prime * result + ((responderId == null) ? 0 : responderId.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Mission other = (Mission) obj;
		if (incidentId == null) {
			if (other.incidentId != null)
				return false;
		} else if (!incidentId.equals(other.incidentId))
			return false;
		if (lastUpdate == null) {
			if (other.lastUpdate != null)
				return false;
		} else if (!lastUpdate.equals(other.lastUpdate))
			return false;
		if (responderId == null) {
			if (other.responderId != null)
				return false;
		} else if (!responderId.equals(other.responderId))
			return false;
		if (status != other.status)
			return false;
		return true;
	}
}