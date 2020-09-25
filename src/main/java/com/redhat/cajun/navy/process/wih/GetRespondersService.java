package com.redhat.cajun.navy.process.wih;

import java.math.BigDecimal;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import com.redhat.cajun.navy.rules.model.Responder;
import com.redhat.cajun.navy.rules.model.Responders;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class GetRespondersService {

	private Responders responders;

	public void onStart(@Observes @Priority(value = 1) StartupEvent ev) {
		double baseLat = 39.297380;
		double baseLong = -105.896259;
		responders = new Responders();
		for(int i = 1; i < 5; i++) {
			Responder rObj = new Responder();
			rObj.setId("responder"+i);
			rObj.setFullname("responder"+i);
			rObj.setBoatCapacity(i);
			rObj.setHasMedical(true);
			rObj.setLatitude(new BigDecimal(baseLat - i/10000));
			rObj.setLongitude(new BigDecimal(baseLong - i/10000));
			responders.add(rObj);
		}
	}

    public Responders getResponders() {
		return responders;
	}
    
}
