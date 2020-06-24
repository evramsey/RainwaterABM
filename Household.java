package rwh;

public class Household {
    /*Index value of household in the model */
    public int index;
    /*Catchment area of rainwater harvesting system [ft^2] */
    private double catchmentArea;
    /*Rainwater storage tank diameter [m] - Cylindrical Tank */
    public double tankDiameter;
    /*Rainwater storage tank height [m] - Cylindrical Tank */
    public double tankHeight;
    /*Rainwater tank storage volume [gal] */
    public double tankStorageVolume;


    /*EPANET Node ID for household negative demand node */
    public String negativeDemandNodeID;
    /*EPANET Node ID for household irrigation demand node */
    public String irrigationDemandNodeID;
    /*EPANET Node ID for household main system node */
    public String mainSystemNodeID;

    /*Current hour of month [0-720] */
    public int currentHour = 0;
    /*Current day of year [0-30] */
    public int currentDay = 0;
    //Willingness to accept value
    public double WTA = 0.0;
    //Willingness to pay value
    public double WTP = 0.0;
    public double lawnSize = 0.0;
    public double irrigEfficiency = 0.0;
    public double kCrop = 0.0;
    public double evapotrans = 0.0;
    public double effectiveRainfall;
    /*Household irrigation demand for the current hour [gal] */
    public double[] irrigationDemand = new double[abm.hoursInSimulation];
    /*Total demand across all households for the current hour [gal] */
    public double[] totalIrrigDemand = new double[abm.hoursInSimulation];
    /*Rainfall during current hour [in] */
    public double[] hourlyRainfall = new double[abm.hoursInSimulation];
    /*Total hourly rainfall volume [gal] */
    public double[] totalHourlyRainfallVolume = new double[abm.hoursInSimulation];
    /*First flush diversion volume for current hour [gal] */
    public double[] firstFlushDiversionVolume = new double[abm.hoursInSimulation];
    /*Rainfall volume to storage tank [gal] */
    public double[] rainfallToTankVolume = new double[abm.hoursInSimulation];
    /*Storage overflow volume from rainfall during the current hour [gal] */
    public double[] rainfallOverflowVolume = new double[abm.hoursInSimulation];
    /*Storage overflow volume from rainfall sent to infiltration bore during the current hour [gal] */
    public double[] rainfallToInfiltrationVolume = new double[abm.hoursInSimulation];
    /*Storage overflow volume from rainfall sent to storm drain during the current hour [gal]  */
    public double[] rainfallToStormDrainVolume = new double[abm.hoursInSimulation];

    /*Number of hours between rainfall events before first flush diversion can be reactivated */
    public int flushDiversionRainfallGap = 2;
    /*Boolean flag container to determine if there was rainfall within the number of hours indicated by flush diversion steps */
    public boolean[] rainfallWithinGapFlag = new boolean[abm.hoursInSimulation];
    /*Number of hours that must go by after a rain event before trade activity resumes */
    //TODO: update rainTradeGap after testing
    public int rainTradeGap = 1;
    /*Number of hours since the last rain event */
    public int[] hoursSinceLastRainEvent = new int[abm.hoursInSimulation];
    /*First flush diversion volumetric requirement [gal] */
    /*TODO: change flush diversion requirement after testing*/
    // public double flushDiversionRequirement = 75.0;
    public double flushDiversionRequirement = 5.0;
    /*Tank storage volume at the beginning of the current hour [gal] */
    public double[] storageVolumeBeginInterval = new double[abm.hoursInSimulation];
    /*Tank storage volume for household demand during the current hour [gal] */
    public double[] storageVolumeForHouseholdDemand = new double[abm.hoursInSimulation];
    /*Tank storage volume after discharge for household demand [gal] */
    public double[] storageVolumeAfterHouseholdDemand = new double[abm.hoursInSimulation];
    /*Tank storage volume to be sent to reclaimed pipeline for rwh.market settlement during the current hour [gal] */
    public double[] storageVolumeForMarket = new double[abm.hoursInSimulation];
    /*Tank storage volume to be sent to reclaimed pipeline for utility settlement during the current hour [gal] */
    public double[] storageVolumeForUtility = new double[abm.hoursInSimulation];
    /*Total tank storage volume to be sent to reclaimed pipeline during the current hour [gal] */
    public double[] totalStorageVolumeToMainSystem = new double[abm.hoursInSimulation];
    /*Flow rate from tank to main system [gpm] */
    public double[] flowrateFromTankToMainSystem = new double[abm.hoursInSimulation];
    /*Tank storage volume after tank discharge for current hour [gal] */
    public double[] storageVolumeAfterDischarge = new double[abm.hoursInSimulation];

    ////////////////////////////////////////////////////////////////////////////

    /*Scheduled tank discharges [gal] - [hour scheduled][hours ahead] */
    public int storageDischargeDuration = 6;
    public double[][] storageDischargeSchedule = new double[abm.hoursInSimulation][storageDischargeDuration];
    /*Maximum tank discharge for any given hour [gal] - This should actually be determined by the chosen pump curve */
    public double[] maximumHourlyDischarge = new double[abm.hoursInSimulation];

    /*Tank storage level at the beginning of the current hour [m] */
    public double[] tankLevelBeginInterval = new double[abm.hoursInSimulation];
    /*Tank storage level after discharge for household demand [m] */
    public double[] tankLevelAfterHouseholdDemand = new double[abm.hoursInSimulation];
    /*Tank storage level after discharge for current hour [m] */
    public double[] tankLevelAfterDischarge = new double[abm.hoursInSimulation];

    public double tankLevelFinal;

    /*Pressure of the negative demand node at beginning of current hour [psi] - Passed from EPANET at beginning of current hour */
    public double[] pressureNegativeDemandNode = new double[abm.hoursInSimulation];
    /*Pressure of the main system demand node at beginning of current hour [psi] - Passed from EPANET at beginning of current hour */
    public double[] pressureMainSystemNode = new double[abm.hoursInSimulation];
    /*Pressure of the irrigation demand node at beginning of current hour [psi] - Passed from EPANET at beginning of current hour */
    public double[] pressureIrrigationDemandNode = new double[abm.hoursInSimulation];
//    /*Estimated pressure of negative demand node with flow from storage tank [psi] */
//    public double[] pressureEstimateNegativeDemandNode = new double[abm.hoursInSimulation];
//    /*Estimated pressure of main system node with flow from storage tank [psi] */
//    public double[] pressureEstimateMainSystemNode = new double[abm.hoursInSimulation];
//    /*Estimated pressure of irrigation demand node with flow from storage tank [psi] */
//    public double[] pressureEstimateIrrigationDemandNode = new double[abm.hoursInSimulation];

    /*Flag for rainwater harvesting tank system existence */
    public boolean hasTank;

    //Storage remaining at the end of simulation
    public double finalStorage = 0.0;

    //Hannah's Variables
    ///////////////////////////////////////////////////////////////////

    double radiusOfPipe = 0.25; //[ft]
    double densityOfWater = (62.4 * 1723); //[lb/in^3]
    double gravity = 32.17; //[ft/s^2]
    double areaOfPipe = ((Math.PI) * Math.pow(radiusOfPipe, 2)); //[ft^2]
    double radiusOfTank = 1.5; //[ft]
    double areaOfTank = (Math.PI * Math.pow(radiusOfTank, 2)); //[ft^2]
    /*Chosen pump flow rate [gpm] */
    public double pumpFlowRate = 1.0; //Q

    ///////////////////////////////////////////////////////////////////

    public int dischargeDelay = 6;

//    public double calculatePumpHead() {
//        //var timeChangingVolumeOfMainTank = (areaOfTank * tankLevelBeginInterval[currentHour])*1728; //converted to inches^3
//        double pressureHead = (pressureNegativeDemandNode[currentHour] / densityOfWater); //[in]
//        double hydraulicHead = ((Math.pow(this.pumpFlowRate, 2)) / areaOfPipe) * (1 / (2 * gravity)); //[in]
//        double pumpHead = tankLevelBeginInterval[currentHour] + pressureHead + hydraulicHead; //[in]
//        //System.out.println("calculatePumpHead = " + pumpHead);
//        return pumpHead;
//    }
//
//	//Fill this out
//    public double calculatePumpEnergyConsumption() {//For 170 psi - Need to call this afterwards to calculate energy consumption
//        //var volumeOfMainTank = (calculatePumpHead() * areaOfTank);
//        //var weightOfWater = (this.totalStorageVolumeToMainSystem[this.currentHour] * densityOfWater) * 8.345; //weight in pounds
//        double energyConsumed;
//        for (int weightOfWater = 422; weightOfWater <= 1000; weightOfWater = weightOfWater + 100) {
//            System.out.println("weightOfWater = " + weightOfWater);
//            System.out.println(weightOfWater * 397.31);
//            System.out.println("poop = " + weightOfWater * 397.31* .0000003766);
//            energyConsumed = weightOfWater * 397.31 * .0000003766;   //(1/2655200)
//            System.out.println("calculatePumpEnergyConsumption = " + energyConsumed);
//        }
//
//        //return energyConsumed;
//        return 0.0;
//    }

    public Household(int index, boolean hasTank, double catchmentArea, double tankDiameter, double tankHeight,
                     double WTA, double WTP, double lawnSize, double irrigEfficiency, double kCrop, double evapotrans,
                     double effectiveRainfall) {
        this.index = index;
        this.hasTank = hasTank;
        this.catchmentArea = catchmentArea;
        this.tankDiameter = tankDiameter;
        this.tankHeight = tankHeight;
        this.tankStorageVolume = ((Math.PI * ((this.tankDiameter * this.tankDiameter) / 4.0)) * this.tankHeight) *
                abm.cubicMeterToGallonConversion; //Cylindrical Tank
        this.WTA = WTA;
        this.WTP = WTP;
        this.lawnSize = lawnSize;
        this.irrigEfficiency = irrigEfficiency;
        this.kCrop = kCrop;
        this.evapotrans = evapotrans;
        this.effectiveRainfall = effectiveRainfall;
        this.storageVolumeBeginInterval[0] = 0.0;
    }

    //This method sets the node ID tags for the three nodes connected to the household
    public void setNodeTags(String IRID, String NDID, String MSID) {
        this.irrigationDemandNodeID = IRID;
        this.negativeDemandNodeID = NDID;
        this.mainSystemNodeID = MSID;
    }

    public void setIrrigationDemand(double[] irriDemand) {
        this.irrigationDemand = irriDemand;
    }

    //This method sets the hourly rainfall [Units of inches]
    public void setHourlyRainfall(double hourRainfall[]) {
        for (int i = 0; i < abm.hoursInSimulation; i++) {
            this.hourlyRainfall[i] = hourRainfall[i];
        }
        setTotalHourlyRainfallVolume();
        setFlushDiversionRainfallWithinGapFlag();
        setHoursSinceLastRainEvent();
    }

    //This method sets the total hourly rainfall volume [Units of gal]
    public void setTotalHourlyRainfallVolume() {
        for (int i = 0; i < abm.hoursInSimulation; i++) {
            this.totalHourlyRainfallVolume[i] = ((this.hourlyRainfall[i] / 12.0) * this.catchmentArea) * abm.cubicFootToGallonConversion;
        }
    }

    //This method sets the flush diversion rainfall within gap flag boolean container [whether there was rainfall in the previous chosen number of hours]
    public void setFlushDiversionRainfallWithinGapFlag() {
        for (int i = 0; i < abm.hoursInSimulation; i++) {
            if (i == 0) {
                this.rainfallWithinGapFlag[i] = false;
            } else if (i <= this.flushDiversionRainfallGap) {
                for (int j = 1; j <= i; j++) {
                    if (this.hourlyRainfall[i - j] > 0.0) {
                        this.rainfallWithinGapFlag[i] = true;
                        break;
                    }
                }
            } else {
                for (int j = 1; j <= this.flushDiversionRainfallGap; j++) {
                    if (this.hourlyRainfall[i - j] > 0.0) {
                        this.rainfallWithinGapFlag[i] = true;
                        break;
                    }
                }
            }
        }
    }

    //Set the number of hours since the last rain event for each hour [Value is zero if rainfall in current hour]
    public void setHoursSinceLastRainEvent() {
        for (int i = 0; i < abm.hoursInSimulation; i++) {
            if (i == 0) {
                this.hoursSinceLastRainEvent[i] = 0;
            } else {
                for (int j = 0; j <= i; j++) {
                    if (this.hourlyRainfall[i - j] > 0.0) {
                        this.hoursSinceLastRainEvent[i] = j;
                        break;
                    }
                }
            }
        }

    }

    //Determine if there has been rain within the specified number of hours set for hours between rain and trade activity
    public boolean rainCheck() {
        return this.hoursSinceLastRainEvent[this.currentHour] < this.rainTradeGap;
    }

    //Increment the current hour of the household and the current day when appropriate
    public void step_increment() {
        this.currentHour++;
        if ((this.currentHour % 24) == 0) {
            this.currentDay++;
        }
    }

    //This method estimates the storage that is available for rwh.market and utility exchanges
    //This value will eventually be dictated by the operational constraints of the pump system and the pressure of
    // the main system node
    public double estimateAvailableStorage() {
        //No exchanges the first 6 hours or the final 6 hours of the year
        if (this.currentHour < dischargeDelay || this.currentHour > (abm.hoursInSimulation - dischargeDelay)) {
            System.out.println("No exchanges first or final " + dischargeDelay + " hours of year!!!!!!!!!!!!!!!!!!!");
            return 0.0;
        } else {
            if (this.hasTank) {
                //Sum the scheduled discharges from the current hour to x hours into the future
                // Then subtract from volume at beginning of current hour
                double scheduledDischarge = 0.0;
                for (int i = 1; i <= storageDischargeDuration - 1; i++) {
                    scheduledDischarge += this.storageDischargeSchedule[this.currentHour][i];
                }
                int stepsLeftOff = 0;
                for (int i = 1; i <= storageDischargeDuration - 1; i++) {
                    for (int j = storageDischargeDuration - 1; j > stepsLeftOff; j--) {
                        scheduledDischarge += this.storageDischargeSchedule[this.currentHour - i][j];
                    }
                    stepsLeftOff++;
                }

                //Need to calculate the free discharge volume available...
                //////////////////////////////////////////////////////////

                double marginalHourlyStorageAvailable = 0.0;
                double[] schedule = new double[storageDischargeDuration];
                //Calculate discharge for the current hour
                for (int i = 1; i <= storageDischargeDuration - 1; i++) {
                    schedule[0] += this.storageDischargeSchedule[this.currentHour - i][i];
                }
                //Calculate discharge for each of the next x hours
                for (int i = 1; i <= storageDischargeDuration - 1; i++) {
                    for (int j = 1; j <= storageDischargeDuration - 1; j++) {
                        schedule[i] += this.storageDischargeSchedule[this.currentHour + i - j][j];
                    }
                }
                //Now we know the scheduled storage discharge for the current hour and the next x hours
                //Now iterate through the next x hours and subtract each scheduled discharge from its max hourly discharge value and tally
                double scheduleDischarge2 = schedule[0];
                for (int i = 1; i <= storageDischargeDuration - 1; i++) {
                    marginalHourlyStorageAvailable += (this.maximumHourlyDischarge[this.currentHour + i] - schedule[i]);
                    scheduleDischarge2 += schedule[i];
                }
                if (scheduledDischarge != scheduleDischarge2) {
                    System.out.println("Problem: scheduledDischarge != scheduleDischarge2");
                    System.exit(0);
                }

                //////////////////////////////////////////////////////////

                double maxPhysicalStorage = (this.storageVolumeBeginInterval[this.currentHour] - scheduledDischarge);
                if (maxPhysicalStorage > marginalHourlyStorageAvailable) {
                    return marginalHourlyStorageAvailable;
                } else {
                    return maxPhysicalStorage;
                }

            } else {
                return 0.0;
            }
        }
    }

    public void scheduleExchangeDischarge(double sellerHistory) {
        /* Check if current hour is within the first n hours or last n hours of the simulation */
        if (this.currentHour < dischargeDelay || this.currentHour > (abm.hoursInSimulation - dischargeDelay)) {
            this.storageVolumeForMarket[this.currentHour] = 0.0;
            this.storageVolumeForUtility[this.currentHour] = 0.0;
            this.totalStorageVolumeToMainSystem[this.currentHour] = 0.0;
        } else {
            this.storageVolumeForMarket[this.currentHour] = sellerHistory;
            this.storageVolumeForUtility[this.currentHour] = 0.0;
            //create array that holds total scheduled discharge for each of the next n hours
            //Iterate through the hours and schedule additional discharge up to the maximum hourly discharge
            double[] schedule = new double[storageDischargeDuration];
            //Calculate discharge for the current hour
            for (int i = 1; i <= this.storageDischargeDuration - 1; i++) {
                schedule[0] += this.storageDischargeSchedule[this.currentHour - i][i];
            }
            //Calculate discharge for each of the next n hours
            for (int i = 1; i <= this.storageDischargeDuration - 1; i++) {
                for (int j = 1; j <= this.storageDischargeDuration - 1; j++) {
                    schedule[i] += this.storageDischargeSchedule[this.currentHour + i - j][j];
                }
            }
            //Distribute exchanged storage for discharge over the upcoming n hours (You cannot schedule any discharge
            // for the current hour, only n hours into future)
            double newDischargeRemaining = sellerHistory;
            for (int i = 1; i <= this.storageDischargeDuration - 1; i++) {
                if (schedule[i] < this.maximumHourlyDischarge[this.currentHour + i]) {
                    if ((schedule[i] + newDischargeRemaining) > this.maximumHourlyDischarge[this.currentHour + i]) {
                        double dischargeScheduled = this.maximumHourlyDischarge[this.currentHour + i] - schedule[i];
                        this.storageDischargeSchedule[this.currentHour][i] += dischargeScheduled;
                        newDischargeRemaining -= dischargeScheduled;
                    } else {
                        double dischargeScheduled = newDischargeRemaining;
                        this.storageDischargeSchedule[this.currentHour][i] += dischargeScheduled;
                        newDischargeRemaining = 0.0;
                    }
                }
            }
            this.totalStorageVolumeToMainSystem[this.currentHour] = schedule[0];
        }
    }

    public void storageCalcBegin() {
        this.storageVolumeForHouseholdDemand[this.currentHour] = 0.0;
        this.storageVolumeAfterHouseholdDemand[this.currentHour] = this.storageVolumeBeginInterval[this.currentHour];
        this.tankLevelAfterHouseholdDemand[this.currentHour] = ((this.storageVolumeAfterHouseholdDemand[this.currentHour])
                * (1 / abm.cubicMeterToGallonConversion)) / (Math.PI * ((this.tankDiameter * this.tankDiameter) / 4.0));
        //Change to storage volume after current hour discharge
        this.storageVolumeAfterDischarge[this.currentHour] = this.storageVolumeAfterHouseholdDemand[this.currentHour]
                - this.totalStorageVolumeToMainSystem[this.currentHour]; //- this.storageVolumeForMarket[this.currentHour] - this.storageVolumeForUtility[this.currentHour];
        this.tankLevelAfterDischarge[this.currentHour] = ((this.storageVolumeAfterDischarge[this.currentHour]) *
                (1 / abm.cubicMeterToGallonConversion)) / (Math.PI * ((this.tankDiameter * this.tankDiameter) / 4.0));
    }

    public void rainfallStorageCalc() {
        firstFlushDiversion(); //Determines first flush diversion volume
        double rainfallLeftover = this.totalHourlyRainfallVolume[this.currentHour] - this.firstFlushDiversionVolume[this.currentHour];

		/* if the amount of water exceeds tank volume, set overflow volume equal to the overage amount*/
        if ((this.storageVolumeAfterDischarge[this.currentHour] + rainfallLeftover) > this.tankStorageVolume) {
            this.rainfallOverflowVolume[this.currentHour] = (this.storageVolumeAfterDischarge[this.currentHour] +
                    rainfallLeftover) - this.tankStorageVolume;
            this.rainfallToTankVolume[this.currentHour] = rainfallLeftover - this.rainfallOverflowVolume[this.currentHour];
            if (this.currentHour == (abm.hoursInSimulation - 1)) {
                this.finalStorage = this.tankStorageVolume;
                this.tankLevelFinal = this.tankHeight;
            } else {
                this.storageVolumeBeginInterval[this.currentHour + 1] = this.tankStorageVolume;
                this.tankLevelBeginInterval[this.currentHour + 1] = this.tankHeight;
            }
        } else {
            this.rainfallOverflowVolume[this.currentHour] = 0.0;
            this.rainfallToTankVolume[this.currentHour] = rainfallLeftover;
            if (this.currentHour == (abm.hoursInSimulation - 1)) {
                this.finalStorage = this.storageVolumeAfterDischarge[this.currentHour] + rainfallLeftover;
                this.tankLevelFinal = ((this.finalStorage) * (1 / abm.cubicMeterToGallonConversion)) / (Math.PI *
                        ((this.tankDiameter * this.tankDiameter) / 4.0));
            } else {
                this.storageVolumeBeginInterval[this.currentHour + 1] = this.storageVolumeAfterDischarge[this.currentHour]
                        + rainfallLeftover;
                this.tankLevelBeginInterval[this.currentHour + 1] = ((this.storageVolumeBeginInterval[this.currentHour
                        + 1]) * (1 / abm.cubicMeterToGallonConversion)) / (Math.PI * ((this.tankDiameter * this.tankDiameter)
                        / 4.0));

            }
        }
        //Change this depending on the city later on ***********************
        this.rainfallToInfiltrationVolume[this.currentHour] = 0.0;
        this.rainfallToStormDrainVolume[this.currentHour] = this.rainfallOverflowVolume[this.currentHour];
        // *****************************************************************

    }

    //This method determines the first flush diversion volume for the current hour
    public void firstFlushDiversion() {
        //Determine how many hours first flush diversion was operating before the current hour
        //If number of hours is greater than zero
        //Add up the first flush diversion volume through this continuous period
        //If volume is less than necessary diversion --> flush diversion continues
        //If volume is equal to necessary diversion --> flush diversion is complete and value is set at zero for current hour
        //If number of hours is equal to zero
        //Then check rainfall gap flag
        //If false --> activate first flush diversion
        //Check hourly rainfall volume versus necessary diversion
        //If volume equals or exceeds necessary diversion --> first flush diversion volume set to necessary diversion
        //If volume is less than necessary diversion --> first flush diversion volume set to hourly rainfall volume
        //If true --> first flush diversion is inactive and set to zero
        if (this.currentHour == 0) {
            if (this.totalHourlyRainfallVolume[0] > this.flushDiversionRequirement) {
                this.firstFlushDiversionVolume[0] = this.flushDiversionRequirement;
            } else {
                this.firstFlushDiversionVolume[0] = this.totalHourlyRainfallVolume[0];
            }
        } else {
            //Determine how many hours first flush diversion was operating before the current hour
            int continuousFlushHours = 0;
            for (int i = 1; i <= this.currentHour; i++) {
                if (this.firstFlushDiversionVolume[this.currentHour - i] == 0.0) {
                    break;
                } else {
                    continuousFlushHours++;
                }
            }
            if (continuousFlushHours > 0) { //If number of hours is greater than zero
                double sumPreviousFlush = 0.0;
                //Add up the first flush diversion volume through this continuous period
                for (int i = 1; i <= continuousFlushHours; i++) {
                    sumPreviousFlush += this.firstFlushDiversionVolume[this.currentHour - i];
                }
                if (sumPreviousFlush < this.flushDiversionRequirement) { //flush diversion continues
                    if (this.totalHourlyRainfallVolume[this.currentHour] > (this.flushDiversionRequirement - sumPreviousFlush)) {
                        this.firstFlushDiversionVolume[this.currentHour] = this.flushDiversionRequirement - sumPreviousFlush;
                    } else {
                        this.firstFlushDiversionVolume[this.currentHour] = this.totalHourlyRainfallVolume[this.currentHour];
                    }
                } else { //flush diversion is complete and value is set at zero for current hour
                    this.firstFlushDiversionVolume[this.currentHour] = 0.0;
                }
            } else { //If number of hours is equal to zero --> Then check rainfall gap flag
                if (!this.rainfallWithinGapFlag[this.currentHour]) { //If false --> activate first flush diversion
                    //Check hourly rainfall volume versus necessary diversion
                    if (this.totalHourlyRainfallVolume[this.currentHour] > this.flushDiversionRequirement) {
                        //If volume exceeds necessary diversion --> first flush diversion volume set to required diversion
                        this.firstFlushDiversionVolume[this.currentHour] = this.flushDiversionRequirement;
                    } else {
                        //If volume is less than necessary diversion --> first flush diversion volume set to hourly rainfall volume
                        this.firstFlushDiversionVolume[this.currentHour] = this.totalHourlyRainfallVolume[this.currentHour];
                    }
                } else { //If true --> first flush diversion is inactive and set to zero
                    this.firstFlushDiversionVolume[this.currentHour] = 0.0;
                }
            }
        }

    }


}
