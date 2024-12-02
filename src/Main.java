public class Main {
    public static void main(String[] args) {
        RocketModel model = new RocketModel();
        RocketController controller = new RocketController(model);
        MainWindow mainWindow = new MainWindow(controller);
        model.addObserver(mainWindow);
    }
}
