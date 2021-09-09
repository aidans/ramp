package org.gicentre.aidan.ramp;

import java.util.Collection;

import org.gicentre.aidan.ramp.DemographicGridmap.AbsRel;
import org.gicentre.aidan.ramp.DemographicGridmap.Record;

import processing.core.PApplet;

public class TileModelAgeAnim extends Tile {

	int[][] modelSums;
	
	public TileModelAgeAnim(int screenXCentre, int screenYCentre, int screenWH, Collection<Record> records, int numCompartments, int currentDatasetIdx, int currentDay){
		super(screenXCentre, screenYCentre, screenWH);

		modelSums=new int[10][numCompartments];
		for (Record record:records) {
			for (int j=0;j<10;j++) { 
				for (int statusIdx=0;statusIdx<numCompartments;statusIdx++) {
					modelSums[j][statusIdx]+=record.resultCounts[currentDatasetIdx][currentDay][j][statusIdx];
				}
			}
		}

	}
	
	public String drawTile(DemographicGridmap demographicGridmap, boolean showAreaName) {
		String tooltip=null;
		int localSum=0;
		for (int j=0;j<modelSums.length;j++)
			for (int k=0;k<modelSums[j].length;k++) {
				localSum+=modelSums[j][k];
			}
		//make background white
		demographicGridmap.fill(255);
		demographicGridmap.noStroke();
		demographicGridmap.rect(screenXCentre*screenWH-screenWH/2,screenYCentre*screenWH-screenWH/2,screenWH,screenWH);
		float h=(float)screenWH/10f;
		for (int j=0;j<10;j++) {
			float xPos=screenXCentre-screenWH/2+1;
			float yPos=screenYCentre-screenWH/2+1+j*h;
			int sum=0;
			for (int statusIdx=0;statusIdx<modelSums[0].length;statusIdx++) 
				sum+=modelSums[j][statusIdx];

			for (int statusIdx=0;statusIdx<modelSums[0].length;statusIdx++) {
				if (demographicGridmap.statusShow[statusIdx]) {
					float w;
					if (demographicGridmap.absRelChanger.getValueEnum()==AbsRel.Absolute)
						w=PApplet.constrain(PApplet.map((float)modelSums[j][statusIdx],0,demographicGridmap.colourScale,0,screenWH-2),0,screenWH-2);
					else
						w=PApplet.constrain(PApplet.map((float)modelSums[j][statusIdx],0,sum,0,screenWH-2),0,screenWH-2);
					int transp=255;
					if (demographicGridmap.absRelChanger.getValueEnum()==AbsRel.RelativeFade)
						transp=(int)PApplet.constrain(PApplet.map(localSum,0,demographicGridmap.colourScale2,0,255),0,255);
					demographicGridmap.fill(demographicGridmap.ctStatus.findColour(statusIdx+1),transp);
					demographicGridmap.rect(xPos,yPos,w,h);
					if (demographicGridmap.mouseX>xPos && demographicGridmap.mouseX<=xPos+w && demographicGridmap.mouseY>yPos && demographicGridmap.mouseY<yPos+h)
						tooltip=modelSums[j][statusIdx]+" ("+(int)(modelSums[j][statusIdx]/(float)sum*100)+"%) people aged "+(j*10)+"-"+((j+1)*10-1)+" are "+demographicGridmap.statuses[statusIdx];
					xPos+=w;
				}
			}
		}
		drawName(showAreaName, demographicGridmap);
		
		return tooltip;
	}

	public void drawTileRelativeSymb(DemographicGridmap demographicGridmap) {
		int localSumAcrossT=0;
		for (int t=0;t<modelSums.length;t++)
			for (int statusIdx=0;statusIdx<modelSums[t].length;statusIdx++)//use all statuses (to get whole population)
				localSumAcrossT+=modelSums[t][statusIdx];
		demographicGridmap.noFill();
		demographicGridmap.stroke(0,0,200,100);
		float w=PApplet.map(localSumAcrossT,0,demographicGridmap.colourScale2,0,screenWH);
		demographicGridmap.ellipse(screenXCentre,screenYCentre,w,w);
	}

	public float getMaxForGlyph(AbsRel absRel) {
		float colourScale=-Float.MAX_VALUE;
		for (int j=0;j<10;j++) {
			float sum=0;
			for (int statusIdx=0;statusIdx<modelSums[0].length;statusIdx++)//use all statuses (to get whole population)
				sum+=modelSums[j][statusIdx];
			colourScale=Math.max(colourScale,sum);
		}
		return colourScale;
	}

	public float getMaxForTransp(AbsRel absRell) {
		//max pop
		float sum=0;
		for (int j=0;j<10;j++)
			for (int statusIdx=0;statusIdx<modelSums[0].length;statusIdx++)//use all statuses (to get whole population)
				sum+=modelSums[j][statusIdx];
		return sum;
	}

}
