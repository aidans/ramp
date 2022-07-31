package org.gicentre.aidan.ramp;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

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


public class DemographicGridmap extends PApplet implements MouseWheelListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	static String APP_NAME="DemographicGridmap, v1.10 (12/10/21)";

	static String DATAFILE_DEMOGRAPHICS_PATH;
	static ArrayList<String> DATAFILE_RESULTS_PATHS;
	static String DATAFILE_FORCE_PATH;
	static String DATAFILE_POLLUTION;
	
	
	static int AGGREGATE_INPUT_M=1000;

	
	ArrayList<Record> gridRecords;
	ArrayList<AreaRecord> areaRecords;
	String[] demographicsAttribNames;
	String[] statuses;
	boolean[] statusShow;
	private int numDays;
	boolean showBoundaries=true;
	
	ColourTable ctDemog,ctResult,ctStatus,ctForce,ctReservoir;

	Rectangle bounds,gridBounds;
	Rectangle2D geoBounds;

	
	ZoomPan zoomPan;
	
	boolean animateThroughTime=true;
	
	int spatialBinSize=30; //in screen pixels
	int attribBinSize=15; //numbers of attributes
	
	Float colourScale=null;
	Float colourScale2=null;
	
	
	LinkedHashMap<String, Path2D> boundaries;
	
	enum Mode{
		Population,
		ModelAgeDayAnim,
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
	
	float projectionTweenStep=1;//when 0, old projection; when 1, new projection 
	
	boolean flagToRedraw=true;

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
				flagToRedraw=true;
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
				flagToRedraw=true;
			}
		});
		modeChanger=new EnumChanger(this, "Mode", Mode.class,pFont);
		modeChanger.setListener(new ValueChangerListener() {
			public void valueChanged(ValueChanger valueChanger) {
				colourScale=null;
				colourScale2=null;
				flagToRedraw=true;
			}
		});
		absRelChanger=new EnumChanger(this, "Abs/rel", AbsRel.class,pFont);
		absRelChanger.setListener(new ValueChangerListener() {
			public void valueChanged(ValueChanger valueChanger) {
				colourScale=null;
				colourScale2=null;
				flagToRedraw=true;
				redraw();
			}
		});
		projectionChanger=new EnumChanger(this, "Projections", Projection.class,pFont);
		projectionChanger.setListener(new ValueChangerListener() {
			public void valueChanged(ValueChanger valueChanger) {
				colourScale=null;
				colourScale2=null;
				if ((projectionChanger.getValueEnum()==Projection.Area && projectionChanger.getPreviousValueEnum()==Projection.GridMap)||(projectionChanger.getValueEnum()==Projection.GridMap && projectionChanger.getPreviousValueEnum()==Projection.Area))
					projectionTweenStep=0;//start the tweening
				flagToRedraw=true;
			}
		});
		this.addMouseWheelListener(this);
		
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
		boundaries=new LinkedHashMap<String, Path2D>();
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
						record.popCounts[j]+=values[j][i];
					
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
							//find out if there's any data
							boolean isEmpty=true;
							stop:
							for (int dayIdx=0;dayIdx<numDays;dayIdx++) 
								for (int statusIdx=0;statusIdx<numStatuses;statusIdx++) 
									for (int demographicIdx=0;demographicIdx<numDemographics;demographicIdx++)
										if (modelData[dayIdx][locationIdx-chunkOrigin][statusIdx*numDemographics+demographicIdx]>0) {
											isEmpty=false;
											break stop;//break out of entire loop
										}
							if (!isEmpty) {

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
		
		Point[] gridCells;//in same order as areas in shapefile 
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

		//AGGREGATE RECORDS BY AREA
		{
			System.out.print("Aggregating by area...");
		
			areaRecords=new ArrayList<DemographicGridmap.AreaRecord>();
			HashMap<String, AreaRecord> areaKey2Record=new HashMap<>();
			for (Record gridRecord:gridRecords) {
				String areaKey=null;
				for (Entry<String,Path2D> entry: boundaries.entrySet()) {
					if (entry.getValue().contains(gridRecord.x,gridRecord.y)) {
						areaKey=entry.getKey();
						break;
					}
				}
				if (areaKey!=null) {
					AreaRecord areaRecord=areaKey2Record.get(areaKey);
					if (areaRecord==null) {
						areaRecord=new AreaRecord();
						areaRecord.x=(int)boundaries.get(areaKey).getBounds().getCenterX();
						areaRecord.y=(int)boundaries.get(areaKey).getBounds().getCenterY();
						if (gridRecord.popCounts!=null) 
							areaRecord.popCounts=new int[gridRecord.popCounts.length];
						if (gridRecord.resultCounts!=null) 
							areaRecord.resultCounts=new int[gridRecord.resultCounts.length][gridRecord.resultCounts[0].length][gridRecord.resultCounts[0][0].length][gridRecord.resultCounts[0][0][0].length];
						areaKey2Record.put(areaKey, areaRecord);
						areaRecord.name=areaKey;
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
			int i=0;
			for (String name:boundaries.keySet()) {
				AreaRecord areaRecord=areaKey2Record.get(name);
				areaRecord.gridX=gridCells[i].x;
				areaRecord.gridY=gridCells[i].y;
				i++;
			}
			println("done ("+areaRecords.size()+" areas).");
		}
				
		System.gc();
	
		long t1=System.currentTimeMillis();
		println("Loaded data for "+gridRecords.size()+" locations (resampled to "+AGGREGATE_INPUT_M+"m) in "+(t1-t)/1000+" seconds.");
		long heapSize = Runtime.getRuntime().totalMemory(); 
		long heapMaxSize = Runtime.getRuntime().maxMemory();

		println(heapSize/1073741824+"/"+heapMaxSize/1073741824+"gb");
	}

	
	private Collection<RampTile> createTiles(){
		final Mode mode=modeChanger.getValueEnum();
		final Projection projection=projectionChanger.getValueEnum();
		final Projection prevProjection=projectionChanger.getPreviousValueEnum();
		final ZoomPanState zoomPanState=zoomPan.getZoomPanState();
		
		int numCols=(int)(bounds.getWidth()/spatialBinSize+1);
		int numRows=(int)(bounds.getHeight()/spatialBinSize+1);

		final String datasetName=datasetChanger.getValue();
		final String comparisonDatasetName=comparisonDatasetChanger.getValue();
		int currentDatasetIdx=datasetNames.indexOf(datasetName);
		int currentBaselineDatasetIdx=datasetNames.indexOf(comparisonDatasetName);

		
		Rectangle geoBoundsInView=null;
		{
			PVector pt1=zoomPan.getDispToCoord(new PVector(bounds.x,bounds.y));
			PVector pt2=zoomPan.getDispToCoord(new PVector(bounds.x+bounds.width,bounds.y+bounds.height));
			float geoXLeft=map(pt1.x,bounds.x,bounds.x+bounds.width,(float)geoBounds.getMinX(),(float)geoBounds.getMaxX());
			float geoYTop=map(pt2.y,bounds.y,bounds.y+bounds.height,(float)geoBounds.getMaxY(),(float)geoBounds.getMinY());
			float geoXRight=map(pt2.x,bounds.x,bounds.x+bounds.width,(float)geoBounds.getMinX(),(float)geoBounds.getMaxX());
			float geoYBottom=map(pt1.y,bounds.y,bounds.y+bounds.height,(float)geoBounds.getMaxY(),(float)geoBounds.getMinY());
			geoBoundsInView=new Rectangle((int)geoXLeft,(int)geoYTop,(int)geoXRight-(int)geoXLeft,(int)geoYBottom-(int)geoYTop);
		}

		HashSet<RampTile> tiles=new HashSet<RampTile>();
		
		if (projection==Projection.Gridded) {
			HashSet[][] griddedRecords=new HashSet[numCols][numRows];
			for (Record record:gridRecords) {
				if (geoBoundsInView.contains(record.x,record.y)) {
					float x=map(record.x,(float)geoBounds.getMinX(),(float)geoBounds.getMaxX(),bounds.x,bounds.x+bounds.width);
					float y=map(record.y,(float)geoBounds.getMaxY(),(float)geoBounds.getMinY(),bounds.y,bounds.y+bounds.height);
					PVector pt=zoomPanState.getCoordToDisp(x,y);
					int xBin=(int)(pt.x/spatialBinSize);
					int yBin=(int)(pt.y/spatialBinSize);
					if (xBin>=0 && xBin<numCols && yBin>=0 && yBin<numRows) {
						if (griddedRecords[xBin][yBin]==null)
							griddedRecords[xBin][yBin]=new HashSet<>();
						griddedRecords[xBin][yBin].add(record);
					}
				}
			}
			for (int col=0;col<numCols;col++) {
				for (int row=0;row<numRows;row++) {
					if (mode==Mode.Population && griddedRecords[col][row]!=null)
						tiles.add(new TilePopulation(bounds.x+col*spatialBinSize+spatialBinSize/2, bounds.y+row*spatialBinSize+spatialBinSize/2, spatialBinSize, griddedRecords[col][row],demographicsAttribNames.length,attribBinSize));
					else if (mode==Mode.ModelAgeDayAnim && griddedRecords[col][row]!=null)
						tiles.add(new TileModelAgeAnim(bounds.x+col*spatialBinSize+spatialBinSize/2, bounds.y+row*spatialBinSize+spatialBinSize/2, spatialBinSize, griddedRecords[col][row],statuses.length,currentDatasetIdx,currentDay));
					else if (mode==Mode.ModelTime && griddedRecords[col][row]!=null)
						tiles.add(new TileModelTime(bounds.x+col*spatialBinSize+spatialBinSize/2, bounds.y+row*spatialBinSize+spatialBinSize/2, spatialBinSize, griddedRecords[col][row],numDays,statuses.length,currentDatasetIdx));
					else if (mode==Mode.ModelComparisonTime && griddedRecords[col][row]!=null)
						tiles.add(new TileModelComparison(bounds.x+col*spatialBinSize+spatialBinSize/2, bounds.y+row*spatialBinSize+spatialBinSize/2, spatialBinSize, griddedRecords[col][row],numDays,statuses.length,currentDatasetIdx,currentBaselineDatasetIdx));
					
				}
			}
		}
		else if (projection==Projection.Area || projection==Projection.GridMap) {
			int i=0;
			for (AreaRecord record:areaRecords) {

				int gridWHForArea=this.spatialBinSize;
				int xForArea=(int)map(record.x,(float)geoBounds.getMinX(),(float)geoBounds.getMaxX(),bounds.x,bounds.x+bounds.width);
				int yForArea=(int)map(record.y,(float)geoBounds.getMaxY(),(float)geoBounds.getMinY(),bounds.y,bounds.y+bounds.height);						

				int gridWHForGridMap=(int)((bounds.getHeight()/gridBounds.height))-5;
				int xForGridMap=(int)map(record.gridX,(float)gridBounds.getMinX(),(float)gridBounds.getMaxX(),bounds.x,bounds.x+bounds.width)+gridWHForGridMap/2;
				int yForGridMap=(int)map(record.gridY,(float)gridBounds.getMaxY(),(float)gridBounds.getMinY(),bounds.y,bounds.y+bounds.height)-gridWHForGridMap/2;
				gridWHForGridMap*=zoomPanState.getZoomScale();//correct for zoom
									
				PVector pt=null;
				int gridWH=0;
				if (projectionTweenStep<1) {
					if (prevProjection==Projection.Area && projection==Projection.GridMap) {
						pt=zoomPanState.getCoordToDisp(lerp(xForArea,xForGridMap,projectionTweenStep),lerp(yForArea,yForGridMap,projectionTweenStep));
						gridWH=(int)lerp(gridWHForArea,gridWHForGridMap,projectionTweenStep);
					}
					else {
						pt=zoomPanState.getCoordToDisp(lerp(xForGridMap,xForArea,projectionTweenStep),lerp(yForGridMap,yForArea,projectionTweenStep));
						gridWH=(int)lerp(gridWHForGridMap,gridWHForArea,projectionTweenStep);
					}
				}
				else if (projection==Projection.Area) {
					pt=zoomPanState.getCoordToDisp(xForArea,yForArea);
					gridWH=gridWHForArea;
				}
				else if (projection==Projection.GridMap) {
					pt=zoomPanState.getCoordToDisp(xForGridMap,yForGridMap);
					gridWH=gridWHForGridMap;
				}
				ArrayList<Record> arrayList=new ArrayList<DemographicGridmap.Record>();
				arrayList.add(record);
				RampTile tile=null;
				if (mode==Mode.Population)
					tile=new TilePopulation((int)pt.x,(int)pt.y,gridWH,arrayList,demographicsAttribNames.length,attribBinSize);
				else if (mode==Mode.ModelAgeDayAnim)
					tile=new TileModelAgeAnim((int)pt.x,(int)pt.y,gridWH,arrayList,statuses.length,currentDatasetIdx,currentDay);
				else if (mode==Mode.ModelTime)
					tile=new TileModelTime((int)pt.x,(int)pt.y,gridWH,arrayList,numDays,statuses.length,currentDatasetIdx);
				else if (mode==Mode.ModelComparisonTime)
					tile=new TileModelComparison((int)pt.x,(int)pt.y,gridWH,arrayList,numDays,statuses.length,currentDatasetIdx,currentBaselineDatasetIdx);
				if (tile!=null) {
					tile.name=record.name;
					tiles.add(tile);
				}
			}
			i++;
		}
		return tiles;
	}
				
				
	public void draw() {
		
		Rectangle geoBoundsInView=null;
		{
			PVector pt1=zoomPan.getDispToCoord(new PVector(bounds.x,bounds.y));
			PVector pt2=zoomPan.getDispToCoord(new PVector(bounds.x+bounds.width,bounds.y+bounds.height));
			float geoXLeft=map(pt1.x,bounds.x,bounds.x+bounds.width,(float)geoBounds.getMinX(),(float)geoBounds.getMaxX());
			float geoYTop=map(pt1.y,bounds.y,bounds.y+bounds.height,(float)geoBounds.getMaxY(),(float)geoBounds.getMinY());
			float geoXRight=map(pt2.x,bounds.x,bounds.x+bounds.width,(float)geoBounds.getMinX(),(float)geoBounds.getMaxX());
			float geoYBottom=map(pt2.y,bounds.y,bounds.y+bounds.height,(float)geoBounds.getMaxY(),(float)geoBounds.getMinY());
			geoBoundsInView=new Rectangle((int)geoXLeft,(int)geoYTop,(int)geoXRight-(int)geoXLeft,(int)geoYBottom-(int)geoYTop);
		}
//		println("Geographical area in view is "+geoBoundsInView.width/1000+"x"+geoBoundsInView.height/1000+"km");
//		println("5pixel width "+(int)(geoBoundsInView.width/(float)bounds.width*5/1000f)+"km");
		
		
		if (!flagToRedraw) {
			return;
		}
		
		
		flagToRedraw=false;
		
		final int mouseX=this.mouseX;
		final int mouseY=this.mouseY;
		final int spatialBinSize=this.spatialBinSize;
		
		final Mode mode=modeChanger.getValueEnum();
		final AbsRel absRel=absRelChanger.getValueEnum();
		
		final Projection projection=projectionChanger.getValueEnum();
		background(200);
		
		
		if (mouseY<50)
			currentDay=(int)map(mouseX,0,width,0,numDays);

		if (mode==Mode.ModelAgeDayAnim || projectionTweenStep<1)
			flagToRedraw=true;
				
		ZoomPanState zoomPanState=zoomPan.getZoomPanState();
		
		int numCols=(int)(bounds.getWidth()/spatialBinSize+1);
		int numRows=(int)(bounds.getHeight()/spatialBinSize+1);
		
		
		String tooltip="";
				
		Collection<RampTile> tiles=createTiles();
		
		//FIND MAX (IF NECESSARY) for glyph
		Float colourScale=this.colourScale;
		if (colourScale==null) {
			//for RELATIVE, max pop is any one age band
			colourScale=-Float.MAX_VALUE;
			for (RampTile tile:tiles) 
				colourScale=max(colourScale,(float)(tile.getMaxForGlyph(absRel)));
			this.colourScale=colourScale;
		}
		//FIND MAX (IF NECESSARY) for transp/symbolsize
		Float colourScale2=this.colourScale2;
		if (colourScale2==null && absRel!=AbsRel.Absolute) {
			//max pop in any square
			colourScale2=-Float.MAX_VALUE;
			for (RampTile tile:tiles) { 
				colourScale2=max(colourScale2,tile.getMaxForTransp(absRel));
			}
			this.colourScale2=colourScale2;
		}

		
		for (RampTile tile:tiles) {
			boolean drawLabels=false;
			if (projection==Projection.GridMap)
				drawLabels=true;
			String tileTooltip=tile.drawTile(this,drawLabels);
			if (tileTooltip!=null)
				tooltip=tileTooltip;
		}

		if (absRel==AbsRel.RelativeSymb && this.colourScale2!=null)
			for (RampTile tile:tiles) 
				tile.drawTileRelativeSymb(this);


		if (projection==Projection.Area || projection==Projection.GridMap) {
			for (RampTile tile:tiles) {
				tile.drawOutlines(this);
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


		//draw vertical line for time on each square
		if ((mode==Mode.ModelComparisonTime || mode==Mode.ModelTime) && bounds.contains(mouseX,mouseY)) {
			stroke(0,0,200,50);
			int t=-1;
			for (RampTile tile:tiles)
				if (mouseX>tile.screenXCentre-tile.screenWH/2 && mouseX<tile.screenXCentre+tile.screenWH/2 && mouseY>tile.screenYCentre-tile.screenWH/2 && mouseY<tile.screenYCentre+tile.screenWH/2) {
					t=(int)map(mouseX,tile.screenXCentre-tile.screenWH/2,tile.screenXCentre+tile.screenWH/2,0,numDays);
					break;
				}
			
			if (t>-1)
				for (RampTile tile:tiles) {
					int x=(int)map(t, 0,numDays, tile.screenXCentre-tile.screenWH/2,tile.screenXCentre+tile.screenWH/2);
					line(x,tile.screenYCentre-tile.screenWH/2+4,x,tile.screenYCentre+tile.screenWH/2-4);
				}
		}



		
		if (helpScreen.getIsActive())
			helpScreen.draw();

		if (animateThroughTime)
			currentDay++;
		if (currentDay>=numDays)
			currentDay=0;

		//draw legend
//		if (mode==Mode.ModelBars|| mode==Mode.ModelBarsComparison || mode==Mode.ModelSpine || mode==Mode.ModelSpineComparison || mode==Mode.ModelGraph || mode==Mode.ModelSpineQuintiles|| mode==Mode.ModelSpineTime || mode==Mode.ModelBarsComparisonByStatus || mode==Mode.ModelSpineBoth) {
		if (!DATAFILE_RESULTS_PATHS.isEmpty() && (mode==Mode.ModelAgeDayAnim||mode==Mode.ModelTime||mode==Mode.ModelComparisonTime)) {
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
		if (showBoundaries && projection!=Projection.GridMap) {
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

		String title="";
		if (mode==Mode.Population)
			if (absRel==AbsRel.Absolute)
				title="Population counts by age group (younger at top)";
			else
				title="Relative population by age group (younger at top)";
		if (mode==Mode.ModelAgeDayAnim)
			if (absRel==AbsRel.Absolute)
				title="Infection status counts by age group (younger at top) for day "+currentDay+" for "+datasetChanger.getValue();
			else
				title="Infection status percentage by age group (younger at top) for day "+currentDay+" for "+datasetChanger.getValue();
		if (mode==Mode.ModelTime)
			if (absRel==AbsRel.Absolute)
				title="Infection status counts over time for "+datasetChanger.getValue();
			else
				title="Infection status percentage over time for "+datasetChanger.getValue();
		if (mode==Mode.ModelComparisonTime)
			if (absRel==AbsRel.Absolute)
				title="Absolute difference in the infection status of "+datasetChanger.getValue()+" compared to "+comparisonDatasetChanger.getValue();
			else
				title="Percentage difference in the infection status of "+datasetChanger.getValue()+" compared to "+comparisonDatasetChanger.getValue();
		text(title,0,0,width,height);
		mouseClicked=false;
		

		if (!tooltip.isEmpty()) {
			drawTooltip(tooltip,mouseX+10,mouseY+10,100);
		}
		
		//timeline
		if ((mode==Mode.ModelAgeDayAnim || mode==Mode.ModelTime) && mouseY<50){
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
		if (projectionTweenStep<=1) {
			projectionTweenStep+=0.05f;
		}
		
	}

	public void mouseMoved() {
		flagToRedraw=true;
	}

	public void mouseDragged() {
		flagToRedraw=true;
	}

	public void mousePressed() {
		flagToRedraw=true;
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
		if (key=='b')
			showBoundaries=!showBoundaries;
		flagToRedraw=true;
	}
	
	public void mouseClicked() {
		mouseClicked=true;
		flagToRedraw=true;
	}
	

	class Record{
		int x,y;
		int[] popCounts; //by demographicgroup
		int[][][][] resultCounts; //first number is the model result set... followed by time, demographicGroup, infectionType
	}

	class AreaRecord extends Record{
		String name;
		int gridX,gridY;
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent arg0) {
		flagToRedraw=true;
	}

}
