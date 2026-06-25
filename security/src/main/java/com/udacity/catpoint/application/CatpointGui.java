package com.udacity.catpoint.application;

import com.udacity.catpoint.data.PretendDatabaseSecurityRepositoryImpl;
import com.udacity.catpoint.data.SecurityRepository;
import com.udacity.catpoint.service.SecurityService;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

/**
 * This is the primary JFrame for the application that contains all the top-level JPanels.
 *
 * We're not using any dependency injection framework, so this class also handles constructing
 * all our dependencies and providing them to other classes as necessary.
 */
public class CatpointGui extends JFrame {
    private transient SecurityRepository securityRepository = new PretendDatabaseSecurityRepositoryImpl();
    private transient SecurityService securityService = new SecurityService(securityRepository);
    private transient DisplayPanel displayPanel = new DisplayPanel(securityService);
    private transient ControlPanel controlPanel = new ControlPanel(securityService);
    private transient SensorPanel sensorPanel = new SensorPanel(securityService);
    private transient ImagePanel imagePanel = new ImagePanel(securityService);

    public CatpointGui() {
        setLocation(100, 100);
        setSize(600, 850);
        setTitle("Very Secure App");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new MigLayout());
        mainPanel.add(displayPanel, "wrap");
        mainPanel.add(imagePanel, "wrap");
        mainPanel.add(controlPanel, "wrap");
        mainPanel.add(sensorPanel);

        getContentPane().add(mainPanel);

    }
}
