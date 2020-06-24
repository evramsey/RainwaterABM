package rwh;

public class Automation {
    /* number of simulation runs */
    static int numSims = 1;
    static int seedNum = 1;

    public Automation() {
    }

    public static void main(String[] args) {
        //TODO: replace hardcoded file names when testing is finished
        String[] fileNames = new String[10];
        fileNames[0] = "/Users/lizramsey/Documents/workspace/RWH/Seattle/Basic Information Seattle.txt";
        fileNames[1] = "/Users/lizramsey/Documents/workspace/RWH/Seattle/Seattle_Rainfall_test.txt";
        fileNames[2] = "/Users/lizramsey/Documents/workspace/RWH/Irrigation Demand Pattern.txt";
        fileNames[3] = "/Users/lizramsey/Documents/workspace/RWH/NodesID.txt";
        fileNames[4] = "/Users/lizramsey/Documents/workspace/RWH/Nodal Pressures.txt";
        fileNames[5] = "/Users/lizramsey/Documents/workspace/RWH/output/Demand At Nodes.txt";
        fileNames[6] = "/Users/lizramsey/Documents/workspace/RWH/output/Negative Demand Output.txt";
        fileNames[7] = "/Users/lizramsey/Documents/workspace/RWH/output/Irrigation Demand Output.txt";
        fileNames[8] = "/Users/lizramsey/Documents/workspace/RWH/Nodal Pressures.txt";
        fileNames[9] = "/Users/lizramsey/Documents/workspace/RWH/output/All Seller Information.txt";
//		fileNames[0] = "/Users/lizramsey/Documents/workspace/RWH/test files/Basic Information Test.txt";
//		fileNames[1] = "/Users/lizramsey/Documents/workspace/RWH/test files/ Rainfall.txt";
//		fileNames[2] = "/Users/lizramsey/Documents/workspace/RWH/test files/Irrigation Demand Pattern Test.txt";
//		fileNames[3] = "/Users/lizramsey/Documents/workspace/RWH/test files/TestNodesID.txt";
//		fileNames[4] = "/Users/lizramsey/Documents/workspace/RWH/testoutputs/Nodal Pressures.txt";
//		fileNames[5] = "/Users/lizramsey/Documents/workspace/RWH/testoutputs/Demand At Nodes.txt";
//		fileNames[6] = "/Users/lizramsey/Documents/workspace/RWH/testoutputs/Negative Demand Output.txt";
//		fileNames[7] = "/Users/lizramsey/Documents/workspace/RWH/testoutputs/Irrigation Demand Output.txt";
//		fileNames[8] = "/Users/lizramsey/Documents/workspace/RWH/testoutputs/Nodal Pressures.txt";
//		fileNames[9] = "/Users/lizramsey/Documents/workspace/RWH/testoutputs/All Seller Information.txt";
//		System.out.println("Enter the file path for the basic information input text file");
//		String[] fileNames = new String[4];
//		Scanner scanInput = new Scanner(System.in);
//		fileNames[0] = scanInput.nextLine();
//		System.out.println("Enter the file path for the tab delimited precipitation input text file");
//		fileNames[1] = scanInput.nextLine();
//		System.out.println("Enter the file path for the tab delimited irrigation demand input text file");
//		fileNames[2] = scanInput.nextLine();
//		System.out.println("Enter the file path for the tab delimited node ID input text file");
//		fileNames[3] = scanInput.nextLine();
// 		System.out.println("Enter the file path for the EPANet nodal pressures input text file");
//		fileNames[4] = scanInput.nextLine();
// 		System.out.println("Enter the file path for the demand at nodes output text file");
//		fileNames[5] = scanInput.nextLine();
// 		System.out.println("Enter the file path for the negative demand/rainwater contribution output text file");
//		fileNames[6] = scanInput.nextLine();
// 		System.out.println("Enter the file path for the irrigation demand output text file");
//		fileNames[7] = scanInput.nextLine();
// 		System.out.println("Enter the file path for the nodal pressure output text file");
//		fileNames[8] = scanInput.nextLine();
// 		System.out.println("Enter the file path for all seller information text file");
//		fileNames[8] = scanInput.nextLine();
        for (int i = 0; i < numSims; i++) {
            abm.main(fileNames, seedNum);
        }
    }
}
