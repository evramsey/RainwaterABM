package rwh;

import sim.engine.SimState;
import sim.engine.Steppable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.IntStream;


public class market implements Steppable {


    /* Current hour of simulation */
    public int currentHour = 0;
    /* Current day of year */
    public int currentDay = 0;

    //An array to contain the households
    public Household[] households;

    //Arrays to contain the final results
    public double[][][] sellerHistory;
    public double[][][] buyerHistory;


    public market(String[] args) {
        setHouseholds();
        setHistories();
    }

    //These steps are performed each trade interval
    public void step(SimState state) {

        System.out.println("Current Increment = " + this.currentHour);
        //readNodalPressures();
        exchange();
        printHouseholdDemands();
        this.currentHour++;
        if ((this.currentHour % 24) == 0) {
            this.currentDay++;
        }
        for (int i = 0; i < abm.numberHouses; i++) {
            this.households[i].step_increment();
        }

        //Tally up the results after the final trade interval
        if (this.currentHour == abm.hoursInSimulation) {
            tallyFlow();
            printIrrigationDemand();
            printNegativeDemand();
        }
    }

    //This method initializes the households with basic information, demand profile, and production profile
    public void setHouseholds() {
        this.households = new Household[abm.numberHouses];

//************************Basic Information Initialization Code*********************
        File infoFile = new File(abm.basicInfoTextName);
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
                double evapotrans = Double.parseDouble(parts[9]);
                double effectiveRainfall = Double.parseDouble(parts[10]);
                Household objectSet = new Household(index, hasTank, catchmentArea, tankDiameter, tankHeight, WTA, WTP,
                        lawnSize, irrigEfficiency, kCrop, evapotrans, effectiveRainfall);
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
        File rainFile = new File(abm.rainfallDataFileName);
        try {
            double[] rainfall = new double[abm.hoursInSimulation + 1];
            Scanner sc = new Scanner(rainFile);
            int index = 0;
            sc.nextLine(); //skip header
            while (sc.hasNextLine()) {
                String i = sc.nextLine();
                String[] parts = i.split("\t");
                rainfall[index] = Double.parseDouble(parts[2]);
                index++;
            }
            for (int i = 0; i < abm.numberHouses; i++) {
                double[] copyRainfall = new double[abm.hoursInSimulation];
                for (int j = 0; j < abm.hoursInSimulation; j++) {
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
        File irrigFile = new File(abm.irrigationDemandFileName);
        int[] irrPattern = new int[abm.hoursInDay];
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

        int[] houseNumArray = IntStream.range(0, abm.numberHouses).toArray();
        shuffleArray(houseNumArray);
        /* generate demand patterns and assign them to randomly selected households*/
        for (int i = 0; i < abm.numberHouses; ) {
            double[] thisDemand = new double[abm.hoursInSimulation];
            while (i < sumIrrList[hourIndex]) {
                int mixedIndex = houseNumArray[i];
                if (this.households[mixedIndex].hasTank) {
                    for (int j = 0; j < abm.hoursInSimulation; j++) {
                        thisDemand[j] = 0.0;
                    }
                } else {
                    thisDemand = generateDemand(hourIndex, mixedIndex);
                }
                this.households[mixedIndex].setIrrigationDemand(thisDemand);
                i += 1;
            }
            hourIndex += 1;
        }
        //************************End of Irrigation Demand Initialization Code************************************

//************************Household Nodes Initialization Code*********************

        //Text infoFile with three columns and as many rows as there are households in the model
        File nodeIDFile = new File(abm.nodeIDFileName);
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


    public double[] generateDemand(int hour, int houseID) {
        // Jacob and Haarhofs 2004 formula for average monthly daily demand
        double dailyIrrigation = (this.households[houseID].lawnSize / this.households[houseID].irrigEfficiency) *
                (((this.households[houseID].kCrop * this.households[houseID].evapotrans)
                        - this.households[houseID].effectiveRainfall) / abm.daysInSimulation);
        dailyIrrigation = dailyIrrigation * abm.cubicMeterToGallonConversion; //convert from cubic meters to gallons
        double irrigDemand[] = new double[abm.hoursInSimulation];
        int index = 0;
        while (index < abm.daysInSimulation) {
            irrigDemand[hour + (index * abm.hoursInDay)] = dailyIrrigation;
            index++;
        }
        return irrigDemand;
    }

    public void exchange() {
        boolean continuationFlag = true;
        int currentRound = 0;
        do {

            double[] excess = new double[abm.numberHouses];
            double[] demand = new double[abm.numberHouses];
            /* iterate through houses */
            for (int i = 0; i < abm.numberHouses; i++) {
                /* set demand and excess to zero if it has recently rained */
                if (this.households[i].rainCheck()) {
                    demand[i] = 0;
                    excess[i] = 0;
//                    System.out.println("Rain check ------------------ No Trading Activity!!!!!");
                } else {
                    if ((this.households[i].irrigationDemand[this.currentHour] - this.buyerHistory[i][this.currentHour][0]) > 0.0) {
                        demand[i] = this.households[i].irrigationDemand[this.currentHour] - this.buyerHistory[i][this.currentHour][0];
                        //System.out.println(demand[i]);
                        //System.exit(0);
                    } else {
                        demand[i] = 0;
                    }

                    if ((this.households[i].estimateAvailableStorage() - this.sellerHistory[i][this.currentHour][0]) > 0.0) {
                        excess[i] = this.households[i].estimateAvailableStorage() - this.sellerHistory[i][this.currentHour][0];
                    } else {
                        excess[i] = 0;
                    }
                }

            }

            //Boolean flag for zero demand
            boolean zeroDemand = true;
            for (int i = 0; i < abm.numberHouses; i++) {
                if (demand[i] > 0.0) {
                    zeroDemand = false;
                    break;
                }
            }
            if (zeroDemand) {
                continuationFlag = false;
            }
            //Boolean flag for zero excess
            boolean zeroExcess = true;
            for (int i = 0; i < abm.numberHouses; i++) {
                if (excess[i] > 0.0) {
                    zeroExcess = false;
                    break;
                }
            }
            if (zeroExcess) {
                continuationFlag = false;
            }

            int buyerCount = 0;
            int sellerCount = 0;
            for (int i = 0; i < abm.numberHouses; i++) {
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
            for (int i = 0; i < abm.numberHouses; i++) {
                if (demand[i] > 0) {
                    buyerIndex[indexB] = i;
                    indexB++;
                }
            }
            //fill seller index
            int indexS = 0;
            for (int i = 0; i < abm.numberHouses; i++) {
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

            int totalExchanges;
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
        for (int i = 0; i < abm.numberHouses; i++) {
            if (this.households[i].hasTank) {
                this.households[i].scheduleExchangeDischarge(this.sellerHistory[i][this.currentHour][0]);
                this.households[i].storageCalcBegin();
                this.households[i].rainfallStorageCalc();
            }
        }
    }

    /* Shuffle an array*/
    private void shuffleArray(int[] array) {
        int index, temp;
        Random random = new Random();
        for (int i = array.length - 1; i > 0; i--) {
            index = random.nextInt(i + 1);
            temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
    }

    //Print demands to a text file
    public void printHouseholdDemands() {
        try {
            File file = new File(abm.nodalDemandOutputFileName);
            PrintWriter writer = new PrintWriter(file);
            for (int i = 0; i < abm.numberHouses; i++) {
                writer.println((-1.0 * (this.households[i].totalStorageVolumeToMainSystem[this.currentHour] / 60.0)) + "\t" + (this.households[i].totalIrrigDemand[this.currentHour] / 60.0));
            }

            writer.close();
        } catch (FileNotFoundException e) {

        }
    }

    //Print total irrigation demand to a text file
    public void printIrrigationDemand() {
        try {
            File file = new File(abm.irrigationDemandOutputFileName);
            PrintWriter writer = new PrintWriter(file);
            for (int i = 0; i < abm.numberHouses; i++) {
                String line = "";
                for (int j = 0; j < abm.hoursInSimulation; j++) {
                    line += (this.households[i].irrigationDemand[j] / 60.0);
                    line += "\t";
                }
                writer.println(line);
            }

            writer.close();
        } catch (FileNotFoundException e) {

        }
    }

    // Print negative demand to a text file
    public void printNegativeDemand() {
        try {
            File file = new File(abm.negativeDemandOutputFileName);
            PrintWriter writer = new PrintWriter(file);
            for (int i = 0; i < abm.numberHouses; i++) {
                String line = "";
                for (int j = 0; j < abm.hoursInSimulation; j++) {
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
        double[] rainfallToTank = new double[abm.hoursInSimulation];
        double[] firstFlush = new double[abm.hoursInSimulation];
        double[] rainfallOverflow = new double[abm.hoursInSimulation];
        double[] storageToMain = new double[abm.hoursInSimulation];
        double[] totalDemand = new double[abm.hoursInSimulation];
        for (int i = 0; i < abm.numberHouses; i++) {
            for (int j = 0; j < abm.hoursInSimulation; j++) {
                rainfallToTank[j] += this.households[i].rainfallToTankVolume[j];
                firstFlush[j] += this.households[i].firstFlushDiversionVolume[j];
                rainfallOverflow[j] += this.households[i].rainfallOverflowVolume[j];
                storageToMain[j] += this.households[i].totalStorageVolumeToMainSystem[j];
                totalDemand[j] += this.households[i].totalIrrigDemand[j];
            }
        }
        try {

            File file = new File(abm.allSellerInfoOutputFileName);
            PrintWriter writer = new PrintWriter(file);
            for (int i = 0; i < abm.hoursInSimulation; i++) {
                writer.println(i + " Rainfall to Tank Volume = \t" + rainfallToTank[i]);
            }
            for (int i = 0; i < abm.hoursInSimulation; i++) {
                writer.println(i + " First Flush Diversion Volume = \t" + firstFlush[i]);
            }
            for (int i = 0; i < abm.hoursInSimulation; i++) {
                writer.println(i + " Rainfall Overflow Volume = \t" + rainfallOverflow[i]);
            }
            for (int i = 0; i < abm.hoursInSimulation; i++) {
                writer.println(i + " Tank Level Beginning of Hour = \t");
            }
            for (int i = 0; i < abm.hoursInSimulation; i++) {
                writer.println(i + " Storage Volume Beginning of Hour = \t");
            }
            for (int i = 0; i < abm.hoursInSimulation; i++) {
                writer.println(i + " Total Storage Volume to Main System = \t" + storageToMain[i]);
            }
            for (int i = 0; i < abm.hoursInSimulation; i++) {
                writer.println(i + " Total Demand of Households = \t" + totalDemand[i]);
            }

            writer.close();
        } catch (FileNotFoundException e) {

        }
    }

    /* Calculate the total number of agents at each time step*/
    public int[] getSummedList(int[] theList) {
        int[] summedList = new int[theList.length];
        int currentSum = 0;
        for (int i = 0; i < theList.length; i++) {
            currentSum = currentSum + theList[i];
            summedList[i] = currentSum;
        }
        return summedList;
    }

    /* Set the sizes of final result arrays*/
    public void setHistories() {
        sellerHistory = new double[abm.numberHouses][abm.hoursInSimulation][3];
        buyerHistory = new double[abm.numberHouses][abm.hoursInSimulation][2];
    }

}
