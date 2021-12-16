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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.gicentre.shputils.ShpUtils;
import org.gicentre.utils.colour.ColourTable;
import org.gicentre.utils.gui.BooleanChanger;
import org.gicentre.utils.gui.EnumChanger;
import org.gicentre.utils.gui.HelpScreen;
import org.gicentre.utils.gui.ValueChanger;
import org.gicentre.utils.gui.ValueChangerListener;
import org.gicentre.utils.move.ZoomPan;
import org.gicentre.utils.move.ZoomPanState;
import org.gicentre.utils.text.WordWrapper;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PVector;


public class EnergyGlyphMap extends PApplet implements MouseWheelListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	static String APP_NAME="DemographicGridmap, v1.10 (12/10/21)";

	static String DATAFILE_DEMOGRAPHICS_PATH;
	static ArrayList<String> DATAFILE_RESULTS_PATHS;
	static String DATAFILE_FORCE_PATH;
	static String DATAFILE_POLLUTION;


	static int AGGREGATE_INPUT_M=500;


	Collection<GridRecord> gridRecords;
	Collection<AreaRecord> areaRecords;

	ColourTable ct;

	Rectangle bounds,gridBounds;
	Rectangle2D geoBounds;


	ZoomPan zoomPan;

	boolean animateThroughTime=true;

	int spatialBinSize=30; //in screen pixels

	Float colourScale=null;
	Float colourScale2=null;


	enum Mode{
		Anim,
		AnimTimeSeries
	}

	boolean mouseClicked=false;

	int wrap=20;

	int currentYear=0;
	int numYears=0;
	int firstYear;

	private HelpScreen helpScreen;


	HashMap<String, String> tile2coordprefix=null;

	ArrayList<String> datasetNames;


	int statusBarH=18+18;
	enum AbsRel{Absolute,Relative, RelativeSymb,RelativeFade}
	EnumChanger<AbsRel> absRelChanger;
	EnumChanger<Mode> modeChanger;
	enum Projection{Gridded,Area,GridMap}
	EnumChanger<Projection> projectionChanger;
	BooleanChanger showBoundariesChanger;

	float projectionTweenStep=1;//when 0, old projection; when 1, new projection 

	boolean flagToRedraw=true;

	static public void main(String[] args) {
		DATAFILE_RESULTS_PATHS=new ArrayList<>();
		for (String arg:args) {
			String[] toks=arg.split("=");
			if (toks[0].equalsIgnoreCase("AGGREGATE_INPUT_M") && toks.length==2) {
				AGGREGATE_INPUT_M=Integer.parseInt(toks[1]);
			}
		}

		PApplet.main(new String[]{"org.gicentre.aidan.ramp.EnergyGlyphMap"});
	}

	public void setup() {

		//		LOAD_OUTPUTS=false;
		//		LOAD_BASELINE=false;

		println("Aidan Slingsby, a.slingsby@city.ac.uk, City, University of London");
		size(800,800);
		ct=ColourTable.getPresetColourTable(ColourTable.PURPLES);

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

//		{
//			double ratioX=(double)bounds.width/gridBounds.width;
//			double ratioY=(double)bounds.height/gridBounds.height;
//			if (ratioX<ratioY) {
//				float diff=(float)(gridBounds.getHeight()-bounds.height/ratioX);
//				gridBounds.setFrame(gridBounds.getX()-diff/2,gridBounds.getY(),bounds.width/ratioX,bounds.height/ratioX);
//			}
//			else {
//				float diff=(float)(bounds.width/ratioY-gridBounds.getWidth());
//				gridBounds.setFrame(gridBounds.getX(),gridBounds.getY()+diff/2,bounds.width/ratioY,bounds.height/ratioY);
//			}
//		}

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
		showBoundariesChanger=new BooleanChanger(this,"Show boundaries",pFont);
		showBoundariesChanger.setListener(new ValueChangerListener() {
			public void valueChanged(ValueChanger valueChanger) {
				flagToRedraw=true;
			}
		});
		this.addMouseWheelListener(this);

	}

	private void loadData() {

		long t=System.currentTimeMillis();

		//find number of timesteps
		int minYear=Integer.MAX_VALUE;
		int  maxYear=Integer.MIN_VALUE;
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy/MM/dd HH:mm");
		try{
			BufferedReader br=new BufferedReader(new FileReader("data/energyGlyphmaps/MSOA_demand_comparison_updated_format.csv"));
			br.readLine();//ignore first line
			while (br.ready()) {
				String[] toks=br.readLine().split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
				int year=sdf.parse(toks[2]).getYear()+1900;
				if (year<minYear)
					minYear=year;
				if (year>maxYear)
					maxYear=year;
			}
			br.close();
		}
		catch(Exception e) {
			System.err.println(e);
		}
		numYears=maxYear-minYear+1;
		System.out.println(numYears+" "+minYear);
		this.firstYear=minYear;
		

		//load boundaries and make arearecords
		Path2D[] shapes=ShpUtils.getShapes("data/energyGlyphmaps/MSOA_2011_EW_BFC/MSOA_2011_EW_BFC.shp");
		String[] shapeCodes=ShpUtils.getAttribsAsStrings("data/energyGlyphmaps/MSOA_2011_EW_BFC/MSOA_2011_EW_BFC.shp","MSOA11CD");
		String[] shapeNames=ShpUtils.getAttribsAsStrings("data/energyGlyphmaps/MSOA_2011_EW_BFC/MSOA_2011_EW_BFC.shp","MSOA11NM");
		geoBounds=ShpUtils.getBB(shapes);
		HashMap<String,AreaRecord> areaRecords=new HashMap<String, EnergyGlyphMap.AreaRecord>();
		for (int i=0;i<shapes.length;i++) {
			AreaRecord areaRecord=new AreaRecord();
			areaRecord.path2d=shapes[i];
			areaRecord.code=shapeCodes[i];
			areaRecord.name=shapeNames[i];
			areaRecord.timeseries=new float[numYears];
			areaRecord.exceeds=new boolean[numYears];
			areaRecords.put(areaRecord.name, areaRecord);
		}

		//load data and add to arearecords
		try{
			BufferedReader br=new BufferedReader(new FileReader("data/energyGlyphmaps/MSOA_demand_comparison_updated_format.csv"));
			br.readLine();//ignore first line
			while (br.ready()) {
				String[] toks=br.readLine().split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
				String name=toks[0];
				float datum=Float.parseFloat(toks[1]);
				int year=sdf.parse(toks[2]).getYear()+1900;
				int yearNum=year-minYear;
				AreaRecord areaRecord=areaRecords.get(name);
				if (areaRecord==null)
					println(toks[0]+" and could not find");
				else {
					areaRecord.timeseries[yearNum]=datum;
					if (toks[16].equals("True"))
						areaRecord.exceeds[yearNum]=true;
				}
				
			}
			br.close();
		}
		catch(IOException e) {
			System.err.println(e);
		}
		catch(ParseException e) {
			System.err.println(e);
		}

		//now make grid records
		HashMap<String,GridRecord> gridRecords=new HashMap<>(); 
		for (AreaRecord areaRecord:areaRecords.values()) {
			ArrayList<Point> points=areaRecord.getGridPoints(AGGREGATE_INPUT_M);
			for (Point point:points){
				GridRecord gridRecord=gridRecords.get(point.x+"-"+point.y);
				if (gridRecord==null) {
					gridRecord=new GridRecord();
					gridRecord.x=point.x;
					gridRecord.y=point.y;
					gridRecords.put(point.x+"-"+point.y, gridRecord);
					gridRecord.timeseries=new float[areaRecord.timeseries.length];
					gridRecord.exceeds=new boolean[areaRecord.exceeds.length];
				}
				else {
					println("Already found!");
				}
				for (int week=0;week<areaRecord.timeseries.length;week++) {
					gridRecord.timeseries[week]=areaRecord.timeseries[week]/points.size();
					if (areaRecord.exceeds[week])
							gridRecord.exceeds[week]=true;
				}

			}

		}

		this.gridRecords=gridRecords.values();
		this.areaRecords=areaRecords.values();

		long t1=System.currentTimeMillis();
		println("Loaded data for "+gridRecords.size()+" locations (resampled to "+AGGREGATE_INPUT_M+"m) in "+(t1-t)/1000+" seconds.");
		long heapSize = Runtime.getRuntime().totalMemory(); 
		long heapMaxSize = Runtime.getRuntime().maxMemory();

		println(heapSize/1073741824+"/"+heapMaxSize/1073741824+"gb");
	}


	private Collection<EnergyTile> createTiles(){
		final Mode mode=modeChanger.getValueEnum();
		final Projection projection=projectionChanger.getValueEnum();
		final Projection prevProjection=projectionChanger.getPreviousValueEnum();
		final ZoomPanState zoomPanState=zoomPan.getZoomPanState();

		int numCols=(int)(bounds.getWidth()/spatialBinSize+1);
		int numRows=(int)(bounds.getHeight()/spatialBinSize+1);

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

		HashSet<EnergyTile> tiles=new HashSet<>();

		if (projection==Projection.Gridded) {
			HashSet[][] griddedRecords=new HashSet[numCols][numRows];
			for (GridRecord record:gridRecords) {
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
					if (mode==Mode.Anim && griddedRecords[col][row]!=null)
						tiles.add(new TileEnergyAnim(bounds.x+col*spatialBinSize+spatialBinSize/2, bounds.y+row*spatialBinSize+spatialBinSize/2, spatialBinSize, griddedRecords[col][row],currentYear,numYears));
					else if (mode==Mode.AnimTimeSeries && griddedRecords[col][row]!=null)
						tiles.add(new TileEnergyTimeseries(bounds.x+col*spatialBinSize+spatialBinSize/2, bounds.y+row*spatialBinSize+spatialBinSize/2, spatialBinSize, griddedRecords[col][row],currentYear,numYears));
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
				ArrayList<GridRecord> arrayList=new ArrayList<EnergyGlyphMap.GridRecord>();
				arrayList.add(record);
				EnergyTile tile=null;
				if (mode==Mode.Anim)
					tile=new TileEnergyAnim((int)pt.x,(int)pt.y,gridWH,arrayList,currentYear,numYears);
				if (mode==Mode.AnimTimeSeries)
					tile=new TileEnergyTimeseries((int)pt.x,(int)pt.y,gridWH,arrayList,currentYear,numYears);
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
			currentYear=(int)map(mouseX,0,width,0,numYears);

		if (mode==Mode.Anim || mode==Mode.AnimTimeSeries || projectionTweenStep<1)
			flagToRedraw=true;

		ZoomPanState zoomPanState=zoomPan.getZoomPanState();

		int numCols=(int)(bounds.getWidth()/spatialBinSize+1);
		int numRows=(int)(bounds.getHeight()/spatialBinSize+1);


		String tooltip="";

		Collection<EnergyTile> tiles=createTiles();

		//FIND MAX (IF NECESSARY) for glyph
		Float colourScale=this.colourScale;
		if (colourScale==null) {
			//for RELATIVE, max pop is any one age band
			colourScale=-Float.MAX_VALUE;
			for (EnergyTile tile:tiles) 
				colourScale=max(colourScale,(float)(tile.getMaxForGlyph(absRel)));
			this.colourScale=colourScale;
		}
		//FIND MAX (IF NECESSARY) for transp/symbolsize
		Float colourScale2=this.colourScale2;
		if (colourScale2==null && absRel!=AbsRel.Absolute) {
			//max pop in any square
			colourScale2=-Float.MAX_VALUE;
			for (EnergyTile tile:tiles) { 
				colourScale2=max(colourScale2,tile.getMaxForTransp(absRel));
			}
			this.colourScale2=colourScale2;
		}


		for (EnergyTile tile:tiles) {
			boolean drawLabels=false;
			if (projection==Projection.GridMap)
				drawLabels=true;
			String tileTooltip=tile.drawTile(this,drawLabels);
			if (tileTooltip!=null)
				tooltip=tileTooltip;
		}

		if (absRel==AbsRel.RelativeSymb && this.colourScale2!=null)
			for (EnergyTile tile:tiles) 
				tile.drawTileRelativeSymb(this);


		if (projection==Projection.Area || projection==Projection.GridMap) {
			for (EnergyTile tile:tiles) {
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
//		if ((mode==Mode.ModelComparisonTime || mode==Mode.ModelTime) && bounds.contains(mouseX,mouseY)) {
//			stroke(0,0,200,50);
//			int t=-1;
//			for (CovidTile tile:tiles)
//				if (mouseX>tile.screenXCentre-tile.screenWH/2 && mouseX<tile.screenXCentre+tile.screenWH/2 && mouseY>tile.screenYCentre-tile.screenWH/2 && mouseY<tile.screenYCentre+tile.screenWH/2) {
//					t=(int)map(mouseX,tile.screenXCentre-tile.screenWH/2,tile.screenXCentre+tile.screenWH/2,0,numDays);
//					break;
//				}
//
//			if (t>-1)
//				for (RampTile tile:tiles) {
//					int x=(int)map(t, 0,numDays, tile.screenXCentre-tile.screenWH/2,tile.screenXCentre+tile.screenWH/2);
//					line(x,tile.screenYCentre-tile.screenWH/2+4,x,tile.screenYCentre+tile.screenWH/2-4);
//				}
//		}




		if (helpScreen.getIsActive())
			helpScreen.draw();

		if (animateThroughTime)
			currentYear++;
		if (currentYear>=numYears)
			currentYear=0;

		//draw legend
		//		if (mode==Mode.ModelBars|| mode==Mode.ModelBarsComparison || mode==Mode.ModelSpine || mode==Mode.ModelSpineComparison || mode==Mode.ModelGraph || mode==Mode.ModelSpineQuintiles|| mode==Mode.ModelSpineTime || mode==Mode.ModelBarsComparisonByStatus || mode==Mode.ModelSpineBoth) {
//		if (!DATAFILE_RESULTS_PATHS.isEmpty() && (mode==Mode.ModelAgeDayAnim||mode==Mode.ModelTime||mode==Mode.ModelComparisonTime)) {
//			int y=height-statusBarH-(statuses.length*12);
//			int legendW=0;
//			textAlign(RIGHT,TOP);
//			textSize(10);
//			for (String status:statuses)
//				legendW=(int)max(legendW,textWidth(status));
//			legendW+=12;
//			fill(255,230);
//			noStroke();
//			rect(width-legendW,y,legendW,height-y);
//			for (int statusIdx=0;statusIdx<statuses.length;statusIdx++) {
//				//				if (mode==Mode.ModelBarsComparison||mode==Mode.ModelSpineComparison) {
//				//					if (statusIdx==selectedStatusIdx) {
//				//						fill(ctStatus.findColour(statusIdx+1));
//				//						rect(width-10,y,10,10);
//				//					}
//				//				}
//				//				else {
//				if (statusShow[statusIdx]) {
//					fill(ctStatus.findColour(statusIdx+1));
//					rect(width-10,y,10,10);
//				}
//				//				}
//				if (mouseX>width-legendW && mouseY>y && mouseY<y+12 && mouseClicked) {
//					//					if (mode==Mode.ModelBarsComparison||mode==Mode.ModelSpineComparison)
//					//						selectedStatusIdx=statusIdx;
//					//					else
//					statusShow[statusIdx]=!statusShow[statusIdx];
//				}
//				fill(80);
//				text(statuses[statusIdx],width-12,y);
//				y+=12;
//			}
//		}


		//draw boundaries
		if (projection!=Projection.GridMap && showBoundariesChanger.getValue()) {
			PVector ptMouse=zoomPanState.getDispToCoord(new PVector(mouseX,mouseY));
			float geoMouseX=map(ptMouse.x,bounds.x,bounds.x+bounds.width,(float)geoBounds.getMinX(),(float)geoBounds.getMaxX());
			float geoMouseY=map(ptMouse.y,bounds.y,bounds.y+bounds.height,(float)geoBounds.getMaxY(),(float)geoBounds.getMinY());

			stroke(150);
			float[] coords=new float[6];
			noFill();
			for (AreaRecord areaRecord:areaRecords) {
				if (areaRecord.path2d.contains(geoMouseX,geoMouseY)) {
					fill(0,80);
					textSize(20);
					textLeading(20);
					textAlign(LEFT,BOTTOM);
					text(areaRecord.name,0,height-statusBarH);
					noFill();
				}
				PathIterator pi=areaRecord.path2d.getPathIterator(null);
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
		
		String dateText=this.firstYear+currentYear+"";
		
		
		
		if (mode==Mode.Anim)
			if (absRel==AbsRel.Absolute)
				title="Electricity demand in "+dateText;
			else
				title="Electricity demand in "+dateText;
		if (mode==Mode.AnimTimeSeries)
			if (absRel==AbsRel.Absolute)
				title="Electricity demand from "+firstYear+" to "+(firstYear+numYears);
			else
				title="Electricity demand from "+firstYear+" to "+(firstYear+numYears);
		text(title,0,0,width,height);
		mouseClicked=false;


		if (!tooltip.isEmpty()) {
			drawTooltip(tooltip,mouseX+10,mouseY+10,100);
		}

		//timeline
		if ((mode==Mode.Anim || mode==Mode.AnimTimeSeries) && mouseY<50){
			noStroke();
			fill(255,200);
			rect(0,0,width,50);
			fill(0,100);
			textAlign(CENTER,CENTER);
			textSize(18);
			stroke(0,50);
			for (int t=1;t<numYears;t+=2) {
				float x=map(t,0,numYears,0,width);
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
			projectionChanger.draw(x, height-statusBarH+3);
			x+=projectionChanger.getWidth()+10;
			showBoundariesChanger.draw(x, height-statusBarH+3);
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
		if (key=='h') {
			helpScreen.setIsActive(!helpScreen.getIsActive());
		}
		flagToRedraw=true;
	}

	public void mouseClicked() {
		mouseClicked=true;
		flagToRedraw=true;
	}


	class GridRecord{
		int x,y;
		float[] timeseries;
		boolean[] exceeds;
	}

	class AreaRecord extends GridRecord{
		String name;
		String code;
		Path2D path2d;
		int gridX,gridY;

		ArrayList<Point> getGridPoints(int gridSize){
			ArrayList<Point> returnPoints=new ArrayList<Point>();
			Rectangle bb=path2d.getBounds();
			for (int i=bb.x;i<bb.x+bb.width;i+=gridSize) {
				for (int j=bb.y;j<bb.y+bb.height;j+=gridSize) {
					int x=i/gridSize*gridSize+gridSize/2;//just to get it in grid alignment
					int y=j/gridSize*gridSize+gridSize/2;
					if (path2d.contains(x,y)) {
						returnPoints.add(new Point(x,y));
					}
					
				}

			}

			return returnPoints;
		}
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent arg0) {
		flagToRedraw=true;
	}

}
