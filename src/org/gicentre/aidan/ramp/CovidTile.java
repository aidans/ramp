package org.gicentre.aidan.ramp;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.PathIterator;

import org.gicentre.aidan.ramp.CovidGlyphMap.AbsRel;

import discretisation.Discretisation;
import processing.core.PApplet;

public abstract class CovidTile {
		
//	int screenXCentre,screenYCentre, screenWH;
	String name;
	Shape  shape;

	
	public CovidTile(Shape shape){
//		this.screenXCentre=screenXCentre;
//		this.screenYCentre=screenYCentre;
//		this.screenWH=screenWH;
		this.shape=shape;
	}
	
	public abstract String drawTile(CovidGlyphMap covidGlyphMap, boolean showAreaName);
	
	public abstract void drawTileRelativeSymb(CovidGlyphMap covidGlyphMap);
	
	public void drawOutlines(CovidGlyphMap covidGlyphMap) {
		covidGlyphMap.pushStyle();
		covidGlyphMap.g.strokeWeight=1;
		covidGlyphMap.stroke(230);
		covidGlyphMap.noFill();

		covidGlyphMap.beginShape();
		float[] coord=new float[6];
		PathIterator pi=shape.getPathIterator(null);
		while (!pi.isDone()) {
			pi.currentSegment(coord);
			covidGlyphMap.vertex(coord[0],coord[1]);
			pi.next();
		}
		covidGlyphMap.endShape(PApplet.CLOSE);
		
		covidGlyphMap.popStyle();
	}

	public abstract float getMaxForGlyph(AbsRel absRel);
	
	public abstract float getMaxForTransp(AbsRel absRell);
	
	public void drawName(boolean showAreaName, CovidGlyphMap covidGlyphMap) {
		if (showAreaName && this.name!=null) {
			covidGlyphMap.fill(0,80);
			covidGlyphMap.textSize(12);
			covidGlyphMap.textAlign(PApplet.CENTER,PApplet.CENTER);
			Rectangle r=shape.getBounds();
			covidGlyphMap.text(name,r.x,r.y,r.width,r.height);
		}

	}
}
