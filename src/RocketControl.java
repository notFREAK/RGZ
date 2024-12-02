public interface RocketControl {
    void startSimulation();
    void stopSimulation();
    void setRocketParameters(double payloadMass, double[] stageMasses, double[] fuelMasses, double thrustPerKgFuel);
    void setCycleDelay(int delay);
    void setFuelConsumptionPerCycle(double fuelConsumption);
    void setAutopilotMode(RocketController.AutopilotMode mode);
}
