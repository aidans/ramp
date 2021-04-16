package org.gicentre.aidan.ramp;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;

import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers.Member;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;


public class ReadH5Variables {


	public static void main(String[] args){
		String filename="/Users/aidans/Desktop/725de7be86184fb1aeb2ac8ace0b1707.h5"; 

		try{
			NetcdfFile netcdfFile = NetcdfFiles.open(filename);

			//Read and list the variables
			for (Variable variable:netcdfFile.getVariables())
				System.out.println(variable.getNameAndDimensions());
			
			//extract the table
	        StructureData[] data=(StructureData[])netcdfFile.findVariable("outbreak-timeseries/table").read().copyToNDJavaArray();
	        
	        //find out the names and types of each member (can omit this block)
	        for (Member member:data[0].getMembers()) {
	        	if (member.getShape().length==0)
	        		System.out.println(member.getName()+" is of type "+member.getDataType()+" ");
	        	else
        		System.out.println(member.getName()+" is of type "+member.getDataType()+" "+member.getShape()[0]);
	        }

	        //write out to CSV
	        BufferedWriter bw=new BufferedWriter(new FileWriter("data/output.csv"));
	        //write header
	        bw.write("date,node,age,state,mean,std\n");
	        for (StructureData structureData:data) {
	        	for (char ch:structureData.getJavaArrayChar("date"))
	        		bw.write((int)ch+"|");
	        	bw.write(",");
	        	for (char ch:structureData.getJavaArrayChar("node"))
	        		bw.write((int)ch+"|");
	        	bw.write(",");
	        	for (char ch:structureData.getJavaArrayChar("age"))
	        		bw.write((int)ch+"|");
	        	bw.write(",");
	        	bw.write((int)structureData.getScalarChar("state")+",");
	        	bw.write(structureData.getScalarDouble("mean")+",");
	        	bw.write(structureData.getScalarDouble("std")+",");
	        	bw.write("\n");
	        }

	        
	        //OPTIONAL load into arrays
	        char[][] date=new char[data.length][];
	        char[][] node=new char[data.length][];
	        char[][] age=new char[data.length][];
	        char[] state=new char[data.length];
	        double[] mean=new double[data.length];
	        double[] std=new double[data.length];
	        //populate the arrays
	        for (int i=0;i<data.length;i++) {
	        	StructureData structureData=data[i];
	        	date[i]=structureData.getJavaArrayChar("date");
	        	node[i]=structureData.getJavaArrayChar("node");
	        	age[i]=structureData.getJavaArrayChar("age");
	        	state[i]=structureData.getScalarChar("state");
	        	mean[i]=structureData.getScalarDouble("mean");
	        	std[i]=structureData.getScalarDouble("std");
	        }
	        
		}
		catch (Exception e){
			System.err.println(e);
		}
	}	
}
