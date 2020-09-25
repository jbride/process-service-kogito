package com.redhat.cajun.navy.process.wih;

import java.math.BigDecimal;
import java.util.Random;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import com.redhat.cajun.navy.rules.model.Incident;
import com.redhat.cajun.navy.rules.model.IncidentPriority;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class GetIncidentPriorityService {

	private Random randomObj = null;

	public void onStart(@Observes @Priority(value = 1) StartupEvent ev) {
		randomObj = new Random(100);
	}

    public IncidentPriority getIncidentPriority(Incident incident) {

		IncidentPriority ipObj = new IncidentPriority();
		ipObj.setIncidentId(incident.getId());
		ipObj.setPriority(new BigDecimal(randomObj.nextDouble()));
		return ipObj;
	}
    
}
