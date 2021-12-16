package org.gicentre.aidan.ramp;

import java.util.Collection;

import org.gicentre.aidan.ramp.DemographicGridmap.AbsRel;
import org.gicentre.aidan.ramp.DemographicGridmap.Record;

import processing.core.PApplet;

public class TileModelComparison extends RampTile {

	int[][] modelDifferences;
	int popSums;
	
	public TileModelComparison(int screenXCentre, int screenYCentre, int screenWH, Collection<Record> records, int numDays, int numStatuses, int currentDatasetIdx, int currentBaselineDatasetIdx){
		super(screenXCentre, screenYCentre, screenWH);

		modelDifferences=new int[numDays][numStatuses];
		for (Record record: records) {
			for (int statusIdx=0;statusIdx<numStatuses;statusIdx++) { 
				for (int t=0;t<numDays;t++) {
					for (int j=0;j<10;j++) { 
						modelDifferences[t][statusIdx]+=record.resultCounts[currentDatasetIdx][t][j][statusIdx]-record.resultCounts[currentBaselineDatasetIdx][t][j][statusIdx];
						if (t==0)//just the first stepstep so we get the population of one
							popSums+=record.resultCounts[currentDatasetIdx][t][j][statusIdx];
					}
				}
			}
		}
	}
	
	public String drawTile(DemographicGridmap demographicGridmap, boolean showAreaName) {
		String tooltip=null;

		final AbsRel absRel=demographicGridmap.absRelChanger.getValueEnum();
		final int numDays=modelDifferences.length;
		final int numStatuses=modelDifferences[0].length;
		final String datasetName=demographicGridmap.datasetChanger.getValue();
		final String comparisonDatasetName=demographicGridmap.comparisonDatasetChanger.getValue();
		
		//make background white
		demographicGridmap.fill(255);
		demographicGridmap.noStroke();
		demographicGridmap.rect(screenXCentre-screenWH/2,screenYCentre-screenWH/2,screenWH,screenWH);
		float w=(float)(screenWH-2)/numDays;
		for (int t=0;t<numDays;t++) {
			float xPos=screenXCentre-screenWH/2+1+t*w;
			float yPosPositive=screenYCentre;
			float yPosNegative=screenYCentre;
			//calc localsum across for the t
			for (int statusIdx=0;statusIdx<numStatuses;statusIdx++) {
				if (demographicGridmap.statusShow[statusIdx]) {
					float h=0;
					if (absRel==AbsRel.Absolute)
						h=PApplet.map((float)Math.abs(modelDifferences[t][statusIdx]),0,demographicGridmap.colourScale,0,screenWH-2);
					else
						if (popSums>0)
							h=PApplet.map(Math.abs((float)modelDifferences[t][statusIdx]/popSums),0,demographicGridmap.colourScale,0,screenWH-2);
					if (h>1) {
						int transp=255;
						if (absRel==AbsRel.RelativeFade)
							transp=(int)PApplet.constrain(PApplet.map(popSums,0,demographicGridmap.colourScale2,0,255),0,255);
						demographicGridmap.fill(demographicGridmap.ctStatus.findColour(statusIdx+1),transp);
						if (modelDifferences[t][statusIdx]>0) {
							demographicGridmap.rect(xPos,yPosPositive,w,-h);
							if (demographicGridmap.mouseX>xPos && demographicGridmap.mouseX<=xPos+w && demographicGridmap.mouseY>yPosPositive-h && demographicGridmap.mouseY<yPosPositive)
								tooltip=Math.abs(modelDifferences[t][statusIdx])+" ("+(int)Math.abs((modelDifferences[t][statusIdx]/(float)popSums*100))+"%) MORE people on day "+t+" are "+demographicGridmap.statuses[statusIdx]+" in "+datasetName+" than in "+comparisonDatasetName;
							yPosPositive-=h;
						}
						else {
							demographicGridmap.rect(xPos,yPosNegative,w,h);
							if (demographicGridmap.mouseX>xPos && demographicGridmap.mouseX<=xPos+w && demographicGridmap.mouseY>yPosNegative && demographicGridmap.mouseY<yPosNegative+h)
								tooltip=Math.abs(modelDifferences[t][statusIdx])+" ("+(int)Math.abs((modelDifferences[t][statusIdx]/(float)popSums*100))+"%) FEWER people on day "+t+" are "+demographicGridmap.statuses[statusIdx]+" in "+datasetName+" than in "+comparisonDatasetName;
							yPosNegative+=h;
						}
					}
				}
			}
		}

		drawName(showAreaName, demographicGridmap);

		return tooltip;
	}

	public void drawTileRelativeSymb(DemographicGridmap demographicGridmap) {
		demographicGridmap.noFill();
		demographicGridmap.stroke(0,0,200,100);
		float w=PApplet.map(popSums,0,demographicGridmap.colourScale2,0,screenWH);
		demographicGridmap.ellipse(screenXCentre,screenYCentre,w,w);
	}


	public float getMaxForGlyph(AbsRel absRel) {
		float max=-Float.MAX_VALUE;
		if (absRel==AbsRel.Absolute) {
			//use the max abs difference
			for (int t=0;t<modelDifferences.length;t++) { 
				float sum=0;
				for (int statusIdx=0;statusIdx<modelDifferences[t].length;statusIdx++)//use all statuses (to get whole population)
					sum+=Math.abs(modelDifferences[t][statusIdx]);//use abs
				max=Math.max(max,sum);
			}
		}
		else {
			//for RELATIVE, use the max difference/pop
			for (int t=0;t<modelDifferences.length;t++) { 
				float sum=0;
				for (int statusIdx=0;statusIdx<modelDifferences[t].length;statusIdx++)//use all statuses (to get whole population)
					if (popSums>0)
						sum+=Math.abs((float)modelDifferences[t][statusIdx]/popSums);//use abs and divide by pop
				max=Math.max(max,sum);
			}
		}		return max;
	}

	public float getMaxForTransp(AbsRel absRel) {
		float max=-Float.MAX_VALUE;
		if (absRel!=AbsRel.Absolute)
			max=Math.max(max,popSums);
		return max;
	}
	

}
