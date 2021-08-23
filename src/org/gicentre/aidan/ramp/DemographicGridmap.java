package org.gicentre.aidan.ramp;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.math3.genetics.Population;
import org.gicentre.shputils.ShpUtils;
import org.gicentre.utils.colour.ColourTable;
import org.gicentre.utils.gui.EnumChanger;
import org.gicentre.utils.gui.HelpScreen;
import org.gicentre.utils.gui.StringChanger;
import org.gicentre.utils.gui.ValueChanger;
import org.gicentre.utils.gui.ValueChangerListener;
import org.gicentre.utils.move.ZoomPan;
import org.gicentre.utils.move.ZoomPanState;
import org.gicentre.utils.text.WordWrapper;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PVector;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;


public class DemographicGridmap extends PApplet{

	static String APP_NAME="DemographicGridmap, v1.8 (16/08/21)";

	static String DATAFILE_DEMOGRAPHICS_PATH;
	static ArrayList<String> DATAFILE_RESULTS_PATHS;
	static String DATAFILE_FORCE_PATH;
	static String DATAFILE_POLLUTION;
	
	
	static int AGGREGATE_INPUT_M=1000;

	
	ArrayList<Record> gridRecords;
	ArrayList<Record> areaRecords;
	String[] demographicsAttribNames;
	private String[] statuses;
	private boolean[] statusShow;
	private int numDays;
	
	ColourTable ctDemog,ctResult,ctStatus,ctForce,ctReservoir;

	Rectangle bounds,gridBounds;
	Rectangle2D geoBounds;
	Point[] gridCells;//in same order as areas in shapefile 

	
	ZoomPan zoomPan;
	
	boolean animateThroughTime=true;
	
	int spatialBinSize=30; //in screen pixels
	int attribBinSize=15; //numbers of attributes
	
	Float colourScale=null;
	Float colourScale2=null;
	
	
	HashMap<String, Path2D> boundaries;
	
	enum Mode{
		Population,
		ModelAgeDay,
		ModelTime,
		ModelComparisonTime,
	}
	
	boolean mouseClicked=false;
	
	int wrap=20;
	
	int currentDay=0;

	
	private HelpScreen helpScreen;
		
	
	HashMap<String, String> tile2coordprefix=null;
	
	ArrayList<String> datasetNames;
	

	int statusBarH=18+18;
	StringChanger datasetChanger;
	StringChanger comparisonDatasetChanger;
	enum AbsRel{Absolute,Relative, RelativeSymb,RelativeFade}
	EnumChanger<AbsRel> absRelChanger;
	EnumChanger<Mode> modeChanger;
	enum Projection{Gridded,Area,GridMap}
	EnumChanger<Projection> projectionChanger;
	

	static public void main(String[] args) {
		DATAFILE_RESULTS_PATHS=new ArrayList<>();
		for (String arg:args) {
			String[] toks=arg.split("=");
			if (toks[0].equalsIgnoreCase("demographics_file"))
				if (toks.length==2) {
					DATAFILE_DEMOGRAPHICS_PATH=toks[1].replaceAll("\"", "");
				}
			if (toks[0].equalsIgnoreCase("results_file")) {
				if (toks.length==2) 
					DATAFILE_RESULTS_PATHS.add(toks[1].replaceAll("\"", ""));
				
			}
			if (toks[0].equalsIgnoreCase("AGGREGATE_INPUT_M") && toks.length==2) {
				AGGREGATE_INPUT_M=Integer.parseInt(toks[1]);
			}
		}
			
		PApplet.main(new String[]{"org.gicentre.aidan.ramp.DemographicGridmap"});
	}
	
	public void setup() {
		
//		LOAD_OUTPUTS=false;
//		LOAD_BASELINE=false;

		println("Aidan Slingsby, a.slingsby@city.ac.uk, City, University of London");
		size(800,800);
		ctDemog=ColourTable.getPresetColourTable(ColourTable.PURPLES);
		ctResult=ColourTable.getPresetColourTable(ColourTable.REDS);
		ctStatus=ColourTable.getPresetColourTable(ColourTable.SET3_8);
		ctForce=ColourTable.getPresetColourTable(ColourTable.GREENS);
		ctReservoir=ColourTable.getPresetColourTable(ColourTable.BLUES);
		
		bounds=new Rectangle(0,0,width,height-statusBarH);
		zoomPan=new ZoomPan(this);
		zoomPan.setZoomMouseButton(RIGHT);
		loadData();		

		{
			double ratioX=bounds.width/geoBounds.getWidth();
			double ratioY=bounds.height/geoBounds.getHeight();
			if (ratioX<ratioY) {
				float diff=(float)(geoBounds.getHeight()-bounds.height/ratioX);
				geoBounds.setFrame(geoBounds.getX(),geoBounds.getY()+diff/2,bounds.width/ratioX,bounds.height/ratioX);
			}
			else {
				float diff=(float)(bounds.width/ratioY-geoBounds.getWidth());
				geoBounds.setFrame(geoBounds.getX()-diff/2,geoBounds.getY(),bounds.width/ratioY,bounds.height/ratioY);
			}
		}

		{
			double ratioX=(double)bounds.width/gridBounds.width;
			double ratioY=(double)bounds.height/gridBounds.height;
			if (ratioX<ratioY) {
				float diff=(float)(gridBounds.getHeight()-bounds.height/ratioX);
				gridBounds.setFrame(gridBounds.getX()-diff/2,gridBounds.getY(),bounds.width/ratioX,bounds.height/ratioX);
			}
			else {
				float diff=(float)(bounds.width/ratioY-gridBounds.getWidth());
				gridBounds.setFrame(gridBounds.getX(),gridBounds.getY()+diff/2,bounds.width/ratioY,bounds.height/ratioY);
			}
		}

		helpScreen=new HelpScreen(this,createFont("Arial",12));
		helpScreen.setHeader(APP_NAME, 20, 16);
		
		helpScreen.putEntry("left drag","Pan");
		helpScreen.putEntry("right drag up/down","Zoom");
		helpScreen.addSpacer();
		helpScreen.putEntry("LEFT and RIGHT","Change grid size");
		helpScreen.putEntry("'[' and ']'","Increase/decrease number of age categories");
		helpScreen.putEntry("LEFT and RIGHT","Change colour scaling");
		helpScreen.putEntry("SHIFT LEFT and SHIRT RIGHT","Change relative symbolism scaling");
		helpScreen.putEntry("Move mouse left/right at top of screen","Manually scroll though time");
		helpScreen.addSpacer();
		helpScreen.putEntry("'s'","Reset colour scaling");
		helpScreen.putEntry("'m/M'","Change display mode (forwards/backwards)");
		helpScreen.putEntry("'d/D'","Change dataset (forwards/backwards)");
		helpScreen.putEntry("'c/C'","Change scaling type");
		helpScreen.putEntry("'a'","Turn on/off animation");
		helpScreen.putEntry("'Click on legend'","Toggle individual status on/off");
		helpScreen.addSpacer();		
		helpScreen.putEntry("'h'", "Show/hide help");
		
		helpScreen.setFooter("Aidan Slingsby, a.slingsby@city.ac.uk, City, University of London",16,10);

		datasetNames=new ArrayList<String>();
		for (String path:DATAFILE_RESULTS_PATHS) {
			String name=new File(path).getName();
			datasetNames.add(name);
		}
		PFont pFont=createDefaultFont(12);
		datasetChanger=new StringChanger(this,"Dataset",datasetNames,pFont);
		datasetChanger.setListener(new ValueChangerListener() {
			public void valueChanged(ValueChanger valueChanger) {
				colourScale=null;
				colourScale2=null;
				if (datasetChanger.getValue().equals(comparisonDatasetChanger.getValue()))
					comparisonDatasetChanger.changeToNextValue();
			}
		});
		comparisonDatasetChanger=new StringChanger(this,"Comparison dataset",datasetNames,pFont);
		comparisonDatasetChanger.changeToNextValue();//so it's a different value
		comparisonDatasetChanger.setListener(new ValueChangerListener() {
			public void valueChanged(ValueChanger valueChanger) {
				colourScale=null;
				colourScale2=null;
				if (comparisonDatasetChanger.getValue().equals(datasetChanger.getValue()))
					datasetChanger.changeToNextValue();
			}
		});
		modeChanger=new EnumChanger(this, "Mode", Mode.class,pFont);
		modeChanger.setListener(new ValueChangerListener() {
			public void valueChanged(ValueChanger valueChanger) {
				colourScale=null;
				colourScale2=null;
			}
		});
		absRelChanger=new EnumChanger(this, "Abs/rel", AbsRel.class,pFont);
		absRelChanger.setListener(new ValueChangerListener() {
			public void valueChanged(ValueChanger valueChanger) {
				colourScale=null;
				colourScale2=null;
			}
		});
		projectionChanger=new EnumChanger(this, "Projections", Projection.class,pFont);
		projectionChanger.setListener(new ValueChangerListener() {
			public void valueChanged(ValueChanger valueChanger) {
				colourScale=null;
				colourScale2=null;
			}
		});
	}
	
	private void loadData() {
		
		
		//load os tile lookups
		
		try {
			tile2coordprefix=new HashMap<String, String>();
			BufferedReader br = new BufferedReader(new FileReader("data/tile_osgb.txt"));
			while (br.ready()) {
				String line=br.readLine();
				if (!line.startsWith("#")) {
					String tileId=line;
					String coordPrefix=br.readLine();
					tile2coordprefix.put(tileId, coordPrefix);
				}
			}
			br.close();
		}	
		catch (IOException e){
			e.printStackTrace();
		}
		
		long t=System.currentTimeMillis();
		gridRecords=new ArrayList<Record>();

		Path2D[] shapes=ShpUtils.getShapes("data/scotland_laulevel1_2011/scotland_laulevel1_2011.shp");
		String[] shapeNames=ShpUtils.getAttribsAsStrings("data/scotland_laulevel1_2011/scotland_laulevel1_2011.shp","name");
		boundaries=new HashMap<String, Path2D>();
		for (int i=0;i<shapes.length;i++) {
			boundaries.put(shapeNames[i],shapes[i]);
		}
		
		
		HashMap<String, Record> gridKey2Record=new HashMap<>();
		//get demographics
		try{
			if (DATAFILE_DEMOGRAPHICS_PATH!=null) {
			print("Loading demographics...");
			NetcdfFile netcdfFile = NetcdfFiles.open(DATAFILE_DEMOGRAPHICS_PATH);
//			for (Variable variable:netcdfFile.getVariables())
//				println(variable.getNameAndDimensions());

			char[][] locations1=(char[][])netcdfFile.findVariable("/grid_area/age/persons/Dimension_1_names").read().copyToNDJavaArray();
			//convert to string
			String[] locations=new String[locations1.length];
//			println("start locations");
			for (int i=0;i<locations1.length;i++) {
				locations[i]=new String(locations1[i]);
//				println(locations[i]);
			}
//			println("end locations");
			char[][] demographicsAttribNames1=(char[][])netcdfFile.findVariable("grid_area/age/persons/Dimension_2_names").read().copyToNDJavaArray();
			//convert to string
			demographicsAttribNames=new String[demographicsAttribNames1.length];
			for (int i=0;i<demographicsAttribNames1.length;i++)
				demographicsAttribNames[i]=new String(demographicsAttribNames1[i]);
			double[][] values=(double[][])netcdfFile.findVariable("/grid_area/age/persons/array").read().copyToNDJavaArray();


			for (int i=0;i<locations.length;i++) {

				//check if it's empty
				boolean isEmpty=true;
				for (int j=0;j<demographicsAttribNames.length;j++)
					if (values[j][i]>0) {
						isEmpty=false;
						break;
					}
				if (!isEmpty) {
					//get location
					String gridRef=locations[i];
					String easting=gridRef.substring(2,4);
					String northing=gridRef.substring(4,6);
					String prefix=tile2coordprefix.get(gridRef.toUpperCase().substring(0,2));
					easting=prefix.substring(0,1)+easting+"000";
					northing=prefix.substring(1)+northing+"000";
					int x=Integer.parseInt(easting)/AGGREGATE_INPUT_M*AGGREGATE_INPUT_M+AGGREGATE_INPUT_M/2;
					int y=Integer.parseInt(northing)/AGGREGATE_INPUT_M*AGGREGATE_INPUT_M+AGGREGATE_INPUT_M/2;
					String gridKey=x+"-"+y;
					Record record=gridKey2Record.get(gridKey);
					if (record==null) {
						record=new Record();
						gridKey2Record.put(gridKey, record);
						gridRecords.add(record);
						record.x=x;
						record.y=y;
						if (geoBounds==null)
							geoBounds=new Rectangle2D.Float(record.x,record.y,0,0);
						else
							geoBounds.add(record.x,record.y);
					}
					if (record.popCounts==null)
						record.popCounts=new int[demographicsAttribNames.length];
					for (int j=0;j<demographicsAttribNames.length;j++) 
						record.popCounts[j]+=(short)values[j][i];
					
				}
			}
			println("done.");
			netcdfFile.close();
			}
			
		}
		catch (IOException e) {
			println(e);
		}			

		//get results
		try {
			if (!DATAFILE_RESULTS_PATHS.isEmpty()) {
				int chuckLocationSize=100000;//number of locations to import at once (depends on memory)
				for (int resultsDataIdx=0;resultsDataIdx<DATAFILE_RESULTS_PATHS.size();resultsDataIdx++) {
					NetcdfFile netcdfFile=null;
					int numLocations=-1;
					String[] locations=null;
					print("Loading model results...");
					netcdfFile = NetcdfFiles.open(DATAFILE_RESULTS_PATHS.get(resultsDataIdx));

					locations=(String[])netcdfFile.findVariable("/abundances/grid_id").read().copyToNDJavaArray();

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
					numLocations=locations.length;

					int[] size=new int[]{numDays,chuckLocationSize,numStatuses*numDemographics};
					for (int chunkOrigin=0;chunkOrigin<locations.length;chunkOrigin+=chuckLocationSize) { 

						int[] origin={0,chunkOrigin,0};

						int[][][] modelData=null;
						if (chunkOrigin+size[1]>numLocations)
							size[1]=numLocations-chunkOrigin;
						print(chunkOrigin+size[1]+"...");
						try {
							modelData = (int[][][])netcdfFile.findVariable("abundances/abuns").read(origin,size).copyToNDJavaArray();
						} catch (InvalidRangeException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						for (int locationIdx=chunkOrigin;locationIdx<chunkOrigin+size[1];locationIdx++) {
							//get location
							String gridRef=locations[locationIdx].replaceAll(" ", "");
							String easting=gridRef.substring(2,4);
							String northing=gridRef.substring(4,6);
							String prefix=tile2coordprefix.get(gridRef.toUpperCase().substring(0,2));
							easting=prefix.substring(0,1)+easting+"000";
							northing=prefix.substring(1)+northing+"000";
							int x=Integer.parseInt(easting)/AGGREGATE_INPUT_M*AGGREGATE_INPUT_M+AGGREGATE_INPUT_M/2;
							int y=Integer.parseInt(northing)/AGGREGATE_INPUT_M*AGGREGATE_INPUT_M+AGGREGATE_INPUT_M/2;
							String gridKey=x+"-"+y;
							Record record=gridKey2Record.get(gridKey);
							if (record==null) {
								record=new Record();
								//								println("Made new");
								gridKey2Record.put(gridKey, record);
								gridRecords.add(record);
								record.x=x;
								record.y=y;
								if (geoBounds==null)
									geoBounds=new Rectangle2D.Float(record.x,record.y,0,0);
								else
									geoBounds.add(record.x,record.y);
							}
							if (record.resultCounts==null)
								record.resultCounts=new int[DATAFILE_RESULTS_PATHS.size()][numDays][numDemographics][numStatuses];

							for (int dayIdx=0;dayIdx<numDays;dayIdx++) 
								for (int statusIdx=0;statusIdx<numStatuses;statusIdx++) 
									for (int demographicIdx=0;demographicIdx<numDemographics;demographicIdx++)
										record.resultCounts[resultsDataIdx][dayIdx][demographicIdx][statusIdx]+=(short)modelData[dayIdx][locationIdx-chunkOrigin][statusIdx*numDemographics+demographicIdx];
						}
					}
					println("done.");
				}
			}
		}
		catch (IOException e) {
			println(e);
		}
		

		//remove locations where everything is null.
		ArrayList<Record> records2=new ArrayList<DemographicGridmap.Record>();
		for (Record record:gridRecords) {
			boolean delete=true;
			if (record.popCounts!=null)
				for (int pop:record.popCounts) 
					if (pop>0) {
						delete=false;
						break;
					}
			if (record.resultCounts!=null) {
				for (int[][][] a1:record.resultCounts)
					for (int[][] a2:a1)
						for (int[] a3:a2)
							for (int v:a3)
								if (v>0) {
									delete=false;
									break;
								}
			}
			if (!delete)
				records2.add(record);
		}
		gridRecords=records2;
		records2=null;
		
		//AGGREGATE RECORDS BY AREA
		{
			System.out.print("Aggregating by area...");
			areaRecords=new ArrayList<DemographicGridmap.Record>();
			HashMap<String, Record> areaKey2Record=new HashMap<>();
			for (Record gridRecord:gridRecords) {
				String areaKey=null;
				for (Entry<String,Path2D> entry: boundaries.entrySet()) {
					if (entry.getValue().contains(gridRecord.x,gridRecord.y)) {
						areaKey=entry.getKey();
						break;
					}
				}
				if (areaKey!=null) {
					Record areaRecord=areaKey2Record.get(areaKey);
					if (areaRecord==null) {
						areaRecord=new Record();
						areaRecord.x=(int)boundaries.get(areaKey).getBounds().getCenterX();
						areaRecord.y=(int)boundaries.get(areaKey).getBounds().getCenterY();
						if (gridRecord.popCounts!=null) 
							areaRecord.popCounts=new int[gridRecord.popCounts.length];
						if (gridRecord.resultCounts!=null) 
							areaRecord.resultCounts=new int[gridRecord.resultCounts.length][gridRecord.resultCounts[0].length][gridRecord.resultCounts[0][0].length][gridRecord.resultCounts[0][0][0].length];
						areaKey2Record.put(areaKey, areaRecord);
						areaRecords.add(areaRecord);
					}
					if (gridRecord.popCounts!=null)
						for (int i=0;i<gridRecord.popCounts.length;i++) 
							areaRecord.popCounts[i]+=gridRecord.popCounts[i];
					if (gridRecord.resultCounts!=null)
						for (int i=0;i<gridRecord.resultCounts.length;i++) 
							for (int j=0;j<gridRecord.resultCounts[0].length;j++) 
								for (int k=0;k<gridRecord.resultCounts[0][0].length;k++) 
									for (int l=0;l<gridRecord.resultCounts[0][0][0].length;l++)
										areaRecord.resultCounts[i][j][k][l]+=gridRecord.resultCounts[i][j][k][l];
				}
			}
			println("done ("+areaRecords.size()+" areas).");
		}
		

		//load gridmap cells
		{
			String[] rows=loadStrings(new File("data/gridmap-layout.csv"));
			gridBounds=new Rectangle();
			gridCells=new Point[rows.length];
			for (int i=0;i<rows.length;i++) {
				int row=Integer.parseInt(rows[i].split(",")[0]);
				int col=Integer.parseInt(rows[i].split(",")[1]);
				if (col>gridBounds.width)
					gridBounds.width=col;
				if (row>gridBounds.height)
					gridBounds.height=row;
				gridCells[i]=new Point(col,row);
			}
			
		}
		
		System.gc();
	
		long t1=System.currentTimeMillis();
		println("Loaded data for "+gridRecords.size()+" locations (resampled to "+AGGREGATE_INPUT_M+"m) in "+(t1-t)/1000+" seconds.");
	}

	
				
				
	public void draw() {
		final int mouseX=this.mouseX;
		final int mouseY=this.mouseY;
		final int spatialBinSize=this.spatialBinSize;
		
		final String datasetName=datasetChanger.getValue();
		final String comparisonDatasetName=comparisonDatasetChanger.getValue();
		final Mode mode=modeChanger.getValueEnum();
		final AbsRel absRel=absRelChanger.getValueEnum();
		final Projection projection=projectionChanger.getValueEnum();
		background(200);
		
		
		if (mouseY<50)
			currentDay=(int)map(mouseX,0,width,0,numDays);
				
		ZoomPanState zoomPanState=zoomPan.getZoomPanState();
		
		int numCols=(int)(bounds.getWidth()/spatialBinSize+1);
		int numRows=(int)(bounds.getHeight()/spatialBinSize+1);
		
		
		String title="";

		String tooltip="";
		
		ArrayList<TileRendererPopPyramid> tileRenderers=new ArrayList<TileRendererPopPyramid>();

		int gridWH;
		if (mode==Mode.Population && demographicsAttribNames!=null) {
			title="Population of residents by age group (younger at top)";
			if (projectionChanger.getValueEnum()==Projection.Gridded) {
				//GRID THE DATA
				int[][][] demogSums=new int[numCols][numRows][demographicsAttribNames.length/attribBinSize];
				for (Record record:gridRecords) {
					float x=map(record.x,(float)geoBounds.getMinX(),(float)geoBounds.getMaxX(),bounds.x,bounds.x+bounds.width);
					float y=map(record.y,(float)geoBounds.getMaxY(),(float)geoBounds.getMinY(),bounds.y,bounds.y+bounds.height);
					PVector pt=zoomPanState.getCoordToDisp(x,y);
					int xBin=(int)(pt.x/spatialBinSize);
					int yBin=(int)(pt.y/spatialBinSize);
					if (record.popCounts!=null && xBin>=0 && xBin<demogSums.length && yBin>=0 && yBin<demogSums[0].length) {
						for (int j=0;j<demographicsAttribNames.length;j++) {
							if (j/attribBinSize<demogSums[xBin][yBin].length) {
								demogSums[xBin][yBin][j/attribBinSize]+=record.popCounts[j];
							}
						}
					}
				}

				//CREATE TILERENDER OBJECTS
				for (int x=0;x<numCols;x++)
					for (int y=0;y<numRows;y++)
						tileRenderers.add(new TileRendererPopPyramid(x*spatialBinSize+spatialBinSize/2,y*spatialBinSize+spatialBinSize/2,spatialBinSize,demogSums[x][y]));
			}
			else if (projectionChanger.getValueEnum()==Projection.Area || projectionChanger.getValueEnum()==Projection.GridMap) {
				gridWH=0;
				int i=0;
				for (Record record:areaRecords) {
					int x=0,y=0;
					if (projectionChanger.getValueEnum()==Projection.Area ) {
						gridWH=this.spatialBinSize;
						x=(int)map(record.x,(float)geoBounds.getMinX(),(float)geoBounds.getMaxX(),bounds.x,bounds.x+bounds.width);
						y=(int)map(record.y,(float)geoBounds.getMaxY(),(float)geoBounds.getMinY(),bounds.y,bounds.y+bounds.height);						
					}
					else if (projectionChanger.getValueEnum()==Projection.GridMap) {
						gridWH=(int)((bounds.getHeight()/gridBounds.height))-5;
						x=(int)map(gridCells[i].x,(float)gridBounds.getMinX(),(float)gridBounds.getMaxX(),bounds.x,bounds.x+bounds.width)+gridWH/2;
						y=(int)map(gridCells[i].y,(float)gridBounds.getMaxY(),(float)gridBounds.getMinY(),bounds.y,bounds.y+bounds.height)-gridWH/2;
						gridWH*=zoomPanState.getZoomScale();//correct for zoom
					}
					PVector pt=zoomPanState.getCoordToDisp(x,y);
					if (pt.x+gridWH/2>0 &&pt.x-gridWH/2<width && pt.y+gridWH/2>0 && pt.y-gridWH/2<height) {
						//aggregate the age bands accordingly
						int[] demogSums=new int[demographicsAttribNames.length/attribBinSize];
						for (int j=0;j<demographicsAttribNames.length;j++) {
							if (j/attribBinSize<demogSums.length) {
								demogSums[j/attribBinSize]+=record.popCounts[j];
							}
						}
						tileRenderers.add(new TileRendererPopPyramid((int)pt.x,(int)pt.y,gridWH,demogSums));
					}
					i++;
				}
			}
			
			//FIND MAX (IF NECESSARY) for glyph
			Float colourScale=this.colourScale;
			if (colourScale==null && absRel==AbsRel.Absolute) {
				//for RELATIVE, max pop is any one age band
				colourScale=-Float.MAX_VALUE;
				for (TileRendererPopPyramid tileRenderer:tileRenderers) 
						for (int j=0;j<tileRenderer.demogSums.length;j++) 
							colourScale=max(colourScale,(float)(tileRenderer.demogSums[j]));
				this.colourScale=colourScale;
			}
			//FIND MAX (IF NECESSARY) for transp/symbolsize
			Float colourScale2=this.colourScale2;
			if (colourScale2==null && absRel!=AbsRel.Absolute) {
				//max pop in any square
				colourScale2=-Float.MAX_VALUE;
				for (TileRendererPopPyramid tileRenderer:tileRenderers) { 
					int localSum=0;
					for (int j=0;j<demographicsAttribNames.length/attribBinSize;j++) {
						if (tileRenderer.demogSums[j]>localSum)
							localSum=tileRenderer.demogSums[j];
					}
					colourScale2=max(colourScale2,localSum);
					this.colourScale2=colourScale2;
				}
			}

			//SETUP RENDERER
			TileRendererPopPyramid.setup(this.g, mouseX, mouseY, attribBinSize, absRel, ctDemog, colourScale, colourScale2);

			//RENDER THEM
			for (TileRendererPopPyramid tileRenderer:tileRenderers) {
				String tileTooltip=tileRenderer.drawTile();
				if (tileTooltip!=null)
					tooltip=tileTooltip;
			}
			for (TileRendererPopPyramid tileRenderer:tileRenderers)
				tileRenderer.drawTileRelativeSymb();

			if (projection==Projection.Area || projection==Projection.GridMap) {
				for (TileRendererPopPyramid tileRenderer:tileRenderers)
					tileRenderer.drawOutlines();
			}
			
			if (projection==Projection.Gridded) {
				//draw grid lines
				stroke(0,30);
				for (int x=0;x<numCols;x++) 
					line(x*spatialBinSize,bounds.y,x*spatialBinSize,bounds.y+bounds.height);
				for (int y=0;y<numRows;y++)
					line(bounds.x,y*spatialBinSize,bounds.x+bounds.width,y*spatialBinSize);
			}

		}
		
		
		
		
		
		else if (mode==Mode.ModelAgeDay && !DATAFILE_RESULTS_PATHS.isEmpty() && !datasetNames.isEmpty()) {
			int currentDatasetIdx=datasetNames.indexOf(datasetName);
			title="Model results by age-band (younger at top) for day "+currentDay;
				title+=" for "+datasetName;
			//GRID THE DATA
			int[][][][] modelSums=new int[numCols][numRows][10][statuses.length];
			for (Record record:gridRecords) {

				float x=map(record.x,(float)geoBounds.getMinX(),(float)geoBounds.getMaxX(),bounds.x,bounds.x+bounds.width);
				float y=map(record.y,(float)geoBounds.getMaxY(),(float)geoBounds.getMinY(),bounds.y,bounds.y+bounds.height);
				PVector pt=zoomPanState.getCoordToDisp(x,y);
				int xBin=(int)(pt.x/spatialBinSize);
				int yBin=(int)(pt.y/spatialBinSize);
				if (xBin>=0 && xBin<numCols && yBin>=0 && yBin<numRows) {
					for (int j=0;j<10;j++) 
						for (int statusIdx=0;statusIdx<statuses.length;statusIdx++) {
//							if (statusShow[statusIdx])
								modelSums[xBin][yBin][j][statusIdx]+=record.resultCounts[currentDatasetIdx][currentDay][j][statusIdx];
						}
				}
			}
			//FIND MAX (IF NECESSARY) for glyph
			Float colourScale=this.colourScale;
			if (colourScale==null && absRel==AbsRel.Absolute) {
				colourScale=-Float.MAX_VALUE;
				for (int x=0;x<numCols;x++) {
					for (int y=0;y<numRows;y++) {
						for (int j=0;j<10;j++) {
							float sum=0;
							for (int statusIdx=0;statusIdx<statuses.length;statusIdx++)//use all statuses (to get whole population)
								sum+=modelSums[x][y][j][statusIdx];
							colourScale=max(colourScale,sum);
						}
					}
				}
				this.colourScale=colourScale;
			}
			//FIND MAX (IF NECESSARY) for transp/symbolsize
			Float colourScale2=this.colourScale2;
			if (colourScale2==null && absRel!=AbsRel.Absolute) {
				//max pop in any square
				colourScale2=-Float.MAX_VALUE;
				for (int x=0;x<numCols;x++) {
					for (int y=0;y<numRows;y++) {
						float sum=0;
						for (int j=0;j<10;j++)
							for (int statusIdx=0;statusIdx<statuses.length;statusIdx++)//use all statuses (to get whole population)
								sum+=modelSums[x][y][j][statusIdx];
						colourScale2=max(colourScale2,sum);
					}
				}
				this.colourScale2=colourScale2;
			}
			//DRAW
			for (int x=0;x<numCols;x++) {
				for (int y=0;y<numRows;y++) {
					int localSum=0;
					for (int j=0;j<modelSums[x][y].length;j++)
						for (int k=0;k<modelSums[x][y][j].length;k++) {
							localSum+=modelSums[x][y][j][k];
						}
					//make background white
					fill(255);
					noStroke();
					rect(x*spatialBinSize,y*spatialBinSize,spatialBinSize,spatialBinSize);
					float h=(float)spatialBinSize/10f;
					for (int j=0;j<10;j++) {
						float xPos=bounds.x+x*spatialBinSize+1;
						float yPos=bounds.y+y*spatialBinSize+1+j*h;
						int sum=0;
						for (int statusIdx=0;statusIdx<statuses.length;statusIdx++) 
							sum+=modelSums[x][y][j][statusIdx];

						for (int statusIdx=0;statusIdx<statuses.length;statusIdx++) {
							if (statusShow[statusIdx]) {
								float w;
								if (absRel==AbsRel.Absolute)
									w=constrain(map((float)modelSums[x][y][j][statusIdx],0,colourScale,0,spatialBinSize-2),0,spatialBinSize-2);
								else
									w=constrain(map((float)modelSums[x][y][j][statusIdx],0,sum,0,spatialBinSize-2),0,spatialBinSize-2);
								int transp=255;
								if (absRel==AbsRel.RelativeFade)
									transp=(int)constrain(map(localSum,0,colourScale2,0,255),0,255);
								fill(ctStatus.findColour(statusIdx+1),transp);
								rect(xPos,yPos,w,h);
								if (mouseX>xPos && mouseX<=xPos+w && mouseY>yPos && mouseY<yPos+h)
									tooltip=modelSums[x][y][j][statusIdx]+" ("+(int)(modelSums[x][y][j][statusIdx]/(float)sum*100)+"%) people aged "+(j*10)+"-"+((j+1)*10-1)+" are "+statuses[statusIdx];
								xPos+=w;
							}
						}
					}
				}
			}
			if (absRel==AbsRel.RelativeSymb) {
				for (int x=0;x<numCols;x++) {
					for (int y=0;y<numRows;y++) {
						int localSum=0;
						for (int j=0;j<modelSums[x][y].length;j++)
							for (int k=0;k<modelSums[x][y][j].length;k++)
								localSum+=modelSums[x][y][j][k];
						noFill();
						stroke(0,100);
						float w=map(localSum,0,colourScale2,0,spatialBinSize);
						ellipse(x*spatialBinSize+spatialBinSize/2,y*spatialBinSize+spatialBinSize/2,w,w);
					}
				}
			}

			if (projection==Projection.Gridded) {
				//draw grid lines
				stroke(0,30);
				for (int x=0;x<numCols;x++) 
					line(x*spatialBinSize,bounds.y,x*spatialBinSize,bounds.y+bounds.height);
				for (int y=0;y<numRows;y++)
					line(bounds.x,y*spatialBinSize,bounds.x+bounds.width,y*spatialBinSize);
			}

		}

		
		
		
		else if (mode==Mode.ModelTime  && !DATAFILE_RESULTS_PATHS.isEmpty() && !datasetNames.isEmpty()) {
			int currentDatasetIdx=datasetNames.indexOf(datasetName);
			title="Model results over time";
				title+=" for "+datasetName;
			//GRID THE DATA
			int[][][][] modelSums=new int[numCols][numRows][numDays][statuses.length];
			for (Record record:gridRecords) {

				float x=map(record.x,(float)geoBounds.getMinX(),(float)geoBounds.getMaxX(),bounds.x,bounds.x+bounds.width);
				float y=map(record.y,(float)geoBounds.getMaxY(),(float)geoBounds.getMinY(),bounds.y,bounds.y+bounds.height);
				PVector pt=zoomPanState.getCoordToDisp(x,y);
				int xBin=(int)(pt.x/spatialBinSize);
				int yBin=(int)(pt.y/spatialBinSize);
				if (xBin>=0 && xBin<numCols && yBin>=0 && yBin<numRows)
					for (int statusIdx=0;statusIdx<statuses.length;statusIdx++)
//						if (statusShow[statusIdx])
							for (int t=0;t<numDays;t++) 
								for (int j=0;j<10;j++) 
									modelSums[xBin][yBin][t][statusIdx]+=record.resultCounts[currentDatasetIdx][t][j][statusIdx];

				
			}
			//FIND MAX (IF NECESSARY)
			Float colourScale=this.colourScale;
			if (colourScale==null && absRel==AbsRel.Absolute) {
					colourScale=-Float.MAX_VALUE;
					for (int x=0;x<numCols;x++) {
						for (int y=0;y<numRows;y++) {
							for (int t=0;t<numDays;t++) { 
								float sum=0;
								for (int statusIdx=0;statusIdx<statuses.length;statusIdx++)//use all statuses (to get whole population)
									sum+=modelSums[x][y][t][statusIdx];
								colourScale=max(colourScale,sum);
							}
						}
					}
					this.colourScale=colourScale;
			}
			//FIND MAX (IF NECESSARY) for transp/symbolsize
			Float colourScale2=this.colourScale2;
			if (colourScale2==null && absRel!=AbsRel.Absolute) {
				//max pop in any square (based on visible status) but summed across all timesteps
				colourScale2=-Float.MAX_VALUE;
				for (int x=0;x<numCols;x++) {
					for (int y=0;y<numRows;y++) {
						float sum=0;
						for (int statusIdx=0;statusIdx<statuses.length;statusIdx++)//use all statuses (to get whole population)
							for (int t=0;t<numDays;t++) 
								sum+=modelSums[x][y][t][statusIdx];
						colourScale2=max(colourScale2,sum);
					}
				}
				this.colourScale2=colourScale2;
			}
			//DRAW
			for (int x=0;x<numCols;x++) {
				for (int y=0;y<numRows;y++) {
					//make background white
					fill(255);
					noStroke();
					rect(x*spatialBinSize,y*spatialBinSize,spatialBinSize,spatialBinSize);
					float w=(float)(spatialBinSize-2)/numDays;
					//calc localsum across t
					int localSumAcrossT=0;
					for (int t=0;t<numDays;t++)
						for (int statusIdx=0;statusIdx<statuses.length;statusIdx++)
							if (statusShow[statusIdx]) 
								localSumAcrossT+=modelSums[x][y][t][statusIdx];
					for (int t=0;t<numDays;t++) {
						float xPos=bounds.x+x*spatialBinSize+1+t*w;
						float yPos=bounds.y+y*spatialBinSize+spatialBinSize;
						//calc localsum across for the t
						int localSum=0;
						int pop=0;
						for (int statusIdx=0;statusIdx<statuses.length;statusIdx++) {
							pop+=modelSums[x][y][t][statusIdx];
							if (statusShow[statusIdx]) 
								localSum+=modelSums[x][y][t][statusIdx];

						}
						for (int statusIdx=0;statusIdx<statuses.length;statusIdx++) {
							if (statusShow[statusIdx]) {
								float h=0;
								if (absRel==AbsRel.Absolute)
									h=constrain(map((float)modelSums[x][y][t][statusIdx],0,colourScale,0,spatialBinSize-2),0,spatialBinSize-2);
								else
									h=constrain(map((float)modelSums[x][y][t][statusIdx],0,localSum,0,spatialBinSize-2),0,spatialBinSize-2);
								int transp=255;
								if (absRel==AbsRel.RelativeFade)
									transp=(int)constrain(map(localSumAcrossT,0,colourScale2,0,255),0,255);
								fill(ctStatus.findColour(statusIdx+1),transp);
								rect(xPos,yPos,w,-h);
								if (mouseX>xPos && mouseX<=xPos+w && mouseY>yPos-h && mouseY<=yPos)
									tooltip=modelSums[x][y][t][statusIdx]+" ("+(int)(modelSums[x][y][t][statusIdx]/(float)pop*100)+"%) people on day "+t+" are "+statuses[statusIdx];
								yPos-=h;
							}
						}
					}
				}
			}
			if (absRel==AbsRel.RelativeSymb) {
				for (int x=0;x<numCols;x++) {
					for (int y=0;y<numRows;y++) {
						int localSumAcrossT=0;
						for (int t=0;t<numDays;t++)
							for (int statusIdx=0;statusIdx<statuses.length;statusIdx++)//use all statuses (to get whole population)
							localSumAcrossT+=modelSums[x][y][t][statusIdx];
						noFill();
						stroke(0,100);
						float w=map(localSumAcrossT,0,colourScale2,0,spatialBinSize);
						ellipse(x*spatialBinSize+spatialBinSize/2,y*spatialBinSize+spatialBinSize/2,w,w);
					}
				}
			}

			//draw grid lines
			stroke(0,30);
			for (int x=0;x<numCols;x++) 
				line(x*spatialBinSize,bounds.y,x*spatialBinSize,bounds.y+bounds.height);
			for (int y=0;y<numRows;y++)
				line(bounds.x,y*spatialBinSize,bounds.x+bounds.width,y*spatialBinSize);

		}
		
		
		
		else if (mode==Mode.ModelComparisonTime && !DATAFILE_RESULTS_PATHS.isEmpty() && !datasetName.equals(comparisonDatasetName)) {
			title="Comparison of two model results over time";
				title+=" for "+datasetName;
			//GRID THE DATA
			int currentDatasetIdx=datasetNames.indexOf(datasetName);
			int currentBaselineDatasetIdx=datasetNames.indexOf(comparisonDatasetName);
			int[][] popSums=new int[numCols][numRows];
			int[][][][] modelDifferences=new int[numCols][numRows][numDays][statuses.length];
			for (Record record:gridRecords) {

				float x=map(record.x,(float)geoBounds.getMinX(),(float)geoBounds.getMaxX(),bounds.x,bounds.x+bounds.width);
				float y=map(record.y,(float)geoBounds.getMaxY(),(float)geoBounds.getMinY(),bounds.y,bounds.y+bounds.height);
				PVector pt=zoomPanState.getCoordToDisp(x,y);
				int xBin=(int)(pt.x/spatialBinSize);
				int yBin=(int)(pt.y/spatialBinSize);
				if (xBin>=0 && xBin<numCols && yBin>=0 && yBin<numRows)
					for (int statusIdx=0;statusIdx<statuses.length;statusIdx++) 
//						if (statusShow[statusIdx])
							for (int t=0;t<numDays;t++) 
								for (int j=0;j<10;j++) { 
									modelDifferences[xBin][yBin][t][statusIdx]+=record.resultCounts[currentDatasetIdx][t][j][statusIdx]-record.resultCounts[currentBaselineDatasetIdx][t][j][statusIdx];
									if (t==0)//just the first stepstep so we get the population of one
										popSums[xBin][yBin]+=record.resultCounts[currentDatasetIdx][t][j][statusIdx];
								}

				
			}
			
			//FIND MAX (IF NECESSARY)
			Float colourScale=this.colourScale;
			if (colourScale==null) {
				if (absRel==AbsRel.Absolute) {
					//use the max abs difference
					colourScale=-Float.MAX_VALUE;
					for (int x=0;x<numCols;x++) {
						for (int y=0;y<numRows;y++) {
							for (int t=0;t<numDays;t++) { 
								float sum=0;
								for (int statusIdx=0;statusIdx<statuses.length;statusIdx++)//use all statuses (to get whole population)
									sum+=abs(modelDifferences[x][y][t][statusIdx]);//use abs
								colourScale=max(colourScale,sum);
							}
						}
					}
					this.colourScale=colourScale;
				}
				else {
					//for RELATIVE, use the max difference/pop
					colourScale=-Float.MAX_VALUE;
					for (int x=0;x<numCols;x++) {
						for (int y=0;y<numRows;y++) {
							for (int t=0;t<numDays;t++) { 
								float sum=0;
								for (int statusIdx=0;statusIdx<statuses.length;statusIdx++)//use all statuses (to get whole population)
									if (popSums[x][y]>0)
										sum+=abs((float)modelDifferences[x][y][t][statusIdx]/popSums[x][y]);//use abs and divide by pop
								colourScale=max(colourScale,sum);
							}
						}
					}
					this.colourScale=colourScale;
				}
			}
			//FIND MAX (IF NECESSARY) for transp/symbolsize
			Float colourScale2=this.colourScale2;
			if (colourScale2==null && absRel!=AbsRel.Absolute) {
				this.colourScale2=colourScale2;
				colourScale2=-Float.MAX_VALUE;
				for (int x=0;x<numCols;x++)
					for (int y=0;y<numRows;y++)
						colourScale2=max(colourScale2,popSums[x][y]);
				this.colourScale2=colourScale2;
			}
			//DRAW
			for (int x=0;x<numCols;x++) {
				for (int y=0;y<numRows;y++) {
					//make background white
					fill(255);
					noStroke();
					rect(x*spatialBinSize,y*spatialBinSize,spatialBinSize,spatialBinSize);
					float w=(float)(spatialBinSize-2)/numDays;
					for (int t=0;t<numDays;t++) {
						float xPos=bounds.x+x*spatialBinSize+1+t*w;
						float yPosPositive=bounds.y+y*spatialBinSize+spatialBinSize/2;
						float yPosNegative=bounds.y+y*spatialBinSize+spatialBinSize/2;
						//calc localsum across for the t
						for (int statusIdx=0;statusIdx<statuses.length;statusIdx++) {
							if (statusShow[statusIdx]) {
								float h=0;
								if (absRel==AbsRel.Absolute)
									h=map((float)abs(modelDifferences[x][y][t][statusIdx]),0,colourScale,0,spatialBinSize-2);
								else
									if (popSums[x][y]>0)
										h=map(abs((float)modelDifferences[x][y][t][statusIdx]/popSums[x][y]),0,colourScale,0,spatialBinSize-2);
								if (h>1) {
									int transp=255;
									if (absRel==AbsRel.RelativeFade)
										transp=(int)constrain(map(popSums[x][y],0,colourScale2,0,255),0,255);
									fill(ctStatus.findColour(statusIdx+1),transp);
									if (modelDifferences[x][y][t][statusIdx]>0) {
										rect(xPos,yPosPositive,w,-h);
										if (mouseX>xPos && mouseX<=xPos+w && mouseY>yPosPositive-h && mouseY<yPosPositive)
											tooltip=abs(modelDifferences[x][y][t][statusIdx])+" ("+(int)abs((modelDifferences[x][y][t][statusIdx]/(float)popSums[x][y]*100))+"%) MORE people on day "+t+" are "+statuses[statusIdx]+" in "+datasetName+" than in "+comparisonDatasetName;
										yPosPositive-=h;
									}
									else {
										rect(xPos,yPosNegative,w,h);
										if (mouseX>xPos && mouseX<=xPos+w && mouseY>yPosNegative && mouseY<yPosNegative+h)
											tooltip=abs(modelDifferences[x][y][t][statusIdx])+" ("+(int)abs((modelDifferences[x][y][t][statusIdx]/(float)popSums[x][y]*100))+"%) FEWER people on day "+t+" are "+statuses[statusIdx]+" in "+datasetName+" than in "+comparisonDatasetName;
										yPosNegative+=h;
									}
								}
							}
						}
					}
				}
			}
			if (absRel==AbsRel.RelativeSymb) {
				for (int x=0;x<numCols;x++) {
					for (int y=0;y<numRows;y++) {
						noFill();
						stroke(0,100);
						float w=map(popSums[x][y],0,colourScale2,0,spatialBinSize);
						ellipse(x*spatialBinSize+spatialBinSize/2,y*spatialBinSize+spatialBinSize/2,w,w);
					}
				}
			}

			//draw grid lines
			stroke(0,30);
			for (int x=0;x<numCols;x++) 
				line(x*spatialBinSize,bounds.y,x*spatialBinSize,bounds.y+bounds.height);
			for (int y=0;y<numRows;y++)
				line(bounds.x,y*spatialBinSize,bounds.x+bounds.width,y*spatialBinSize);

		}
		
		
		if (helpScreen.getIsActive())
			helpScreen.draw();

		if (animateThroughTime)
			currentDay++;
		if (currentDay>=numDays)
			currentDay=0;

		//draw legend
//		if (mode==Mode.ModelBars|| mode==Mode.ModelBarsComparison || mode==Mode.ModelSpine || mode==Mode.ModelSpineComparison || mode==Mode.ModelGraph || mode==Mode.ModelSpineQuintiles|| mode==Mode.ModelSpineTime || mode==Mode.ModelBarsComparisonByStatus || mode==Mode.ModelSpineBoth) {
		if (!DATAFILE_RESULTS_PATHS.isEmpty() && (mode==Mode.ModelAgeDay||mode==Mode.ModelTime||mode==Mode.ModelComparisonTime)) {
			int y=height-statusBarH-(statuses.length*12);
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
//				if (mode==Mode.ModelBarsComparison||mode==Mode.ModelSpineComparison) {
//					if (statusIdx==selectedStatusIdx) {
//						fill(ctStatus.findColour(statusIdx+1));
//						rect(width-10,y,10,10);
//					}
//				}
//				else {
					if (statusShow[statusIdx]) {
						fill(ctStatus.findColour(statusIdx+1));
						rect(width-10,y,10,10);
					}
//				}
				if (mouseX>width-legendW && mouseY>y && mouseY<y+12 && mouseClicked) {
//					if (mode==Mode.ModelBarsComparison||mode==Mode.ModelSpineComparison)
//						selectedStatusIdx=statusIdx;
//					else
						statusShow[statusIdx]=!statusShow[statusIdx];
				}
				fill(80);
				text(statuses[statusIdx],width-12,y);
				y+=12;
			}
		}

		
		//draw boundaries
		if (projection!=Projection.GridMap) {
			PVector ptMouse=zoomPanState.getDispToCoord(new PVector(mouseX,mouseY));
			float geoMouseX=map(ptMouse.x,bounds.x,bounds.x+bounds.width,(float)geoBounds.getMinX(),(float)geoBounds.getMaxX());
			float geoMouseY=map(ptMouse.y,bounds.y,bounds.y+bounds.height,(float)geoBounds.getMaxY(),(float)geoBounds.getMinY());

			stroke(150);
			float[] coords=new float[6];
			noFill();
			for (Entry<String, Path2D> entry:boundaries.entrySet()) {
				if (entry.getValue().contains(geoMouseX,geoMouseY)) {
					fill(0,80);
					textSize(20);
					textLeading(20);
					textAlign(LEFT,BOTTOM);
					text(entry.getKey(),0,height-statusBarH);
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
					float x=map(coords[0],(float)geoBounds.getMinX(),(float)geoBounds.getMaxX(),bounds.x,bounds.x+bounds.width);
					float y=map(coords[1],(float)geoBounds.getMaxY(),(float)geoBounds.getMinY(),bounds.y,bounds.y+bounds.height);
					PVector pt=zoomPanState.getCoordToDisp(x,y);
					vertex(pt.x,pt.y);
					pi.next();
				}
				endShape(CLOSE);
			}
		}
		
		
		fill(0,100);
		textSize(30);
		textLeading(30);
		textAlign(LEFT,TOP);

		title+=" ("+mode.toString()+")";
		text(title,0,0,width,height);
		mouseClicked=false;
		

		if (!tooltip.isEmpty()) {
			drawTooltip(tooltip,mouseX+10,mouseY+10,100);
		}
		
		//timeline
		if ((mode==Mode.ModelAgeDay || mode==Mode.ModelTime) && mouseY<50){
			noStroke();
			fill(255,200);
			rect(0,0,width,50);
			fill(0,100);
			textAlign(CENTER,CENTER);
			textSize(18);
			stroke(0,50);
			for (int t=1;t<numDays;t+=2) {
				float x=map(t,0,numDays,0,width);
				text(t+"",x,50/2);
				line(x,0,x,50);
			}
		}
		
		//drawchangers
		{
			noStroke();
			fill(255);
			rect(0,height,width,-statusBarH);
			fill(0);
			int x=0;
			modeChanger.draw(x, height-statusBarH+3);
			x+=modeChanger.getWidth()+10;
			absRelChanger.draw(x, height-statusBarH+3);
			x+=absRelChanger.getWidth()+10;
			if (mode!=Mode.Population) {
				datasetChanger.enable();
				datasetChanger.setRespondToMouse(true);
				datasetChanger.draw(x, height-statusBarH+3);
				if (mode==Mode.ModelComparisonTime) {
					comparisonDatasetChanger.enable();
					comparisonDatasetChanger.setRespondToMouse(true);
					comparisonDatasetChanger.draw(x, height-statusBarH+20);
				}
				else {
					comparisonDatasetChanger.disable();
					comparisonDatasetChanger.setRespondToMouse(false);
				}
				x+=datasetChanger.getWidth()+10;
			}
			else {
				datasetChanger.disable();
				datasetChanger.setRespondToMouse(false);
			}
			projectionChanger.draw(x, height-statusBarH+3);
			x+=projectionChanger.getWidth()+10;
		}
	}
	

	
	private void drawTooltip(String label,int boxX, int boxY, int w) {
		pushStyle();
		textSize(10);
		textLeading(11);
		List<String> labelLines=WordWrapper.wordWrap(label, w, this);
		int boxW=4+(int)w+4;
		int boxH=4+(int)g.textLeading*labelLines.size()+4;

		if (boxX+boxW>width)
			boxX=width-boxW;
		if (boxY+boxH>height)
			boxY=height-boxH;
		
		stroke(200);
		fill(255,241,167,200);
		rect(boxX,boxY,boxW,boxH);

		textAlign(LEFT,TOP);
		fill(0);
		int y=boxY+4;
		for (String labelLine:labelLines) {
			text(labelLine,boxX+4,y);
			y+=g.textLeading;
		}
		popStyle();
	}
	
	public void keyPressed() {
		if (key==CODED && keyCode==UP) {
			spatialBinSize--;
			if (spatialBinSize<2)
				spatialBinSize=2;
		}
		if (key==CODED && keyCode==DOWN) {
			spatialBinSize++;
		}
		if (key=='[') {
			attribBinSize--;
			if (attribBinSize<1)
				attribBinSize=1;
		}
		if (key==']') {
			attribBinSize++;
			if (attribBinSize>demographicsAttribNames.length)
				attribBinSize=demographicsAttribNames.length-1;
		}
		if (key==CODED && keyCode==LEFT && colourScale!=null && keyEvent!=null && !keyEvent.isShiftDown()) {
			colourScale+=colourScale/10f;
		}
		if (key==CODED && keyCode==RIGHT && colourScale!=null && keyEvent!=null && !keyEvent.isShiftDown()) {
			colourScale-=colourScale/10f;
		}
		if (key==CODED && keyCode==LEFT && colourScale2!=null && keyEvent!=null && keyEvent.isShiftDown()) {
			colourScale2+=colourScale2/10f;
		}
		if (key==CODED && keyCode==RIGHT && colourScale2!=null && keyEvent!=null && keyEvent.isShiftDown()) {
			colourScale2-=colourScale2/10f;
		}
		if (key=='s') {
			colourScale=null;
			colourScale2=null;
		}
		if (key=='m') {
			modeChanger.changeToNextValue();;
		}
		if (key=='M') {
			modeChanger.changeToPrevValue();
		}
		if (key=='a') {
			animateThroughTime=!animateThroughTime;
		}
		if (key=='r')
			zoomPan.reset();
		if (key=='c')
			absRelChanger.changeToNextValue();
		if (key=='C')
			absRelChanger.changeToPrevValue();
		if (key=='d')
			datasetChanger.changeToNextValue();
		if (key=='d')
			datasetChanger.changeToPrevValue();
		if (key=='h') {
			helpScreen.setIsActive(!helpScreen.getIsActive());
		}
	
	}
	
	public void mouseClicked() {
		mouseClicked=true;
	}
	

	class Record{
		int x,y;
		int[] popCounts; //by demographicgroup
		int[][][][] resultCounts; //first number is the model result set... followed by time, demographicGroup, infectionType
//		short[] resultForce; //by time
//		short[] resultReservoir; //by time
//		short[] baselineForce; //by time
//		short[] baselineReservoir; //by time
	}

}
