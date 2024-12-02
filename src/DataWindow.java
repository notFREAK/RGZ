import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DataWindow extends JFrame implements RocketObserver {
    private final List<Double> timeData = new ArrayList<>();
    private final List<Double> speedData = new ArrayList<>();
    private final List<Double> altitudeData = new ArrayList<>();
    private final List<Double> massData = new ArrayList<>();

    private double time = 0;

    private final JPanel speedPanel;
    private final JPanel altitudePanel;
    private final JPanel massPanel;

    private double maxTime = 0;
    private double maxSpeed = 0;
    private double maxAltitude = 0;
    private double maxMass = 0;

    private final RocketController controller;

    public DataWindow(RocketController controller) {
        this.controller = controller;
        setTitle("Данные");
        setSize(1200, 300);
        setLayout(new GridLayout(1, 4));

        speedPanel = new GraphPanel(timeData, speedData, "Скорость", "Время (с)", "Скорость (м/с)");
        altitudePanel = new GraphPanel(timeData, altitudeData, "Высота", "Время (с)", "Высота (м)");
        massPanel = new GraphPanel(timeData, massData, "Масса", "Время (с)", "Масса (кг)");

        add(speedPanel);
        add(altitudePanel);
        add(massPanel);

        setVisible(true);
    }

    @Override
    public void onStageSeparation(int stageNumber) {

    }

    @Override
    public void onUpdateStatus(double currentMass, double speed, double x, double y, double angle, int remainingStages, double[] fuelMasses, double[] initialFuelMasses) {
        time += controller.getModel().getDeltaTime();
        timeData.add(time);
        speedData.add(speed);
        double altitude = Math.sqrt(x*x+y*y) - RocketModel.EARTH_RADIUS;
        altitudeData.add(altitude);
        massData.add(currentMass);

        maxTime = time;
        maxSpeed = Math.max(maxSpeed, speed);
        maxAltitude = Math.max(maxAltitude, altitude);
        maxMass = Math.max(maxMass, currentMass);

        SwingUtilities.invokeLater(() -> {
            speedPanel.repaint();
            altitudePanel.repaint();
            massPanel.repaint();
        });
    }

    private class GraphPanel extends JPanel {
        private final List<Double> xData;
        private final List<Double> yData;
        private final String title;
        private final String xLabel;
        private final String yLabel;

        public GraphPanel(List<Double> xData, List<Double> yData, String title, String xLabel, String yLabel) {
            this.xData = xData;
            this.yData = yData;
            this.title = title;
            this.xLabel = xLabel;
            this.yLabel = yLabel;
            setPreferredSize(new Dimension(300, 300));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (xData.isEmpty() || yData.isEmpty()) return;

            Graphics2D g2 = (Graphics2D) g;

            int padding = 40;
            int labelPadding = 20;

            int width = getWidth();
            int height = getHeight();

            double xMax = maxTime;
            double yMax = switch (title) {
                case "Скорость" -> maxSpeed;
                case "Высота" -> maxAltitude;
                case "Масса" -> maxMass;
                default -> 0;
            };

            g2.drawLine(padding, height - padding, padding, padding);
            g2.drawLine(padding, height - padding, width - padding, height - padding);

            g2.drawString(xLabel, width / 2, height - labelPadding);
            g2.drawString(yLabel, labelPadding, height / 2);

            g2.drawString(title, width / 2 - g2.getFontMetrics().stringWidth(title) / 2, labelPadding);

            double xScale = (width - 2 * padding) / xMax;
            double yScale = (height - 2 * padding) / yMax;

            int prevX = padding;
            int prevY = height - padding;

            for (int i = 0; i < xData.size(); i++) {
                int x = padding + (int) (xData.get(i) * xScale);
                int y = height - padding - (int) (yData.get(i) * yScale);

                g2.drawLine(prevX, prevY, x, y);

                prevX = x;
                prevY = y;
            }
        }
    }
}
