package org.gicentre.aidan.ramp;

import java.text.DecimalFormat;
import java.util.Collection;

import org.gicentre.aidan.ramp.EnergyGlyphMap.GridRecord;

import processing.core.PApplet;

import org.gicentre.aidan.ramp.EnergyGlyphMap.AbsRel;

public class TileEnergyTimeseries extends EnergyTile {

	float[] timeseries;
	boolean[] exceeds;
	int currentWeek;
	
	public TileEnergyTimeseries(int screenXCentre, int screenYCentre, int screenWH, Collection<GridRecord> records, int currentWeek,int numWeeks){
		super(screenXCentre, screenYCentre, screenWH);

		timeseries=new float[numWeeks];
		exceeds=new boolean[numWeeks];
		this.currentWeek=currentWeek;
		
		for (GridRecord record:records)
			for (int t=0;t<record.timeseries.length;t++) {
				this.timeseries[t]+=record.timeseries[t];
				if (record.exceeds[t])
					this.exceeds[t]=true;
			}
	}
	
	
	public String drawTile(EnergyGlyphMap energyGlyphMap, boolean showAreaName) {

		String tooltip="";
		if (energyGlyphMap.mouseX>screenXCentre-screenWH/2 && energyGlyphMap.mouseY>screenYCentre-screenWH/2 && energyGlyphMap.mouseX>screenXCentre+screenWH/2 && energyGlyphMap.mouseY<screenYCentre+screenWH/2)
			tooltip=new DecimalFormat("#.0").format(timeseries[currentWeek]);
		
		//make white
		energyGlyphMap.noStroke();
		energyGlyphMap.fill(255);
		energyGlyphMap.rect(screenXCentre-screenWH/2,screenYCentre-screenWH/2,screenWH,screenWH);		
		
		if (PApplet.max(timeseries)==0)
			return "";

		//exceeds
		energyGlyphMap.noStroke();
		for (int t=0;t<timeseries.length;t++) {
			if (exceeds[t]) {
				energyGlyphMap.fill(255,200,200);
				float x1=PApplet.map(t,0,timeseries.length,screenXCentre-screenWH/2+1,screenXCentre+screenWH/2-1);
				float x2=PApplet.map(t+1,0,timeseries.length,screenXCentre-screenWH/2+1,screenXCentre+screenWH/2-1);
				energyGlyphMap.rect(x1,screenYCentre-screenWH/2,x2-x1,screenWH);
			}
		}
		
		//sparklines
		energyGlyphMap.stroke(0,0,200);
		energyGlyphMap.noFill();
		energyGlyphMap.beginShape();
		for (int t=0;t<timeseries.length;t++) {
			float y=0;
			if (energyGlyphMap.absRelChanger.getValueEnum()==AbsRel.Absolute)
				y=PApplet.map(this.timeseries[t],0,energyGlyphMap.colourScale,screenYCentre+screenWH/2-1,screenYCentre-screenWH/2+1);
			else
				y=PApplet.map(this.timeseries[t],0,PApplet.max(this.timeseries),screenYCentre+screenWH/2-1,screenYCentre-screenWH/2+1);
			energyGlyphMap.vertex(PApplet.map(t,0,timeseries.length,screenXCentre-screenWH/2+1,screenXCentre+screenWH/2-1),y);
		}
		energyGlyphMap.endShape();

		drawName(showAreaName, energyGlyphMap);
		
		return tooltip;
	}

	public void drawTileRelativeSymb(EnergyGlyphMap energyGlyphMap) {
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
