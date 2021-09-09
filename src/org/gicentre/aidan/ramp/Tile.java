package org.gicentre.aidan.ramp;

import org.gicentre.aidan.ramp.DemographicGridmap.AbsRel;

import processing.core.PApplet;

public abstract class Tile {
	
	static String mapTitle;
	
	int screenXCentre,screenYCentre, screenWH;
	String name;

	
	public Tile(int screenXCentre, int screenYCentre, int screenWH){
		this.screenXCentre=screenXCentre;
		this.screenYCentre=screenYCentre;
		this.screenWH=screenWH;
	}
	
	public abstract String drawTile(DemographicGridmap demographicGridmap, boolean showAreaName);
	
	public abstract void drawTileRelativeSymb(DemographicGridmap demographicGridmap);
	
	public abstract void drawOutlines(DemographicGridmap demographicGridmap);
	
	public abstract float getMaxForGlyph(AbsRel absRel);
	
	public abstract float getMaxForTransp(AbsRel absRell);
	
	public void drawName(boolean showAreaName, DemographicGridmap demographicGridmap) {
		if (showAreaName && this.name!=null) {
			demographicGridmap.fill(0,80);
			demographicGridmap.textSize(12);
			demographicGridmap.textAlign(PApplet.CENTER,PApplet.CENTER);
			demographicGridmap.text(name,screenXCentre-screenWH/2,screenYCentre-screenWH/2,screenWH,screenWH);
		}

	}
}
