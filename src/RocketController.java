public class RocketController implements RocketControl {
    private final RocketModel model;
    private boolean settingsConfirmed = false;
    private boolean engineOn = true;

    public enum AutopilotMode { MANUAL, MAX_DISTANCE, STABLE_ORBIT }
    private AutopilotMode autopilotMode = AutopilotMode.MANUAL;

    public RocketController(RocketModel model) {
        this.model = model;
    }

    @Override
    public void startSimulation() {
        if (settingsConfirmed) {
            model.startSimulation();
        } else {
            throw new IllegalStateException("Настройки не были подтверждены.");
        }
    }

    @Override
    public void stopSimulation() {
        model.stopSimulation();
    }

    @Override
    public void setRocketParameters(double payloadMass, double[] stageMasses, double[] fuelMasses, double thrustPerKgFuel) {
        model.setRocketParameters(payloadMass, stageMasses, fuelMasses, thrustPerKgFuel);
    }

    @Override
    public void setCycleDelay(int delay) {
        model.setCycleDelay(delay);
    }

    @Override
    public void setFuelConsumptionPerCycle(double fuelConsumption) {
        model.setFuelConsumptionPerCycle(fuelConsumption);
    }

    @Override
    public void setAutopilotMode(AutopilotMode mode) {
        this.autopilotMode = mode;
        model.setAutopilotMode(mode);
    }

    public AutopilotMode getAutopilotMode() {
        return autopilotMode;
    }

    public RocketModel getModel() {
        return model;
    }

    public void setSettingsConfirmed(boolean confirmed) {
        this.settingsConfirmed = confirmed;
    }

    public boolean isSettingsConfirmed() {
        return settingsConfirmed;
    }

    public void toggleEngine() {
        engineOn = !engineOn;
        model.setEngineOn(engineOn);
    }

    public boolean isEngineOn() {
        return engineOn;
    }

}
