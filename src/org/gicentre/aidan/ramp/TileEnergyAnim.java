package org.gicentre.aidan.ramp;

import java.applet.Applet;
import java.text.DecimalFormat;
import java.util.Collection;

import org.gicentre.aidan.ramp.EnergyGlyphMap.GridRecord;

import processing.core.PApplet;

import org.gicentre.aidan.ramp.EnergyGlyphMap.AbsRel;

public class TileEnergyAnim extends EnergyTile {

	float[] timeseries;
	int currentWeek;
	
	public TileEnergyAnim(int screenXCentre, int screenYCentre, int screenWH, Collection<GridRecord> records, int currentWeek,int numWeeks){
		super(screenXCentre, screenYCentre, screenWH);

		timeseries=new float[numWeeks];
		this.currentWeek=currentWeek;
		
		for (GridRecord record:records)
			for (int t=0;t<record.timeseries.length;t++)
				timeseries[t]+=record.timeseries[t];
	}
	
	
	public String drawTile(EnergyGlyphMap energyGlyphMap, boolean showAreaName) {
		String tooltip=new DecimalFormat("#.0").format(timeseries[currentWeek]);
		if (energyGlyphMap.absRelChanger.getValueEnum()==AbsRel.Absolute)
			energyGlyphMap.fill(energyGlyphMap.ct.findColour(PApplet.map(timeseries[currentWeek], 0, energyGlyphMap.colourScale, 0, 1)));
		else
			energyGlyphMap.fill(energyGlyphMap.ct.findColour(PApplet.map(timeseries[currentWeek], 0, PApplet.max(this.timeseries), 0, 1)));
		energyGlyphMap.noStroke();
		energyGlyphMap.rect(screenXCentre-screenWH/2,screenYCentre-screenWH/2,screenWH,screenWH);
//		float h=(float)screenWH/10f;
//		for (int j=0;j<10;j++) {
//			float xPos=screenXCentre-screenWH/2+1;
//			float yPos=screenYCentre-screenWH/2+1+j*h;
//			int sum=0;
//			for (int statusIdx=0;statusIdx<modelSums[0].length;statusIdx++) 
//				sum+=modelSums[j][statusIdx];
//
//			for (int statusIdx=0;statusIdx<modelSums[0].length;statusIdx++) {
//				if (demographicGridmap.statusShow[statusIdx]) {
//					float w;
//					if (demographicGridmap.absRelChanger.getValueEnum()==AbsRel.Absolute)
//						w=PApplet.constrain(PApplet.map((float)modelSums[j][statusIdx],0,demographicGridmap.colourScale,0,screenWH-2),0,screenWH-2);
//					else
//						w=PApplet.constrain(PApplet.map((float)modelSums[j][statusIdx],0,sum,0,screenWH-2),0,screenWH-2);
//					int transp=255;
//					if (demographicGridmap.absRelChanger.getValueEnum()==AbsRel.RelativeFade)
//						transp=(int)PApplet.constrain(PApplet.map(localSum,0,demographicGridmap.colourScale2,0,255),0,255);
//					demographicGridmap.fill(demographicGridmap.ctStatus.findColour(statusIdx+1),transp);
//					demographicGridmap.rect(xPos,yPos,w,h);
//					if (demographicGridmap.mouseX>xPos && demographicGridmap.mouseX<=xPos+w && demographicGridmap.mouseY>yPos && demographicGridmap.mouseY<yPos+h)
//						tooltip=modelSums[j][statusIdx]+" ("+(int)(modelSums[j][statusIdx]/(float)sum*100)+"%) people aged "+(j*10)+"-"+((j+1)*10-1)+" are "+demographicGridmap.statuses[statusIdx];
//					xPos+=w;
//				}
//			}
//		}
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
