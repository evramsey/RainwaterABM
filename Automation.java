package rwh;

public class Automation {
	/* number of simulation runs */
	static int numSims = 1;
	static int seedNum = 1;
	public Automation() {
	}

	public static void main(String[] args) {
		//TODO: replace hardcoded file names when testing is finished
		String[] fileNames = new String[4];
		fileNames[0] = "/Users/lizramsey/Documents/workspace/RWH/Basic Information.txt";
		fileNames[1] = "/Users/lizramsey/Documents/workspace/RWH/Rainfall.txt";
		fileNames[2] = "/Users/lizramsey/Documents/workspace/RWH/Irrigation Demand Pattern.txt";
		fileNames[3] = "/Users/lizramsey/Documents/workspace/RWH/NodesID.txt";
//		System.out.println("Enter the file path for the basic information text file");
//		String[] fileNames = new String[4];
//		Scanner scanInput = new Scanner(System.in);
//		fileNames[0] = scanInput.nextLine();
//		System.out.println("Enter the file path for the tab delimited precipitation text file");
//		fileNames[1] = scanInput.nextLine();
//		System.out.println("Enter the file path for the tab delimited irrigation demand text file");
//		fileNames[2] = scanInput.nextLine();
//		System.out.println("Enter the file path for the tab delimited node ID text file");
//		fileNames[3] = scanInput.nextLine();

		for (int i = 0; i < numSims; i++) {
			rwh_abm.main(fileNames, seedNum);
		}
	}
}