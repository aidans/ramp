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

public class TileCovidRadialTimeseries extends CovidTile {

	float[] timeseries;
	int currentWeek;
	int pop;
	
	public TileCovidRadialTimeseries(Shape shape, Collection<GridRecord> records, int currentWeek,int numWeeks){
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

		covidGlyphMap.pushStyle();
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
		float angleInc=PApplet.TWO_PI/timeseries.length;
		for (int t=0;t<timeseries.length;t++) {
			float radius=0;
			if (covidGlyphMap.absRelChanger.getValueEnum()==AbsRel.Absolute)
				radius=PApplet.map(this.timeseries[t],0,covidGlyphMap.colourScale,r.width/2/6f,r.width/2);
//				radius=PApplet.map(this.timeseries[t],0,covidGlyphMap.colourScale,0,r.width/2);
			else
				radius=PApplet.map(this.timeseries[t],0,PApplet.max(this.timeseries),r.width/6/6f,r.width/2);
//				radius=PApplet.map(this.timeseries[t],0,PApplet.max(this.timeseries),0,r.width/2);
			
			covidGlyphMap.vertex((float)r.getCenterX()+radius*(float)Math.cos(angleInc*t),(float)r.getCenterY()+radius*(float)Math.sin(angleInc*t));
			
		}
		covidGlyphMap.endShape();

		//current timestep
		{
//			float radius=0;
//			if (covidGlyphMap.absRelChanger.getValueEnum()==AbsRel.Absolute)
//				radius=PApplet.map(this.timeseries[currentWeek],0,covidGlyphMap.colourScale,r.width/2/4f,r.width/2);
//			else
//				radius=PApplet.map(this.timeseries[currentWeek],0,PApplet.max(this.timeseries),r.width/2/4f,r.width/2);

//			covidGlyphMap.noStroke();
//			covidGlyphMap.fill(0,0,150,100);
//			covidGlyphMap.ellipse((float)r.getCenterX()+radius*(float)Math.cos(angleInc*currentWeek),(float)r.getCenterY()+radius*(float)Math.sin(angleInc*currentWeek),2,2);

			covidGlyphMap.stroke(150,100);
			float radius=r.width*2;
			covidGlyphMap.line((float)r.getCenterX(),(float)r.getCenterY(),(float)r.getCenterX()+radius*(float)Math.cos(angleInc*currentWeek),(float)r.getCenterY()+radius*(float)Math.sin(angleInc*currentWeek));
		}


		covidGlyphMap.popStyle();		
		
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
