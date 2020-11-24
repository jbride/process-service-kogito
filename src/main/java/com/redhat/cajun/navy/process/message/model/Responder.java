package com.redhat.cajun.navy.process.message.model;

import java.math.BigDecimal;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Responder {

    private String id;

    private String name;

    private String phoneNumber;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private Integer boatCapacity;

    private Boolean medicalKit;

    private Boolean available;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public Integer getBoatCapacity() {
        return boatCapacity;
    }

    public Boolean isMedicalKit() {
        return medicalKit;
    }

    public Boolean isAvailable() {
        return available;
    }

    public static class Builder {

        private final Responder responder;

        public Builder(String id) {
            this.responder = new Responder();
            responder.id = id;
        }

        public Builder name(String name) {
            responder.name = name;
            return this;
        }

        public Builder phoneNumber(String phoneNumber) {
            responder.phoneNumber = phoneNumber;
            return this;
        }

        public Builder latitude(BigDecimal latitude) {
            responder.latitude = latitude;
            return this;
        }

        public Builder longitude(BigDecimal longitude) {
            responder.longitude = longitude;
            return this;
        }

        public Builder boatCapacity(Integer boatCapacity) {
            responder.boatCapacity = boatCapacity;
            return this;
        }

        public Builder medicalKit(Boolean medicalKit) {
            responder.medicalKit = medicalKit;
            return this;
        }

        public Builder available(Boolean available) {
            responder.available = available;
            return this;
        }

        public Responder build() {
            return responder;
        }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Responder responder = (Responder) o;

        if (!id.equals(responder.id)) return false;
        if (!Objects.equals(name, responder.name)) return false;
        if (!Objects.equals(phoneNumber, responder.phoneNumber))
            return false;
        if (!Objects.equals(latitude, responder.latitude)) return false;
        if (!Objects.equals(longitude, responder.longitude)) return false;
        if (!Objects.equals(boatCapacity, responder.boatCapacity))
            return false;
        if (!Objects.equals(medicalKit, responder.medicalKit)) return false;
        return Objects.equals(available, responder.available);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (phoneNumber != null ? phoneNumber.hashCode() : 0);
        result = 31 * result + (latitude != null ? latitude.hashCode() : 0);
        result = 31 * result + (longitude != null ? longitude.hashCode() : 0);
        result = 31 * result + (boatCapacity != null ? boatCapacity.hashCode() : 0);
        result = 31 * result + (medicalKit != null ? medicalKit.hashCode() : 0);
        result = 31 * result + (available != null ? available.hashCode() : 0);
        return result;
    }
}
