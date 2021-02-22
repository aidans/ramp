# Meeting notes 


## 22th February 2021 (update, not meeting)

I was able to get OD commuter data for whole UK - from which I extracted Scotland - from the "WF01BUK" table from http://wicid.ukdataservice.ac.uk/cider/wicid/downloads.php from the "2011 Census United Kingdom - Safeguarded section".
 
<table width="100%">
<tr>
<td><img src="Screenshot 2021-02-22 at 12.17.29.png" width="250"/></td>
<td>OD map of commuting (home-work), using a logarithmic colour scaling. Outline colour indicates total number of origin (home) flows.</td>
</tr>
<tr>
<td><img src="Screenshot 2021-02-22 at 12.18.05.png" width="250"/></td>
<td>DO map of commuting (work-home).  Outline colour indicates total number of destination (work) flows.</td>
</tr>
</table>



## 12th February 2021 

Aidan and Claire met to talk about next steps. Claire is currently merging the COVID model code back into the original ecological model. Aidan likes the potential that some of the work we do could also be used in an ecological/biodivesity context.

It would be nice to write a paper about OD maps for assessing geographical similarity. Aidan's suggestions are:

- a [Visualization in Environmental Sciences](https://www.dropbox.com/s/mbeq1qorwa37777/CfP_C%26G_VisualisationInEnvironmentalSciences.pdf?dl=0) Special Issue in Computers & Graphics. Deadline:  30th July
- The [EnvirVis workshop](https://www.informatik.uni-leipzig.de/bsv/envirvis2021/). Deadline: 5th March

We also agreed that we would turn our focus to work on *model output comparisons*. One of the model parameters is the relative contributions of *spatial spread vs commuting*. Claire said that it is really difficult to assess the differences with such a high-resolution spatial model. We agreed to start to do some model comparisons to *directly inform model development*.

### Actions:
 - [ ] Richard and Claire: play with [SimilarityODMap-v1.3](https://github.com/aidans/ramp/releases/tag/SimilarityODMap-v1.3) and maybe start to think about a paper output (see above suggestions)
 - [ ] Claire to provide a couple of model outputs for comparison



## 29th January 2021 

Had a call with Richard where we experimented with variants of the Kullback–Leibler Divergence formula from [his paper](https://arxiv.org/abs/1404.6520).

We also adapted the code to distance-weight the counts in other categories, controlled by the `p` parameter. Its an asymmetrical measure.

```
	public static double modifiedKLDivergence(double[] ns2, double[] ns1){
		double klDiv = 0;
		double p=0.001;

		double ns1Sum=0,ns2Sum=0;
		for (double n1:ns1)
			ns1Sum+=n1;
		for (double n2:ns2)
			ns2Sum+=n2;
		double nsSum=ns1Sum+ns2Sum; //sum of both ns1 ns2 

		for (int i = 0; i<ns1.length; i++) {
			double zns1=0,zns=0; 
			for (int j = 0; j<ns1.length; j++) {
				zns1+=Math.pow(p,Math.abs(i-j))*ns1[j];
				zns+=Math.pow(p,Math.abs(i-j))*(ns1[j]+ns2[j]);
			}
			if (ns1[i]>0) {
		        klDiv += (ns1[i]/ns1Sum) * Math.log( (zns1/nsSum) / (zns/(float)nsSum));
//				klDiv += (ns1[i]/ns1Sum) * Math.log( (ns1[i]/ns1Sum) / ((ns1[i]/ns1Sum)+(ns2[i]/ns2Sum)));
//				klDiv -= (ns1[i]/ns1Sum) * Math.log( (ns1[i]/ns1Sum) / ((ns1[i]+ns2[i])/(float)nsSum));
			}
		}
		return Math.exp(klDiv);
	}
```

<table>
<tr>
<td><img src="Screenshot 2021-01-30 at 11.51.43.png" width="250"/></td>
<td>OD map of above measure<br/>
	<i>Jason says ...</i> interesting to see similar looking maps in different places in an OD map - this is pretty rare - we aere usually looking for autocorrelation. But the islands, West and North have similar levels of simlarity and so the maps repeat. I guesss that here we see trends that are (presumably) to do with 'type of place' rather than location.
	</td>
</tr>
<tr>
<td><img src="Screenshot 2021-01-30 at 11.53.13.png" width="250"/></td>
<td>How "representative" (approximately) Glasgows's population is to everywhere else. TODO: Richard to check</td>
</tr>
<tr>
<td><img src="Screenshot 2021-01-30 at 11.53.30.png" width="250"/></td>
<td>How "representative" somewhere in Aberdeenshire is to everywhere else. High values in the Central Belt because it contains a subset of their population. TODO: Richard to check</td>
</tr>
</table>

See new release [SimilarityODMap-v1.3](https://github.com/aidans/ramp/releases/tag/SimilarityODMap-v1.3).

Eventually, we hope you use this for comparing population outcome by demographic group by model outputs.

### Actions:
 - [ ] Richard: check my screenshot captions above
 - [ ] Richard and Claire: use [SimilarityODMap-v1.3](https://github.com/aidans/ramp/releases/tag/SimilarityODMap-v1.3) to explore the demographic data and think about similarity measures.
 - [ ] Richard and Claire: Any other statistics that we should try?




## 24th January 2021 

Richard sent a suitable Kullback–Leibler Divergence formula. I implemented it and sent a screenshot.  

Not send the modified app yet, but emailed: 

<table>
<tr>
<td><img src="Screenshot 2021-01-24 at 19.28.27.png" width="250"/></td>
<td>"A screenshot attached. An obvious thing to note is that the darker the colour, the less similar (opposite to the correlation ones). Also worth pointing out that with few data point (the ones in the sea) are dark - I need to exclude or fade these out somehow. "
</td>
</tr>
</table>

### Actions:
 - [ ] Richard and Claire: your views are welcome



## 22th January 2021 

Did a screenshare demo of [RAMP_SimilarityODMap-v1.2](https://www.dropbox.com/s/iqv4cdzbr1kfii8/RAMP_SimilarityODMap-v1.2.zip?dl=0)

Positive feedback. 

Aidan: not sure if correlation is the right measure (one would expect pretty much everywhere to be correlated. Richard: correlation works well. But should also try the other diversity measures including [Kullback–Leibler divergence](https://en.wikipedia.org/wiki/Kullback%E2%80%93Leibler_divergence). 

All: Would be useful to have indication of number of data points. 

Aidan: Would also like to apply to biodiversity measures with Claire/Richard’s help (additional work outside RAMP)


<table>

<tr>
<td><img src="Picture 0.png" width="250"/></td>
<td>Proportion of population in the different age groups 
</td>
</tr>

<tr>
<td><img src="Picture 1.png" width="250"/></td>
<td>Difference in proportion of population in the different age groups compared to mouseovered cell (outlined in red and shown on map in bottom right). 
</td>
</tr>

<tr>
<td><img src="Picture 2.png" width="250"/></td>
<td>Population correction compared to mouseovered cell (outlined in red and shown on map in bottom right).
</td>
</tr>

<tr>
<td><img src="Picture 3.png" width="250"/></td>
<td>OD map of population correlation 
</td>
</tr>

<tr>
<td><img src="Picture 4.png" width="250"/></td>
<td>OD map of population correlation with different zoom level for Ds (compared to Os). 
</td>
</tr>

</table>

 


### Actions:
 - [X] Richard and Claire: to have a play with the prototype and give further views
 - [X] Richard: to send details of other similarity measures
 

 

## 15th January 2021 

Kick-off for RAMP visualisation work (officially from February) 

Aidan: Types of work we could work on for this project 

 - Explore inputs - OD map 
   - Movement data 
   - Local movement 
   - Diversity 

 - Explore how model works 
   - spread by movement 
   - spread by distance 

 - Comparison of model outputs 
   - lockdown scenarios (e.g. keeping certain age groups inside )
   - environment scenarios (e.g. pollution)
   - areas that respond in the same ways 

Richard: Similarity of populations 

Aidan: Proposed "OD maps" for flows and any other spatial interaction measure, e.g. similarity 

Richard: Suggested using correlation coefficients at first. Also suggested experimenting with different extents of origin and destination of OD maps (e.g. similarity for zoomed-in area to whole of Scotland).

### Actions:
 - [X] Implement OD maps to show similarity (using correlation) and with the possibility of different zoom for origins and destinations. 
 - [X] Richard: to send details of other similarity measures
 

 
