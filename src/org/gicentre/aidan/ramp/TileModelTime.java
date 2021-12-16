package org.gicentre.aidan.ramp;

import java.util.Collection;

import org.gicentre.aidan.ramp.DemographicGridmap.AbsRel;
import org.gicentre.aidan.ramp.DemographicGridmap.Record;

import processing.core.PApplet;

public class TileModelTime extends RampTile {

	int[][] modelSums;
	
	public TileModelTime(int screenXCentre, int screenYCentre, int screenWH, Collection<Record> records, int numDays, int numCompartments, int currentDatasetIdx){
		super(screenXCentre, screenYCentre, screenWH);

		modelSums=new int[numDays][numCompartments];
		for (Record record:records) {
			for (int statusIdx=0;statusIdx<numCompartments;statusIdx++)
				for (int t=0;t<numDays;t++) 
					for (int j=0;j<10;j++) { 
						modelSums[t][statusIdx]+=record.resultCounts[currentDatasetIdx][t][j][statusIdx];
			if (modelSums[t][statusIdx]<0)
				System.out.println(modelSums[t][statusIdx]);
					}
		}

	}
	
	public String drawTile(DemographicGridmap demographicGridmap, boolean showAreaName) {
		String tooltip=null;
		//make background white
		demographicGridmap.fill(255);
		demographicGridmap.noStroke();
		demographicGridmap.rect(screenXCentre-screenWH/2,screenYCentre-screenWH/2,screenWH,screenWH);
		float w=(float)(screenWH-2)/modelSums.length;
		//calc localsum across t
		int localSumAcrossT=0;
		for (int t=0;t<modelSums.length;t++)
			for (int statusIdx=0;statusIdx<modelSums[t].length;statusIdx++)
				if (demographicGridmap.statusShow[statusIdx]) 
					localSumAcrossT+=modelSums[t][statusIdx];
		for (int t=0;t<modelSums.length;t++) {
			float xPos=screenXCentre-screenWH/2+1+t*w;
			float yPos=screenYCentre-screenWH/2+screenWH;
			//calc localsum across for the t
			int localSum=0;
			int pop=0;
			for (int statusIdx=0;statusIdx<modelSums[0].length;statusIdx++) {
				pop+=modelSums[t][statusIdx];
				if (demographicGridmap.statusShow[statusIdx]) 
					localSum+=modelSums[t][statusIdx];

			}
			for (int statusIdx=0;statusIdx<modelSums[t].length;statusIdx++) {
				if (demographicGridmap.statusShow[statusIdx]) {
					float h=0;
					if (demographicGridmap.absRelChanger.getValueEnum()==AbsRel.Absolute)
						h=PApplet.constrain(PApplet.map((float)modelSums[t][statusIdx],0,demographicGridmap.colourScale,0,screenWH-2),0,screenWH-2);
					else
						h=PApplet.constrain(PApplet.map((float)modelSums[t][statusIdx],0,localSum,0,screenWH-2),0,screenWH-2);
					int transp=255;
					if (demographicGridmap.absRelChanger.getValueEnum()==AbsRel.RelativeFade)
						transp=(int)PApplet.constrain(PApplet.map(localSumAcrossT,0,demographicGridmap.colourScale2,0,255),0,255);
					demographicGridmap.fill(demographicGridmap.ctStatus.findColour(statusIdx+1),transp);
					demographicGridmap.rect(xPos,yPos,w,-h);
					if (demographicGridmap.mouseX>xPos && demographicGridmap.mouseX<=xPos+w && demographicGridmap.mouseY>yPos-h && demographicGridmap.mouseY<=yPos)
						tooltip=modelSums[t][statusIdx]+" ("+(int)(modelSums[t][statusIdx]/(float)pop*100)+"%) people on day "+t+" are "+demographicGridmap.statuses[statusIdx];
					yPos-=h;
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
		if (absRel==AbsRel.Absolute) {
			for (int t=0;t<modelSums.length;t++) { 
				float sum=0;
				for (int statusIdx=0;statusIdx<modelSums[t].length;statusIdx++)//use all statuses (to get whole population)
					sum+=modelSums[t][statusIdx];
				colourScale=Math.max(colourScale,sum);
			}
		}
		return colourScale;
	}

	public float getMaxForTransp(AbsRel absRell) {
		float sum=0;
		for (int statusIdx=0;statusIdx<modelSums[0].length;statusIdx++)//use all statuses (to get whole population)
			for (int t=0;t<modelSums.length;t++) 
				sum+=modelSums[t][statusIdx];
		return sum;
	}

}
