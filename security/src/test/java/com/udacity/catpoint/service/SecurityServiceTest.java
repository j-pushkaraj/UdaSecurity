package com.udacity.catpoint.service;

import com.udacity.catpoint.application.StatusListener;
import com.udacity.catpoint.data.*;
import com.udacity.catpoint.image.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private ImageService imageService;

    private SecurityService securityService;

    @BeforeEach
    void setUp() {
        securityService = new SecurityService(securityRepository, imageService);
    }

    // 1. If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void sensorActivatedWhenArmed_transitionsToPendingAlarm(ArmingStatus armingStatus) {
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        Sensor sensor = new Sensor("Door Sensor", SensorType.DOOR);
        sensor.setActive(false);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    // 2. If alarm is armed and a sensor becomes activated and the system is already pending alarm, set off the alarm.
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void sensorActivatedWhenArmedAndPending_transitionsToAlarm(ArmingStatus armingStatus) {
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        Sensor sensor = new Sensor("Window Sensor", SensorType.WINDOW);
        sensor.setActive(false);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // 3. If pending alarm and all sensors are inactive, return to no alarm state.
    @Test
    void sensorDeactivatedWhenPendingAndAllSensorsInactive_transitionsToNoAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        Sensor s1 = new Sensor("s1", SensorType.DOOR);
        s1.setActive(true);
        Sensor s2 = new Sensor("s2", SensorType.WINDOW);
        s2.setActive(false);
        Set<Sensor> sensors = new HashSet<>(Set.of(s1, s2));
        when(securityRepository.getSensors()).thenReturn(sensors);

        securityService.changeSensorActivationStatus(s1, false);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void sensorDeactivatedWhenPendingButOtherSensorsActive_staysInPendingAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        Sensor s1 = new Sensor("s1", SensorType.DOOR);
        s1.setActive(true);
        Sensor s2 = new Sensor("s2", SensorType.WINDOW);
        s2.setActive(true);
        Set<Sensor> sensors = new HashSet<>(Set.of(s1, s2));
        when(securityRepository.getSensors()).thenReturn(sensors);

        securityService.changeSensorActivationStatus(s1, false);

        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // 4. If alarm is active, change in sensor state should not affect the alarm state.
    @Test
    void sensorActivatedWhenAlarmActive_staysInAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        Sensor sensor = new Sensor("Door Sensor", SensorType.DOOR);
        sensor.setActive(false);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    void sensorDeactivatedWhenAlarmActive_staysInAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        Sensor sensor = new Sensor("Door Sensor", SensorType.DOOR);
        sensor.setActive(true);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // 5. If a sensor is activated while already active and the system is in pending state, change it to alarm state.
    @Test
    void sensorActivatedWhileAlreadyActiveAndPending_transitionsToAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        Sensor sensor = new Sensor("Door Sensor", SensorType.DOOR);
        sensor.setActive(true);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // 6. If a sensor is deactivated while already inactive, make no changes to the alarm state.
    @Test
    void sensorDeactivatedWhileAlreadyInactive_noAlarmStateChange() {
        Sensor sensor = new Sensor("Door Sensor", SensorType.DOOR);
        sensor.setActive(false);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // 7. If the camera image contains a cat while the system is armed-home, put the system into alarm status.
    @Test
    void catDetectedWhenArmedHome_transitionsToAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(eq(image), anyFloat())).thenReturn(true);

        securityService.processImage(image);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // 8. If the camera image does not contain a cat, change the status to no alarm as long as the sensors are not active.
    @Test
    void noCatDetectedAndSensorsInactive_transitionsToNoAlarm() {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(eq(image), anyFloat())).thenReturn(false);
        Sensor sensor = new Sensor("Door Sensor", SensorType.DOOR);
        sensor.setActive(false);
        when(securityRepository.getSensors()).thenReturn(Set.of(sensor));

        securityService.processImage(image);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void noCatDetectedButSensorsActive_noAlarmStateChange() {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(eq(image), anyFloat())).thenReturn(false);
        Sensor sensor = new Sensor("Door Sensor", SensorType.DOOR);
        sensor.setActive(true);
        when(securityRepository.getSensors()).thenReturn(Set.of(sensor));

        securityService.processImage(image);

        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // 9. If the system is disarmed, set the status to no alarm.
    @Test
    void disarmSystem_transitionsToNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // 10. If the system is armed, reset all sensors to inactive.
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void armSystem_resetsAllSensorsToInactive(ArmingStatus armingStatus) {
        Sensor s1 = new Sensor("s1", SensorType.DOOR);
        s1.setActive(true);
        Sensor s2 = new Sensor("s2", SensorType.WINDOW);
        s2.setActive(true);
        when(securityRepository.getSensors()).thenReturn(Set.of(s1, s2));

        securityService.setArmingStatus(armingStatus);

        verify(securityRepository, times(1)).updateSensor(s1);
        verify(securityRepository, times(1)).updateSensor(s2);
        assert(!s1.getActive());
        assert(!s2.getActive());
    }

    // 11. If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
    @Test
    void armedHomeWithCat_transitionsToAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(eq(image), anyFloat())).thenReturn(true);

        securityService.processImage(image);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Extra helper/coverage tests to hit 100% methods of SecurityService
    @Test
    void addAndRemoveStatusListener() {
        StatusListener listener = mock(StatusListener.class);
        securityService.addStatusListener(listener);
        securityService.removeStatusListener(listener);
    }

    @Test
    void getAlarmStatus() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        assert(securityService.getAlarmStatus() == AlarmStatus.ALARM);
    }

    @Test
    void getSensors() {
        Set<Sensor> sensors = Set.of(new Sensor("s1", SensorType.DOOR));
        when(securityRepository.getSensors()).thenReturn(sensors);
        assert(securityService.getSensors() == sensors);
    }

    @Test
    void addSensor() {
        Sensor sensor = new Sensor("s1", SensorType.DOOR);
        securityService.addSensor(sensor);
        verify(securityRepository, times(1)).addSensor(sensor);
    }

    @Test
    void removeSensor() {
        Sensor sensor = new Sensor("s1", SensorType.DOOR);
        securityService.removeSensor(sensor);
        verify(securityRepository, times(1)).removeSensor(sensor);
    }

    @Test
    void getArmingStatus() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        assert(securityService.getArmingStatus() == ArmingStatus.ARMED_HOME);
    }

    @Test
    void singleArgumentConstructor() {
        SecurityService testService = new SecurityService(securityRepository);
        org.junit.jupiter.api.Assertions.assertNotNull(testService);
    }

    @Test
    void armHomeWhileCatAlreadyDetected_transitionsToAlarm() {
        // 1. Scan cat while disarmed
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        lenient().when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(image);

        // 2. Change arming status to ARMED_HOME
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        verify(securityRepository, atLeastOnce()).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void statusListeners_notifiedOnEvents() {
        StatusListener listener = mock(StatusListener.class);
        securityService.addStatusListener(listener);

        // Trigger setAlarmStatus
        securityService.setAlarmStatus(AlarmStatus.PENDING_ALARM);
        verify(listener, times(1)).notify(AlarmStatus.PENDING_ALARM);

        // Trigger catDetected via processImage
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(image);
        verify(listener, times(1)).catDetected(true);
    }

    @Test
    void sensorActivatedWhenDisarmed_noAlarmStateChange() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        Sensor sensor = new Sensor("Door Sensor", SensorType.DOOR);
        sensor.setActive(false);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    void sensorActivatedWhileAlreadyActiveAndNotPending_noAlarmStateChange() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        Sensor sensor = new Sensor("Door Sensor", SensorType.DOOR);
        sensor.setActive(true);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }
}
