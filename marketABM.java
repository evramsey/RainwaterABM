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
    public static double INCH_TO_MM_CONVERSION = 25.4;
    public static double MM_TO_M_CONVERSION = 0.001;

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
    public static double potEvapotranspiration;
    /** Effective rainfall [m] (Jacobs and Haarhofs, 2004) */
    public static double effectiveRainfall;

    /** Variables updated by the model */

    /** Current hour of simulation */
    public static int currentHour = 0;
    /** Array of household agents*/
    public static Household[] households;
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
        initializeRainfall();
        initializeClimateInfo();
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
        households = new Household[numberHouses];
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
                households[index] = hh;
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
                households[index].setNodeTags(IRID, NDID, MSID);
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

    /** Initialize rainfall data for the simulation from input file and calculate effective rainfall
     * (Jacobs and Hoorhof, 2004)
     * */
    private static void initializeRainfall(){
        File rainFile = new File(marketABM.rainfallDataFileName);
        double rainSum = 0.0;
        try {
            Scanner sc = new Scanner(rainFile);
            sc.nextLine(); //skip header
            while (sc.hasNextLine()) {
                String i = sc.nextLine();
                String[] parts = i.split("\t");
                double rain = Double.parseDouble(parts[2]);
                rainfall.add(rain);
                rainSum += rain;
            }
            sc.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        double mmRainSum = rainSum * INCH_TO_MM_CONVERSION;
        if (mmRainSum < 25){
            effectiveRainfall = mmRainSum;
        }
        else if (mmRainSum > 25 & mmRainSum < 152){
            effectiveRainfall = 0.504 * mmRainSum + 12.4;
        }
        else if (mmRainSum >= 152){
            effectiveRainfall = 89.0;
        }
        effectiveRainfall = effectiveRainfall * MM_TO_M_CONVERSION;
    }

    private void initializeClimateInfo(){
        File climateFile = new File(marketABM.climateInfoTextName);
        try {
            Scanner sc = new Scanner(climateFile);
            String i = sc.nextLine();
            this.potEvapotranspiration = Double.parseDouble(i.split("\t")[1]);
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
        int[] houseNumArray = IntStream.range(0, consumer_households.size()).toArray();
        int[] shuffledArray = shuffleArray(houseNumArray);
        // generate demand patterns and assign them to randomly selected households
        for (int i = 0; i < consumer_households.size();){
            while (i < sumIrrList[hourIndex]) {
                double[] thisDemand;
                int mixedIndex = shuffledArray[i];
                thisDemand = generateDemand(hourIndex, mixedIndex);
                consumer_households.get(mixedIndex).setIrrigationDemand(thisDemand);
                i += 1;
            }
            hourIndex +=1;
        }
        for (int i = 0; i < prosumer_households.size(); i++){
            double[] thisDemand = new double[hoursInSimulation];
            prosumer_households.get(i).setIrrigationDemand(thisDemand);
        }
        for (int i = 0; i < households.length; i++){
            if (! households[i].hasTank){
                double[] demandArray = households[i].irrigDemandPattern;
                for (int j = 0; j < demandArray.length; j ++){
                    System.out.print(demandArray[j] + ", ");
                }
            }
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
        double dailyIrrigation = (this.households[houseID].lawnSize / this.households[houseID].irrigEfficiency) *
                (((this.households[houseID].kCrop * this.potEvapotranspiration)
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
