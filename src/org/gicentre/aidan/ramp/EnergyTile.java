package org.gicentre.aidan.ramp;

import org.gicentre.aidan.ramp.EnergyGlyphMap.AbsRel;

import processing.core.PApplet;

public abstract class EnergyTile {
		
	int screenXCentre,screenYCentre, screenWH;
	String name;

	
	public EnergyTile(int screenXCentre, int screenYCentre, int screenWH){
		this.screenXCentre=screenXCentre;
		this.screenYCentre=screenYCentre;
		this.screenWH=screenWH;
	}
	
	public abstract String drawTile(EnergyGlyphMap energyGlyphMap, boolean showAreaName);
	
	public abstract void drawTileRelativeSymb(EnergyGlyphMap energyGlyphMap);
	
	public void drawOutlines(EnergyGlyphMap energyGlyphMap) {
		energyGlyphMap.pushStyle();
		energyGlyphMap.g.strokeWeight=1;
		energyGlyphMap.stroke(100);
		energyGlyphMap.noFill();
		energyGlyphMap.rect(screenXCentre-screenWH/2, screenYCentre-screenWH/2, screenWH, screenWH);
		energyGlyphMap.popStyle();
	}

	public abstract float getMaxForGlyph(AbsRel absRel);
	
	public abstract float getMaxForTransp(AbsRel absRell);
	
	public void drawName(boolean showAreaName, EnergyGlyphMap energyGlyphMap) {
		if (showAreaName && this.name!=null) {
			energyGlyphMap.fill(0,80);
			energyGlyphMap.textSize(12);
			energyGlyphMap.textAlign(PApplet.CENTER,PApplet.CENTER);
			energyGlyphMap.text(name,screenXCentre-screenWH/2,screenYCentre-screenWH/2,screenWH,screenWH);
		}

	}
}
