package com.udacity.catpoint.service;

import com.udacity.catpoint.data.*;
import com.udacity.catpoint.image.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityServiceIntegrationTest {

    private SecurityService securityService;
    private SecurityRepository securityRepository;

    @BeforeEach
    void setUp() {
        securityRepository = new FakeSecurityRepository();
    }

    @Test
    void sensorFlow_integration() {
        Sensor s1 = new Sensor("Door Sensor", SensorType.DOOR);
        Sensor s2 = new Sensor("Window Sensor", SensorType.WINDOW);
        s1.setActive(true);
        s2.setActive(true);
        securityRepository.addSensor(s1);
        securityRepository.addSensor(s2);

        // Initialize service
        ImageService imageService = (img, thresh) -> false;
        securityService = new SecurityService(securityRepository, imageService);

        // 1. Arm the system (reset all sensors to inactive)
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        assertEquals(ArmingStatus.ARMED_HOME, securityService.getArmingStatus());
        assertFalse(s1.getActive());
        assertFalse(s2.getActive());
        assertEquals(AlarmStatus.NO_ALARM, securityService.getAlarmStatus());

        // 2. Activate sensor 1 -> PENDING_ALARM
        securityService.changeSensorActivationStatus(s1, true);
        assertEquals(AlarmStatus.PENDING_ALARM, securityService.getAlarmStatus());

        // 3. Activate sensor 2 -> ALARM
        securityService.changeSensorActivationStatus(s2, true);
        assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());

        // 4. Disarm system -> NO_ALARM
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        assertEquals(AlarmStatus.NO_ALARM, securityService.getAlarmStatus());
    }

    @Test
    void catDetectionFlow_integration() {
        final boolean[] catDetectedValue = {true};
        ImageService imageService = (img, thresh) -> catDetectedValue[0];
        securityService = new SecurityService(securityRepository, imageService);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        // 1. Cat detected -> ALARM
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        securityService.processImage(image);
        assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());

        // 2. No cat and sensors inactive -> NO_ALARM
        catDetectedValue[0] = false;
        securityService.processImage(image);
        assertEquals(AlarmStatus.NO_ALARM, securityService.getAlarmStatus());
    }

    @Test
    void armHomeWhileCatDetected_integration() {
        ImageService imageService = (img, thresh) -> true; // always shows a cat
        securityService = new SecurityService(securityRepository, imageService);
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        // 1. Scan picture while disarmed -> no alarm change
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        securityService.processImage(image);
        assertEquals(AlarmStatus.NO_ALARM, securityService.getAlarmStatus());

        // 2. Make it armed ARMED_HOME -> ALARM
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
    }
}
