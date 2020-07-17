package rwh;

import sim.engine.SimState;
import sim.engine.Steppable;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by lizramsey on 6/25/20.
 */
public class Household implements Steppable{
    /** Index value of household in the model */
    public int index;
    /** Whether agent has a rainwater tank */
    public boolean hasTank;
    /** Catchment area of rainwater harvesting system [ft^2] */
    private double catchmentArea;
    /** Rainwater storage tank diameter [m] - Cylindrical Tank */
    public double tankDiameter;
    /** Rainwater storage tank height [m] - Cylindrical Tank */
    public double tankHeight;
    /** Rainwater tank storage volume [gal] */
    public double maxTankStorageVolume;
    /** Willingness to accept value */
    public double WTA;
    /** Willingness to pay value */
    public double WTP;
    /** Size of irrigated area */
    public double lawnSize;
    /** Irrigation efficiency (0-1, 1 being perfectly efficient) */
    public double irrigEfficiency;
    /** Lawn crop coefficient */
    public double kCrop;
    /** EPANET Node ID for household negative demand node */
    public String negativeDemandNodeID;
    /** EPANET Node ID for household irrigation demand node */
    public String irrigationDemandNodeID;
    /** EPANET Node ID for household main system node */
    public String mainSystemNodeID;
    /** Agent's irrigation demand at each time step [gal] */
    public double[] irrigDemandPattern;

    /**-----Variables changed by the model throughout simulation-----*/

    /** Current amount of water in agent's tank [gal] */
    public double currentTankStorageVolume;
    /** Amount of water an agent needs for irrigation [gal] at start of step */
    public double currentStartIrrigDemand;
    /** Amount of water an agent needs for irrigation [gal] at end of step */
    public double currentEndIrrigDemand;
    /** Current amount of water an agent sells/ inputs into the system */
    public double currentNegativeDemand;
    /** Amount of water flushed for water quality concerns*/
    public double amountFlushed = 0;


    /** -----Variables to change as needed----- */


    /** First flush volume requirement for water quality concerns*/
    public double flushDiversionVolume = 20.0;
    /** Number of hours allowed before another first flush is required (i.e., if it rains within this window of time,
     * another flush is not required*/
    public int numHoursForFlushDiversion = 24;

    public Household(int index, boolean hasTank, double catchmentArea, double tankDiameter, double tankHeight,
                     double WTA, double WTP, double lawnSize, double irrigEfficiency, double kCrop) {
        this.index = index;
        this.hasTank = hasTank;
        this.catchmentArea = catchmentArea;
        this.tankDiameter = tankDiameter;
        this.tankHeight = tankHeight;
        this.maxTankStorageVolume = ((Math.PI * ((this.tankDiameter * this.tankDiameter) / 4.0)) * this.tankHeight) *
                marketABM.M3_TO_GAL_CONVERSION; //Cylindrical Tank
        this.WTA = WTA;
        this.WTP = WTP;
        this.lawnSize = lawnSize;
        this.irrigEfficiency = irrigEfficiency;
        this.kCrop = kCrop;
        this.currentTankStorageVolume = 0.0;
    }

    /** Set the node ID tags for the three nodes connected to the household */
    public void setNodeTags(String IRID, String NDID, String MSID) {
        this.irrigationDemandNodeID = IRID;
        this.negativeDemandNodeID = NDID;
        this.mainSystemNodeID = MSID;
    }

    public void setIrrigationDemand(double[] irriDemand) {
        this.irrigDemandPattern = irriDemand;
    }

    public void setTankStorage(double hourlyRainfall){
        double hourlyRainVol = ((hourlyRainfall / 12.0) * this.catchmentArea) * marketABM.FT3_TO_GAL_CONVERSION;
        this.currentTankStorageVolume += hourlyRainVol;
        if (this.currentTankStorageVolume > this.maxTankStorageVolume){
            this.currentTankStorageVolume = this.maxTankStorageVolume;
        }
    }

    public void step(SimState state) {
        this.currentNegativeDemand = 0; // reset the amount of water to put in the system this hour
        this.currentStartIrrigDemand = getCurrentStartIrrigDemand();  //the amount needed at beginning of time step (goes into EPANet)
        this.currentEndIrrigDemand = this.currentStartIrrigDemand;      //the amount needed by the end of the time step
        double rain = marketABM.rainfall.get(marketABM.currentHour);    //the amount of rain falling this hour
        if (this.hasTank & rain > 0) {
            setTankStorage(marketABM.rainfall.get(marketABM.currentHour));
            if (shouldFlush()) {
                flush();
            }
        }
        if (marketABM.hoursSinceRain >= marketABM.rainTradeGap) {
            if (this.currentStartIrrigDemand > 0) {
                trade();
            }
        }
    }

    /**
     * Select a random prosumer household, check if it has water in storage and is willing to trade
     * If so, update prosumer's tank levels and negative demand (i.e. water input into system) and own end irrigation
     * Keep consuming water until demands are met or no prosumers are left
     */
    public void trade(){
        ArrayList<Household> shuffledProsumers = shuffleHouses(marketABM.prosumer_households);
        int index = 0;
        while (this.currentEndIrrigDemand > 0){
            if (index == shuffledProsumers.size()){
                break;
            }
            Household traderHousehold = shuffledProsumers.get(index);
            double TradeAmount;

            if (this.currentEndIrrigDemand > traderHousehold.currentTankStorageVolume & traderHousehold.WTA < this.WTP) {
                TradeAmount = traderHousehold.currentTankStorageVolume;
                traderHousehold.currentTankStorageVolume = 0;
            }
            else{
                TradeAmount = this.currentEndIrrigDemand;
                traderHousehold.currentTankStorageVolume = traderHousehold.currentTankStorageVolume - TradeAmount;
            }
            this.currentEndIrrigDemand = this.currentEndIrrigDemand - TradeAmount;
            traderHousehold.currentNegativeDemand = traderHousehold.currentNegativeDemand + TradeAmount;
            if (traderHousehold.currentNegativeDemand > 0) {
                System.out.println("trader negative demand " + traderHousehold.currentNegativeDemand);
            }
            index++;
        }
    }

    public double getCurrentStartIrrigDemand(){
        double demand;
        if ((marketABM.hoursSinceRain >= marketABM.rainTradeGap)) {
            demand = this.irrigDemandPattern[marketABM.currentHour];
        }
        else{
            demand = 0.0;
        }
        return demand;
    }

    public boolean shouldFlush(){
        //check if the last time flush happened exceeds limit
        if (marketABM.hoursSinceRain >= numHoursForFlushDiversion) {
            //if so, reset the amount flushed to zero
            this.amountFlushed = 0;
            // agent should flush
            return true;
        }
        else {
            //check if enough was flushed last time
            if (this.amountFlushed == this.flushDiversionVolume) {
                //if so, don't flush
                return false;
            }
            //if not, flush
            else {
                return true;
            }
        }
    }

    public void flush(){
        // if there isn't enough water to flush
        if (this.flushDiversionVolume > this.currentTankStorageVolume){
            this.amountFlushed = this.currentTankStorageVolume;
            this.currentTankStorageVolume = 0;
        } else{
            this.currentTankStorageVolume -= this.flushDiversionVolume;
            this.amountFlushed = this.flushDiversionVolume;
        }
    }

    /** Shuffle an array
     * @param houseArray     array to shuffle
     */
    public static ArrayList<Household> shuffleHouses(ArrayList<Household> houseArray) {
        ArrayList<Household> arrayCopy = new ArrayList<Household>(houseArray);
        Collections.shuffle(arrayCopy);
        return arrayCopy;
    }
}
