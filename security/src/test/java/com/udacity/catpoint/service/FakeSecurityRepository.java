package com.udacity.catpoint.service;

import com.udacity.catpoint.data.AlarmStatus;
import com.udacity.catpoint.data.ArmingStatus;
import com.udacity.catpoint.data.SecurityRepository;
import com.udacity.catpoint.data.Sensor;

import java.util.HashSet;
import java.util.Set;

public class FakeSecurityRepository implements SecurityRepository {
    private final Set<Sensor> sensors = new HashSet<>();
    private AlarmStatus alarmStatus = AlarmStatus.NO_ALARM;
    private ArmingStatus armingStatus = ArmingStatus.DISARMED;

    @Override
    public void addSensor(Sensor sensor) {
        sensors.add(sensor);
    }

    @Override
    public void removeSensor(Sensor sensor) {
        sensors.remove(sensor);
    }

    @Override
    public void updateSensor(Sensor sensor) {
        sensors.remove(sensor);
        sensors.add(sensor);
    }

    @Override
    public void setAlarmStatus(AlarmStatus alarmStatus) {
        this.alarmStatus = alarmStatus;
    }

    @Override
    public void setArmingStatus(ArmingStatus armingStatus) {
        this.armingStatus = armingStatus;
    }

    @Override
    public Set<Sensor> getSensors() {
        return sensors;
    }

    @Override
    public AlarmStatus getAlarmStatus() {
        return alarmStatus;
    }

    @Override
    public ArmingStatus getArmingStatus() {
        return armingStatus;
    }
}
