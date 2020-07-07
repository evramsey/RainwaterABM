# RainwaterABM

Smart Grid for Microtrading Harvested Rainwater

Authors: J. Monroe, A.K. Fasaee, J. Pesantez, M. DiCarlo, E. Ramsey, E.Z. Berglund
Programmed by: E. Ramsey and J. Monroe


INTRODUCTION
-------------------------
This Java package contains an agent-based model of a decentralized rainwater harvesting network. 
The output files are formatted to correspond to nodes within EPANet to allow for the 
investigation of hydraulic feasibility.


REQUIREMENTS
-------------------------
The model requires the Java Development Kit 1.8 and installation of the latest MASON software 
(https://cs.gmu.edu/~eclab/projects/mason/).


INPUT FILES
-------------------------

The program needs five input .txt files to run. The files are listed below in order, and a 
description of the required format for each file is given. All files must be tab delimited and 
have a header. Example files are included in this package for reference.

1) Basic Information
	This file is a list of the variables assigned to each household agent. Each row 
	represents an agent, and the columns give the values for each variable. 
	The variables are, in order:
	a.) Whether the household has a water storage tank (TRUE/FALSE)
	b.) The size of the household's catchment area multiplied by the assumed catchment 
	    efficiency [ft^2]*
	c.) The diameter of the rainwater tank [m]
	d.) The height of the rainwater tank [m]
	e.) The payment per gallon of rainwater a household is willing to accept (WTA) [$/gal]
	f.) The payment per gallon of rainwater a household is willing to pay (WTP) [$/gal]
	g.) The irrigated area [m^2] **
	h.) The irrigation efficiency (0-1)
	i.) The crop coefficient for the irrigated area (0-1)

    We assume for this feasibility study that all households have the same tank heights, thank 
    diameters, WTAs, WTPs, irrigation area sizes, catchment sizes and irrigation efficiencies.

    * 	The household catchment area is assumed to be the roof size, taken from the (MORGAN: citation?)
	and assuming an 85% efficiency.
    ** 	The irrigated areas for this study are assumed to be lawns. Lawn area, A, is calculated as:
	
	A = 0.9 * [(1/housing density) - roof size]
	
	Housing density information for each city is taken from (MORGAN: citation?) and the roof
	size is the same value used for the catchment area calculation.
	

2) Climate Information
	This file contains two variables: the monthly potential evapotranspiration [m/month]
	and the effective rainfall [m/day], r. The monthly potential evapotranspiration is taken from
	USGS/EROS data. The effective rainfall, r, is calculated according to the formula
	(Jacob and Haarhof, 2004):
	
	r = R 			if		R < 25
	r = (0.504*R + 12.4)	if		25 <= R <= 152
	r = 89.0		if		R > 152

	where R is the cumulative measured rainfall for the month [mm/month].

3) Hourly Precipitation Data
	This file contains the hourly precipitation [in]. The first column specifies the date,
	the second column specifies the time, and the third gives the measured precipitation
	value. The data we use for this study are collected by USGS 
	(https://waterdata.usgs.gov).
	
4) Irrigation Demand Pattern	
	Agents are assumed to irrigate at the same time each day (i.e. every 24 time steps).
	This file contains the number of agents that are irrigate at each hour. These values
	are derived from Willis et al. (2011).

5) EPANet Node ID file
	This file lists the names of all nodes in a corresponding EPANet file. Each row 
	corresponds to a household; the first column is the irrigation node ID, the second column
	is the negative demand node ID (i.e., the rainwater the prosumers pump into the system), and
	the third column is the meter ID. Each row here corresponds to the agent at the same
	position in the Basic Information file. 


OUTPUT FILES
-------------------------
The model also requires the user to specify three output file paths. The output files are:
The demand at each node (Mohammad: Is this necessary for EPANet? I wasn't sure what the file was for
so the code doesn't yet do this. We should update it or this text accordingly)., the Negative Demand
file, and the Irrigation Demand file. Both the Negative and the Irrigation Demand files have a 
column header denoting the corresponding EPANet household ID, and each row corresponds to each hourly
timestep of the model.

RUNNING THE MODEL
-------------------------
The main class of the model is Automation.java. After the simulation begins to run, the model 
will require console input of the complete file paths of all input and output files.

NOTE FOR STSA: the filenames are hard coded to make the testing process run faster. Change
the lines 14-21 in Automation.java as needed. We will set up the console to run before publishing.

Currently, the model is run for 1 iteration and specifies a seed. These can be changed at Lines 5 and 6
(numSims and seedNum, respectively) of Automation.java as needed.

NOTE FOR STSA: The variables we discussed changing for testing are:

1) the amount of water flushed: flushDiversionVolume (Line 63 in Household.java)
2) the length of time between first flushes: numHoursForFlushDiversion (Line 66 in Household.java)
3) the length of time between rain and the resumption of trading: rainTradeGap (Line 62 in marketABM.java)
4) the size of catchment areas to reflect a neighborhood of tanks (Column b in the Basic Info text)
5) the amount of rainfall to see what the thresholds are for trading (Precipitation text).



REFERENCES
-------------------------

Jacobs, H.E. and Haarhoff, J. (2004) Structure and data requirements of an end-use
model for residential water demand and return flow. Water SA Vol. 30 No. 3. 293-304

Willis, R.M., Stewart, R.A., Williams, P.R., Hacker, C.H., Emmonds, S.C., and Capati, G.
(2011) Residential Potable and Recycled Water End Uses in a Dual Reticulated Supply System.
Desalination 272(1-3). 201-211.
