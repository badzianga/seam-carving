import static java.lang.Integer.parseInt;

public class Main {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("[ERROR] Provide two arguments - file path and number of carves");
            System.exit(1);
        }
        String filePath = args[0];
        int carves = parseInt(args[1]);

        SeamCarver seamCarver = new SeamCarver(filePath);
        seamCarver.run(carves);
    }

}
