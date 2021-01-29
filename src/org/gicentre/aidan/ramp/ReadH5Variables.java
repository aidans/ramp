package org.gicentre.aidan.ramp;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;


public class ReadH5Variables {


	public static void main(String[] args){
		String filename="/Users/aidans/Documents/projects/2010-06_ramp/demographics.h5"; 

		try{
			NetcdfFile netcdfFile = NetcdfFiles.open(filename);

			//Read and list the variables
			for (Variable variable:netcdfFile.getVariables())
				System.out.println(variable.getNameAndDimensions());
		}
		catch (Exception e){
		}
	}	
}
