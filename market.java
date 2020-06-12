package rwh;

import sim.engine.SimState;
import sim.engine.Steppable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;


public class market implements Steppable {

    public market(String[] args) {
        setInputFiles(args);

    }

    /*Current hour of month [0-720] */
    public int currentHour = 0;
    /*Current day of year [0-364] */
    public int currentDay = 0;
    /*Number of hours in a day */
    public int hoursInDay = 24;
    /*Number of days in Month (April) */
    public int daysInMonth = 30;
    /*Number of hours in month (April) */
    public int hoursInMonth = 720;

    private String basicInfoTextName;
    private String rainfallDataFileName;
    private String irrigationDemandFileName;
    private String nodeIDFileName;

    public boolean initialized = false;

    //This int represents the number of households in the system
    //There must be this many households in the basic information input file!!!
    //TODO: change to count number of lines
    public int numberHouses = 2016;
    //An array to contain the households
    public Household[] households;

    //Final Result Containers
    public double[][][] sellerHistory = new double[this.numberHouses][this.hoursInMonth][3];
    public double[][][] buyerHistory = new double[this.numberHouses][this.hoursInMonth][2];

    /* Set input file names equal to command line arguments*/
    public void setInputFiles(String[] fileNames){
        basicInfoTextName = fileNames[0];
        rainfallDataFileName = fileNames[1];
        irrigationDemandFileName = fileNames[2];
        nodeIDFileName = fileNames[3];
    }



    //These steps are performed each trade interval
    public void step(SimState state) {
        //Initialization call (only performed once)
        if (initialized == false) {
            set_households();
        }
        System.out.println("Current Increment = " + this.currentHour);
        readNodalPressures();
        exchange();
        printHouseholdDemands();
        this.currentHour++;
        if ((this.currentHour % 24) == 0) {
            this.currentDay++;
        }
        for (int i = 0; i < this.numberHouses; i++) {
            this.households[i].step_increment();
        }

        //Tally up the results after the final trade interval
        if (this.currentHour == this.hoursInMonth) {
            tallyFlow();
            printIrrigationDemand();
            printNegativeDemand();
        }
    }

    //This method initializes the households with basic information, demand profile, and production profile
    public void set_households() {
        this.households = new Household[this.numberHouses];

//************************Basic Information Initialization Code*********************
        File infoFile = new File(basicInfoTextName);
        try {
            Scanner sc = new Scanner(infoFile);
            int index = 0;
            while (sc.hasNextLine()) {
                String i = sc.nextLine();
                String[] parts = i.split("\t");
                boolean hasTank = Boolean.parseBoolean(parts[0]);
                double catchmentArea = Double.parseDouble(parts[1]);
                double tankDiameter = Double.parseDouble(parts[2]);
                double tankHeight = Double.parseDouble(parts[3]);
                double WTA = Double.parseDouble(parts[4]);
                double WTP = Double.parseDouble(parts[5]);
                Household objectSet = new Household(index, hasTank, catchmentArea, tankDiameter, tankHeight, WTA, WTP);
                this.households[index] = objectSet;
                index++;
            }
            sc.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
//************************End of Basic Information Initialization Code*********************

//************************Rainfall Initialization Code***************************************

        //Rainfall must be a columnar data set with 720 rows --> Cells indicate rainfall in inches
        File rainFile = new File(rainfallDataFileName);
        try {
            double[] rainfall = new double[this.hoursInMonth + 1];
            Scanner sc = new Scanner(rainFile);
            int index = 0;
            while (sc.hasNextLine()) {
                String i = sc.nextLine();
                rainfall[index] = Double.parseDouble(i);
                index++;
            }
            for (int i = 0; i < this.numberHouses; i++) {
                double[] copyRainfall = new double[this.hoursInMonth];
                for (int j = 0; j < this.hoursInMonth; j++) {
                    copyRainfall[j] = rainfall[j];
                }
                this.households[i].setHourlyRainfall(copyRainfall);
            }
            sc.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
 //************************End of Rainfall Initialization Code************************************

//************************Irrigation Demand Initialization Code***************************************
        File irrigFile = new File(irrigationDemandFileName);
        int[] irrPattern = new int[this.hoursInDay];
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
        int ind = 0;
        for (int i = 0; i < this.numberHouses; ) {
            double[] thisDemand;
            while (i < sumIrrList[ind]) {
                thisDemand = generateDemand(ind);
                this.households[i].setIrrigationDemand(thisDemand);
                i += 1;
            }
            ind += 1;
        }
 //************************End of Irrigation Demand Initialization Code************************************

//************************Household Nodes Initialization Code*********************

        //Text infoFile with three columns and as many rows as there are households in the model
        File nodeIDFile = new File(nodeIDFileName);
        try {
            Scanner sc = new Scanner(nodeIDFile);
            int index = 0;
            while (sc.hasNextLine()) {
                String i = sc.nextLine();
                String[] parts = i.split("\t");
                String IRID = parts[0];
                String NDID = parts[1];
                String MSID = parts[2];
                this.households[index].setNodeTags(IRID, NDID, MSID);
                index++;
            }
            sc.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

//************************End of Nodes Initialization Code*********************
    }

    public double[] generateDemand(int hour) {
        //TODO: update testIrrigation value
        double testIrrigation = 500.0; //gallons per day, to replace with appropriate calculation function later
        double irrigDemand[] = new double[this.hoursInMonth];
        int index = 0;
        while (index < this.daysInMonth) {
            irrigDemand[hour + (index * this.hoursInDay)] = testIrrigation;
            index++;
        }
        return irrigDemand;
    }

    public int[] getSummedList(int[] theList) {
        int[] summedList = new int[theList.length];
        int currentSum = 0;
        for (int i = 0; i < theList.length; i++) {
            currentSum = currentSum + theList[i];
            summedList[i] = currentSum;
        }
        return summedList;
    }

    public void exchange() {
        boolean continuationFlag = true;
        int currentRound = 0;
        do {

            double[] excess = new double[this.numberHouses];
            double[] demand = new double[this.numberHouses];
            for (int i = 0; i < this.numberHouses; i++) {
                if (this.households[i].rainCheck()) {
                    demand[i] = 0;
                    excess[i] = 0;
                    System.out.println("Rain check ------------------ No Trading Activity!!!!!");
                } else {
                    if ((this.households[i].getDemand() - this.buyerHistory[i][this.currentHour][0]) > 0.0) {
                        demand[i] = this.households[i].getDemand() - this.buyerHistory[i][this.currentHour][0];
                        //System.out.println(demand[i]);
                        //System.exit(0);
                    } else {
                        demand[i] = 0;
                    }

                    if ((this.households[i].estimateAvailableStorage() - this.sellerHistory[i][this.currentHour][0]) > 0.0) {
                        excess[i] = this.households[i].estimateAvailableStorage() - this.sellerHistory[i][this.currentHour][0];
                        //System.out.println(excess[i]);
                        //System.exit(0);
                    } else {
                        excess[i] = 0;
                    }
                }

            }

            //Boolean flag for zero demand
            boolean zeroDemand = true;
            for (int i = 0; i < this.numberHouses; i++) {
                if (demand[i] > 0.0) {
                    zeroDemand = false;
                }
            }
            if (zeroDemand) {
                continuationFlag = false;
            }
            //Boolean flag for zero excess
            boolean zeroExcess = true;
            for (int i = 0; i < this.numberHouses; i++) {
                if (excess[i] > 0.0) {
                    zeroExcess = false;
                }
            }
            if (zeroExcess) {
                continuationFlag = false;
            }

            int buyerCount = 0;
            int sellerCount = 0;
            for (int i = 0; i < this.numberHouses; i++) {
                if (excess[i] > 0) {
                    sellerCount++;
                }
                if (demand[i] > 0) {
                    buyerCount++;
                }
            }
            int[] buyerIndex = new int[buyerCount];
            int[] sellerIndex = new int[sellerCount];
            //fill buyer index
            int indexB = 0;
            for (int i = 0; i < this.numberHouses; i++) {
                if (demand[i] > 0) {
                    buyerIndex[indexB] = i;
                    indexB++;
                }
            }
            //fill seller index
            int indexS = 0;
            for (int i = 0; i < this.numberHouses; i++) {
                if (excess[i] > 0) {
                    sellerIndex[indexS] = i;
                    indexS++;
                }
            }

            //Functions for buyers
            boolean[] buyerCovered = new boolean[buyerCount];
            for (int i = 0; i < buyerCount; i++) {
                buyerCovered[i] = false;
            }
            int[] buyersOrdered = new int[buyerCount];
            //System.out.println("Buyers Ordered = " + buyersOrdered.length);
            for (int i = 0; i < buyerCount; i++) {
                int currentMaxIndex = 1000;
                double currentMax = -10000000.0;
                for (int j = 0; j < buyerCount; j++) {
                    if (this.households[buyerIndex[j]].WTP >= currentMax && buyerCovered[j] == false) {
                        currentMaxIndex = buyerIndex[j];
                        currentMax = this.households[buyerIndex[j]].WTP;
                    }
                }
                buyersOrdered[i] = currentMaxIndex;
                for (int k = 0; k < buyerCount; k++) {
                    if (buyerIndex[k] == currentMaxIndex) {
                        buyerCovered[k] = true;
                    }
                }
            }

            //Functions for sellers
            boolean[] sellerCovered = new boolean[sellerCount];
            for (int i = 0; i < sellerCount; i++) {
                sellerCovered[i] = false;
            }
            int[] sellersOrdered = new int[sellerCount];
            for (int i = 0; i < sellerCount; i++) {
                int currentMinIndex = 1000;
                double currentMin = 10000000.0;
                for (int j = 0; j < sellerCount; j++) {
                    if (this.households[sellerIndex[j]].WTA <= currentMin && sellerCovered[j] == false) {
                        currentMinIndex = sellerIndex[j];
                        currentMin = this.households[sellerIndex[j]].WTA;
                    }
                }
                sellersOrdered[i] = currentMinIndex;
                for (int k = 0; k < sellerCount; k++) {
                    if (sellerIndex[k] == currentMinIndex) {
                        sellerCovered[k] = true;
                    }
                }
            }

            int totalExchanges = 0;
            if (buyersOrdered.length < sellersOrdered.length) {
                totalExchanges = buyersOrdered.length;
            } else {
                totalExchanges = sellersOrdered.length;
            }

            System.out.println(buyersOrdered.length);
            //Boolean flag to check WTA and WTP of first paired traders
            if (sellersOrdered.length == 0 || buyersOrdered.length == 0) {
                continuationFlag = false;
                if (currentRound > 0) {
                    System.out.println("No buyers");
                    System.out.println("Current Hour = " + this.currentHour);
                    System.out.println();
                    //System.exit(0);
                }
            } else {
                if (this.households[buyersOrdered[0]].WTP < this.households[sellersOrdered[0]].WTA) {
                    continuationFlag = false;
                }
            }


            for (int i = 0; i < totalExchanges; i++) {
                int sellerIndex2 = sellersOrdered[i];
                int buyerIndex2 = buyersOrdered[i];
                double seller_wta = this.households[sellerIndex2].WTA;
                double buyer_wtp = this.households[buyerIndex2].WTP;
                double seller_cap = excess[sellerIndex2];
                double buyer_need = demand[buyerIndex2];
                //Exchange Amount in units of kWh
                double exchangeAmount = 0.0;
                if (seller_wta <= buyer_wtp) {
                    if (seller_cap >= buyer_need) {
                        exchangeAmount = buyer_need;
                    } else if (seller_cap <= 0) {
                        exchangeAmount = 0;
                    } else {
                        exchangeAmount = seller_cap;
                    }

                    if (true) {
                        System.out.println("----------- An Exchange Has Been Made");
                        System.out.println("Seller: " + sellerIndex2 + " Seller WTA = " + seller_wta + " Buyer: " + buyerIndex2 + " Buyer WTP = " + buyer_wtp);
                        System.out.println("Seller Cap = " + seller_cap);
                        System.out.println("Buyer Need = " + buyer_need);
                        System.out.println("Exchange Amount = " + exchangeAmount);
                    }

                    //System.exit(0);


                } else {
                    System.out.println("No Exchange Made -----------");
                    System.out.println("Seller: " + sellerIndex2 + " Seller WTA = " + seller_wta + " Buyer: " + buyerIndex2 + " Buyer WTP = " + buyer_wtp);
                    System.out.println("Seller Cap = " + seller_cap);
                    System.out.println("Buyer Need = " + buyer_need);
                }

                System.out.println("Exchange = " + exchangeAmount);
                if (exchangeAmount > 0) {
                    this.sellerHistory[sellerIndex2][this.currentHour][0] += exchangeAmount;
                    this.sellerHistory[sellerIndex2][this.currentHour][1] += buyer_wtp * exchangeAmount;
                    this.buyerHistory[buyerIndex2][this.currentHour][0] += exchangeAmount;
                    this.buyerHistory[buyerIndex2][this.currentHour][1] += buyer_wtp * exchangeAmount;
                }
            }

            currentRound++;
        } while (continuationFlag);

        System.out.println("End of Trade Interval - " + this.currentHour + "   ******************************");
        for (int i = 0; i < this.numberHouses; i++) {
            if (this.households[i].hasTank) {
                this.households[i].scheduleExchangeDischarge(this.sellerHistory[i][this.currentHour][0]);
                this.households[i].storageCalcBegin();
                this.households[i].rainfallStorageCalc();
            }
        }


    }

    public void readNodalPressures() {
        //
        String filePath = "/Users/lizramsey/Documents/workspace/RWH/Nodal Pressures.txt";

        File file = new File(filePath);

        try {

            double[] nodePressure = new double[this.numberHouses * 3];

            Scanner sc = new Scanner(file);

            int index = 0;

            while (sc.hasNextLine()) {
                String i = sc.nextLine();
                String[] parts = i.split("\t");
                double IRP = Double.parseDouble(parts[0]);
                double NDP = Double.parseDouble(parts[1]);
                double MSP = Double.parseDouble(parts[2]);

                this.households[index].pressureIrrigationDemandNode[this.currentHour] = IRP;
                this.households[index].pressureNegativeDemandNode[this.currentHour] = NDP;
                this.households[index].pressureMainSystemNode[this.currentHour] = MSP;

                index++;
            }

            for (int i = 0; i < this.numberHouses; i++) {
                //this.households[i].readNodalPressures(nodeTag, nodePressure);
            }

            sc.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    //This method prints household node pressures to a text file
    public void printHouseholdPressures() {
        try {

            String fileName = "/Users/lizramsey/Documents/workspace/RWH/output/Pressure At Nodes.txt";
            File file = new File(fileName);
            PrintWriter writer = new PrintWriter(file);
            for (int i = 0; i < this.numberHouses; i++) {
                writer.println(this.households[i].pressureIrrigationDemandNode[this.currentHour] + "\t" + this.households[i].pressureNegativeDemandNode[this.currentHour] + "\t" + this.households[i].pressureMainSystemNode[this.currentHour]);
            }

            writer.close();
        } catch (FileNotFoundException e) {

        }
    }


    //This method prints demands to a text file
    public void printHouseholdDemands() {
        try {

            String fileName = "/Users/lizramsey/Documents/workspace/RWH/output/Demand At Nodes.txt";
            File file = new File(fileName);
            PrintWriter writer = new PrintWriter(file);
            for (int i = 0; i < this.numberHouses; i++) {
                writer.println((-1.0 * (this.households[i].totalStorageVolumeToMainSystem[this.currentHour] / 60.0)) + "\t" + (this.households[i].totalDemand[this.currentHour] / 60.0));
            }

            writer.close();
        } catch (FileNotFoundException e) {

        }
    }

    //
    public void printIrrigationDemand() {
        try {

            String fileName = "/Users/lizramsey/Documents/workspace/RWH/output/Irrigation Demand Output.txt";
            File file = new File(fileName);
            PrintWriter writer = new PrintWriter(file);
            for (int i = 0; i < this.numberHouses; i++) {
                String line = "";
                for (int j = 0; j < this.hoursInMonth; j++) {
                    line += (this.households[i].irrigationDemand[j] / 60.0);
                    line += "\t";
                }
                writer.println(line);
            }

            writer.close();
        } catch (FileNotFoundException e) {

        }
    }

    //
    public void printNegativeDemand() {
        try {

            String fileName = "/Users/lizramsey/Documents/workspace/RWH/output/Negative Demand Output.txt";
            File file = new File(fileName);
            PrintWriter writer = new PrintWriter(file);
            for (int i = 0; i < this.numberHouses; i++) {
                String line = "";
                for (int j = 0; j < this.hoursInMonth; j++) {
                    line += (-this.households[i].totalStorageVolumeToMainSystem[j] / 60.0);
                    line += "\t";
                }
                writer.println(line);
            }

            writer.close();
        } catch (FileNotFoundException e) {

        }
    }

    //
    public void tallyFlow() {
        double[] rainfallToTank = new double[this.hoursInMonth];
        double[] firstFlush = new double[this.hoursInMonth];
        double[] rainfallOverflow = new double[this.hoursInMonth];
        double[] storageToMain = new double[this.hoursInMonth];
        double[] totalDemand = new double[this.hoursInMonth];
        for (int i = 0; i < this.numberHouses; i++) {
            for (int j = 0; j < this.hoursInMonth; j++) {
                rainfallToTank[j] += this.households[i].rainfallToTankVolume[j];
                firstFlush[j] += this.households[i].firstFlushDiversionVolume[j];
                rainfallOverflow[j] += this.households[i].rainfallOverflowVolume[j];
                storageToMain[j] += this.households[i].totalStorageVolumeToMainSystem[j];
                totalDemand[j] += this.households[i].totalDemand[j];
            }
        }
        try {

            String fileName = "/Users/lizramsey/Documents/workspace/RWH/output/All Seller Information.txt";
            File file = new File(fileName);
            PrintWriter writer = new PrintWriter(file);
            for (int i = 0; i < 744; i++) {
                writer.println(i + " Rainfall to Tank Volume = \t" + rainfallToTank[i]);
            }
            for (int i = 0; i < 744; i++) {
                writer.println(i + " First Flush Diversion Volume = \t" + firstFlush[i]);
            }
            for (int i = 0; i < 744; i++) {
                writer.println(i + " Rainfall Overflow Volume = \t" + rainfallOverflow[i]);
            }
            for (int i = 0; i < 744; i++) {
                writer.println(i + " Tank Level Beginning of Hour = \t");
            }
            for (int i = 0; i < 744; i++) {
                writer.println(i + " Storage Volume Beginning of Hour = \t");
            }
            for (int i = 0; i < 744; i++) {
                writer.println(i + " Total Storage Volume to Main System = \t" + storageToMain[i]);
            }
            for (int i = 0; i < 744; i++) {
                writer.println(i + " Total Demand of Households = \t" + totalDemand[i]);
            }

            writer.close();
        } catch (FileNotFoundException e) {

        }
    }

}
