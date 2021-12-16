package org.gicentre.aidan.ramp;

import java.applet.Applet;
import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.text.DecimalFormat;
import java.util.Collection;

import org.gicentre.aidan.ramp.CovidGlyphMap.GridRecord;

import processing.core.PApplet;

import org.gicentre.aidan.ramp.CovidGlyphMap.AbsRel;

public class TileCovidAnim extends CovidTile {

	float[] timeseries;
	int currentWeek;
	
	public TileCovidAnim(Shape shape,Collection<GridRecord> records, int currentWeek,int numWeeks){
		super(shape);

		timeseries=new float[numWeeks];
		this.currentWeek=currentWeek;
		
		for (GridRecord record:records)
			for (int t=0;t<record.casesTimeseries.length;t++)
				timeseries[t]+=record.casesTimeseries[t];
	}
	
	
	public String drawTile(CovidGlyphMap covidGlyphMap, boolean showAreaName) {
		String tooltip=new DecimalFormat("#.0").format(timeseries[currentWeek]);
		if (covidGlyphMap.absRelChanger.getValueEnum()==AbsRel.Absolute)
			covidGlyphMap.fill(covidGlyphMap.ct.findColour(PApplet.map(timeseries[currentWeek], 0, covidGlyphMap.colourScale, 0, 1)));
		else
			covidGlyphMap.fill(covidGlyphMap.ct.findColour(PApplet.map(timeseries[currentWeek], 0, PApplet.max(this.timeseries), 0, 1)));
		covidGlyphMap.noStroke();
//		covidGlyphMap.rect(screenXCentre-screenWH/2,screenYCentre-screenWH/2,screenWH,screenWH);

		covidGlyphMap.beginShape();
		float[] coord=new float[6];
		PathIterator pi=shape.getPathIterator(null);
		while (!pi.isDone()) {
			pi.currentSegment(coord);
			covidGlyphMap.vertex(coord[0],coord[1]);
			pi.next();
		}
		covidGlyphMap.endShape(PApplet.CLOSE);
		
		drawName(showAreaName, covidGlyphMap);
		
		return tooltip;
	}

	public void drawTileRelativeSymb(CovidGlyphMap covidGlyphMap) {
//		int localSumAcrossT=0;
//		for (int t=0;t<modelSums.length;t++)
//			for (int statusIdx=0;statusIdx<modelSums[t].length;statusIdx++)//use all statuses (to get whole population)
//				localSumAcrossT+=modelSums[t][statusIdx];
//		demographicGridmap.noFill();
//		demographicGridmap.stroke(0,0,200,100);
//		float w=PApplet.map(localSumAcrossT,0,demographicGridmap.colourScale2,0,screenWH);
//		demographicGridmap.ellipse(screenXCentre,screenYCentre,w,w);
	}

	public float getMaxForGlyph(AbsRel absRel) {
		return PApplet.max(timeseries);
	}

	public float getMaxForTransp(AbsRel absRel) {
		//max pop
//		float sum=0;
//		for (int j=0;j<10;j++)
//			for (int statusIdx=0;statusIdx<modelSums[0].length;statusIdx++)//use all statuses (to get whole population)
//				sum+=modelSums[j][statusIdx];
//		return sum;
		return PApplet.max(timeseries);
		}

}
