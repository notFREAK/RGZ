public interface RocketObserver {
    void onStageSeparation(int stageNumber);
    void onUpdateStatus(double currentMass, double speed, double x, double y, double angle, int remainingStages, double[] fuelMasses, double[] initialFuelMasses);
}
