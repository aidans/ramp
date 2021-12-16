package org.gicentre.aidan.ramp;

import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;

public class ReadH5VariablesForJason {


	public static void main(String[] args){
		String filename="/Users/aidans/Documents/projects/2021-11_peat/2021-11-05_data/Claire Harris - abundances1.h5"; 

		try{
			NetcdfFile netcdfFile = NetcdfFiles.open(filename);

			//Read and list the variables
			for (Variable variable:netcdfFile.getVariables())
				System.out.println(variable.getNameAndDimensions());
					
			for (Object obj:(Object[])netcdfFile.findVariable("abundances/grid_id").read().copyTo1DJavaArray()) {
				System.out.println(obj);
			}
		}
		catch (Exception e) {
			System.err.println(e);
		}			
	}	
}
