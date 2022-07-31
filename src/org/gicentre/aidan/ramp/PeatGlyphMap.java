package org.gicentre.aidan.ramp;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.gicentre.aidan.ramp.CovidGlyphMap.Projection;
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

import discretisation.Discretisation;
import discretisation.HexGridCell;
import discretisation.SqGridCell;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PVector;


public class PeatGlyphMap extends PApplet implements MouseWheelListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	static String APP_NAME="DemographicGridmap, v1.10 (12/10/21)";

	static String DATAFILE_DEMOGRAPHICS_PATH;
	static ArrayList<String> DATAFILE_RESULTS_PATHS;
	static String DATAFILE_FORCE_PATH;
	static String DATAFILE_POLLUTION;


	static int AGGREGATE_INPUT_M=1;


	Collection<Record> records;
	ArrayList<String> speciesList;

	ColourTable ct;

	Rectangle bounds,gridBounds;
	Rectangle2D geoBounds;


	ZoomPan zoomPan;

	boolean animateThroughTime=true;

	int spatialBinSize=30; //in screen pixels
	int attribBinSize=15; //numbers of attributes
	int numDemographicsCategories=91;

	Float colourScale=null;
	Float colourScale2=null;
	
	Discretisation discretisation;



	enum Mode{
		Heat,
	}
	
	enum CellShape{
		Square, Hexagon;
	}

	boolean mouseClicked=false;

	int wrap=20;

	int currentWeek=0;
	int numWeeks=0;
	long firstT;

	private HelpScreen helpScreen;

	ArrayList<String> datasetNames;


	int statusBarH=18+18;
	enum AbsRel{Absolute,Relative, RelativeSymb,RelativeFade}
	EnumChanger<AbsRel> absRelChanger;
	EnumChanger<Mode> modeChanger;
	BooleanChanger showBoundariesChanger;
	EnumChanger<CellShape> cellshapeChanger;

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

		PApplet.main(new String[]{"org.gicentre.aidan.ramp.PeatGlyphMap"});
	}

	public void setup() {

		//		LOAD_OUTPUTS=false;
		//		LOAD_BASELINE=false;

		println("Aidan Slingsby, a.slingsby@city.ac.uk, City, University of London");
		size(800,800);
		ct=ColourTable.getPresetColourTable(ColourTable.PURPLES);

		smooth();

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
		cellshapeChanger=new EnumChanger(this, "cellShape", CellShape.class,pFont);
		cellshapeChanger.setListener(new ValueChangerListener() {
			public void valueChanged(ValueChanger valueChanger) {
				flagToRedraw=true;
				redraw();
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

		HashMap<Integer, String> id2Species=new HashMap<Integer, String>();

		//load os tile lookups
		HashMap<String, String> tile2coordprefix=new HashMap<String, String>();
		try {
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

		try{	
			BufferedReader br=new BufferedReader(new FileReader("data/peat/2022-05-05_data/BSBI_species.csv"));
			br.readLine();
			while (br.ready()) {
				String[] toks=br.readLine().split(",");
				id2Species.put(Integer.parseInt(toks[1]), toks[0]);
			}
		}
		catch (IOException e) {
			println(e);
		}
		
		records=new HashSet<PeatGlyphMap.Record>();

		try {
			BufferedReader br=new BufferedReader(new FileReader("data/peat/2022-05-05_data/BSBI_occurrences.csv"));
			String[] header=br.readLine().split(",");
			while (br.ready()) {
				String[] toks=br.readLine().split(",");
				
				String prefix=tile2coordprefix.get(toks[0].toUpperCase().substring(0,2));
				String easting=toks[0].substring(2,3);
				String northing=toks[0].substring(3,4);
				easting=prefix.substring(0,1)+easting+"0000";
				northing=prefix.substring(1)+northing+"0000";
				int x=Integer.parseInt(easting)/AGGREGATE_INPUT_M*AGGREGATE_INPUT_M+AGGREGATE_INPUT_M/2;
				int y=Integer.parseInt(northing)/AGGREGATE_INPUT_M*AGGREGATE_INPUT_M+AGGREGATE_INPUT_M/2;

				if (geoBounds==null)
					geoBounds=new Rectangle(x,y,0,0);
				else
					geoBounds.add(x,y);
				
				Record record=new Record();
				record.x=x;
				record.y=y;
				record.presentSpIds=new HashSet<Integer>();
				for (int i=1;i<toks.length;i++) {
					if (toks[i].trim().equals("1"))
						record.presentSpIds.add(Integer.parseInt(header[i]));
				}
				records.add(record);
			}
		}
		catch (Exception e) {
			println(e);
		}
		
				
		long t1=System.currentTimeMillis();
		println("Loaded data for "+records.size()+" locations (resampled to "+AGGREGATE_INPUT_M+"m) in "+(t1-t)/1000+" seconds.");
		long heapSize = Runtime.getRuntime().totalMemory(); 
		long heapMaxSize = Runtime.getRuntime().maxMemory();

		println(heapSize/1073741824+"/"+heapMaxSize/1073741824+"gb");
		
		
		//Export as csv
		try {
			BufferedWriter bw = new BufferedWriter(new PrintWriter("/Users/aidans/Desktop/biodiversity.tsv"));
			bw.write("x\ty\tids\n");
			for (Record record: records) {
				bw.write(record.x+"\t"+record.y+"\t");
				for (int spId:record.presentSpIds)
					bw.write(spId+",");
				bw.write("\n");
			}
			bw.close();
		}
		catch (IOException e) {
			println(e);
		}
	}


	private Collection<PeatTile> createTiles(){
		final Mode mode=modeChanger.getValueEnum();
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

		HashSet<PeatTile> tiles=new HashSet<>();

			HashSet[][] griddedRecords=new HashSet[numCols][numRows];
			for (Record record:records) {
				if (geoBoundsInView.contains(record.x,record.y)) {
					float x=map(record.x,(float)geoBounds.getMinX(),(float)geoBounds.getMaxX(),bounds.x,bounds.x+bounds.width);
					float y=map(record.y,(float)geoBounds.getMaxY(),(float)geoBounds.getMinY(),bounds.y,bounds.y+bounds.height);
					PVector pt=zoomPanState.getCoordToDisp(x,y);
					
//					int xBin=(int)(pt.x/spatialBinSize);
//					int yBin=(int)(pt.y/spatialBinSize);
					if (cellshapeChanger.getValueEnum()==CellShape.Square)
						discretisation = new SqGridCell(spatialBinSize);
					else
						discretisation = new HexGridCell(spatialBinSize);
					Point spatialBin=discretisation.getColRow((int)pt.x,(int)pt.y);
					if (spatialBin.x>=0 && spatialBin.x<numCols && spatialBin.y>=0 && spatialBin.y<numRows) {
						if (griddedRecords[spatialBin.x][spatialBin.y]==null)
							griddedRecords[spatialBin.x][spatialBin.y]=new HashSet<>();
						griddedRecords[spatialBin.x][spatialBin.y].add(record);
					}
				}
			}
			for (int col=0;col<numCols;col++) {
				for (int row=0;row<numRows;row++) {
					Shape shape=discretisation.getShape(col, row);
					if (mode==Mode.Heat && griddedRecords[col][row]!=null)
						tiles.add(new PeatTileHeat(shape, griddedRecords[col][row],numDemographicsCategories,attribBinSize));						
				}
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

		background(200);


		if (mouseY<50)
			currentWeek=(int)map(mouseX,0,width,0,numWeeks);

		if (projectionTweenStep<1)
			flagToRedraw=true;

		ZoomPanState zoomPanState=zoomPan.getZoomPanState();

		int numCols=(int)(bounds.getWidth()/spatialBinSize+1);
		int numRows=(int)(bounds.getHeight()/spatialBinSize+1);


		String tooltip="";

		Collection<PeatTile> tiles=createTiles();

		//FIND MAX (IF NECESSARY) for glyph
		Float colourScale=this.colourScale;
		if (colourScale==null) {
			//for RELATIVE, max pop is any one age band
			colourScale=-Float.MAX_VALUE;
			for (PeatTile tile:tiles) 
				colourScale=max(colourScale,(float)(tile.getMaxForGlyph(absRel)));
			this.colourScale=colourScale;
		}
		//FIND MAX (IF NECESSARY) for transp/symbolsize
		Float colourScale2=this.colourScale2;
		if (colourScale2==null && absRel!=AbsRel.Absolute) {
			//max pop in any square
			colourScale2=-Float.MAX_VALUE;
			for (PeatTile tile:tiles) { 
				colourScale2=max(colourScale2,tile.getMaxForTransp(absRel));
			}
			this.colourScale2=colourScale2;
		}

		for (PeatTile tile:tiles) {
			boolean drawLabels=false;
			String tileTooltip=tile.drawTile(this,drawLabels);
			if (tileTooltip!=null)
				tooltip=tileTooltip;
		}

		

		if (absRel==AbsRel.RelativeSymb && this.colourScale2!=null)
			for (PeatTile tile:tiles) 
				tile.drawTileRelativeSymb(this);


		

		for (PeatTile tile:tiles) {
			tile.drawOutlines(this);
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
			currentWeek++;
		if (currentWeek>=numWeeks)
			currentWeek=0;

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




		fill(0,100);
		textSize(30);
		textLeading(30);
		textAlign(LEFT,TOP);

		String title="";

		String dateText=new SimpleDateFormat("MMM yyyy").format(this.firstT+(currentWeek*1000L*60*60*24*7));



		if (mode==Mode.Heat)
			if (absRel==AbsRel.Absolute)
				title="Number of species";
			else
				title="Number of species";
		text(title,0,0,width,height);
		mouseClicked=false;


		if (!tooltip.isEmpty()) {
			drawTooltip(tooltip,mouseX+10,mouseY+10,100);
		}

//		//timeline
//		if ((mode==Mode.Anim || mode==Mode.AnimTimeSeries || mode==Mode.AnimRadialTimeSeries) && mouseY<50){
//			noStroke();
//			fill(255,200);
//			rect(0,0,width,50);
//			fill(0,100);
//			textAlign(CENTER,CENTER);
//			textSize(18);
//			stroke(0,50);
//			for (int t=1;t<numWeeks;t+=2) {
//				float x=map(t,0,numWeeks,0,width);
//				text(t+"",x,50/2);
//				line(x,0,x,50);
//			}
//		}

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
			showBoundariesChanger.draw(x, height-statusBarH+2);
			x+=showBoundariesChanger.getWidth()+10;
			cellshapeChanger.draw(x, height-statusBarH+3);
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
		if (key=='[') {
			attribBinSize--;
			if (attribBinSize<1)
				attribBinSize=1;
		}
		if (key==']') {
			attribBinSize++;
			if (attribBinSize>numDemographicsCategories)
				attribBinSize=numDemographicsCategories-1;
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


	class Record{
		int x,y;
		HashSet<Integer> presentSpIds;
	}


	@Override
	public void mouseWheelMoved(MouseWheelEvent arg0) {
		flagToRedraw=true;
	}

}
