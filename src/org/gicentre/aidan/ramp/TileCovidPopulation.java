package org.gicentre.aidan.ramp;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.util.Collection;

import org.gicentre.aidan.ramp.CovidGlyphMap.AbsRel;
import org.gicentre.aidan.ramp.CovidGlyphMap.GridRecord;
import org.gicentre.utils.colour.ColourTable;

import processing.core.PApplet;
import processing.core.PGraphicsJava2D;


public class TileCovidPopulation extends CovidTile {

	static String mapTitle="Population of residents by age group (younger at top)";
	static ColourTable ctDemog=ColourTable.getPresetColourTable(ColourTable.PURPLES);

	
	int[][] demogSums;//gender (0=male, 1=female) then ageband
	
	public TileCovidPopulation(Shape shape, Collection<GridRecord> records, int numDemographicCategories, int attribBinSize){
		super(shape);
		this.demogSums=new int[2][numDemographicCategories/attribBinSize];
		for (GridRecord record:records)
			for (int genderIdx=0;genderIdx<2;genderIdx++)
				for (int j=0;j<numDemographicCategories;j++)
				if (j/attribBinSize<demogSums[genderIdx].length)
					demogSums[genderIdx][j/attribBinSize]+=record.pop[genderIdx][j];
	}

	public String drawTile(CovidGlyphMap covidGlyphMap, boolean showAreaName) {
		String tooltip=null;

		int localSum=0;
		for (int i=0;i<2;i++)
			for (int j=0;j<demogSums[i].length;j++)
				if (demogSums[i][j]>localSum)
					localSum=demogSums[i][j];

		Shape oldClip=((PGraphicsJava2D)covidGlyphMap.g).g2.getClip();
		((PGraphicsJava2D)covidGlyphMap.g).g2.setClip(shape);
		
		//make background white
		covidGlyphMap.fill(255);
		covidGlyphMap.noStroke();

		covidGlyphMap.beginShape();
		float[] coord=new float[6];
		PathIterator pi=shape.getPathIterator(null);
		while (!pi.isDone()) {
			pi.currentSegment(coord);
			covidGlyphMap.vertex(coord[0],coord[1]);
			pi.next();
		}
		covidGlyphMap.endShape(PApplet.CLOSE);
		
		Rectangle r=shape.getBounds();

		//draw glyphs
			for (int j=0;j<demogSums[0].length;j++) {
				float w;
				if (covidGlyphMap.absRelChanger.getValueEnum()==AbsRel.Absolute) 
					w=PApplet.constrain(PApplet.map((float)demogSums[0][j]+(float)demogSums[1][j],0,covidGlyphMap.colourScale,0,r.width/2),0,r.width-2);
				else
					w=PApplet.constrain(PApplet.map((float)demogSums[0][j]+(float)demogSums[1][j],0,localSum,0,r.width-2),0,r.width-2);
				int transp=255;
				if (covidGlyphMap.absRelChanger.getValueEnum()==AbsRel.RelativeFade)
					transp=(int)PApplet.constrain(PApplet.map(localSum,0,covidGlyphMap.colourScale2,0,255),0,255);
				covidGlyphMap.fill(ctDemog.findColour(0.6f),transp);
				covidGlyphMap.rect((int)r.getCenterX()-w/2,r.y+j*((float)(r.width-2)/demogSums[0].length),w,((float)(r.height-2)/demogSums[0].length));
				if (r.contains(covidGlyphMap.mouseX,covidGlyphMap.mouseY))
					tooltip=(demogSums[0][j]+demogSums[1][j])+" people";
			}
		
		drawName(showAreaName, covidGlyphMap);
			
		((PGraphicsJava2D)covidGlyphMap.g).g2.setClip(oldClip);
		
		return tooltip;
	}
				
	public void drawTileRelativeSymb(CovidGlyphMap covidGlyphMap) {
		if (covidGlyphMap.absRelChanger.getValueEnum()==AbsRel.RelativeSymb) {
			int localSum=0;
			for (int genderIdx=0;genderIdx<2;genderIdx++)
				for (int j=0;j<demogSums[genderIdx].length;j++)
					if (demogSums[genderIdx][j]>localSum)
						localSum=demogSums[genderIdx][j];
			covidGlyphMap.noFill();
			covidGlyphMap.stroke(0,0,200,100);
			Rectangle r=shape.getBounds();
			float w=PApplet.map(localSum,0,covidGlyphMap.colourScale2,0,r.width);
			covidGlyphMap.ellipse((int)r.getCenterX(),(int)r.getCenterY(),w,w);
		}
	}

	public float getMaxForGlyph(AbsRel absRel) {
		float max=0;
		for (int genderIdx=0;genderIdx<2;genderIdx++)
			for (int j=0;j<demogSums[genderIdx].length;j++) 
				max=Math.max(max,(float)(demogSums[genderIdx][j]));
		return max;
	}
	
	public float getMaxForTransp(AbsRel absRel) {
		int localSum=0;
		for (int genderIdx=0;genderIdx<2;genderIdx++)
			for (int j=0;j<demogSums.length;j++) {
				localSum=demogSums[genderIdx][j];
		}
		return localSum;

	}
	
}
