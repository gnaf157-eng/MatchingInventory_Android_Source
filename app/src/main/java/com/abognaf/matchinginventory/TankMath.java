package com.abognaf.matchinginventory;

public final class TankMath {
    private TankMath() {}

    // Horizontal cylinder. h is measured from the bottom to liquid level.
    public static double liters(double lengthCm, double diameterCm, double hCm) {
        if (lengthCm <= 0 || diameterCm <= 0) return 0;
        double r = diameterCm / 2.0;
        if (hCm <= 0) return 0;
        if (hCm >= diameterCm) {
            return Math.PI * r * r * lengthCm / 1000.0;
        }
        double term = Math.max(0.0, 2.0 * r * hCm - hCm * hCm);
        double area = r * r * Math.acos((r - hCm) / r)
                - (r - hCm) * Math.sqrt(term);
        return area * lengthCm / 1000.0; // cm3 -> liters
    }
}