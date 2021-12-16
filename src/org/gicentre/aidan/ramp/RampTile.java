package org.gicentre.aidan.ramp;

import org.gicentre.aidan.ramp.DemographicGridmap.AbsRel;

import processing.core.PApplet;

public abstract class RampTile {
		
	int screenXCentre,screenYCentre, screenWH;
	String name;

	
	public RampTile(int screenXCentre, int screenYCentre, int screenWH){
		this.screenXCentre=screenXCentre;
		this.screenYCentre=screenYCentre;
		this.screenWH=screenWH;
	}
	
	public abstract String drawTile(DemographicGridmap demographicGridmap, boolean showAreaName);
	
	public abstract void drawTileRelativeSymb(DemographicGridmap demographicGridmap);
	
	public void drawOutlines(DemographicGridmap demographicGridmap) {
		demographicGridmap.pushStyle();
		demographicGridmap.g.strokeWeight=1;
		demographicGridmap.stroke(100);
		demographicGridmap.noFill();
		demographicGridmap.rect(screenXCentre-screenWH/2, screenYCentre-screenWH/2, screenWH, screenWH);
		demographicGridmap.popStyle();
	}

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
