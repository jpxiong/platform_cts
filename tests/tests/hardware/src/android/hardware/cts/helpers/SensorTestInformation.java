/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.hardware.cts.helpers;

import android.hardware.Sensor;

/**
 * A 'property' bag of sensor information used for testing purposes.
 */
// TODO: Refactor this class and SensorCtsHelper into several more well defined helper classes
public class SensorTestInformation {
    private SensorTestInformation() {}

    public enum SensorReportingMode {
        CONTINUOUS,
        ON_CHANGE,
        ONE_SHOT,
    }

    @SuppressWarnings("deprecation")
    public static SensorReportingMode getReportingMode(int sensorType) {
        switch(sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
            case Sensor.TYPE_MAGNETIC_FIELD:
            case Sensor.TYPE_ORIENTATION:
            case Sensor.TYPE_GYROSCOPE:
            case Sensor.TYPE_PRESSURE:
            case Sensor.TYPE_GRAVITY:
            case Sensor.TYPE_LINEAR_ACCELERATION:
            case Sensor.TYPE_ROTATION_VECTOR:
            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
            case Sensor.TYPE_GAME_ROTATION_VECTOR:
            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                return SensorReportingMode.CONTINUOUS;
            case Sensor.TYPE_LIGHT:
            case Sensor.TYPE_TEMPERATURE:
            case Sensor.TYPE_PROXIMITY:
            case Sensor.TYPE_RELATIVE_HUMIDITY:
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
            case Sensor.TYPE_STEP_DETECTOR:
            case Sensor.TYPE_STEP_COUNTER:
                return SensorReportingMode.ON_CHANGE;
            case Sensor.TYPE_SIGNIFICANT_MOTION:
                return SensorReportingMode.ONE_SHOT;
            default:
                return null;
        }
    }

    public static String getSensorName(int sensorType) {
        return String.format("%s (%d)", getSimpleSensorName(sensorType), sensorType);
    }

    @SuppressWarnings("deprecation")
    public static String getSimpleSensorName(int sensorType) {
        switch(sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                return "Accelerometer";
            case Sensor.TYPE_MAGNETIC_FIELD:
                return "Magnetic Field";
            case Sensor.TYPE_ORIENTATION:
                return "Orientation";
            case Sensor.TYPE_GYROSCOPE:
                return "Gyroscope";
            case Sensor.TYPE_LIGHT:
                return "Light";
            case Sensor.TYPE_PRESSURE:
                return "Pressure";
            case Sensor.TYPE_TEMPERATURE:
                return "Temperature";
            case Sensor.TYPE_PROXIMITY:
                return "Proximity";
            case Sensor.TYPE_GRAVITY:
                return "Gravity";
            case Sensor.TYPE_LINEAR_ACCELERATION:
                return "Linear Acceleration";
            case Sensor.TYPE_ROTATION_VECTOR:
                return "Rotation Vector";
            case Sensor.TYPE_RELATIVE_HUMIDITY:
                return "Relative Humidity";
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                return "Ambient Temperature";
            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                return "Magnetic Field Uncalibrated";
            case Sensor.TYPE_GAME_ROTATION_VECTOR:
                return "Game Rotation Vector";
            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                return "Gyroscope Uncalibrated";
            case Sensor.TYPE_SIGNIFICANT_MOTION:
                return "Significant Motion";
            case Sensor.TYPE_STEP_DETECTOR:
                return "Step Detector";
            case Sensor.TYPE_STEP_COUNTER:
                return "Step Counter";
            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                return "Geomagnetic Rotation Vector";
            default:
                return "<Unknown>";
        }
    }

    @SuppressWarnings("deprecation")
    public static String getSanitizedSensorName(int sensorType) {
        switch(sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                return "Accelerometer";
            case Sensor.TYPE_MAGNETIC_FIELD:
                return "MagneticField";
            case Sensor.TYPE_ORIENTATION:
                return "Orientation";
            case Sensor.TYPE_GYROSCOPE:
                return "Gyroscope";
            case Sensor.TYPE_LIGHT:
                return "Light";
            case Sensor.TYPE_PRESSURE:
                return "Pressure";
            case Sensor.TYPE_TEMPERATURE:
                return "Temperature";
            case Sensor.TYPE_PROXIMITY:
                return "Proximity";
            case Sensor.TYPE_GRAVITY:
                return "Gravity";
            case Sensor.TYPE_LINEAR_ACCELERATION:
                return "LinearAcceleration";
            case Sensor.TYPE_ROTATION_VECTOR:
                return "RotationVector";
            case Sensor.TYPE_RELATIVE_HUMIDITY:
                return "RelativeHumidity";
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                return "AmbientTemperature";
            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                return "MagneticFieldUncalibrated";
            case Sensor.TYPE_GAME_ROTATION_VECTOR:
                return "GameRotationVector";
            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                return "GyroscopeUncalibrated";
            case Sensor.TYPE_SIGNIFICANT_MOTION:
                return "SignificantMotion";
            case Sensor.TYPE_STEP_DETECTOR:
                return "StepDetector";
            case Sensor.TYPE_STEP_COUNTER:
                return "StepCounter";
            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                return "GeomagneticRotationVector";
            default:
                return String.format("UnknownSensorType%d", sensorType);
        }
    }
}
