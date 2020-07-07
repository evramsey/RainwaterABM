package rwh;

import sim.engine.SimState;
import sim.engine.Steppable;

import java.io.File;
import java.io.FileWriter;

/**
 * Created by lizramsey on 7/3/20.
 */
public class DataCollector implements Steppable {
    String nodeDemandFile;
    String negativeDemandFile;
    String irrigDemandFile;
    String allSellerInfoFile;
    String negativeDemandOutput;
    String irrigationDemandOutput = "";
    String nodalDemandOutput;

    public DataCollector(String nodeDemandOutFile, String negativeDemandOutFile, String irrigDemandOutFile, String
            allSellerInfoOutFile){
        this.nodeDemandFile = nodeDemandOutFile;
        this.negativeDemandFile = negativeDemandOutFile;
        this.irrigDemandFile = irrigDemandOutFile;
        this.allSellerInfoFile = allSellerInfoOutFile;
    }

    public void step(SimState state){
        if (marketABM.currentHour == 0){
            printHeader();
        }
        printIrrigDemands();
        printNegativeDemands();
        if (marketABM.currentHour == marketABM.hoursInSimulation - 1){
            createOutputFile(marketABM.negativeDemandOutputFileName, "Negative");
            createOutputFile(marketABM.irrigationDemandOutputFileName, "Irrigation");
        }
    }

    public void createOutputFile(String fileName, String demandType){
        String demandString;
        if (demandType == "Irrigation"){
            demandString = this.irrigationDemandOutput;
        }
        else if(demandType == "Negative"){
            demandString = this.negativeDemandOutput;
        }
        else{
            demandString = this.nodalDemandOutput;
        }
        try{
            File file = new File(fileName);
            FileWriter writer = new FileWriter(file);
            writer.write(demandString);
            writer.flush();
            writer.close();
        }
        catch (java.io.IOException e){
            System.out.println(e);
            System.exit(0);
        }

    }

    public void printIrrigDemands(){
        String s = marketABM.currentHour + "\t";
        for (int i = 0; i < marketABM.numberHouses; i++) {
            s += (marketABM._households[i].currentStartIrrigDemand/marketABM.MINUTES_IN_HOUR) + "\t";
            }
        this.irrigationDemandOutput += s + "\n";
    }

    public void printNegativeDemands(){
        String s = marketABM.currentHour + "\t";
        for (int i = 0; i < marketABM.numberHouses; i++) {
            s += (marketABM._households[i].currentNegativeDemand/marketABM.MINUTES_IN_HOUR) + "\t";
        }
        this.negativeDemandOutput += s + "\n";
    }

    public void printHeader(){
        String s = "Time step\t";
        for (int i = 0; i < marketABM.numberHouses; i ++){
            String houseID = marketABM._households[i].mainSystemNodeID;
            s += houseID + "\t";
        }
        this.irrigationDemandOutput += s + "\n";
        this.negativeDemandOutput += s + "\n";
    }
}
