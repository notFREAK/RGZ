import javax.swing.*;
import java.awt.*;

public class SettingsWindow extends JFrame {
    private final RocketController controller;

    private JTextField payloadMassField;
    private JTextField[] stageMassFields;
    private JTextField[] fuelMassFields;
    private JTextField thrustField;

    private JTextField cycleDelayField;
    private JTextField fuelConsumptionField;
    private JSpinner orbitAltitudeSpinner;

    public SettingsWindow(RocketController controller) {
        this.controller = controller;
        initUI();
    }

    private void initUI() {
        setTitle("Настройки симуляции");
        setSize(550, 600);
        setLayout(new BorderLayout());

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        payloadMassField = new JTextField("10");
        contentPanel.add(new JLabel("Полезная масса (кг):"));
        contentPanel.add(payloadMassField);

        stageMassFields = new JTextField[3];
        fuelMassFields = new JTextField[3];

        for (int i = 0; i < 3; i++) {
            stageMassFields[i] = new JTextField("5");
            fuelMassFields[i] = new JTextField("3");

            contentPanel.add(new JLabel("Масса ступени " + (i + 1) + " (кг):"));
            contentPanel.add(stageMassFields[i]);

            contentPanel.add(new JLabel("Масса топлива ступени " + (i + 1) + " (кг):"));
            contentPanel.add(fuelMassFields[i]);
        }

        thrustField = new JTextField("3500");
        contentPanel.add(new JLabel("Тяга на кг топлива:"));
        contentPanel.add(thrustField);

        cycleDelayField = new JTextField("100");
        contentPanel.add(new JLabel("Задержка цикла симуляции (мс):"));
        contentPanel.add(cycleDelayField);

        fuelConsumptionField = new JTextField("0.01");
        contentPanel.add(new JLabel("Сжигаемое топливо за цикл (кг):"));
        contentPanel.add(fuelConsumptionField);

        JButton manualButton = new JButton("Вручную");
        JButton maxDistanceButton = new JButton("Наибольшее удаление при полёте");
        JButton stableOrbitButton = new JButton("Стабильная орбита");

        contentPanel.add(new JLabel("Режим автопилота:"));
        JPanel autopilotPanel = new JPanel();
        autopilotPanel.add(manualButton);
        autopilotPanel.add(maxDistanceButton);
        autopilotPanel.add(stableOrbitButton);
        contentPanel.add(autopilotPanel);

        JButton applyButton = new JButton("Применить настройки");
        contentPanel.add(applyButton);

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        add(scrollPane, BorderLayout.CENTER);

        manualButton.addActionListener(e -> controller.setAutopilotMode(RocketController.AutopilotMode.MANUAL));
        maxDistanceButton.addActionListener(e -> controller.setAutopilotMode(RocketController.AutopilotMode.MAX_DISTANCE));
        stableOrbitButton.addActionListener(e -> controller.setAutopilotMode(RocketController.AutopilotMode.STABLE_ORBIT));

        applyButton.addActionListener(e -> {
            if (applySettings()) {
                if (controller.getAutopilotMode() == RocketController.AutopilotMode.STABLE_ORBIT) {
                    double targetOrbitAltitude = ((Number) orbitAltitudeSpinner.getValue()).doubleValue();
                    controller.getModel().setTargetOrbitAltitude(targetOrbitAltitude);
                }
                JOptionPane.showMessageDialog(this, "Настройки успешно применены.", "Информация", JOptionPane.INFORMATION_MESSAGE);
                controller.setSettingsConfirmed(true);
            }
        });
        JLabel orbitAltitudeLabel = new JLabel("Целевая высота орбиты (м):");
        orbitAltitudeSpinner = new JSpinner(new SpinnerNumberModel(200000, 100000, 10000000, 10000));
        orbitAltitudeSpinner.setEnabled(false);

        JPanel orbitPanel = new JPanel();
        orbitPanel.add(orbitAltitudeLabel);
        orbitPanel.add(orbitAltitudeSpinner);
        contentPanel.add(orbitPanel);

        manualButton.addActionListener(e -> {
            controller.setAutopilotMode(RocketController.AutopilotMode.MANUAL);
            orbitAltitudeSpinner.setEnabled(false);
        });
        maxDistanceButton.addActionListener(e -> {
            controller.setAutopilotMode(RocketController.AutopilotMode.MAX_DISTANCE);
            orbitAltitudeSpinner.setEnabled(false);
        });
        stableOrbitButton.addActionListener(e -> {
            controller.setAutopilotMode(RocketController.AutopilotMode.STABLE_ORBIT);
            orbitAltitudeSpinner.setEnabled(true); // Включаем ввод высоты орбиты
        });
        setVisible(true);
    }

    private boolean applySettings() {
        try {
            double payloadMass = Double.parseDouble(payloadMassField.getText());
            double[] stageMasses = new double[3];
            double[] fuelMasses = new double[3];
            for (int i = 0; i < 3; i++) {
                stageMasses[i] = Double.parseDouble(stageMassFields[i].getText());
                fuelMasses[i] = Double.parseDouble(fuelMassFields[i].getText());
            }
            double thrustPerKgFuel = Double.parseDouble(thrustField.getText());
            int cycleDelay = Integer.parseInt(cycleDelayField.getText());
            double fuelConsumptionPerCycle = Double.parseDouble(fuelConsumptionField.getText());

            controller.setRocketParameters(payloadMass, stageMasses, fuelMasses, thrustPerKgFuel);
            controller.setCycleDelay(cycleDelay);
            controller.setFuelConsumptionPerCycle(fuelConsumptionPerCycle);

            return true;
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Ошибка ввода данных. Пожалуйста, введите корректные числовые значения.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
}
