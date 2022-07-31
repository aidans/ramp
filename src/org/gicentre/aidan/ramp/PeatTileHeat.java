package org.gicentre.aidan.ramp;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.util.Collection;
import java.util.HashSet;

import org.gicentre.aidan.ramp.PeatGlyphMap.AbsRel;
import org.gicentre.aidan.ramp.PeatGlyphMap.Record;
import org.gicentre.utils.colour.ColourTable;

import processing.core.PApplet;
import processing.core.PGraphicsJava2D;


public class PeatTileHeat extends PeatTile {

	static String mapTitle="Population of residents by age group (younger at top)";
	static ColourTable ctDemog=ColourTable.getPresetColourTable(ColourTable.PURPLES);

	
	int count;
	
	public PeatTileHeat(Shape shape, Collection<Record> records, int numDemographicCategories, int attribBinSize){
		super(shape);
		HashSet<Integer> ids=new HashSet<Integer>();
		for (Record record:records)
			ids.addAll(record.presentSpIds);
		count=ids.size();
	}

	public String drawTile(PeatGlyphMap peatGlyphMap, boolean showAreaName) {
		String tooltip=null;

		Shape oldClip=((PGraphicsJava2D)peatGlyphMap.g).g2.getClip();
		((PGraphicsJava2D)peatGlyphMap.g).g2.setClip(shape);
		
		peatGlyphMap.fill(ctDemog.findColour(count/peatGlyphMap.colourScale));
		peatGlyphMap.noStroke();
		//fill it!
		
		
		peatGlyphMap.beginShape();
		float[] coord=new float[6];
		PathIterator pi=shape.getPathIterator(null);
		while (!pi.isDone()) {
			pi.currentSegment(coord);
			peatGlyphMap.vertex(coord[0],coord[1]);
			pi.next();
		}
		peatGlyphMap.endShape(PApplet.CLOSE);
		
		Rectangle r=shape.getBounds();

			
		((PGraphicsJava2D)peatGlyphMap.g).g2.setClip(oldClip);
		
		return tooltip;
	}
				
	public void drawTileRelativeSymb(PeatGlyphMap peatGlyphMap) {
//		if (peatGlyphMap.absRelChanger.getValueEnum()==AbsRel.RelativeSymb) {
//			int localSum=0;
//			for (int genderIdx=0;genderIdx<2;genderIdx++)
//				for (int j=0;j<demogSums[genderIdx].length;j++)
//					if (demogSums[genderIdx][j]>localSum)
//						localSum=demogSums[genderIdx][j];
//			peatGlyphMap.noFill();
//			peatGlyphMap.stroke(0,0,200,100);
//			Rectangle r=shape.getBounds();
//			float w=PApplet.map(localSum,0,peatGlyphMap.colourScale2,0,r.width);
//			peatGlyphMap.ellipse((int)r.getCenterX(),(int)r.getCenterY(),w,w);
//		}
	}

	public float getMaxForGlyph(AbsRel absRel) {
//		float max=0;
//		for (int genderIdx=0;genderIdx<2;genderIdx++)
//			for (int j=0;j<demogSums[genderIdx].length;j++) 
//				max=Math.max(max,(float)(demogSums[genderIdx][j]));
//		return max;
		return count;
	}
	
	public float getMaxForTransp(AbsRel absRel) {
//		int localSum=0;
//		for (int genderIdx=0;genderIdx<2;genderIdx++)
//			for (int j=0;j<demogSums.length;j++) {
//				localSum=demogSums[genderIdx][j];
//		}
//		return localSum;
		return count;
	}

	
}
