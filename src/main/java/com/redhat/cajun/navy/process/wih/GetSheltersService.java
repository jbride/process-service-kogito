package com.redhat.cajun.navy.process.wih;

import java.math.BigDecimal;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import com.redhat.cajun.navy.rules.model.Destination;
import com.redhat.cajun.navy.rules.model.Destinations;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class GetSheltersService {

	private Destinations destinations;

	public void onStart(@Observes @Priority(value = 1) StartupEvent ev) {
		double baseLat = 39.297380;
		double baseLong = -105.896259;
		destinations = new Destinations();
		for(int i = 1; i < 5; i++) {
			Destination dObj = new Destination();
			dObj.setLatitude(new BigDecimal(baseLat + (i/1000)));
			dObj.setLongitude(new BigDecimal(baseLong + (i/1000)));
			dObj.setName("shelter"+i);
			destinations.add(dObj);
		}
	}

    public Destinations getShelters() {

		return destinations;
	}
    
}
