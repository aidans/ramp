package org.gicentre.aidan.ramp;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.PathIterator;

import org.gicentre.aidan.ramp.PeatGlyphMap.AbsRel;

import discretisation.Discretisation;
import processing.core.PApplet;

public abstract class PeatTile {
		
//	int screenXCentre,screenYCentre, screenWH;
	String name;
	Shape  shape;

	
	public PeatTile(Shape shape){
//		this.screenXCentre=screenXCentre;
//		this.screenYCentre=screenYCentre;
//		this.screenWH=screenWH;
		this.shape=shape;
	}
	
	public abstract String drawTile(PeatGlyphMap peatGlyphMap, boolean showAreaName);
	
	public abstract void drawTileRelativeSymb(PeatGlyphMap peatGlyphMap);
	
	public void drawOutlines(PeatGlyphMap peatGlyphMap) {
		peatGlyphMap.pushStyle();
		peatGlyphMap.g.strokeWeight=1;
		peatGlyphMap.stroke(230);
		peatGlyphMap.noFill();

		peatGlyphMap.beginShape();
		float[] coord=new float[6];
		PathIterator pi=shape.getPathIterator(null);
		while (!pi.isDone()) {
			pi.currentSegment(coord);
			peatGlyphMap.vertex(coord[0],coord[1]);
			pi.next();
		}
		peatGlyphMap.endShape(PApplet.CLOSE);
		
		peatGlyphMap.popStyle();
	}

	public abstract float getMaxForGlyph(AbsRel absRel);
	
	public abstract float getMaxForTransp(AbsRel absRell);
	
	public void drawName(boolean showAreaName, PeatGlyphMap covidGlyphMap) {
		if (showAreaName && this.name!=null) {
			covidGlyphMap.fill(0,80);
			covidGlyphMap.textSize(12);
			covidGlyphMap.textAlign(PApplet.CENTER,PApplet.CENTER);
			Rectangle r=shape.getBounds();
			covidGlyphMap.text(name,r.x,r.y,r.width,r.height);
		}

	}
}
