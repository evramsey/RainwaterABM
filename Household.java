package rwh;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;


public class Household {
	/*Index value of household in the model */
	public int index;
	/*Catchment area of rainwater harvesting system [ft^2] */
	public double catchmentArea;
	/*Rainwater storage tank diameter [m] - Cylindrical Tank */
	public double tankDiameter;
	/*Rainwater storage tank height [m] - Cylindrical Tank */
	public double tankHeight;
	/*Rainwater tank storage volume [gal] */
	public double tankStorageVolume;
	/*First flush diversion coefficient - [0.00-1.00] (0.10 standard value) - Applies to single rain event - Not currently used */
	//public double firstFlushDiversionCoefficient = 0.10;
	
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
    
	/*Number of hours in month (April) */
    public int hoursInMonth = 720;
    /*Number of days in month (April) */
    public int daysInMonth = 30;
    
    //Willingness to accept value
    public double WTA = 0.0;
    //Willingness to pay value
    public double WTP = 0.0;
    
    /*Global irrigation demand pattern [gal] */
    public double[] theoreticalIrrigationDemand = new double[this.hoursInMonth];
    /*Household irrigation demand for the current hour [gal] */
    public double[] irrigationDemand = new double[this.hoursInMonth];
    /*Total household demand for the current hour [gal] */
    public double[] totalDemand = new double[this.hoursInMonth];
    
    /*Daily maximum temperature [degrees F]??? */
    public double[] dailyMaxTemperature = new double[this.daysInMonth];
    
    /*Rainfall during current hour [in] */
    public double[] hourlyRainfall = new double[this.hoursInMonth];
    /*Total hourly rainfall volume [gal] */
    public double[] totalHourlyRainfallVolume = new double[this.hoursInMonth];
    /*First flush diversion volume for current hour [gal] */
    public double[] firstFlushDiversionVolume = new double[this.hoursInMonth];
    /*Rainfall volume to storage tank [gal] */
    public double[] rainfallToTankVolume = new double[this.hoursInMonth];
    /*Storage overflow volume from rainfall during the current hour [gal] */
    public double[] rainfallOverflowVolume = new double[this.hoursInMonth];
    /*Storage overflow volume from rainfall sent to infiltration bore during the current hour [gal] */
    public double[] rainfallToInfiltrationVolume = new double[this.hoursInMonth];
    /*Storage overflow volume from rainfall sent to storm drain during the current hour [gal]  */
    public double[] rainfallToStormDrainVolume = new double[this.hoursInMonth];
    
    /*Number of hours between rainfall events before first flush diversion can be reactivated */
    public int flushDiversionRainfallGap = 2;
    /*Boolean flag container to determine if there was rainfall within the number of hours indicated by flush diversion steps */
    public boolean[] rainfallWithinGapFlag = new boolean[this.hoursInMonth];
    /*Number of hours that must go by after a rain event before trade activity resumes */
    public int rainTradeGap = 48;
    /*Number of hours since the last rain event */
    public int[] hoursSinceLastRainEvent = new int[this.hoursInMonth];
    /*First flush diversion volumetric requirement [gal] */
    public double flushDiversionRequirement = 75.0;
    
    /*Tank storage volume at the beginning of the current hour [gal] */
    public double[] storageVolumeBeginInterval = new double[this.hoursInMonth];
    /*Tank storage volume for household demand during the current hour [gal] */
    public double[] storageVolumeForHouseholdDemand = new double[this.hoursInMonth];
    /*Tank storage volume after discharge for household demand [gal] */
    public double[] storageVolumeAfterHouseholdDemand = new double[this.hoursInMonth];
    /*Tank storage volume to be sent to reclaimed pipeline for rwh.market settlement during the current hour [gal] */
    public double[] storageVolumeForMarket = new double[this.hoursInMonth];
    /*Tank storage volume to be sent to reclaimed pipeline for utility settlement during the current hour [gal] */
    public double[] storageVolumeForUtility = new double[this.hoursInMonth];
    /*Total tank storage volume to be sent to reclaimed pipeline during the current hour [gal] */
    public double[] totalStorageVolumeToMainSystem = new double[this.hoursInMonth];
    /*Flow rate from tank to main system [gpm] */
    public double[] flowrateFromTankToMainSystem = new double[this.hoursInMonth];
    /*Tank storage volume after tank discharge for current hour [gal] */
    public double[] storageVolumeAfterDischarge = new double[this.hoursInMonth];
    
    ////////////////////////////////////////////////////////////////////////////
    
    /*Scheduled tank discharges [gal] - [hour scheduled][hours ahead] */
    public double[][] storageDischargeSchedule = new double[this.hoursInMonth][6];
    /*Maximum tank discharge for any given hour [gal] - This should actually be determined by the chosen pump curve */
    public double[] maximumHourlyDischarge = new double[this.hoursInMonth];
    
    ////////////////////////////////////////////////////////////////////////////

    /*Tank storage level at the beginning of the current hour [m] */
    public double[] tankLevelBeginInterval = new double[this.hoursInMonth];
    /*Tank storage level after discharge for household demand [m] */
    public double[] tankLevelAfterHouseholdDemand = new double[this.hoursInMonth];
    /*Tank storage level after discharge for current hour [m] */
    public double[] tankLevelAfterDischarge = new double[this.hoursInMonth];
    
    public double tankLevelFinal;
    
    /*Pressure of the negative demand node at beginning of current hour [psi] - Passed from EPANET at beginning of current hour */
    public double[] pressureNegativeDemandNode = new double[this.hoursInMonth];
    /*Pressure of the main system demand node at beginning of current hour [psi] - Passed from EPANET at beginning of current hour */
    public double[] pressureMainSystemNode = new double[this.hoursInMonth];
    /*Pressure of the irrigation demand node at beginning of current hour [psi] - Passed from EPANET at beginning of current hour */
    public double[] pressureIrrigationDemandNode = new double[this.hoursInMonth];
    /*Estimated pressure of negative demand node with flow from storage tank [psi] */
    public double[] pressureEstimateNegativeDemandNode = new double[this.hoursInMonth];
    /*Estimated pressure of main system node with flow from storage tank [psi] */
    public double[] pressureEstimateMainSystemNode = new double[this.hoursInMonth];
    /*Estimated pressure of irrigation demand node with flow from storage tank [psi] */
    public double[] pressureEstimateIrrigationDemandNode = new double[this.hoursInMonth];

	/*Flag for rainwater harvesting tank system existence */
    public boolean hasTank;
	
	//Default range is 17-22
	/*Discharge Start Time - For Predefined Storage Method only */ 
	public int startDischarge = 17;
	/*Discharge Stop Time - For Predefined Storage Method only */
	public int stopDischarge = 22;

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
    
    public double calculatePumpHead() {
        //var timeChangingVolumeOfMainTank = (areaOfTank * tankLevelBeginInterval[currentHour])*1728; //converted to inches^3
        double pressureHead = (pressureNegativeDemandNode[currentHour] / densityOfWater); //[in]
        double hydraulicHead = ((Math.pow(this.pumpFlowRate, 2)) / areaOfPipe) * (1 / (2 * gravity)); //[in]
        double pumpHead = tankLevelBeginInterval[currentHour] + pressureHead + hydraulicHead; //[in]
        //System.out.println("calculatePumpHead = " + pumpHead);
        return pumpHead;
    }
    
	//Fill this out
    public double calculatePumpEnergyConsumption() {//For 170 psi - Need to call this afterwards to calculate energy consumption
        //var volumeOfMainTank = (calculatePumpHead() * areaOfTank);
        //var weightOfWater = (this.totalStorageVolumeToMainSystem[this.currentHour] * densityOfWater) * 8.345; //weight in pounds
        double energyConsumed;
        for (int weightOfWater = 422; weightOfWater <= 1000; weightOfWater = weightOfWater + 100) {
            System.out.println("weightOfWater = " + weightOfWater);
            System.out.println(weightOfWater * 397.31);
            System.out.println("poop = " + weightOfWater * 397.31* .0000003766);
            energyConsumed = weightOfWater * 397.31 * .0000003766;   //(1/2655200)
            System.out.println("calculatePumpEnergyConsumption = " + energyConsumed);
        }
        
        //return energyConsumed;
        return 0.0;
    }

	

	public Household(int index, boolean hasTank, double catchmentArea, double tankDiameter, double tankHeight, double WTA, double WTP) {
		this.index = index;
		this.hasTank = hasTank;
		this.catchmentArea = catchmentArea;
		this.tankDiameter = tankDiameter;
		this.tankHeight = tankHeight;
		this.tankStorageVolume = ((Math.PI * ((this.tankDiameter * this.tankDiameter) / 4.0)) * this.tankHeight) * 264.172; //Cylindrical Tank
		this.WTA = WTA;
		this.WTP = WTP;
		
		this.storageVolumeBeginInterval[0] = 0.0;
		
	}
	
	public String print_Household() {
		return ("Household(" + this.index  + ", " + this.hasTank + ", " + this.catchmentArea + ")");
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
    
    //This method sets the total hourly demand of the residential household
    public void setTotalDemand() {
    	for (int i = 0; i < this.hoursInMonth; i++) {
    		this.totalDemand[i] = this.irrigationDemand[i];
    	}
    }
    
    //This method sets the maximum hourly discharge for the household for a month
    //Value is set to 75% of theoretical irrigation demand for each hour
    public void setMaximumHourlyDischarge() {
    	for (int i = 0; i < this.hoursInMonth; i++) {
    		if ((this.theoreticalIrrigationDemand[i] * 0.75 * 9) > 60.0) {
    			this.maximumHourlyDischarge[i] = 60.0;
    		} else {
    			this.maximumHourlyDischarge[i] = (this.theoreticalIrrigationDemand[i] * 0.75 * 9);
    		}
    	}
    }
    
    //This method sets the hourly rainfall [Units of inches]
    public void setHourlyRainfall(double hourRainfall[]) {
    	for (int i = 0; i < this.hoursInMonth; i++) {
    		this.hourlyRainfall[i] = hourRainfall[i];
    	}
    	setTotalHourlyRainfallVolume();
    	setFlushDiversionRainfallWithinGapFlag();
    	setHoursSinceLastRainEvent();
    }
    
    //This method sets the total hourly rainfall volume [Units of gal]
    public void setTotalHourlyRainfallVolume() {
    	for (int i = 0; i < this.hoursInMonth; i++) {
    		this.totalHourlyRainfallVolume[i] = ((this.hourlyRainfall[i] / 12.0) * this.catchmentArea) * (7.48052);
    		if (i > 200 && i < 300) {
    			//System.out.println(i + " Total Hourly Rainfall Volume = " + this.totalHourlyRainfallVolume[i]);
    		}
    	}
    	//System.exit(0);
    }
    
    //This method sets the flush diversion rainfall within gap flag boolean container [whether there was rainfall in the previous chosen number of hours]
    public void setFlushDiversionRainfallWithinGapFlag() {
    	for (int i = 0; i < this.hoursInMonth; i++) {
    		if (i == 0) {
    			this.rainfallWithinGapFlag[i] = false;
    		} else if (i <= this.flushDiversionRainfallGap) {
    			for(int j = 1; j <= i; j++) {
    				if (this.hourlyRainfall[i - j] > 0.0) {
    					this.rainfallWithinGapFlag[i] = true;
    					break;
    				}
    			}
    	    } else {
    	    	for(int j = 1; j <= this.flushDiversionRainfallGap; j++) {
    				if (this.hourlyRainfall[i - j] > 0.0) {
    					this.rainfallWithinGapFlag[i] = true;
    					break;
    				}
    			}
    		}
    	}
    }
    
    //This method sets the number of hours since the last rain event for each hour [Value is zero if rainfall in current hour]
    public void setHoursSinceLastRainEvent() {
    	for (int i = 0; i < this.hoursInMonth; i++) {
    		if (i == 0) {
    			this.hoursSinceLastRainEvent[i] = 0;
    		} else {
    			for(int j = 0; j <= i; j++) {
//					System.out.println("i: " + String.valueOf(i) + "  j: " + String.valueOf(j));
//					System.out.println("i-j: " + String.valueOf(i-j));
//					System.out.println("hourly rainfall:" + this.hourlyRainfall[i-j]);
    				if (this.hourlyRainfall[i - j] > 0.0) {
    					this.hoursSinceLastRainEvent[i] = j;
    					break;
    				}
    			}
    	    }
    	}

    }
    
    //This method determines if there has been rain within the specified number of hours set for hours between rain and trade activity
    public boolean rainCheck() {
		System.out.println("hours since rain at time " + String.valueOf(this.currentHour) + ": " + String.valueOf(this.hoursSinceLastRainEvent[this.currentHour]));
		if (this.hoursSinceLastRainEvent[this.currentHour] < this.rainTradeGap) {
    		return true;
    	} else {
    		return false;
    	}
    }

    //This method increments the current hour of the household and the current day when appropriate
    public void step_increment() {
    	this.currentHour++;
    	if ((this.currentHour % 24) == 0) {
    		this.currentDay++;
    	}
    }
    
    //This method estimates the storage that is available for rwh.market and utility exchanges
    //This value will eventually be dictated by the operational constraints of the pump system and the pressure of the main system node
    public double estimateAvailableStorage() {
    	//No exchanges the first 6 hours or the final 6 hours of the year
    	if (this.currentHour < 6 || this.currentHour > (this.hoursInMonth - 6)) {
    		System.out.println("No exchanges first or final 6 hours of year!!!!!!!!!!!!!!!!!!!");
    		return 0.0;
    	} else {
    		if (this.hasTank) {
        		//Sum the scheduled discharges from the current hour to 5 hours into the future --> Then subtract from volume at beginning of current hour
        		double scheduledDischarge = 0.0;
        		for (int i = 1; i <= 5; i++) {
        			scheduledDischarge += this.storageDischargeSchedule[this.currentHour][i];
        		}
        		int stepsLeftOff = 0;
        		for (int i = 1; i <= 5; i++) {
        			for (int j = 5; j > stepsLeftOff; j--) {
        				scheduledDischarge += this.storageDischargeSchedule[this.currentHour - i][j];
        			}
        			stepsLeftOff++;
        		}
        		if ((this.storageVolumeBeginInterval[this.currentHour] - scheduledDischarge) > 0.0) {
        			//System.out.println(this.storageVolumeBeginInterval[this.currentHour] - scheduledDischarge);
        		}
        		//System.exit(0);
        		
        		//Need to calculate the free discharge volume available...
        		//////////////////////////////////////////////////////////
        		
        		double marginalHourlyStorageAvailable = 0.0;
        		double[] schedule = new double[6];
            	//Calculate discharge for the current hour
            	for (int i = 1; i <= 5; i++) {
            		schedule[0] += this.storageDischargeSchedule[this.currentHour - i][i];
            	}
            	//Calculate discharge for each of the next 5 hours
        		for (int i = 1; i <= 5; i++) {
        			for (int j = 1; j <= 5; j++) {
        				schedule[i] += this.storageDischargeSchedule[this.currentHour + i - j][j];
        			}
        		}
        		//Now we know the scheduled storage discharge for the current hour and the next 5 hours
        		//Now iterate through the next 5 hours and subtract each scheduled discharge from its max hourly discharge value and tally
        		double scheduleDischarge2 = schedule[0];
        		for (int i = 1; i <= 5; i++) {
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
    
    //This method returns the total non-potable water demand for the current hour
    //Returns zero if the household has a rainwater tank
    public double getDemand() {
    	if (this.hasTank) {
    		return 0.0;
    	} else {
    		return this.totalDemand[this.currentHour];
    	}
    }
    
    public void scheduleExchangeDischarge(double sellerHistory) {
    	if (this.currentHour < 6 || this.currentHour > (this.hoursInMonth - 6)) {
    		System.out.println("No exchanges first or final 6 hours of year!!!!!!!!!!!!!!!!!!!");
    		this.storageVolumeForMarket[this.currentHour] = 0.0;
        	this.storageVolumeForUtility[this.currentHour] = 0.0;
    		this.totalStorageVolumeToMainSystem[this.currentHour] = 0.0;
    	} else {
    		this.storageVolumeForMarket[this.currentHour] = sellerHistory;
        	this.storageVolumeForUtility[this.currentHour] = 0.0;
        	//create array that holds total scheduled discharge for each of the next 5 hours
        	//Iterate through the hours and schedule additional discharge up to the maximum hourly discharge
        	double[] schedule = new double[6];
        	//Calculate discharge for the current hour
        	for (int i = 1; i <= 5; i++) {
        		schedule[0] += this.storageDischargeSchedule[this.currentHour - i][i];
        	}
        	//Calculate discharge for each of the next 5 hours
    		for (int i = 1; i <= 5; i++) {
    			for (int j = 1; j <= 5; j++) {
    				schedule[i] += this.storageDischargeSchedule[this.currentHour + i - j][j];
    			}
    		}
    		//Distribute exchanged storage for discharge over the upcoming 5 hours (You cannot schedule any discharge for the current hour, only 5 hours into future)
    		double newDischargeRemaining = sellerHistory;
    		for (int i = 1; i <= 5; i++) {
    			//Change this for the array!!!!!!
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
    			} else {
    				System.out.println("Maximum hourly discharge already scheduled****************");
    			}
    		}
    		/*
    		for (int i = 1; i <= 5; i++) {
    			//Change this for the array!!!!!!
    			if (schedule[i] < this.maximumHourlyDischarge) {
    				if ((schedule[i] + newDischargeRemaining) > this.maximumHourlyDischarge) {
    					double dischargeScheduled = this.maximumHourlyDischarge - schedule[i];
    					this.storageDischargeSchedule[this.currentHour][i] += dischargeScheduled;
    					newDischargeRemaining -= dischargeScheduled;
    				} else {
    					double dischargeScheduled = newDischargeRemaining;
    					this.storageDischargeSchedule[this.currentHour][i] += dischargeScheduled;
    					newDischargeRemaining = 0.0;
    				}
    			} else {
    				System.out.println("Maximum hourly discharge already scheduled****************");
    			}
    		}
    		*/
    		this.totalStorageVolumeToMainSystem[this.currentHour] = schedule[0];
    	}
		
    }
    
    public void fillExchangeVolume(double sellerHistory) {
    	this.storageVolumeForMarket[this.currentHour] = sellerHistory;
    	this.storageVolumeForUtility[this.currentHour] = 0.0;
    	this.totalStorageVolumeToMainSystem[this.currentHour] = this.storageVolumeForMarket[this.currentHour] + this.storageVolumeForUtility[this.currentHour];
    }
    
    public void storageCalcBegin() {
    	this.storageVolumeForHouseholdDemand[this.currentHour] = 0.0;
    	this.storageVolumeAfterHouseholdDemand[this.currentHour] = this.storageVolumeBeginInterval[this.currentHour];
    	this.tankLevelAfterHouseholdDemand[this.currentHour] = ((this.storageVolumeAfterHouseholdDemand[this.currentHour]) * (1/264.172)) / (Math.PI * ((this.tankDiameter * this.tankDiameter) / 4.0));
    	//Change to storage volume after current hour discharge
    	this.storageVolumeAfterDischarge[this.currentHour] = this.storageVolumeAfterHouseholdDemand[this.currentHour] - this.totalStorageVolumeToMainSystem[this.currentHour]; //- this.storageVolumeForMarket[this.currentHour] - this.storageVolumeForUtility[this.currentHour];
    	this.tankLevelAfterDischarge[this.currentHour] = ((this.storageVolumeAfterDischarge[this.currentHour]) * (1/264.172)) / (Math.PI * ((this.tankDiameter * this.tankDiameter) / 4.0));
    }
    
    public void rainfallStorageCalc() {
    	firstFlushDiversion(); //Determines first flush diversion volume
    	double rainfallLeftover = this.totalHourlyRainfallVolume[this.currentHour] - this.firstFlushDiversionVolume[this.currentHour];
		if ((this.storageVolumeAfterDischarge[this.currentHour] + rainfallLeftover) > this.tankStorageVolume) {
			this.rainfallOverflowVolume[this.currentHour] = (this.storageVolumeAfterDischarge[this.currentHour] + rainfallLeftover) - this.tankStorageVolume;
			System.out.println("Tank Storage Volume = " + this.tankStorageVolume);
			//System.exit(0);
			this.rainfallToTankVolume[this.currentHour] = rainfallLeftover - this.rainfallOverflowVolume[this.currentHour];
			if (this.currentHour == (this.hoursInMonth - 1)) {
				this.finalStorage = this.tankStorageVolume;
				this.tankLevelFinal = this.tankHeight;
			} else {
				this.storageVolumeBeginInterval[this.currentHour + 1] = this.tankStorageVolume;
				this.tankLevelBeginInterval[this.currentHour + 1] = this.tankHeight;
			}
		} else {
			this.rainfallOverflowVolume[this.currentHour] = 0.0;
			this.rainfallToTankVolume[this.currentHour] = rainfallLeftover;
			if (this.currentHour == (this.hoursInMonth - 1)) {
				this.finalStorage = this.storageVolumeAfterDischarge[this.currentHour] + rainfallLeftover;
				this.tankLevelFinal = ((this.finalStorage) * (1/264.172)) / (Math.PI * ((this.tankDiameter * this.tankDiameter) / 4.0));
			} else {
				this.storageVolumeBeginInterval[this.currentHour + 1] = this.storageVolumeAfterDischarge[this.currentHour] + rainfallLeftover;
				this.tankLevelBeginInterval[this.currentHour + 1] = ((this.storageVolumeBeginInterval[this.currentHour + 1]) * (1/264.172)) / (Math.PI * ((this.tankDiameter * this.tankDiameter) / 4.0));
						
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


  
    //This method reads through the full EPANET list of nodal pressures and picks out the values that correspond to the ND, IR, and MS nodes of the household
    public void readNodalPressures(String[] tag, double[] pressure) {
    	for(int i = 0; i < tag.length; i++) {
    		if (tag[i] == this.mainSystemNodeID) {
    			this.pressureMainSystemNode[this.currentHour] = pressure[i];
    		} else if (tag[i] == this.negativeDemandNodeID) {
    			this.pressureNegativeDemandNode[this.currentHour] = pressure[i];
    		} else if (tag[i] == this.irrigationDemandNodeID) {
    			this.pressureIrrigationDemandNode[this.currentHour] = pressure[i];
    		}
    	}
    }
    
    public void printRainfallToTank() {
    	try {
        	
        	String fileName = "C:\\Rainwater Harvesting\\Seller Information.txt";
        	File file = new File(fileName);
        	PrintWriter writer = new PrintWriter(file);
        	for (int i = 0; i < 744; i++) {
        		writer.println(i + " Rainfall to Tank Volume = \t" + this.rainfallToTankVolume[i]);
        	}
        	for (int i = 0; i < 744; i++) {
        		writer.println(i + " First Flush Diversion Volume = \t" + this.firstFlushDiversionVolume[i]);
        	}
        	for (int i = 0; i < 744; i++) {
        		writer.println(i + " Rainfall Overflow Volume = \t" + this.rainfallOverflowVolume[i]);
        	}
        	for (int i = 0; i < 744; i++) {
        		writer.println(i + " Tank Level Beginning of Hour = \t" + this.tankLevelBeginInterval[i]);
        	}
        	for (int i = 0; i < 744; i++) {
        		writer.println(i + " Storage Volume Beginning of Hour = \t" + this.storageVolumeBeginInterval[i]);
        	}
        	for (int i = 0; i < 744; i++) {
        		writer.println(i + " Total Storage Volume to Main System = \t" + this.totalStorageVolumeToMainSystem[i]);
        	}
        	
        	writer.close();
        	} catch (FileNotFoundException e) {
        		
        	}
    }

}
