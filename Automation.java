package rwh;

public class Automation {
    /* number of simulation runs */
    static int numSims = 1;
    static int seedNum = 1;

    public Automation() {
    }

    public static void main(String[] args) {

        String[] fileNames = new String[9];
        fileNames[0] = "";
        fileNames[1] = "";
        fileNames[2] = "";
        fileNames[3] = "";
        fileNames[4] = "";
        fileNames[5] = "";
        fileNames[6] = "";
        fileNames[7] = "";
//		System.out.println("Enter the file path for the household information input text file");
//		String[] fileNames = new String[11];
//		Scanner scanInput = new Scanner(System.in);
//		fileNames[0] = scanInput.nextLine();
//      System.out.println("Enter the file path for the climate information text file ");
//		fileNames[1] = scanInput.nextLine();
// 		System.out.println("Enter the file path for the tab delimited precipitation input text file");
//      fileNames[2] = scanInput.nextLine();
//		System.out.println("Enter the file path for the tab delimited irrigation demand input text file");
//		fileNames[3] = scanInput.nextLine();
//		System.out.println("Enter the file path for the tab delimited node ID input text file");
//		fileNames[4] = scanInput.nextLine();
// 		System.out.println("Enter the file path for the demand at nodes output text file");
//		fileNames[5] = scanInput.nextLine();
// 		System.out.println("Enter the file path for the negative demand/rainwater contribution output text file");
//		fileNames[6] = scanInput.nextLine();
// 		System.out.println("Enter the file path for the irrigation demand output text file");
//		fileNames[7] = scanInput.nextLine();
        for (int i = 0; i < numSims; i++) {
            marketABM.main(fileNames, seedNum);

        }
    }
}
