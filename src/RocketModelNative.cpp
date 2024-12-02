#include <jni.h>
#include <math.h>

const double GRAVITATIONAL_CONSTANT = 6.67430e-11;
const double EARTH_MASS = 5.972e24;
const double EARTH_RADIUS = 6.371e6;

extern "C" {

JNIEXPORT jdoubleArray JNICALL Java_RocketModel_nativeUpdateRocketState(JNIEnv *env, jobject obj, jdouble currentMass, jdouble vx, jdouble vy, jdouble x, jdouble y, jdouble thrust, jdouble nx, jdouble ny, jdouble tx, jdouble ty, jdouble rocketAngle, jdouble deltaTime) {
    double angleRad = rocketAngle * M_PI / 180.0;
    double cosAngle = cos(angleRad);
    double sinAngle = sin(angleRad);

    double thrustX = thrust * (cosAngle * nx + sinAngle * tx);
    double thrustY = thrust * (cosAngle * ny + sinAngle * ty);

    double r = sqrt(x * x + y * y);
    double gravityMagnitude = GRAVITATIONAL_CONSTANT * EARTH_MASS / (r * r);
    double gx = -gravityMagnitude * nx;
    double gy = -gravityMagnitude * ny;

    double ax = (thrustX / currentMass) + gx;
    double ay = (thrustY / currentMass) + gy;

    vx += ax * deltaTime;
    vy += ay * deltaTime;

    x += vx * deltaTime;
    y += vy * deltaTime;

    double speed = sqrt(vx * vx + vy * vy);

    jdoubleArray result = env->NewDoubleArray(5);
    if (result == NULL) {
        return NULL;
    }
    jdouble temp[5];
    temp[0] = vx;
    temp[1] = vy;
    temp[2] = x;
    temp[3] = y;
    temp[4] = speed;
    env->SetDoubleArrayRegion(result, 0, 5, temp);
    return result;
}

JNIEXPORT jdoubleArray JNICALL Java_RocketModel_nativeCalculateOrbitAngle(JNIEnv *env, jobject obj, jdouble x, jdouble y, jdouble vx, jdouble vy, jdouble speed, jdouble targetOrbitAltitude) {
    double r = sqrt(x * x + y * y);
    double currentAltitude = r - EARTH_RADIUS;

    double initialAngle = 0;
    double finalAngle = 90;

    double gravityTurnStartAltitude = 0;

    double gravityMagnitude = GRAVITATIONAL_CONSTANT * EARTH_MASS / (r * r);
    double gy = gravityMagnitude * (y / r);
    double gravityTurnEndAltitude = targetOrbitAltitude;

    double EarthAngleRad = -atan2(x, y);
    double EarthAngle = EarthAngleRad * 180.0 / M_PI;
    double SpeedAngle = -atan2(vx, vy);
    double sinSpeedEarth = cos(SpeedAngle - EarthAngleRad);
    double RocketMaxAltitude = speed * speed * sinSpeedEarth * sinSpeedEarth / (2 * gy) + currentAltitude;

    double targetAngle = finalAngle + EarthAngle;
    bool engineOnFlag = true;

    if (RocketMaxAltitude < gravityTurnStartAltitude) {
        targetAngle = initialAngle;
    } else if (RocketMaxAltitude < gravityTurnEndAltitude) {
        double ratio = (RocketMaxAltitude - gravityTurnStartAltitude) / (gravityTurnEndAltitude - gravityTurnStartAltitude);
        targetAngle = initialAngle + (finalAngle - initialAngle) * ratio + EarthAngle;
    } else {
        double mu = GRAVITATIONAL_CONSTANT * EARTH_MASS;
        double requiredOrbitVelocity = sqrt(mu / r);
        double currentVelocity = speed * sin(SpeedAngle - EarthAngleRad);
        double ratio = (RocketMaxAltitude - gravityTurnEndAltitude - gravityTurnStartAltitude) / (gravityTurnEndAltitude - gravityTurnStartAltitude);
        targetAngle = finalAngle * ratio + finalAngle + EarthAngle;
        if (currentVelocity >= requiredOrbitVelocity && currentVelocity <= requiredOrbitVelocity * 1.01) {
            engineOnFlag = false;
        } else {
            engineOnFlag = true;
        }
    }

    jdoubleArray result = env->NewDoubleArray(2);
    if (result == NULL) {
        return NULL;
    }
    jdouble temp[2];
    temp[0] = targetAngle;
    temp[1] = engineOnFlag ? 1.0 : 0.0;
    env->SetDoubleArrayRegion(result, 0, 2, temp);
    return result;
}

}
