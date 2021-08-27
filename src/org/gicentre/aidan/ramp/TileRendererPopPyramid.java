package org.gicentre.aidan.ramp;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import org.gicentre.aidan.ramp.DemographicGridmap.AbsRel;
import org.gicentre.utils.colour.ColourTable;

import processing.core.PApplet;
import processing.core.PGraphics;

public class TileRendererPopPyramid {

	static PGraphics canvas;
	static int mouseX,mouseY;
	static int attribBinSize;
	static AbsRel absRel;
	static ColourTable ctDemog;
	static Float colourScale,colourScale2;
	
	int screenXCentre,screenYCentre, screenWH;
	int[] demogSums;
	String name;
	
	static void setup(PGraphics canvas,int mouseX,int mouseY, int attribBinSize, AbsRel absRel, ColourTable ctDemog, Float colourScale, Float colourScale2) {
		TileRendererPopPyramid.canvas=canvas;
		TileRendererPopPyramid.mouseX=mouseX;
		TileRendererPopPyramid.mouseY=mouseY;
		TileRendererPopPyramid.attribBinSize=attribBinSize;
		TileRendererPopPyramid.absRel=absRel;
		TileRendererPopPyramid.ctDemog=ctDemog;
		TileRendererPopPyramid.colourScale=colourScale;
		TileRendererPopPyramid.colourScale2=colourScale2;
		
	}
	
	public TileRendererPopPyramid(int screenXCentre, int screenYCentre, int screenWH, int[] demogSums){
		this.screenXCentre=screenXCentre;
		this.screenYCentre=screenYCentre;
		this.screenWH=screenWH;
		this.demogSums=demogSums;
	}
	
	public String drawTile(boolean showAreaName) {
		String tooltip=null;
		//DRAW
		int localSum=0;
		for (int j=0;j<demogSums.length;j++)
			if (demogSums[j]>localSum)
				localSum=demogSums[j];
		//make background white
		canvas.fill(255);
		canvas.noStroke();
		canvas.rect(screenXCentre-screenWH/2,screenYCentre-screenWH/2,screenWH,screenWH);
		//draw glyphs
		for (int j=0;j<demogSums.length;j++) {
			float w;
			if (absRel==AbsRel.Absolute) 
				w=PApplet.constrain(PApplet.map((float)demogSums[j],0,colourScale,0,screenWH-2),0,screenWH-2);
			else
				w=PApplet.constrain(PApplet.map((float)demogSums[j],0,localSum,0,screenWH-2),0,screenWH-2);
			int transp=255;
			if (absRel==AbsRel.RelativeFade)
				transp=(int)PApplet.constrain(PApplet.map(localSum,0,colourScale2,0,255),0,255);
			canvas.fill(ctDemog.findColour(0.6f),transp);
			Rectangle2D r=new Rectangle.Float(screenXCentre-w/2,screenYCentre-screenWH/2+1+j*((float)(screenWH-2)/demogSums.length),w,((float)(screenWH-2)/demogSums.length));
			canvas.rect((float)r.getX(),(float)r.getY(),(float)r.getWidth(),(float)r.getHeight());
			if (r.contains(mouseX,mouseY))
				tooltip=demogSums[j]+" people";
		}
		if (showAreaName && this.name!=null) {
			canvas.fill(0,80);
			canvas.textSize(12);
			canvas.textAlign(PApplet.CENTER,PApplet.CENTER);
			canvas.text(name,screenXCentre-screenWH/2,screenYCentre-screenWH/2,screenWH,screenWH);
		}
		return tooltip;
	}
				
	public void drawTileRelativeSymb() {
		if (absRel==AbsRel.RelativeSymb) {
			int localSum=0;
			for (int j=0;j<demogSums.length;j++)
				if (demogSums[j]>localSum)
					localSum=demogSums[j];
			canvas.noFill();
			canvas.stroke(0,0,200,100);
			float w=PApplet.map(localSum,0,colourScale2,0,screenWH);
			canvas.ellipse(screenXCentre,screenYCentre,w,w);
		}
	}

	public void drawOutlines() {
		canvas.pushStyle();
		canvas.strokeWeight=1;
		canvas.stroke(100);
		canvas.noFill();
		canvas.rect(screenXCentre-screenWH/2, screenYCentre-screenWH/2, screenWH, screenWH);
		canvas.popStyle();
	}
}
