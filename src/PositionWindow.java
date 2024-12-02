import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class PositionWindow extends JFrame implements RocketObserver {
    private final RocketController controller;
    private JSpinner angleSpinner;
    private DrawingPanel drawingPanel;
    private final Object trajectoryLock = new Object();

    private double rocketAngle = 90;
    private final List<Point2D.Double> trajectoryPoints = new CopyOnWriteArrayList<>();
    private List<Point2D.Double> predictedTrajectoryPoints = new CopyOnWriteArrayList<>();
    public PositionWindow(RocketController controller) {
        this.controller = controller;
        initUI();
    }

    private void initUI() {
        setTitle("Положение");
        setSize(800, 600);
        setLayout(new BorderLayout());

        angleSpinner = new JSpinner(new SpinnerNumberModel(90.0, 0.0, 180.0, 0.1));
        angleSpinner.addChangeListener(e -> {
            if (controller.getAutopilotMode() == RocketController.AutopilotMode.MANUAL) {
                rocketAngle = (double) angleSpinner.getValue();
                controller.getModel().setRocketAngle(rocketAngle);
                if (!angleSpinner.isEnabled()) {
                    angleSpinner.setEnabled(true);
                }
            } else {
                angleSpinner.setValue(controller.getModel().getRocketAngle());
                if (angleSpinner.isEnabled()) {
                    angleSpinner.setEnabled(false);
                }
            }
        });


        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Текущий угол (градусы):"));
        topPanel.add(angleSpinner);
        add(topPanel, BorderLayout.NORTH);

        drawingPanel = new DrawingPanel();
        add(drawingPanel, BorderLayout.CENTER);

        setVisible(true);
    }

    @Override
    public void onStageSeparation(int stageNumber) {

    }

    @Override
    public void onUpdateStatus(double currentMass, double speed, double x, double y, double angle, int remainingStages, double[] fuelMasses, double[] initialFuelMasses) {
        SwingUtilities.invokeLater(() -> {
                angleSpinner.setValue(angle);

            angleSpinner.setEnabled(controller.getAutopilotMode() == RocketController.AutopilotMode.MANUAL);
            });

        synchronized (trajectoryLock) {
            trajectoryPoints.add(new Point2D.Double(x, y));
        }

        calculatePredictedTrajectory(x, y, controller.getModel().getSpeedX(), controller.getModel().getSpeedY());

        SwingUtilities.invokeLater(() -> drawingPanel.repaint());
    }


    private void calculatePredictedTrajectory(double x0, double y0, double vx0, double vy0) {
        SwingWorker<List<Point2D.Double>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Point2D.Double> doInBackground() {
                List<Point2D.Double> predictedPoints = new ArrayList<>();
                double t = 0;
                double dt = controller.getModel().getDeltaTime();
                double x = x0;
                double y = y0;
                double vx = vx0;
                double vy = vy0;

                int maxSteps = 100000;
                int steps = 0;

                while (Math.sqrt(x * x + y * y) >= RocketModel.EARTH_RADIUS && t < 10000 && steps < maxSteps) {
                    double r = Math.sqrt(x * x + y * y);
                    double gravityMagnitude = RocketModel.GRAVITATIONAL_CONSTANT * RocketModel.EARTH_MASS / (r * r);
                    double gx = -gravityMagnitude * (x / r);
                    double gy = -gravityMagnitude * (y / r);

                    vx += gx * dt;
                    vy += gy * dt;

                    x += vx * dt;
                    y += vy * dt;
                    synchronized (trajectoryLock) {
                        predictedPoints.add(new Point2D.Double(x, y));
                    }
                    t += dt;
                    steps++;
                }
                return predictedPoints;
            }

            @Override
            protected void done() {
                try {
                    List<Point2D.Double> predictedPoints = get();
                    synchronized (trajectoryLock) {
                        predictedTrajectoryPoints = predictedPoints;
                    }
                    drawingPanel.repaint();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }



    private class DrawingPanel extends JPanel {
        private double scale = 0.000001;
        private double translateX = 0;
        private double translateY = 0;
        private int lastMouseX, lastMouseY;
        private final double launchX;
        private final double launchY;

        public DrawingPanel() {
            launchX = 0;
            launchY = RocketModel.EARTH_RADIUS;
            addMouseWheelListener(new MouseAdapter() {
                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    double delta = -e.getPreciseWheelRotation() * scale * 0.1;
                    scale += delta;
                    if (scale < 0.000001) {
                        scale = 0.000001;
                    } else if (scale > 1000) {
                        scale = 1000;
                    }
                    repaint();
                }
            });


            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    lastMouseX = e.getX();
                    lastMouseY = e.getY();
                }
            });

            addMouseMotionListener(new MouseAdapter() {
                public void mouseDragged(MouseEvent e) {
                    int dx = e.getX() - lastMouseX;
                    int dy = e.getY() - lastMouseY;

                    translateX += dx;
                    translateY += dy;

                    lastMouseX = e.getX();
                    lastMouseY = e.getY();

                    repaint();
                }
            });

        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (trajectoryPoints.isEmpty()) return;

            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            AffineTransform originalTransform = g2d.getTransform();
            g2d.translate((double) getWidth() / 2 + translateX, (double) getHeight() / 2 + translateY);
            g2d.scale(1, -1);
            g2d.translate(-launchX * scale, -launchY * scale);
            g2d.scale(scale, scale);
            int earthRadius = (int) (RocketModel.EARTH_RADIUS);
            g2d.setColor(Color.DARK_GRAY);
            g2d.fillOval((int) (-RocketModel.EARTH_RADIUS), (int) (-RocketModel.EARTH_RADIUS), earthRadius * 2, earthRadius * 2);
            g2d.setColor(Color.RED);
            drawPath(g2d, trajectoryPoints);

            g2d.setColor(Color.GREEN);
            drawPath(g2d, predictedTrajectoryPoints);

            if (!trajectoryPoints.isEmpty()) {
                Point2D.Double lastPoint = trajectoryPoints.get(trajectoryPoints.size() - 1);
                double x = lastPoint.x;
                double y = lastPoint.y;

                double angleRad = Math.toRadians(controller.getModel().getRocketAngle());

                AffineTransform rocketTransform = g2d.getTransform();

                g2d.translate(x, y);
                g2d.rotate(angleRad);

                g2d.setColor(Color.BLACK);
                g2d.fillRect(((Number)(-2/scale)).intValue(), 0, ((Number)(4/scale)).intValue(), ((Number)(10/scale)).intValue());

                g2d.setTransform(rocketTransform);
            }
            g2d.setTransform(originalTransform);
        }

        private void drawPath(Graphics2D g2d, List<Point2D.Double> points) {
            if (points.size() < 2) return;
            Path2D path = new Path2D.Double();
            boolean firstPoint = true;
            for (Point2D.Double point : points) {
                double x = point.x;
                double y = point.y;
                if (firstPoint) {
                    path.moveTo(x, y);
                    firstPoint = false;
                } else {
                    path.lineTo(x, y);
                }
            }
            g2d.draw(path);
        }

    }
}
