import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class RocketModel {

    public static final double EARTH_RADIUS = 6_371_000;
    public static final double GRAVITATIONAL_CONSTANT = 6.67430e-11;
    public static final double EARTH_MASS = 5.972e24;

    private double payloadMass;
    private double[] stageMasses;
    private double[] fuelMasses;
    private double[] initialFuelMasses;
    private double thrustPerKgFuel;

    private double currentMass;
    private double speed;
    private double vx;
    private double vy;
    private double x;
    private double y;

    private double rocketAngle = 90;
    private boolean engineOn = true;

    private int remainingStages;

    private int cycleDelay = 100;
    private double fuelConsumptionPerCycle = 0.01;
    private double deltaTime = cycleDelay / 1000.0;
    private double targetOrbitAltitude = 200000;

    private static final double MAX_ANGLE_CHANGE_RATE = 10.0;
    private RocketController.AutopilotMode autopilotMode = RocketController.AutopilotMode.MANUAL;

    private final List<RocketObserver> observers = new ArrayList<>();
    private boolean running = false;

    static {
        System.loadLibrary("RocketModelNative");
    }

    private boolean useNativeCode = false;

    public native double[] nativeUpdateRocketState(double currentMass, double vx, double vy, double x, double y, double thrust, double nx, double ny, double tx, double ty, double rocketAngle, double deltaTime);

    private native double[] nativeCalculateOrbitAngle(double x, double y, double vx, double vy, double speed, double targetOrbitAltitude);

    private Thread simulationThread;

    public void startSimulation() {
        resetSimulationVariables();
        if (simulationThread != null && simulationThread.isAlive()) {
            return;
        }

        simulationThread = new Thread(() -> {
            running = true;
            while (running && y >= 0) {
                long lastUpdateTime = System.nanoTime();
                updateRocketState();
                notifyObservers();

                long currentUpdateTime = System.nanoTime();
                double realDeltaTime = (currentUpdateTime - lastUpdateTime);
                try {
                    Thread.sleep(cycleDelay);
                } catch (InterruptedException e) {
                    running = false;
                    break;
                }
            }
            running = false;
        });
        simulationThread.start();
    }

    public void stopSimulation() {
        running = false;
        if (simulationThread != null) {
            simulationThread.interrupt();
            try {
                simulationThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void resetSimulationVariables() {
        x = 0;
        y = EARTH_RADIUS;
        speed = 0;
        vx = 0;
        vy = 0;
        currentMass = payloadMass + Arrays.stream(stageMasses).sum() + Arrays.stream(fuelMasses).sum();
        remainingStages = stageMasses.length;
        fuelMasses = initialFuelMasses.clone();
        running = false;
    }

    private void updateRocketState() {
        if (remainingStages == 0 && engineOn) {
            engineOn = false;
            setEngineOn(false);
        }

        int currentStage = remainingStages - 1;

        double thrust = 0;
        if (engineOn && remainingStages > 0) {
            double fuelNeeded = fuelConsumptionPerCycle;
            if (fuelMasses[currentStage] < fuelNeeded) {
                fuelNeeded = fuelMasses[currentStage];
            }

            fuelMasses[currentStage] -= fuelNeeded;

            thrust = fuelNeeded / deltaTime * thrustPerKgFuel;

            if (fuelMasses[currentStage] <= 0) {
                fuelMasses[currentStage] = 0;
                separateStage();
                return;
            }
        }
        currentMass = payloadMass;
        for (int i = 0; i < remainingStages; i++) {
            currentMass += stageMasses[i] + fuelMasses[i];
        }

        double r = Math.sqrt(x * x + y * y);
        double nx = x / r;
        double ny = y / r;

        double tx = -ny;
        double ty = nx;

        updateRocketAngle();

        if (useNativeCode) {
            double[] result = nativeUpdateRocketState(currentMass, vx, vy, x, y, thrust, nx, ny, tx, ty, rocketAngle, deltaTime);
            vx = result[0];
            vy = result[1];
            x = result[2];
            y = result[3];
            speed = result[4];
        } else {
            double angleRad = Math.toRadians(rocketAngle);
            double cosAngle = Math.cos(angleRad);
            double sinAngle = Math.sin(angleRad);

            double thrustX = thrust * (cosAngle * nx + sinAngle * tx);
            double thrustY = thrust * (cosAngle * ny + sinAngle * ty);

            double gravityMagnitude = GRAVITATIONAL_CONSTANT * EARTH_MASS / (r * r);
            double gx = -gravityMagnitude * nx;
            double gy = -gravityMagnitude * ny;

            double ax = (thrustX / currentMass) + gx;
            double ay = (thrustY / currentMass) + gy;

            vx += ax * deltaTime;
            vy += ay * deltaTime;

            x += vx * deltaTime;
            y += vy * deltaTime;

            speed = Math.sqrt(vx * vx + vy * vy);
        }

        double distanceToCenter = Math.sqrt(x * x + y * y);
        if (distanceToCenter <= EARTH_RADIUS) {
            nx = x / distanceToCenter;
            ny = y / distanceToCenter;
            x = nx * EARTH_RADIUS;
            y = ny * EARTH_RADIUS;

            vx = 0;
            vy = 0;
            running = false;
            return;
        }
    }

    public void addObserver(RocketObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(RocketObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers() {
        for (RocketObserver observer : observers) {
            observer.onUpdateStatus(currentMass, speed, x, y, rocketAngle, remainingStages, fuelMasses, initialFuelMasses);
        }
    }

    private void separateStage() {
        remainingStages--;
        for (RocketObserver observer : observers) {
            observer.onStageSeparation(remainingStages + 1);
        }
    }

    public void setEngineOn(boolean engineOn) {
        this.engineOn = engineOn;
    }

    private void updateRocketAngle() {
        switch (autopilotMode) {
            case STABLE_ORBIT:
                double targetAngle;
                if (useNativeCode) {
                    double[] result = nativeCalculateOrbitAngle(x, y, vx, vy, speed, targetOrbitAltitude);
                    targetAngle = result[0];
                    boolean newEngineOn = result[1] > 0.5;
                    if (engineOn != newEngineOn) {
                        engineOn = newEngineOn;
                        setEngineOn(engineOn);
                    }
                } else {
                    targetAngle = calculateOrbitAngle();
                }
                rocketAngle = approachTargetAngle(rocketAngle, targetAngle, deltaTime);
                break;
            case MAX_DISTANCE:
                rocketAngle = approachTargetAngle(rocketAngle, 45, deltaTime);
                break;
            case MANUAL:
            default:
                break;
        }
    }

    private double approachTargetAngle(double currentAngle, double targetAngle, double deltaTime) {
        double angleDifference = normalizeAngle(targetAngle - currentAngle);
        double maxAngleChange = MAX_ANGLE_CHANGE_RATE * deltaTime;
        if (Math.abs(angleDifference) > maxAngleChange) {
            currentAngle += Math.signum(angleDifference) * maxAngleChange;
        } else {
            currentAngle = targetAngle;
        }
        return normalizeAngle(currentAngle);
    }

    private double normalizeAngle(double angle) {
        angle = angle % 360;
        if (angle < -180) angle += 360;
        if (angle > 180) angle -= 360;
        return angle;
    }

    private double calculateOrbitAngle() {
        double r = Math.sqrt(x * x + y * y);
        double currentAltitude = r - EARTH_RADIUS;

        double initialAngle = 0;
        double finalAngle = 90;

        double gravityTurnStartAltitude = 0;

        double gravityMagnitude = GRAVITATIONAL_CONSTANT * EARTH_MASS / (r * r);
        double gy = gravityMagnitude * (y / r);
        double gravityTurnEndAltitude = targetOrbitAltitude;

        double EarthAngleRad = -Math.atan2(x, y);
        double EarthAngle = Math.toDegrees(EarthAngleRad);
        double SpeedAngle = -Math.atan2(vx, vy);
        double sinSpeedEarth = Math.cos(SpeedAngle - EarthAngleRad);
        double RocketMaxAltitude = speed * speed * sinSpeedEarth * sinSpeedEarth / (2 * gy) + currentAltitude;

        double targetAngle = finalAngle + EarthAngle;

        if (RocketMaxAltitude < gravityTurnStartAltitude) {
            targetAngle = initialAngle;
        } else if (RocketMaxAltitude < gravityTurnEndAltitude) {
            double ratio = (RocketMaxAltitude - gravityTurnStartAltitude) / (gravityTurnEndAltitude - gravityTurnStartAltitude);
            targetAngle = initialAngle + (finalAngle - initialAngle) * ratio + EarthAngle;
        } else {
            double mu = GRAVITATIONAL_CONSTANT * EARTH_MASS;
            double requiredOrbitVelocity = Math.sqrt(mu / r);
            double currentVelocity = speed * Math.sin(SpeedAngle - EarthAngleRad);
            double ratio = (RocketMaxAltitude - gravityTurnEndAltitude - gravityTurnStartAltitude) / (gravityTurnEndAltitude - gravityTurnStartAltitude);
            targetAngle = finalAngle * ratio + finalAngle + EarthAngle;
            if (currentVelocity >= requiredOrbitVelocity && currentVelocity <= requiredOrbitVelocity * 1.01) {
                engineOn = false;
                setEngineOn(false);
            } else {
                if (!engineOn) {
                    engineOn = true;
                    setEngineOn(true);
                }
            }
        }
        System.out.println(targetAngle - EarthAngle);
        return targetAngle;
    }

    public void setRocketParameters(double payloadMass, double[] stageMasses, double[] fuelMasses, double thrustPerKgFuel) {
        this.payloadMass = payloadMass;
        this.stageMasses = stageMasses;
        this.fuelMasses = fuelMasses;
        this.initialFuelMasses = fuelMasses.clone();
        this.thrustPerKgFuel = thrustPerKgFuel;
        this.remainingStages = stageMasses.length;
        this.currentMass = payloadMass + Arrays.stream(stageMasses).sum() + Arrays.stream(fuelMasses).sum();
        this.x = 0;
        this.y = EARTH_RADIUS;
        this.speed = 0;
        this.vx = 0;
        this.vy = 0;
        this.rocketAngle = 0;
    }

    public void setCycleDelay(int delay) {
        this.cycleDelay = delay;
        this.deltaTime = delay / 1000.0;
    }

    public void setFuelConsumptionPerCycle(double fuelConsumption) {
        this.fuelConsumptionPerCycle = fuelConsumption;
    }

    public void setRocketAngle(double angle) {
        if (autopilotMode == RocketController.AutopilotMode.MANUAL) {
            this.rocketAngle = angle;
        }
    }

    public void setAutopilotMode(RocketController.AutopilotMode mode) {
        this.autopilotMode = mode;
    }

    public double getDeltaTime() {
        return deltaTime;
    }

    public double getRocketAngle() {
        return rocketAngle;
    }

    public double getSpeedX() {
        return vx;
    }

    public double getSpeedY() {
        return vy;
    }

    public RocketController.AutopilotMode getAutopilotMode() {
        return autopilotMode;
    }

    public void setUseNativeCode(boolean useNativeCode) {
        this.useNativeCode = useNativeCode;
    }

    public void setTargetOrbitAltitude(double targetOrbitAltitude) {
        this.targetOrbitAltitude = targetOrbitAltitude;
    }

}
