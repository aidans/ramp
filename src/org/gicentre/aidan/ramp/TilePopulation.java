package org.gicentre.aidan.ramp;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.Collection;

import org.gicentre.aidan.ramp.DemographicGridmap.AbsRel;
import org.gicentre.utils.colour.ColourTable;

import processing.core.PApplet;
import processing.core.PGraphics;

import org.gicentre.aidan.ramp.DemographicGridmap.Record;

public class TilePopulation extends RampTile {

	static String mapTitle="Population of residents by age group (younger at top)";
	static ColourTable ctDemog=ColourTable.getPresetColourTable(ColourTable.PURPLES);

	
	int[] demogSums;
	
	public TilePopulation(int screenXCentre, int screenYCentre, int screenWH, Collection<Record> records, int numDemographicCategories, int attribBinSize){
		super(screenXCentre, screenYCentre, screenWH);
		this.demogSums=new int[numDemographicCategories/attribBinSize];
		for (Record record:records)
			for (int j=0;j<numDemographicCategories;j++)
				if (j/attribBinSize<demogSums.length)
					demogSums[j/attribBinSize]+=record.popCounts[j];
	}

	public String drawTile(DemographicGridmap demographicGridmap, boolean showAreaName) {
		String tooltip=null;
		//DRAW
		int localSum=0;
		for (int j=0;j<demogSums.length;j++)
			if (demogSums[j]>localSum)
				localSum=demogSums[j];
		//make background white
		demographicGridmap.fill(255);
		demographicGridmap.noStroke();
		demographicGridmap.rect(screenXCentre-screenWH/2,screenYCentre-screenWH/2,screenWH,screenWH);
		//draw glyphs
		for (int j=0;j<demogSums.length;j++) {
			float w;
			if (demographicGridmap.absRelChanger.getValueEnum()==AbsRel.Absolute) 
				w=PApplet.constrain(PApplet.map((float)demogSums[j],0,demographicGridmap.colourScale,0,screenWH-2),0,screenWH-2);
			else
				w=PApplet.constrain(PApplet.map((float)demogSums[j],0,localSum,0,screenWH-2),0,screenWH-2);
			int transp=255;
			if (demographicGridmap.absRelChanger.getValueEnum()==AbsRel.RelativeFade)
				transp=(int)PApplet.constrain(PApplet.map(localSum,0,demographicGridmap.colourScale2,0,255),0,255);
			demographicGridmap.fill(ctDemog.findColour(0.6f),transp);
			Rectangle2D r=new Rectangle.Float(screenXCentre-w/2,screenYCentre-screenWH/2+1+j*((float)(screenWH-2)/demogSums.length),w,((float)(screenWH-2)/demogSums.length));
			demographicGridmap.rect((float)r.getX(),(float)r.getY(),(float)r.getWidth(),(float)r.getHeight());
			if (r.contains(demographicGridmap.mouseX,demographicGridmap.mouseY))
				tooltip=demogSums[j]+" people";
		}
		
		drawName(showAreaName, demographicGridmap);
			
		return tooltip;
	}
				
	public void drawTileRelativeSymb(DemographicGridmap demographicGridmap) {
		if (demographicGridmap.absRelChanger.getValueEnum()==AbsRel.RelativeSymb) {
			int localSum=0;
			for (int j=0;j<demogSums.length;j++)
				if (demogSums[j]>localSum)
					localSum=demogSums[j];
			demographicGridmap.noFill();
			demographicGridmap.stroke(0,0,200,100);
			float w=PApplet.map(localSum,0,demographicGridmap.colourScale2,0,screenWH);
			demographicGridmap.ellipse(screenXCentre,screenYCentre,w,w);
		}
	}

	public float getMaxForGlyph(AbsRel absRel) {
		float max=0;
		for (int j=0;j<demogSums.length;j++) 
			max=Math.max(max,(float)(demogSums[j]));
		return max;
	}
	
	public float getMaxForTransp(AbsRel absRell) {
		int localSum=0;
		for (int j=0;j<demogSums.length;j++) {
			if (demogSums[j]>localSum)
				localSum=demogSums[j];
		}
		return localSum;

	}
	
}
