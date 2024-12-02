import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MainWindow extends JFrame implements RocketObserver {
    private final RocketController controller;
    private JButton engineToggleButton;

    private JButton startStopButton;
    private JLabel statusLabel;

    private JProgressBar[] fuelBars;
    private JLabel[] fuelLabels;
    private JLabel positionLabel; // Новый лейбл для позиции

    private boolean isSimulating = false;

    private final List<JFrame> childWindows = new ArrayList<>();

    public MainWindow(RocketController controller) {
        this.controller = controller;
        initUI();
    }

    private void initUI() {
        setTitle("Rocket Simulation");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel buttonPanel = new JPanel();
        JButton settingsButton = new JButton("Настройки симуляции");
        JButton dataButton = new JButton("Данные");
        JButton positionButton = new JButton("Положение");

        buttonPanel.add(settingsButton);
        buttonPanel.add(dataButton);
        buttonPanel.add(positionButton);

        add(buttonPanel, BorderLayout.NORTH);

        JPanel fuelPanel = new JPanel();
        fuelPanel.setLayout(new BoxLayout(fuelPanel, BoxLayout.Y_AXIS));

        fuelBars = new JProgressBar[3];
        fuelLabels = new JLabel[3];
        for (int i = 0; i < 3; i++) {
            fuelBars[i] = new JProgressBar(0, 100);
            fuelBars[i].setValue(100);
            fuelBars[i].setForeground(Color.GREEN);

            fuelLabels[i] = new JLabel("Ступень " + (i + 1));
            fuelPanel.add(fuelLabels[i]);
            fuelPanel.add(fuelBars[i]);
        }

        add(fuelPanel, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BorderLayout());

        JPanel controlLabelPanel = new JPanel();
        controlLabelPanel.setLayout(new BorderLayout());


        JPanel controlButtonPanel = new JPanel();
        controlButtonPanel.setLayout(new BorderLayout());
        engineToggleButton = new JButton("Выключить двигатель");


        statusLabel = new JLabel("Готово к запуску");
        positionLabel = new JLabel("Положение x: 0.00 м, y: 0.00 м"); // Инициализируем лейбл позиции
        startStopButton = new JButton("Начать Симуляцию");
        controlLabelPanel.add(statusLabel, BorderLayout.NORTH);
        controlLabelPanel.add(positionLabel, BorderLayout.CENTER); // Добавляем лейбл позиции
        controlButtonPanel.add(engineToggleButton, BorderLayout.CENTER);
        controlButtonPanel.add(startStopButton, BorderLayout.SOUTH);
        controlPanel.add(controlLabelPanel, BorderLayout.NORTH);
        controlPanel.add(controlButtonPanel, BorderLayout.SOUTH);
        add(controlPanel, BorderLayout.SOUTH);

        settingsButton.addActionListener(e -> openSettingsWindow());
        dataButton.addActionListener(e -> openDataWindow());
        positionButton.addActionListener(e -> openPositionWindow());

        startStopButton.addActionListener(e -> {
            if (!isSimulating) {
                if (controller.isSettingsConfirmed()) {
                    controller.startSimulation();
                    isSimulating = true;
                    startStopButton.setText("Остановить Симуляцию");
                    statusLabel.setText("Симуляция запущена");
                } else {
                    JOptionPane.showMessageDialog(this, "Пожалуйста, подтвердите настройки перед запуском симуляции.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                controller.stopSimulation();
                isSimulating = false;
                startStopButton.setText("Начать Симуляцию");
                statusLabel.setText("Симуляция остановлена");
            }
        });
        engineToggleButton.addActionListener(e -> {
            controller.toggleEngine();
            if (controller.isEngineOn()) {
                engineToggleButton.setText("Выключить двигатель");
            } else {
                engineToggleButton.setText("Включить двигатель");
            }
        });
        setVisible(true);
    }

    private void openSettingsWindow() {
        SettingsWindow settingsWindow = new SettingsWindow(controller);
        childWindows.add(settingsWindow);
    }

    private void openDataWindow() {
        DataWindow dataWindow = new DataWindow(controller);
        controller.getModel().addObserver(dataWindow);
        childWindows.add(dataWindow);
    }

    private void openPositionWindow() {
        PositionWindow positionWindow = new PositionWindow(controller);
        controller.getModel().addObserver(positionWindow);
        childWindows.add(positionWindow);
    }

    @Override
    public void onStageSeparation(int stageNumber) {
        System.out.println("Ступень  " + stageNumber + " отделилась!");
    }

    @Override
    public void onUpdateStatus(double currentMass, double speed, double x, double y, double angle, int remainingStages, double[] fuelMasses, double[] initialFuelMasses) {
        SwingUtilities.invokeLater(() -> {
            double altitude = Math.sqrt(x*x+y*y) - RocketModel.EARTH_RADIUS;
            statusLabel.setText(String.format("Масса: %.2f кг, Скорость: %.2f м/с, Высота: %.2f м", currentMass, speed, altitude));

            positionLabel.setText(String.format("Положение x: %.2f м, y: %.2f м", x, y)); // Обновляем лейбл позиции

            for (int i = 0; i < 3; i++) {
                if (i < remainingStages) {
                    int fuelPercentage = (int) (fuelMasses[i] / initialFuelMasses[i] * 100);
                    fuelBars[i].setValue(fuelPercentage);
                    if (fuelPercentage == 0) {
                        fuelBars[i].setForeground(Color.RED);
                    } else {
                        fuelBars[i].setForeground(Color.GREEN);
                    }
                    fuelLabels[i].setText(String.format("Ступень %d: %.2f кг топлива", i + 1, fuelMasses[i]));
                } else {
                    fuelBars[i].setValue(0);
                    fuelBars[i].setForeground(Color.RED);
                    fuelLabels[i].setText(String.format("Ступень %d отделена!", i + 1));
                }
            }

            if (controller.isEngineOn()) {
                engineToggleButton.setText("Выключить двигатель");
            } else {
                engineToggleButton.setText("Включить двигатель");
            }
        });
    }

}
