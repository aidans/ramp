package org.gicentre.aidan.ramp;

import java.awt.Rectangle;
import java.awt.Shape;
import java.text.DecimalFormat;
import java.util.Collection;

import org.gicentre.aidan.ramp.CovidGlyphMap.GridRecord;

import processing.core.PApplet;
import processing.core.PGraphics2D;
import processing.core.PGraphicsJava2D;

import org.gicentre.aidan.ramp.CovidGlyphMap.AbsRel;

public class TileCovidTimeseries extends CovidTile {

	float[] timeseries;
	int currentWeek;
	int pop;
	
	public TileCovidTimeseries(Shape shape, Collection<GridRecord> records, int currentWeek,int numWeeks){
		super(shape);

		timeseries=new float[numWeeks];
		this.currentWeek=currentWeek;
		
		for (GridRecord record:records) {
			for (int t=0;t<record.casesTimeseries.length;t++)
				timeseries[t]+=record.casesTimeseries[t];
			for (int genderIdx=0;genderIdx<2;genderIdx++)
				for (int j=0;j<record.pop[genderIdx].length;j++) 
					pop+=record.pop[genderIdx][j];
		}
	}
	
	
	public String drawTile(CovidGlyphMap covidGlyphMap, boolean showAreaName) {

		String tooltip=new DecimalFormat("#.0").format(timeseries[currentWeek]);
		
		Rectangle r=shape.getBounds();
		
		Shape oldClip=((PGraphicsJava2D)covidGlyphMap.g).g2.getClip();
		((PGraphicsJava2D)covidGlyphMap.g).g2.setClip(shape);
		
		//make white
		covidGlyphMap.noStroke();
		covidGlyphMap.fill(255);
		covidGlyphMap.rect(r.x,r.y,r.width,r.height);		
		
		if (covidGlyphMap.absRelChanger.getValueEnum()==AbsRel.RelativeFade) {
			covidGlyphMap.stroke(150,150,255,PApplet.constrain((int)PApplet.map(pop, 0, covidGlyphMap.colourScale2, 0, 255),0,255));
		}
		else
			covidGlyphMap.stroke(150,150,255);
		covidGlyphMap.noFill();
		covidGlyphMap.beginShape();
		for (int t=0;t<timeseries.length;t++) {
			float y=0;
			if (covidGlyphMap.absRelChanger.getValueEnum()==AbsRel.Absolute)
				y=PApplet.map(this.timeseries[t],0,covidGlyphMap.colourScale,r.y+r.height-1,r.y+1);
			else
				y=PApplet.map(this.timeseries[t],0,PApplet.max(this.timeseries),r.y+r.height-1,r.y+1);
			covidGlyphMap.vertex(PApplet.map(t,0,timeseries.length,r.x-1,r.x+r.width+1),y);
		}
		covidGlyphMap.endShape();

		drawName(showAreaName, covidGlyphMap);
		
		((PGraphicsJava2D)covidGlyphMap.g).g2.setClip(oldClip);
		
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
		//total pop
		return pop;
	}
}
