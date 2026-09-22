package com.abognaf.matchinginventory;

public class Tank {
    public long id;
    public String name;
    public String fuelType;
    public double lengthCm;
    public double diameterCm;

    public Tank(long id, String name, String fuelType, double lengthCm, double diameterCm) {
        this.id = id;
        this.name = name;
        this.fuelType = fuelType;
        this.lengthCm = lengthCm;
        this.diameterCm = diameterCm;
    }
}