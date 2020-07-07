package rwh;

import ec.util.MersenneTwisterFast;
import sim.engine.SimState;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.stream.IntStream;
import java.util.ArrayList;

public class marketABM extends SimState {

    /** Static constants */
    public static int HOURS_IN_DAY = 24;
    public static double M3_TO_GAL_CONVERSION = 264.172;
    public static double FT3_TO_GAL_CONVERSION = 7.48052;
    public static int MINUTES_IN_HOUR = 60;

    /** Random number generator */
    public static MersenneTwisterFast rng = new MersenneTwisterFast();

    /** Model file names set by user input in Automation.java */
    public static int hoursInSimulation;
    public static int daysInSimulation;
    public static int numberHouses;
    public static String basicInfoTextName;
    public static String climateInfoTextName;
    public static String rainfallDataFileName;
    public static String irrigationDemandFileName;
    public static String nodeIDFileName;
    public static String nodalDemandOutputFileName;
    public static String negativeDemandOutputFileName;
    public static String irrigationDemandOutputFileName;
    public static String allSellerInfoOutputFileName;

    /** Constants set by climate input file */

    /** Potential evapotranspiration [m/month] */
    public double potEvapotranspiration;
    /** Effective rainfall [m] (Jacobs and Haarhofs, 2004) */
    public double effectiveRainfall;

    /** Variables updated by the model */

    /** Current hour of simulation */
    public static int currentHour = 0;
    /** Array of household agents*/
    public static Household[] _households;
    /** ArrayList of households with tanks */
    public static ArrayList<Household> prosumer_households = new ArrayList<>();
    /** ArrayList of households without tanks */
    public static ArrayList<Household> consumer_households = new ArrayList<>();
    /** Rainfall data for simulation*/
    public static ArrayList<Double> rainfall = new ArrayList<>();
    /** Number of hours since last rain event */
    public static int hoursSinceRain = 0;


    /** Variables to change as needed */

    /** Number of time steps between rain event and resumption of trading */
    public static int rainTradeGap = 24;

    /** Construct marketABM with seed */
    public marketABM(long seed) {
        super(seed);
    }

    /** Initialize simulation*/
    public void start(String[] arguments) {
        schedule.clear();
        super.start();
        setHouseholds();
        setNodeIDs();
        initializeClimateInfo();
        initializeRainfall();
        initializeIrrigPattern();
        DataCollector dc = new DataCollector(nodalDemandOutputFileName, negativeDemandOutputFileName,
                irrigationDemandOutputFileName, allSellerInfoOutputFileName);
        schedule.scheduleRepeating(schedule.EPOCH, 1, dc);
    }

    /**
     * Main method, set up model and run
     * @param args
     * @param seedIndex
     */
    public static void main(String[] args, int seedIndex) {
        setInputAndOutputFiles(args);
        setNumSteps(rainfallDataFileName);
        setNumHouses(basicInfoTextName);
        marketABM marketABM = new marketABM(seedIndex);
        marketABM.start(args);
        long steps = 0;
        while (steps < hoursInSimulation) {
            setHoursSinceRain();
            if (!marketABM.schedule.step(marketABM))
                break;

            steps = marketABM.schedule.getSteps();
            currentHour += 1;
        }
        marketABM.finish();

    }

    /** Set input file names equal to command line arguments
     * @param fileNames array of input file names
     * */
    public static void setInputAndOutputFiles(String[] fileNames) {
        basicInfoTextName = fileNames[0];
        climateInfoTextName = fileNames[1];
        rainfallDataFileName = fileNames[2];
        irrigationDemandFileName = fileNames[3];
        nodeIDFileName = fileNames[4];
        nodalDemandOutputFileName = fileNames[5];
        negativeDemandOutputFileName = fileNames[6];
        irrigationDemandOutputFileName = fileNames[7];
        allSellerInfoOutputFileName = fileNames[8];
    }

    /** Set the number of steps according to number of lines in data file
     * @param inFile    the name of the input file
     * */
    public static void setNumSteps(String inFile) {
        int numLines = getNumberLines(inFile);
        hoursInSimulation = numLines;
        daysInSimulation = numLines / 24;
    }

    /** Set the number of houses according to number of lines in data file
     * @param inFile    the name of the input file
     * */
    public static void setNumHouses(String inFile) {
        int numLines = getNumberLines(inFile);
        numberHouses = numLines;
        _households = new Household[numberHouses];
    }

    /** Count and return the number of lines in a file
     * @param inFile     the name of the input file
     * @return numLines  the number of lines
     * */
    public static int getNumberLines(String inFile) {
        File file = new File(inFile);
        int numLines = 0;
        try {
            Scanner sc = new Scanner(file);
            numLines = 0;
            while (sc.hasNextLine()) {
                sc.nextLine();
                numLines += 1;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return numLines - 1;  // Subtract header from number of data lines
    }

    /** Initialize household agents from basic information text file */
    private void setHouseholds() {
        //this._households = new Household[marketABM.numberHouses];
        File infoFile = new File(marketABM.basicInfoTextName);
        try {
            Scanner sc = new Scanner(infoFile);
            int index = 0;
            sc.nextLine(); //skip header
            while (sc.hasNextLine()) {
                String i = sc.nextLine();
                String[] parts = i.split("\t");
                boolean hasTank = Boolean.parseBoolean(parts[0]);
                double catchmentArea = Double.parseDouble(parts[1]);
                double tankDiameter = Double.parseDouble(parts[2]);
                double tankHeight = Double.parseDouble(parts[3]);
                double WTA = Double.parseDouble(parts[4]);
                double WTP = Double.parseDouble(parts[5]);
                double lawnSize = Double.parseDouble(parts[6]);
                double irrigEfficiency = Double.parseDouble(parts[7]);
                double kCrop = Double.parseDouble(parts[8]);
                Household hh = new Household(index, hasTank, catchmentArea, tankDiameter, tankHeight, WTA, WTP,
                        lawnSize, irrigEfficiency, kCrop);
                schedule.scheduleRepeating(hh);
                _households[index] = hh;
                if (hasTank) {
                    prosumer_households.add(hh);
                } else{
                    consumer_households.add(hh);
                }
                index++;
            }
            sc.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /** Set the EPAnet node ID tags for each household*/
    private void setNodeIDs(){
        File nodeIDFile = new File(marketABM.nodeIDFileName);
        try {
            Scanner sc = new Scanner(nodeIDFile);
            int index = 0;
            while (sc.hasNextLine()) {
                String i = sc.nextLine();
                String[] parts = i.split("\t");
                String IRID = parts[0];
                String NDID = parts[1];
                String MSID = parts[2];
                _households[index].setNodeTags(IRID, NDID, MSID);
                index++;
            }
            sc.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }
    /** Set the number of time steps since last rain event is greater than the rainTradeGap*/
    private static void setHoursSinceRain(){
        if (rainfall.get(currentHour) > 0){
            hoursSinceRain = 0;
        }
        else{
            hoursSinceRain ++;
        }
    }

    /** Initialize rainfall data for the simulation from input file */
    private static void initializeRainfall(){
        File rainFile = new File(marketABM.rainfallDataFileName);
        try {
            Scanner sc = new Scanner(rainFile);
            sc.nextLine(); //skip header
            while (sc.hasNextLine()) {
                String i = sc.nextLine();
                String[] parts = i.split("\t");
                rainfall.add(Double.parseDouble(parts[2]));
            }
            sc.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void initializeClimateInfo(){
        File climateFile = new File(marketABM.climateInfoTextName);
        try {
            Scanner sc = new Scanner(climateFile);
            String i = sc.nextLine();
            this.potEvapotranspiration = Double.parseDouble(i.split("\t")[1]);
            i = sc.nextLine();
            this.effectiveRainfall = Double.parseDouble(i.split("\t")[1]);
            sc.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /** Initialize irrigation patterns to assign to each household */
    private void initializeIrrigPattern() {
        File irrigFile = new File(marketABM.irrigationDemandFileName);
        int[] irrPattern = new int[marketABM.HOURS_IN_DAY];
        try {
            Scanner sc = new Scanner(irrigFile);
            sc.nextLine(); //skip header
            int index = 0;
            while (sc.hasNextLine()) {
                String i = sc.nextLine();
                Scanner lsc = new Scanner(i);
                lsc.nextInt(); //skip hour label
                irrPattern[index] = lsc.nextInt();
                index++;
            }
            sc.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        int[] sumIrrList = getSummedList(irrPattern);
        int hourIndex = 0;
        int[] houseNumArray = IntStream.range(0, marketABM.consumer_households.size()).toArray();
        int[] shuffledArray = shuffleArray(houseNumArray);
        // generate demand patterns and assign them to randomly selected _households
//        for (int i = 0; i < marketABM.numberHouses; ) {
//
//            while (i < sumIrrList[hourIndex]) {
//                double[] thisDemand = new double[marketABM.hoursInSimulation];
//                int mixedIndex = shuffledArray[i];
//                // if agent doesn't have a tank, generate demand pattern; if it has a tank, assume zero demand
//                if (!this._households[mixedIndex].hasTank) {
//                    thisDemand = generateDemand(hourIndex, mixedIndex);
//                }
//                this._households[mixedIndex].setIrrigationDemand(thisDemand);
//                i += 1;
//            }
//            hourIndex += 1;
//        }
        for (int i = 0; i < marketABM.consumer_households.size();){
            while (i < sumIrrList[hourIndex]) {
                double[] thisDemand = new double[marketABM.hoursInSimulation];
                int mixedIndex = shuffledArray[i];
                thisDemand = generateDemand(hourIndex, mixedIndex);
                this._households[mixedIndex].setIrrigationDemand(thisDemand);
                i += 1;
            }
            hourIndex +=1;
        }
        for (int i = 0; i < marketABM.prosumer_households.size(); i++){
            double[] thisDemand = new double[marketABM.hoursInSimulation];
            this._households[marketABM.consumer_households.size() + i].setIrrigationDemand(thisDemand);
        }

    }


    /**
     * Calculate the daily demand for a household and assign it to be consumed at a specific hour each day
     * @param hour      the hour at which household agent waters lawn each day
     * @param houseID   the ID number of the household agent
     * @return an array of irrigation demand [gal/hour] at each hour of simulation for a household
     */
    public double[] generateDemand(int hour, int houseID) {
        // Jacob and Haarhofs 2004 formula for average monthly daily demand
        double dailyIrrigation = (this._households[houseID].lawnSize / this._households[houseID].irrigEfficiency) *
                (((this._households[houseID].kCrop * this.potEvapotranspiration)
                        - this.effectiveRainfall) / marketABM.daysInSimulation);
        dailyIrrigation = dailyIrrigation * marketABM.M3_TO_GAL_CONVERSION; //convert from cubic meters to gallons
        double irrigDemand[] = new double[marketABM.hoursInSimulation];
        int index = 0;
        while (index < marketABM.daysInSimulation) {
            irrigDemand[hour + (index * marketABM.HOURS_IN_DAY)] = dailyIrrigation;
            index++;
        }
        return irrigDemand;

    }

    /** Shuffle an array
     * @param array     array to shuffle
     */
    public static int[] shuffleArray(int[] array) {
        int index, temp;
        int[] arrayCopy = array.clone();
        for (int i = array.length - 1; i > 0; i--) {
            index = rng.nextInt(i + 1);
            temp = arrayCopy[index];
            arrayCopy[index] = arrayCopy[i];
            arrayCopy[i] = temp;
        }
        return arrayCopy;
    }

    /** Calculate the cumulative sum of agents at each time step
     * @param theList       list of agents to sum
     * @return list of cumulative sums
     */
    public int[] getSummedList(int[] theList) {
        int[] summedList = new int[theList.length];
        int currentSum = 0;
        for (int i = 0; i < theList.length; i++) {
            currentSum = currentSum + theList[i];
            summedList[i] = currentSum;
        }
        return summedList;
    }
}



  
  
