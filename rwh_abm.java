package rwh;

import sim.engine.SimState;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class abm extends SimState {
    public static int hoursInDay = 24;
    public static int hoursInSimulation;
    public static int daysInSimulation;
    public static int numberHouses;
    public static String basicInfoTextName;
    public static String rainfallDataFileName;
    public static String irrigationDemandFileName;
    public static String nodeIDFileName;
    public static String nodalPressureInputFileName;
    public static String nodalDemandOutputFileName;
    public static String negativeDemandOutputFileName;
    public static String irrigationDemandOutputFileName;
    public static String nodalPressureOutputFileName;
    public static String allSellerInfoOutputFileName;

    public static double cubicMeterToGallonConversion = 264.172;
    public static double cubicFootToGallonConversion = 7.48052;


    public abm(long seed) {
        super(seed);
    }

    public void start(String[] arguments) {
        super.start();
        schedule.scheduleRepeating(new market(arguments));
    }

    public static void main(String[] args, int seedIndex) {
        setInputAndOutputFiles(args);
        setNumSteps(rainfallDataFileName);
        setNumHouses(basicInfoTextName);
        abm abm = new abm(seedIndex);
        abm.start(args);
        long steps = 0;
        while (steps < hoursInSimulation) {
            if (!abm.schedule.step(abm))
                break;
            steps = abm.schedule.getSteps();
            if (steps % 500 == 0)
                System.out.println("Steps: " + steps + " Time: " + abm.schedule.getTime());
        }
        abm.finish();
    }

    /* Set input file names equal to command line arguments*/
    public static void setInputAndOutputFiles(String[] fileNames) {
        basicInfoTextName = fileNames[0];
        rainfallDataFileName = fileNames[1];
        irrigationDemandFileName = fileNames[2];
        nodeIDFileName = fileNames[3];
        nodalPressureInputFileName = fileNames[4];
        nodalDemandOutputFileName = fileNames[5];
        negativeDemandOutputFileName = fileNames[6];
        irrigationDemandOutputFileName = fileNames[7];
        nodalPressureOutputFileName = fileNames[8];
        allSellerInfoOutputFileName = fileNames[9];
    }

    /* Set the number of steps according to number of lines in data file */
    public static void setNumSteps(String inFile) {
        int numLines = getNumberLines(inFile);
        hoursInSimulation = numLines;
        daysInSimulation = numLines / 24;
    }

    /* Set the number of houses according to number of lines in data file */
    public static void setNumHouses(String inFile) {
        int numLines = getNumberLines(inFile);
        numberHouses = numLines - 1; // Skip header of info file
    }

    /* Count and return the number of lines in a file*/
    public static int getNumberLines(String fileName) {
        File file = new File(fileName);
        int index = 0;
        try {
            Scanner sc = new Scanner(file);
            index = 0;
            while (sc.hasNextLine()) {
                sc.nextLine();
                index += 1;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return index;
    }

}
  
  
