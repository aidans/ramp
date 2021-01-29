package org.gicentre.aidan.ramp;

import java.awt.Rectangle;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;

import org.gicentre.shputils.ShpUtils;
import org.gicentre.utils.colour.ColourTable;
import org.gicentre.utils.gui.HelpScreen;
import org.gicentre.utils.move.ZoomPan;
import org.gicentre.utils.move.ZoomPanState;

import processing.core.PApplet;
import processing.core.PVector;
import ucar.ma2.ArrayInt;
import ucar.ma2.ArrayShort;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;


public class DemographicGridmap extends PApplet{
	
	ArrayList<Record> records;
	String[] demographicsAttribNames;
	private String[] statuses;
	private boolean[] statusShow;
	private int numDays;
	
	ColourTable ctDemog,ctResult,ctStatus,ctForce,ctReservoir;

	Rectangle bounds;
	Rectangle2D geoBounds;
	ZoomPan zoomPan;
	
	boolean animateThroughTime=true;
	
	int spatialBinSize=30; //in screen pixels
	int attribBinSize=15; //numbers of attributes
	
	Float colourScale=null;
	
	float scale,geoRatio;
	
	float[] minPopCounts,maxPopCounts;
	
	HashMap<String, Path2D> boundaries;
	HashMap<String, String> boundaryNames;
	
	enum Mode{
		Null,
		DemogColour,
		DemogBars,
		ModelBars,
		ModelSpine,
		ModelSpineTime,
		ModelGraph,
		ModelSpineQuintiles,
		ForceColour,
		ReservoirColour,
		ForceBars,
		ReservoirBars,
		ForceTime,
		ReservoirTime;
		
		boolean ableToDisplay() {
			switch (this) {
			case Null:
				return false;
			case DemogColour:
				return LOAD_DEMOGRAPHICS;
			case DemogBars:
				return LOAD_DEMOGRAPHICS;
			case ModelBars:
				return LOAD_OUTPUTS;
			case ModelSpine:
				return LOAD_OUTPUTS;
			case ModelSpineTime:
				return LOAD_OUTPUTS;
			case ModelGraph:
				return LOAD_OUTPUTS;
			case ModelSpineQuintiles:
				return LOAD_OUTPUTS;
			case ForceColour:
				return LOAD_FORCE;
			case ReservoirColour:
				return LOAD_FORCE;
			case ForceBars:
				return LOAD_FORCE;
			case ReservoirBars:
				return LOAD_FORCE;
			case ForceTime:
				return LOAD_FORCE;
			case ReservoirTime:
				return LOAD_FORCE;

			default:
				return false;
			}
			
		}
	}
	Mode mode=Mode.DemogColour;
	
	boolean mouseClicked=false;
	
	int wrap=20;
	
	int currentDay=0;

	
	
	private HelpScreen helpScreen;
	static String DATAFILE_DEMOGRAPHICS_PATH;
	static String DATAFILE_RESULTS_PATH;
	static String DATAFILE_FORCE_PATH;
	static String APP_NAME="DemographicGridmap, v1.3";
	
	static final boolean LOAD_DEMOGRAPHICS=true; //always true
	static boolean LOAD_OUTPUTS=true;
	static boolean LOAD_FORCE=true;
		
	static public void main(String[] args) {
		System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "ERROR");
		DATAFILE_DEMOGRAPHICS_PATH=args[0];
		DATAFILE_RESULTS_PATH=args[1];
		if (args.length>2 && args[2].equals("suppressAbuns"))
			LOAD_OUTPUTS=false;
		PApplet.main(new String[]{"org.gicentre.ramp.aidan.DemographicGridmap"});
	}
	
	public void setup() {
		println("Aidan Slingsby, a.slingsby@city.ac.uk, City, University of London");
		size(700,700);
		ctDemog=ColourTable.getPresetColourTable(ColourTable.PURPLES);
		ctResult=ColourTable.getPresetColourTable(ColourTable.REDS);
		ctStatus=ColourTable.getPresetColourTable(ColourTable.SET3_8);
		ctForce=ColourTable.getPresetColourTable(ColourTable.GREENS);
		ctReservoir=ColourTable.getPresetColourTable(ColourTable.BLUES);
		
		bounds=new Rectangle(0,0,width,height);
		zoomPan=new ZoomPan(this);
		zoomPan.setZoomMouseButton(RIGHT);
		loadData();		

		moveToNextDisplayableMode();
		
		float ratioX=bounds.width/(float)geoBounds.getWidth();
		float ratioY=bounds.height/(float)geoBounds.getHeight();
		scale=min(ratioX,ratioY);
		geoRatio=(float)(geoBounds.getHeight()/geoBounds.getWidth());

		
		helpScreen=new HelpScreen(this,createFont("Arial",12));
		helpScreen.setHeader(APP_NAME, 20, 16);
		
		helpScreen.putEntry("left drag","Pan");
		helpScreen.putEntry("right drag up/down","Zoom");
		helpScreen.addSpacer();
		helpScreen.putEntry("LEFT and RIGHT","Change grid size");
		helpScreen.putEntry("UP and DOWN","Increase/decrease number of age categories");
		helpScreen.putEntry("'[' and ']'","Change colour scaling");
		helpScreen.putEntry("'s'","Reset colour scaling");
		helpScreen.putEntry("'m/M'","Change display mode (forwards/backwards");
		helpScreen.putEntry("'a'","Animate throught time");
		helpScreen.putEntry("'Click on legend'","Toggle individual status on/off");
		helpScreen.addSpacer();		
		helpScreen.putEntry("'h'", "Show/hide help");
		
		helpScreen.setFooter("Aidan Slingsby, a.slingsby@city.ac.uk, City, University of London",16,10);

		
	}
	
	private void loadData() {
		print("Loading data...");
		long t=System.currentTimeMillis();
		records=new ArrayList<DemographicGridmap.Record>();

		HashMap<String, Record> location2Records=new HashMap<String, DemographicGridmap.Record>();
		
		boundaries=new HashMap<String, Path2D>();
		boundaryNames=new HashMap<String, String>();
		Path2D[] shapes=ShpUtils.getShapes("data/uk_nuts3_osgb.shp");
		String[] nuts3Ids=ShpUtils.getAttribsAsStrings("data/uk_nuts3_osgb.shp","NUTS_ID");
		for (int i=0;i<nuts3Ids.length;i++)
			if (nuts3Ids[i].startsWith("UKM"))
				boundaries.put(nuts3Ids[i], shapes[i]);

		try {
			BufferedReader br=new BufferedReader(new FileReader("data/NUTS_Level_3__2015__to_NUTS_Level_3__2018__Lookup_in_the_United_Kingdom.csv"));
			br.readLine();
			while (br.ready()) {
				String[] toks=br.readLine().split(",");
				if (boundaries.keySet().contains(toks[0])) {
					boundaryNames.put(toks[0],toks[3]);
				}
			}
		}
		catch (IOException e) {

		}
		
		
		//get demographics
		try{
			NetcdfFile netcdfFile = NetcdfFiles.open(DATAFILE_DEMOGRAPHICS_PATH);
			for (Variable variable:netcdfFile.getVariables())
			println(variable.getNameAndDimensions());

			String[] locations=(String[])netcdfFile.findVariable("/grid1km/1year/persons/Dimension_1_names").read().copyToNDJavaArray();
			demographicsAttribNames=(String[])netcdfFile.findVariable("/grid1km/1year/persons/Dimension_2_names").read().copyToNDJavaArray();
			double[][] values=(double[][])netcdfFile.findVariable("/grid1km/1year/persons/array").read().copyToNDJavaArray();

			minPopCounts=new float[demographicsAttribNames.length];
			Arrays.fill(minPopCounts,Float.MAX_VALUE);
			maxPopCounts=new float[demographicsAttribNames.length];
			Arrays.fill(maxPopCounts,-Float.MAX_VALUE);

			boolean isEmpty=true;
			for (int i=0;i<locations.length;i++) {
				Record record=new Record();
				record.popCounts=new short[demographicsAttribNames.length];
				for (int j=0;j<demographicsAttribNames.length;j++) {
					record.popCounts[j]=(short)values[j][i];
					minPopCounts[j]=min(minPopCounts[j],record.popCounts[j]);
					maxPopCounts[j]=max(maxPopCounts[j],record.popCounts[j]);
					if (record.popCounts[j]>0)
						isEmpty=false;
				}
				if (!isEmpty) {
					record.x=Short.parseShort(locations[i].split("-")[0]);
					record.y=Short.parseShort(locations[i].split("-")[1]);
					if (geoBounds==null)
						geoBounds=new Rectangle2D.Float(record.x,record.y,0,0);
					else
						geoBounds.add(record.x,record.y);
					location2Records.put(locations[i],record);
					records.add(record);
				}

				netcdfFile.close();
			}
		}
		catch (IOException e) {
			println(e);
		}

		//get results
		NetcdfFile netcdfFile;
		try {
			netcdfFile = NetcdfFiles.open(DATAFILE_RESULTS_PATH);
			String[] locations=(String[])netcdfFile.findVariable("/abundances/grid_id").read().copyToNDJavaArray();
			numDays=(int)netcdfFile.findVariable("/abundances/times").read().getSize();
			ArrayList<String> statuses=new ArrayList<String>();
			for (String compartment:(String[])netcdfFile.findVariable("/abundances/compartment").read().copyToNDJavaArray()) {
				String status=compartment.replaceAll("\\d*$", "");
				if (!statuses.contains(status))
					statuses.add(status);
			}
			this.statuses=new String[statuses.size()];
			statuses.toArray(this.statuses);
			this.statusShow=new boolean[statuses.size()];
			Arrays.fill(statusShow, true);

			int numStatuses=statuses.size();
			int numDemographics=10;
			int numLocations=locations.length;

			if (LOAD_OUTPUTS){
				int[] size=new int[]{numDays,1,numStatuses*numDemographics};
				Variable variable=netcdfFile.findVariable("/abundances/abuns");
				for (int locationIdx=0;locationIdx<numLocations;locationIdx++) {
					Record record=location2Records.get(locations[locationIdx].replaceAll(" km", ""));
					if (record!=null) {
						int[] origin={0,locationIdx,0};
						ArrayInt.D3 values=null;
						try{
							values=(ArrayInt.D3)variable.read(origin,size);
						}
						catch (InvalidRangeException e) {
							println(e);
						}
						for (int dayIdx=0;dayIdx<numDays;dayIdx++) {
							for (int statusIdx=0;statusIdx<numStatuses;statusIdx++) {
								for (int demographicIdx=0;demographicIdx<numDemographics;demographicIdx++) {
									if (record.resultCounts==null)
										record.resultCounts=new short[numDays][numDemographics][numStatuses];
									record.resultCounts[dayIdx][demographicIdx][statusIdx]=(short)values.get(dayIdx,0,statusIdx*numDemographics+demographicIdx);
								}
							}
						}
					}
				}
			}

			if (LOAD_FORCE){
				int[] size=new int[]{numDays,1,2};
				Variable variable=netcdfFile.findVariable("/abundances/abuns_virus");
				for (int locationIdx=0;locationIdx<numLocations;locationIdx++) {
					Record record=location2Records.get(locations[locationIdx].replaceAll(" km", ""));
					if (record!=null) {
						int[] origin={0,locationIdx,0};
						ArrayInt.D3 values=null;
						try{
							values=(ArrayInt.D3)variable.read(origin,size);
						}
						catch (InvalidRangeException e) {
							println(e);
						}
						for (int dayIdx=0;dayIdx<numDays;dayIdx++) {
							if (record.resultForce==null)
								record.resultForce=new short[numDays];
							record.resultForce[dayIdx]=(short)values.get(dayIdx,0,0);
							if (record.resultReservoir==null)
								record.resultReservoir=new short[numDays];
							record.resultReservoir[dayIdx]=(short)values.get(dayIdx,0,1);
						}
					}
				}
			}
			
			netcdfFile.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		System.gc();
	
		println("done.");
		long t1=System.currentTimeMillis();
		println(records.size()+" records in "+(t1-t)/1000+" seconds");
	}

	public void draw() {
		background(200);
		
		if (mouseY>height-20)
			currentDay=(int)map(mouseX,0,width,0,numDays);
		
		int graphMax=0;
		
		int forceMax=0;
		int reservoirMax=0;
		
		ZoomPanState zoomPanState=zoomPan.getZoomPanState();
		int[][][] demogSums=new int[(int)(bounds.getWidth()/spatialBinSize)][(int)bounds.getHeight()/spatialBinSize][demographicsAttribNames.length/attribBinSize];
		
		int[][][][] modelSums=null;
		if (mode==Mode.ModelBars || mode==Mode.ModelSpine)
			modelSums=new int[(int)(bounds.getWidth()/spatialBinSize)][(int)bounds.getHeight()/spatialBinSize][10][statuses.length];

		int[][][][] modelSumsTime=null;
		if (mode==Mode.ModelSpineTime || mode==Mode.ModelGraph)
			modelSumsTime=new int[(int)(bounds.getWidth()/spatialBinSize)][(int)bounds.getHeight()/spatialBinSize][numDays][statuses.length];//no demographics

		short[][][][] modelSumsQuintiles=null;
		ArrayList<ValuePair<Float, short[]>>[][] modelSumsQuintilesList=null;
		if (mode==Mode.ModelSpineQuintiles) {
			modelSumsQuintiles=new short[(int)(bounds.getWidth()/spatialBinSize)][(int)bounds.getHeight()/spatialBinSize][5][statuses.length];//no demographics
			modelSumsQuintilesList=new ArrayList[(int)(bounds.getWidth()/spatialBinSize)][(int)bounds.getHeight()/spatialBinSize];
		}

		int[][] forceReservoirCounts=null;
		if (mode==Mode.ForceColour || mode==Mode.ReservoirColour||mode==Mode.ForceBars || mode==Mode.ReservoirBars || mode==Mode.ForceTime || mode==Mode.ReservoirTime) 
			forceReservoirCounts=new int[(int)(bounds.getWidth()/spatialBinSize)][(int)bounds.getHeight()/spatialBinSize];
		
		//include days here, because we need to compute max for whole period (and also for animation
		int[][][] forceAvgs=null;
		if (mode==Mode.ForceColour || mode==Mode.ForceBars || mode==Mode.ForceTime)
			forceAvgs=new int[(int)(bounds.getWidth()/spatialBinSize)][(int)bounds.getHeight()/spatialBinSize][numDays];
		
		int[][][] reservoirAvgs=null;
		if (mode==Mode.ReservoirColour || mode==Mode.ReservoirBars || mode==Mode.ReservoirTime)
			reservoirAvgs=new int[(int)(bounds.getWidth()/spatialBinSize)][(int)bounds.getHeight()/spatialBinSize][numDays];

		for (Record record:records) {
			float x=record.x*scale;
			float y=bounds.y+bounds.height-record.y*scale;
			PVector pt=zoomPanState.getCoordToDisp(x,y);
			int xBin=(int)(pt.x/spatialBinSize);
			int yBin=(int)(pt.y/spatialBinSize);
			if (xBin>=0 && xBin<demogSums.length && yBin>=0 && yBin<demogSums[0].length) {
				for (int j=0;j<demographicsAttribNames.length;j++) {
					if (j/attribBinSize<demogSums[xBin][yBin].length) {
						demogSums[xBin][yBin][j/attribBinSize]+=record.popCounts[j];
					}
				}
				if (mode==Mode.ModelBars || mode==Mode.ModelSpine) {
					if (record.resultCounts!=null) 
						for (int j=0;j<10;j++) 
							for (int statusIdx=0;statusIdx<statuses.length;statusIdx++) {
								if (statusShow[statusIdx])
									modelSums[xBin][yBin][j][statusIdx]+=record.resultCounts[currentDay][j][statusIdx];
						}
				}
				if (mode==Mode.ModelSpineTime || mode==Mode.ModelGraph) {
					if (record.resultCounts!=null) 
						for (int statusIdx=0;statusIdx<statuses.length;statusIdx++)  
							if (statusShow[statusIdx]) {
								for (int day=0;day<numDays;day++) {
									for (int j=0;j<10;j++) {
										modelSumsTime[xBin][yBin][day][statusIdx]+=record.resultCounts[day][j][statusIdx];
									}
								}
							}
				}
				if (mode==Mode.ModelSpineQuintiles) {
					if (record.resultCounts!=null) { 
						ArrayList<ValuePair<Float, short[]>> al=modelSumsQuintilesList[xBin][yBin];
						if (al==null){
							al=new ArrayList<>();
							modelSumsQuintilesList[xBin][yBin]=al;
						}
						short[] vs=new short[statuses.length];
						int sum=0;
						for (int statusIdx=0;statusIdx<statuses.length;statusIdx++) {
							if (statusShow[statusIdx]) {
								for (int j=0;j<10;j++)
									vs[statusIdx]+=record.resultCounts[currentDay][j][statusIdx];
								sum+=vs[statusIdx];
							}
						}
						if (sum>0) {//only include if there's some data
							ValuePair<Float, short[]> vp=new ValuePair<Float, short[]>(vs[0]/(float)sum, vs);
							al.add(vp);
						}
					}
				}
				if (mode==Mode.ForceColour || mode==Mode.ReservoirColour||mode==Mode.ForceBars || mode==Mode.ReservoirBars || mode==Mode.ForceTime || mode==Mode.ReservoirTime)					if (record.resultForce!=null)
							forceReservoirCounts[xBin][yBin]++;
					
				if (mode==Mode.ForceColour || mode==Mode.ForceBars || mode==Mode.ForceTime)
					if (record.resultForce!=null)
						for (int day=0;day<numDays;day++)
							forceAvgs[xBin][yBin][day]+=record.resultForce[day];//do all days
				
				if (mode==Mode.ReservoirColour || mode==Mode.ReservoirBars || mode==Mode.ReservoirTime)
					if (record.resultReservoir!=null)
						for (int day=0;day<numDays;day++)
							reservoirAvgs[xBin][yBin][day]+=record.resultReservoir[day];
			}
		}
		if (mode==Mode.ModelGraph)
			for (int xBin=0;xBin<modelSumsTime.length;xBin++)
				for (int yBin=0;yBin<modelSumsTime[xBin].length;yBin++)
					for (int statusIdx=0;statusIdx<statuses.length;statusIdx++)
						if (statusShow[statusIdx]) {
							for (int day=0;day<numDays;day++)
								graphMax=max(graphMax,modelSumsTime[xBin][yBin][day][statusIdx]);
						}
			
		
		if (mode==Mode.ModelSpineQuintiles) {
			for (int xBin=0;xBin<modelSumsQuintilesList.length;xBin++) {
				for (int yBin=0;yBin<modelSumsQuintilesList[xBin].length;yBin++) {
					for (int statusIdx=0;statusIdx<statuses.length;statusIdx++) {
						if (statusShow[statusIdx]) {
							ArrayList<ValuePair<Float, short[]>> al=modelSumsQuintilesList[xBin][yBin];
							if (al!=null && !al.isEmpty()){
								Collections.sort(al,ValuePair.getValue1Comparator());
								float binSize=al.size()/5f;
								modelSumsQuintiles[xBin][yBin][0]=al.get((int)(binSize/2)).v2;
								modelSumsQuintiles[xBin][yBin][1]=al.get((int)(binSize+binSize/2)).v2;
								modelSumsQuintiles[xBin][yBin][2]=al.get((int)(binSize*2+binSize/2)).v2;
								modelSumsQuintiles[xBin][yBin][3]=al.get((int)(binSize*3+binSize/2)).v2;
								modelSumsQuintiles[xBin][yBin][4]=al.get((int)(binSize*4+binSize/2)).v2;
							}
						}
					}
				}
			}
			modelSumsQuintilesList=null;
		}
		if (mode==Mode.ForceColour || mode==Mode.ForceBars || mode==Mode.ForceTime) {
			//find average
			for (int xBin=0;xBin<forceAvgs.length;xBin++) {
				for (int yBin=0;yBin<forceAvgs[xBin].length;yBin++) {
					if (forceReservoirCounts[xBin][yBin]>0) {
						//for all days
						for (int day=0;day<numDays;day++) {
							forceAvgs[xBin][yBin][day]/=forceReservoirCounts[xBin][yBin];
							forceMax=max(forceMax,forceAvgs[xBin][yBin][day]);
						}
					}
				}
			}
		}

		if (mode==Mode.ReservoirColour || mode==Mode.ReservoirBars || mode==Mode.ReservoirTime) {
			//find average
			for (int xBin=0;xBin<reservoirAvgs.length;xBin++) {
				for (int yBin=0;yBin<reservoirAvgs[xBin].length;yBin++) {
					if (forceReservoirCounts[xBin][yBin]>0) {
						//for all days
						for (int day=0;day<numDays;day++) {
							reservoirAvgs[xBin][yBin][day]/=forceReservoirCounts[xBin][yBin];
							reservoirMax=max(reservoirMax,reservoirAvgs[xBin][yBin][day]);
						}
					}
				}
			}
		}

		
		//if need to rescale colour/width
		Float colourScale=this.colourScale;
		if (colourScale==null) {
			colourScale=-Float.MAX_VALUE;
			for (int x=0;x<demogSums.length;x++) {
				for (int y=0;y<demogSums.length;y++) {
					if (mode==Mode.DemogBars||mode==Mode.DemogColour) {
						for (int j=0;j<demographicsAttribNames.length/attribBinSize;j++) {
							colourScale=max(colourScale,(float)(demogSums[x][y][j]));
						}
					}
					else if (mode==Mode.ModelBars) {
						for (int j=0;j<10;j++) {
							float sum=0;
							for (int statusIdx=0;statusIdx<statuses.length;statusIdx++)
								if (statusShow[statusIdx])
									sum+=modelSums[x][y][j][statusIdx];

							colourScale=max(colourScale,sum);
						}
					}
				}
			}
			this.colourScale=colourScale;
		}
				
		//make squares with data white
		if (mode==Mode.DemogBars || mode==Mode.ModelBars || mode==Mode.ModelGraph) {
			for (int x=0;x<demogSums.length;x++) {
				for (int y=0;y<demogSums[x].length;y++) {
					boolean empty=true;
					for (int j=0;j<demogSums[x][y].length;j++) {
						if (demogSums[x][y][j]>0)
							empty=false;
						if (!empty) {
							fill(255);
							rect(x*spatialBinSize,y*spatialBinSize,spatialBinSize,spatialBinSize);
						}
					}
				}
			}
		}
		

		noStroke();
		for (int x=0;x<demogSums.length;x++) {
			for (int y=0;y<demogSums[x].length;y++) {
				boolean empty=true;
				for (int j=0;j<demogSums[x][y].length;j++)
					if (demogSums[x][y][j]>0)
						empty=false;
				if (!empty) {
					if (mode==Mode.DemogColour || mode==Mode.DemogBars) {
						for (int j=0;j<demogSums[x][y].length;j++) {
							if (mode==Mode.DemogColour) {
								fill(ctDemog.findColour(map((float)demogSums[x][y][j],0,colourScale,0,1)));
								rect(x*spatialBinSize,y*spatialBinSize+j*((float)spatialBinSize/demogSums[x][y].length),spatialBinSize,((float)(spatialBinSize)/demogSums[x][y].length));
							}
							else if (mode==Mode.DemogBars) {
								float w=constrain(map((float)demogSums[x][y][j],0,colourScale,0,spatialBinSize),0,spatialBinSize);
								fill(ctDemog.findColour(0.6f));
								rect(x*spatialBinSize+spatialBinSize/2-w/2,y*spatialBinSize+j*((float)spatialBinSize/demogSums[x][y].length),w,((float)(spatialBinSize)/demogSums[x][y].length));						
							}
						}
					}
					if (mode==Mode.ModelBars) {
						float h=(float)spatialBinSize/10f;
						for (int j=0;j<10;j++) {
							float yPos=y*spatialBinSize+j*h;
							float xPos=x*spatialBinSize;
							for (int statusIdx=0;statusIdx<statuses.length;statusIdx++) {
								if (statusShow[statusIdx]) {
									float w=constrain(map((float)modelSums[x][y][j][statusIdx],0,colourScale,0,spatialBinSize),0,spatialBinSize);
									fill(ctStatus.findColour(statusIdx+1));
									rect(xPos,yPos,w,h);
									xPos+=w;
								}
							}
						}
					}
					if (mode==Mode.ModelSpine) {
						float h=(float)spatialBinSize/10f;
						for (int j=0;j<10;j++) {
							float yPos=y*spatialBinSize+j*h;
							float xPos=x*spatialBinSize;
							float sum=0;
							for (int statusIdx=0;statusIdx<statuses.length;statusIdx++)
								if (statusShow[statusIdx])
									sum+=modelSums[x][y][j][statusIdx];
							for (int statusIdx=0;statusIdx<statuses.length;statusIdx++) {
								if (statusShow[statusIdx]) {
									float w=constrain(map((float)modelSums[x][y][j][statusIdx],0,sum,0,spatialBinSize),0,spatialBinSize);
									fill(ctStatus.findColour(statusIdx+1));
									rect(xPos,yPos,w,h);
									xPos+=w;
								}
							}
						}
					}
					if (mode==Mode.ModelSpineTime) {
						float w=(float)spatialBinSize/numDays;
						for (int day=0;day<numDays;day++) {
							float xPos=x*spatialBinSize+day*w;
							float yPos=y*spatialBinSize;
							float sum=0;
							for (int statusIdx=0;statusIdx<statuses.length;statusIdx++)
								if (statusShow[statusIdx])
									sum+=modelSumsTime[x][y][day][statusIdx];
							for (int statusIdx=0;statusIdx<statuses.length;statusIdx++) {
								if (statusShow[statusIdx]) {
									float h=constrain(map((float)modelSumsTime[x][y][day][statusIdx],0,sum,0,spatialBinSize),0,spatialBinSize);
									fill(ctStatus.findColour(statusIdx+1));
									rect(xPos,yPos,w,h);
									yPos+=h;
								}
							}
						}
					}
					if (mode==Mode.ModelGraph) {
						noFill();
						for (int statusIdx=0;statusIdx<statuses.length;statusIdx++) {
							if (statusShow[statusIdx]) {
								stroke(ctStatus.findColour(statusIdx+1));
								beginShape();
								for (int day=0;day<numDays;day++) {

									vertex(
											map(day,0,numDays,x*spatialBinSize,x*spatialBinSize+spatialBinSize),
											map(modelSumsTime[x][y][day][statusIdx],0,graphMax,y*spatialBinSize,y*spatialBinSize-spatialBinSize)
											);
								}
								endShape();
							}
						}
					}
					if (mode==Mode.ModelSpineQuintiles) {
						float h=(float)spatialBinSize/2/5f;
						for (int q=0;q<5;q++) {
							float yPos=y*spatialBinSize+spatialBinSize/4+q*h;
							float xPos=x*spatialBinSize;
							float sum=0;
							for (int statusIdx=0;statusIdx<statuses.length;statusIdx++)
								if (statusShow[statusIdx])
									sum+=modelSumsQuintiles[x][y][q][statusIdx];
							if (sum>0) {
								for (int statusIdx=0;statusIdx<statuses.length;statusIdx++) {
									if (statusShow[statusIdx]) {
										float w=constrain(map((float)modelSumsQuintiles[x][y][q][statusIdx],0,sum,0,spatialBinSize),0,spatialBinSize);
										fill(ctStatus.findColour(statusIdx+1));
										rect(xPos,yPos,w,h);
										xPos+=w;
									}
								}
							}
						}
					}
					if (mode==Mode.ForceColour) {
						fill(ctForce.findColour(map((float)forceAvgs[x][y][currentDay],0,forceMax,0,1)));
						rect(x*spatialBinSize,y*spatialBinSize,spatialBinSize,spatialBinSize);
					}
					if (mode==Mode.ForceBars) {
						float wH=sqrt(constrain(map((float)forceAvgs[x][y][currentDay],0,forceMax,0,pow(spatialBinSize,2)),0,pow(spatialBinSize,2)));
						fill(ctForce.findColour(0.6f));
						rect(x*spatialBinSize+spatialBinSize/2-wH/2,y*spatialBinSize+spatialBinSize/2-wH/2,wH,wH);						
					}
					if (mode==Mode.ForceTime) {
						fill(ctForce.findColour(0.6f));
						for (int day=0;day<numDays;day++) {
							float w=spatialBinSize/(float)numDays;
							float h=constrain(map((float)forceAvgs[x][y][day],0,forceMax,0,spatialBinSize),0,spatialBinSize);
							rect(x*spatialBinSize+w*day,y*spatialBinSize+spatialBinSize, w,-h);
						}
					}
					if (mode==Mode.ReservoirColour) {
						fill(ctReservoir.findColour(map((float)reservoirAvgs[x][y][currentDay],0,reservoirMax,0,1)));
						rect(x*spatialBinSize,y*spatialBinSize,spatialBinSize,spatialBinSize);
					}
					if (mode==Mode.ReservoirBars) {
						float wH=sqrt(constrain(map((float)reservoirAvgs[x][y][currentDay],0,reservoirMax,0,pow(spatialBinSize,2)),0,pow(spatialBinSize,2)));
						fill(ctReservoir.findColour(0.6f));
						rect(x*spatialBinSize+spatialBinSize/2-wH/2,y*spatialBinSize+spatialBinSize/2-wH/2,wH,wH);						
					}
					if (mode==Mode.ReservoirTime) {
						fill(ctReservoir.findColour(0.6f));
						for (int day=0;day<numDays;day++) {
							float w=spatialBinSize/(float)numDays;
							float h=constrain(map((float)reservoirAvgs[x][y][day],0,reservoirMax,0,spatialBinSize),0,spatialBinSize);
							rect(x*spatialBinSize+w*day,y*spatialBinSize+spatialBinSize, w,-h);
						}
					}
					
				}
			}
		}

		stroke(0,30);
		for (int x=0;x<demogSums.length;x++) 
			line(x*spatialBinSize,bounds.y,x*spatialBinSize,bounds.y+bounds.height);
		
		for (int y=0;y<demogSums[0].length;y++)
			line(bounds.x,y*spatialBinSize,bounds.x+bounds.width,y*spatialBinSize);
		
		if (helpScreen.getIsActive())
			helpScreen.draw();

		if (animateThroughTime)
			currentDay++;
		if (currentDay>=numDays)
			currentDay=0;

		//draw legend
		if (mode==Mode.ModelBars || mode==Mode.ModelSpine || mode==Mode.ModelGraph || mode==Mode.ModelSpineQuintiles|| mode==Mode.ModelSpineTime) {
			int y=height-(statuses.length*12);
			int legendW=0;
			textAlign(RIGHT,TOP);
			textSize(10);
			for (String status:statuses)
				legendW=(int)max(legendW,textWidth(status));
			legendW+=12;
			fill(255,230);
			noStroke();
			rect(width-legendW,y,legendW,height-y);
			for (int statusIdx=0;statusIdx<statuses.length;statusIdx++) {
				if (statusShow[statusIdx]) {
					fill(ctStatus.findColour(statusIdx+1));
					rect(width-10,y,10,10);
				}
				if (mouseX>width-legendW && mouseY>y && mouseY<y+12 && mouseClicked) {
					statusShow[statusIdx]=!statusShow[statusIdx];
				}
				fill(80);
				text(statuses[statusIdx],width-12,y);
				y+=12;
			}
		}

		
		//draw boundaries
		PVector ptMouse=zoomPanState.getDispToCoord(new PVector(mouseX,mouseY));
		int geoMouseX=(int)map(ptMouse.x,0,bounds.height/geoRatio,9013,470013);
		int geoMouseY=(int)map(ptMouse.y,bounds.height,0,530301.5f,1217301.5f);
		stroke(150);
		float[] coords=new float[6];
		noFill();
		for (Entry<String, Path2D> entry:boundaries.entrySet()) {
			if (entry.getValue().contains(geoMouseX,geoMouseY)) {
				fill(0,80);
				textSize(20);
				textAlign(LEFT,BOTTOM);
				text(boundaryNames.get(entry.getKey()),0,height);
				noFill();
			}
			PathIterator pi=entry.getValue().getPathIterator(null);
			beginShape();
			while (!pi.isDone()) {
				int type=pi.currentSegment(coords);
				if (type==PathIterator.SEG_MOVETO) {
					endShape(CLOSE);
					beginShape();
				}
				float x=map(coords[0],9013,470013,0,bounds.height/geoRatio);
				float y=map(coords[1],530301.5f,1217301.5f,bounds.height,0);
				PVector pt=zoomPanState.getCoordToDisp(x,y);
				vertex(pt.x,pt.y);
				pi.next();
			}
			endShape(CLOSE);
		}
		
		
		
		fill(0,100);
		textSize(30);
		textLeading(30);
		textAlign(LEFT,TOP);
		String title="";
		if (mode==Mode.DemogColour)
			title="Population by age group (younger at top)";
		if (mode==Mode.DemogBars)
			title="Population of residents by age group (younger at top)";
		if (mode==Mode.ModelBars)
			title="Population modelled status by age group (younger at top) for day "+currentDay;
		if (mode==Mode.ModelGraph)
			title="Population modelled status over time";
		if (mode==Mode.ModelSpine)
			title="Population modelled status proportion by age group (younger at top) for day "+currentDay;
		if (mode==Mode.ModelSpineQuintiles)
			title="Quintiles (by proportion susceptable) of modelled status proportion for day "+currentDay;
		if (mode==Mode.ModelSpineTime)
			title="Population modelled status proportion over time (left to right)";
		if (mode==Mode.ForceColour)
			title="Force of infection for day "+currentDay;
		if (mode==Mode.ReservoirColour)
			title="Environmental virus reservoir for day "+currentDay;
		if (mode==Mode.ForceBars)
			title="Force of infection for day "+currentDay;
		if (mode==Mode.ReservoirBars)
			title="Environmental virus reservoir for day "+currentDay;
		if (mode==Mode.ForceTime)
			title="Force of infection over time (left to right)";
		if (mode==Mode.ReservoirTime)
			title="Environmental virus reservoir over time (left to right)";
		text(title,0,0,width,height);
		mouseClicked=false;
	}
	
	public void keyPressed() {
		if (key==CODED && keyCode==LEFT) {
			spatialBinSize--;
			if (spatialBinSize<2)
				spatialBinSize=2;
		}
		if (key==CODED && keyCode==RIGHT) {
			spatialBinSize++;
		}
		if (key==CODED && keyCode==UP) {
			attribBinSize--;
			if (attribBinSize<1)
				attribBinSize=1;
		}
		if (key==CODED && keyCode==DOWN) {
			attribBinSize++;
			if (attribBinSize>demographicsAttribNames.length)
				attribBinSize=demographicsAttribNames.length-1;
		}
		if (key=='[') {
			colourScale+=colourScale/10f;
		}
		if (key==']') {
			colourScale-=colourScale/10f;
		}
		if (key=='s') {
			colourScale=null;
		}
		if (key=='m') {
			moveToNextDisplayableMode();
		}
		if (key=='M') {
			moveToPrevDisplayableMode();
		}
		if (key=='a') {
			animateThroughTime=!animateThroughTime;
		}
		if (key=='h') {
			helpScreen.setIsActive(!helpScreen.getIsActive());
		}
	
	}
	
	private void moveToNextDisplayableMode() {
		int ord=mode.ordinal();
		do {
			ord++;
			if (ord>=Mode.values().length)
				ord=0;
			this.mode=Mode.values()[ord];
		} while(!this.mode.ableToDisplay());
	}

	private void moveToPrevDisplayableMode() {
		int ord=mode.ordinal();
		do {
			ord--;
			if (ord<0)
				ord=Mode.values().length-1;
			this.mode=Mode.values()[ord];
		} while(!this.mode.ableToDisplay());
	}
	
	public void mouseClicked() {
		mouseClicked=true;
	}
	
	class Record{
		short x,y;
		short[] popCounts; //by demographicgroup
		short[][][] resultCounts; //by time, demographicGroup, infectionType
		short[] resultForce; //by time
		short[] resultReservoir; //by time
	}
	
}
