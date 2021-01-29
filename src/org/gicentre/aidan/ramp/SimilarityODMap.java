package org.gicentre.aidan.ramp;

import java.awt.Rectangle;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.gicentre.shputils.ShpUtils;
import org.gicentre.utils.colour.ColourTable;
import org.gicentre.utils.gui.HelpScreen;
import org.gicentre.utils.move.ZoomPan;
import org.gicentre.utils.move.ZoomPanState;
import org.gicentre.utils.slippymap.LonLatBounds;
import org.gicentre.utils.slippymap.SlippyMap;
import org.gicentre.utils.slippymap.SlippyMapType;
import org.gicentre.utils.spatial.OSGB;

import processing.core.PApplet;
import processing.core.PVector;
import ucar.ma2.ArrayInt;
import ucar.ma2.ArrayShort;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;


public class SimilarityODMap extends PApplet implements MouseWheelListener{
	
	ArrayList<Record> records;
	String[] demographicsAttribNames;
	
	ColourTable ctCorr, ctKLDiv;

	Rectangle bounds;
	Rectangle2D geoBounds;
	ZoomPan zoomPan,zoomPan2;
	
	SlippyMap slippyMap;
	ZoomPan slippyMapZoomPan;
	Rectangle slippyMapBounds;

	int spatialBinSize=60; //in screen pixels
	int spatialBinSize2=60;
	int attribBinSize=15; //numbers of attributes
	
	Float colourScale=null;
	
	float scale,geoRatio;
	OSGB osgb;
	
	boolean synchroniseBothZooms=true;
	
	float[] minPopCounts,maxPopCounts;
	
	HashMap<String, Path2D> boundaries;
	HashMap<String, String> boundaryNames;
	
	enum Mode{
		DemogAbsBars,
		DemogPropBars,
		DemographicCorrelation,
		DemographicKLDivergence,
	}
	Mode mode=Mode.DemogAbsBars;
	
	boolean compareMode=false;
	
	int wrap=20;
	
	PearsonsCorrelation correlation=new PearsonsCorrelation();
	
	private HelpScreen helpScreen;
	static String DATAFILE_DEMOGRAPHICS_PATH;
	static String DATAFILE_RESULTS_PATH;
	static String DATAFILE_FORCE_PATH;
	static String APP_NAME="SimilarityODMap, v1.2";
	
	static final boolean LOAD_DEMOGRAPHICS=true; //always true
	static boolean LOAD_OUTPUTS=true;
	static boolean LOAD_FORCE=true;
	
	static public void main(String[] args) {
		DATAFILE_DEMOGRAPHICS_PATH=args[0];
		PApplet.main(new String[]{"org.gicentre.aidan.ramp.SimilarityODMap"});
	}
	
	public void setup() {
		println("Aidan Slingsby, a.slingsby@city.ac.uk, City, University of London");
		size(700,700);
		this.addMouseWheelListener(this);
		smooth();
		
		ctCorr=ColourTable.getPresetColourTable(ColourTable.RD_BU);
		ctKLDiv=ColourTable.getPresetColourTable(ColourTable.ORANGES);

		bounds=new Rectangle(0,0,width,height);
		zoomPan=new ZoomPan(this);
		zoomPan.setZoomMouseButton(RIGHT);
		zoomPan2=new ZoomPan(this);
		zoomPan2.setZoomMouseButton(RIGHT);
		loadData();		

		slippyMapBounds=new Rectangle(bounds.width-150, bounds.height-150,150,150);
		slippyMapZoomPan=new ZoomPan(this);
		slippyMapZoomPan.setZoomMouseButton(RIGHT);
		slippyMap=new SlippyMap(this,slippyMapZoomPan,slippyMapBounds);
		slippyMap.setBingApiKey("AoVLv4X3PU31TbprsfobnWkIS5xiyg-_txBKYQ9EpWdRubeu_iSjBzU3PMa-q0o2");
		slippyMap.setMapType(SlippyMapType.BING_ROAD);
//		slippyMap.setMapType(SlippyMapType.OSM_MAPNIK);
		
		float ratioX=bounds.width/(float)geoBounds.getWidth();
		float ratioY=bounds.height/(float)geoBounds.getHeight();
		scale=min(ratioX,ratioY);
		geoRatio=(float)(geoBounds.getHeight()/geoBounds.getWidth());

		osgb=new OSGB();
		
		helpScreen=new HelpScreen(this,createFont("Arial",12));
		helpScreen.setHeader(APP_NAME, 20, 16);
		
		helpScreen.putEntry("left drag","Pan");
		helpScreen.putEntry("right drag up/down","Zoom");
		helpScreen.putEntry("LEFT and RIGHT","Change grid size");
		helpScreen.addSpacer();
		helpScreen.putEntry("'c'","Comparison model (comparison to cells where mouse is)");
		helpScreen.addSpacer();
		helpScreen.putEntry("'y'","Link/unlink zoompan inside small multiples (below has no effect if linked)");
		helpScreen.putEntry("SHIFT left drag","Pan inside small multiples");
		helpScreen.putEntry("SHIFT right drag up/down","Zoom insidesmall multiples");
		helpScreen.putEntry("SHIFT LEFT and RIGHT","Change grid size of small multiples");
		helpScreen.addSpacer();
		helpScreen.putEntry("UP and DOWN","Increase/decrease number of age categories");
		helpScreen.putEntry("'[' and ']'","Change colour scaling");
		helpScreen.putEntry("'s'","Reset colour scaling");
		helpScreen.putEntry("'m'","Change display mode");
		helpScreen.addSpacer();		
		helpScreen.putEntry("'h'", "Show/hide help");
		
		helpScreen.setFooter("Aidan Slingsby, a.slingsby@city.ac.uk, City, University of London",16,10);

		
	}
	
	private void loadData() {
		print("Loading data...");
		long t=System.currentTimeMillis();
		records=new ArrayList<SimilarityODMap.Record>();

		HashMap<String, Record> location2Records=new HashMap<String, SimilarityODMap.Record>();
		
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

	
		System.gc();
	
		println("done.");
		long t1=System.currentTimeMillis();
		println(records.size()+" records in "+(t1-t)/1000+" seconds");
	}

	public void draw() {
		background(200);
		
		if (synchroniseBothZooms) {
			zoomPan2.setZoomScale(zoomPan.getZoomScale());
			zoomPan2.setPanOffset(zoomPan.getPanOffset().x,zoomPan.getPanOffset().y);
			spatialBinSize2=spatialBinSize;
		}
		
		ZoomPanState zoomPanState=zoomPan.getZoomPanState();
		ZoomPanState zoomPanState2=zoomPan2.getZoomPanState();

		
		if (keyEvent!=null) {
			if (keyEvent.isShiftDown()) {
				zoomPan.setMouseMask(-1);
				zoomPan2.setMouseMask(0);
			}
			else {
				zoomPan.setMouseMask(0);
				zoomPan2.setMouseMask(-1);
			}
		}
		

		//double becasue the pearsonscoorealtion needs double
		double[][][] demogSums=new double[(int)(bounds.getWidth()/spatialBinSize)][(int)bounds.getHeight()/spatialBinSize][demographicsAttribNames.length/attribBinSize];
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
			}
		}
		double[][][] demogSums2=null;
		if (mode==Mode.DemographicCorrelation || mode==Mode.DemographicKLDivergence) {
			demogSums2=new double[(int)(bounds.getWidth()/spatialBinSize2)][(int)bounds.getHeight()/spatialBinSize2][demographicsAttribNames.length/attribBinSize];
			for (Record record:records) {
				float x=record.x*scale;
				float y=bounds.y+bounds.height-record.y*scale;
				PVector pt=zoomPanState2.getCoordToDisp(x,y);
				int xBin=(int)(pt.x/spatialBinSize2);
				int yBin=(int)(pt.y/spatialBinSize2);
				if (xBin>=0 && xBin<demogSums2.length && yBin>=0 && yBin<demogSums2[0].length) {
					for (int j=0;j<demographicsAttribNames.length;j++) {
						if (j/attribBinSize<demogSums2[xBin][yBin].length) {
							demogSums2[xBin][yBin][j/attribBinSize]+=record.popCounts[j];
						}
					}
				}
			}
		}
		
		
		int mouseoveredX=mouseX/spatialBinSize;
		int mouseoveredY=mouseY/spatialBinSize;
		if (mouseoveredX>=demogSums.length)
			mouseoveredX=demogSums.length-1;
		if (mouseoveredY>=demogSums.length)
			mouseoveredY=demogSums[0].length-1;
		//calc latlon bounds
		{
			PVector pt1=getOSGBCoord(mouseoveredX*spatialBinSize,mouseoveredY*spatialBinSize,zoomPanState);
			PVector pt2=getOSGBCoord(mouseoveredX*spatialBinSize+spatialBinSize,mouseoveredY*spatialBinSize+spatialBinSize,zoomPanState);
			PVector pt1OSGB=osgb.invTransformCoords(pt1);
			PVector pt2OSGB=osgb.invTransformCoords(pt2);
			LonLatBounds lonLatBounds=new LonLatBounds(pt1OSGB.x,pt1OSGB.y,pt2OSGB.x,pt2OSGB.y);
			slippyMap.zoomTo(lonLatBounds);
		}


		double[][][] displayValues=null;
		if (mode==Mode.DemogAbsBars) {
			if (compareMode) {
				displayValues=new double[demogSums.length][demogSums[0].length][demogSums[0][0].length];
				for (int x=0;x<demogSums.length;x++) 
					for (int y=0;y<demogSums[x].length;y++) 
						for (int j=0;j<demogSums[x][x].length;j++)
							displayValues[x][y][j]=demogSums[x][y][j]-demogSums[mouseoveredX][mouseoveredY][j];
			}
			else
				displayValues=demogSums;
		}
		else if (mode==Mode.DemogPropBars) {
			displayValues=new double[demogSums.length][demogSums[0].length][demogSums[0][0].length];
			double mouseOveredSum=0;
			for (int j=0;j<demogSums[mouseoveredX][mouseoveredY].length;j++)
				mouseOveredSum+=demogSums[mouseoveredX][mouseoveredY][j];		
			for (int x=0;x<demogSums.length;x++) {
				for (int y=0;y<demogSums[x].length;y++) {
					double sum=0;
					for (int j=0;j<demogSums[x][y].length;j++)
						sum+=demogSums[x][y][j];		
					for (int j=0;j<demogSums[x][x].length;j++) {
						if (compareMode)
							displayValues[x][y][j]=(demogSums[x][y][j]/sum)-(demogSums[mouseoveredX][mouseoveredY][j]/mouseOveredSum);
						else
							displayValues[x][y][j]=demogSums[x][y][j]/sum;
						if (Double.isNaN(displayValues[x][y][j]))
							displayValues[x][y][j]=0;
					}
				}
			}
		}

		
		
		
		//if need to rescale colour/width
		Float colourScale=this.colourScale;
		if (colourScale==null) {
			if (mode==Mode.DemogAbsBars || mode==Mode.DemogPropBars) {
				colourScale=-Float.MAX_VALUE;
				for (int x=0;x<demogSums.length;x++) {
					for (int y=0;y<demogSums.length;y++) {
						for (int j=0;j<demographicsAttribNames.length/attribBinSize;j++) {
							colourScale=max(colourScale,(float)(displayValues[x][y][j]));
						}
					}
				}
			}
			else if (mode==Mode.DemographicKLDivergence) {
				if (compareMode) {
					colourScale=-Float.MAX_VALUE;
					for (int x1=0;x1<demogSums.length;x1++) {
						for (int y1=0;y1<demogSums.length;y1++) {
							float v=(float)modifiedKLDivergence(demogSums[mouseoveredX][mouseoveredY], demogSums2[x1][y1]);
							if (!Float.isNaN(v))
								colourScale=max(colourScale,v);						
						}
					}
				}
				else {
					colourScale=-Float.MAX_VALUE;
					for (int x1=0;x1<demogSums.length;x1++) {
						for (int y1=0;y1<demogSums.length;y1++) {
							for (int x2=0;x2<demogSums.length;x2++) {
								for (int y2=0;y2<demogSums.length;y2++) {
									float v=(float)modifiedKLDivergence(demogSums[x1][y1], demogSums2[x2][y2]);
									if (!Float.isNaN(v))
										colourScale=max(colourScale,v);
								}
							}
						}
					}
				}
			}
			this.colourScale=colourScale;
		}
		
		println("colourscale="+colourScale);
				
		//make squares with data white
		if (mode==Mode.DemogAbsBars || mode==Mode.DemogPropBars) {
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
		

		float odSqXCount=0,odSqYCount=0,odSqWH=0,odSqXOff=0,odSqYOff=0;
		if (mode==Mode.DemographicCorrelation || mode==Mode.DemographicKLDivergence) {
			odSqXCount=bounds.width/spatialBinSize2;
			odSqYCount=bounds.height/spatialBinSize2;
			float odSqW=(spatialBinSize-4)/odSqXCount;
			float odSqH=(spatialBinSize-4)/odSqYCount;
			odSqWH=min(odSqW,odSqH);
			odSqXOff=(spatialBinSize-odSqWH*odSqXCount)/2;
			odSqYOff=(spatialBinSize-odSqWH*odSqYCount)/2;
		}
		
		noStroke();
		for (int x=0;x<demogSums.length;x++) {
			for (int y=0;y<demogSums[x].length;y++) {
				boolean empty=true;
				for (int j=0;j<demogSums[x][y].length;j++)
					if (demogSums[x][y][j]>0)
						empty=false;
				if (!empty) {
					if (mode==Mode.DemogAbsBars || mode==Mode.DemogPropBars) {
						for (int j=0;j<demogSums[x][y].length;j++) {
							if (compareMode) {
								float w=constrain(map((float)displayValues[x][y][j],-colourScale,colourScale,-spatialBinSize/2,spatialBinSize/2),-spatialBinSize/2,spatialBinSize/2);
								if (w<0)
									fill(255,150,150);
								else
									fill(150,150,255);
								rect(x*spatialBinSize+spatialBinSize/2,y*spatialBinSize+(demogSums[x][y].length-j-1)*((float)spatialBinSize/demogSums[x][y].length),w,((float)(spatialBinSize)/displayValues[x][y].length));

							}
							else {
								float w=constrain(map((float)displayValues[x][y][j],0,colourScale,0,spatialBinSize),0,spatialBinSize);
								fill(180);
								rect(x*spatialBinSize,y*spatialBinSize+(demogSums[x][y].length-j-1)*((float)spatialBinSize/demogSums[x][y].length),w,((float)(spatialBinSize)/displayValues[x][y].length));
							}
						}
					}
					else if (mode==Mode.DemographicCorrelation || mode==Mode.DemographicKLDivergence) {
						//create correlation matrix (or if SHIFT down) correlations with mouseovered
						if (compareMode) {
							if (x==mouseoveredX && y==mouseoveredY) {
								for (int x1=0;x1<odSqXCount;x1++) {
									for (int y1=0;y1<odSqYCount;y1++) {
										if (mode==Mode.DemographicCorrelation) {
											float corr=(float)correlation.correlation(demogSums[mouseoveredX][mouseoveredY], demogSums2[x1][y1]);
											//need to do DEST squares
											if (!Float.isNaN(corr))
												fill(ctCorr.findColour(corr));
											else
												noFill();
										}
										else if (mode==Mode.DemographicKLDivergence) {
											float klDiv=(float)modifiedKLDivergence(demogSums[mouseoveredX][mouseoveredY], demogSums2[x1][y1]);
											if (!Float.isNaN(klDiv))
												fill(ctKLDiv.findColour(klDiv/colourScale));
											else 
												noFill();
										}
										rect(x1*spatialBinSize2,y1*spatialBinSize2,spatialBinSize2,spatialBinSize2);
									}
								}
							}
						}
						else {
							for (int x1=0;x1<odSqXCount;x1++) {
								for (int y1=0;y1<odSqYCount;y1++) {
									if (mode==Mode.DemographicCorrelation) {
										float corr=(float)correlation.correlation(demogSums[x][y], demogSums2[x1][y1]);
										//need to do DEST squares
										if (!Float.isNaN(corr))
											fill(ctCorr.findColour(corr));
										else
											noFill();
									}
									else if (mode==Mode.DemographicKLDivergence) {
										float klDiv=(float)modifiedKLDivergence(demogSums[x][y], demogSums2[x1][y1]);
										if (!Float.isNaN(klDiv))
											fill(ctKLDiv.findColour(klDiv/colourScale));
										else 
											noFill();
									}										
									rect(x*spatialBinSize+odSqXOff+x1*odSqWH,y*spatialBinSize+odSqYOff+y1*odSqWH,odSqWH,odSqWH);										
								}
							}
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
		
		//draw boundaries
		stroke(150);
		float[] coords=new float[6];
		noFill();
		for (Entry<String, Path2D> entry:boundaries.entrySet()) {
			PVector geoMouse=getOSGBCoord(mouseX,mouseY,zoomPanState);
			if (entry.getValue().contains(geoMouse.x,geoMouse.y)) {
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
		if (mode==Mode.DemogAbsBars)
			title="Population count by age group (younger at bottom)";
		if (mode==Mode.DemogPropBars)
			title="Population proportion by age group (younger at bottom)";
		if (mode==Mode.DemographicCorrelation)
			title="Similarity of population (correlation) to other cells";
		if (mode==Mode.DemographicKLDivergence)
			title="Similarity of population (KLD) to other cells";
		text(title,0,0,width,height);
		
		
		
		if (compareMode) {
			fill(0);
			textSize(10);
			textAlign(CENTER,BOTTOM);
			text("Comparison cell",slippyMapBounds.x+slippyMapBounds.width/2,slippyMapBounds.y);
			slippyMap.draw();
			pushStyle();
			noFill();
			stroke(255,0,0);
			strokeWeight(3);
			rect(mouseoveredX*spatialBinSize,mouseoveredY*spatialBinSize,spatialBinSize,spatialBinSize);
			popStyle();
		}
		
		if (helpScreen.getIsActive())
			helpScreen.draw();
		noLoop();
		
	}
	
	public static double modifiedKLDivergence(double[] ns2, double[] ns1){
		double klDiv = 0;
		double p=0.001;

		double ns1Sum=0,ns2Sum=0;
		for (double n1:ns1)
			ns1Sum+=n1;
		for (double n2:ns2)
			ns2Sum+=n2;
		double nsSum=ns1Sum+ns2Sum; //sum of both ns1 ns2 

		for (int i = 0; i<ns1.length; i++) {
			double zns1=0,zns=0; 
			for (int j = 0; j<ns1.length; j++) {
				zns1+=Math.pow(p,Math.abs(i-j))*ns1[j];
				zns+=Math.pow(p,Math.abs(i-j))*(ns1[j]+ns2[j]);
			}
			if (ns1[i]>0) {
		        klDiv += (ns1[i]/ns1Sum) * Math.log( (zns1/nsSum) / (zns/(float)nsSum));
//				klDiv += (ns1[i]/ns1Sum) * Math.log( (ns1[i]/ns1Sum) / ((ns1[i]/ns1Sum)+(ns2[i]/ns2Sum)));
//				klDiv -= (ns1[i]/ns1Sum) * Math.log( (ns1[i]/ns1Sum) / ((ns1[i]+ns2[i])/(float)nsSum));
			}
		}
		return Math.exp(klDiv);
	}
	
	private PVector getOSGBCoord(int mouseX,int mouseY,ZoomPanState zoomPanState) {
		PVector ptMouse=zoomPanState.getDispToCoord(new PVector(mouseX,mouseY));
		int geoMouseX=(int)map(ptMouse.x,0,bounds.height/geoRatio,9013,470013);
		int geoMouseY=(int)map(ptMouse.y,bounds.height,0,530301.5f,1217301.5f);
		return new PVector(geoMouseX,geoMouseY);
	}
	
	public void keyPressed() {
		if (key==CODED && keyCode==LEFT && !keyEvent.isShiftDown()) {
			spatialBinSize--;
			if (spatialBinSize<2)
				spatialBinSize=2;
			println("1");
		}
		if (key==CODED && keyCode==LEFT && keyEvent.isShiftDown()) {
			spatialBinSize2-=bounds.width/spatialBinSize;
			if (spatialBinSize2<2)
				spatialBinSize2=2;
			println("2");
		}
		if (key==CODED && keyCode==RIGHT && !keyEvent.isShiftDown()) {
			spatialBinSize++;
		}
		if (key==CODED && keyCode==RIGHT && keyEvent.isShiftDown()) {
			spatialBinSize2+=bounds.width/spatialBinSize;
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
		if (key=='[' && colourScale!=null) {
			colourScale+=colourScale/10f;
			
		}
		if (key==']' && colourScale!=null) {
			colourScale-=colourScale/10f;
		}
		if (key=='s') {
			colourScale=null;
		}
		if (key=='m') {
			int ord=mode.ordinal();
			ord++;
			if (ord>=Mode.values().length)
				ord=0;
			this.mode=Mode.values()[ord];
			this.colourScale=null;
		}
		if (key=='h') {
			helpScreen.setIsActive(!helpScreen.getIsActive());
		}
		if (key=='y') {
			synchroniseBothZooms=!synchroniseBothZooms;
		}
		if (key=='c')
			compareMode=!compareMode;
		loop();
	}
	
	public void mouseMoved() {
		loop();
	}
	
	public void mouseDragged() {
		loop();
	}
	
	public 
	
	class Record{
		short x,y;
		short[] popCounts; //by demographicgroup
		short[][][] resultCounts; //by time, demographicGroup, infectionType
		short[] resultForce; //by time
		short[] resultReservoir; //by time
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent arg0) {
		loop();
		
	}
	
	
}
