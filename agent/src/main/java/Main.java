import java.util.Scanner;

/**
 * @author Mike Mirzayanov (mirzayanovmr@gmail.com)
 */
public class Main {
    public static void main(String[] args) {
        new Main().run();
    }

    private void run() {
        Scanner scanner = new Scanner(System.in);

        while (scanner.hasNext()) {
            String command = scanner.next();

            if ("end".equals(command)) {
                break;
            }

            process(command);
        }
    }

    private void process(String command) {
        oo(command);
    }

    private void oo(String command) {
        System.out.println("`" + command + ": вот так да?");
    }
}
