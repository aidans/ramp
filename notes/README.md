# Meeting (and non-meeting) notes 

## 24th February 2021 (non-meeting update)

On 19th Feb, Claire sent two results set - with and with commuting data. These are at `../data/2021-02-19_comparison.zip`. We want to do a comparison of these. (Note that these use fake commuting data: @Claire: see the entry below for where to get OD commuting data at OA level.

As a precursor to proper comparison, [RAMP_DemographicGridmap-v1.4](https://github.com/aidans/ramp/releases/tag/RAMP_DemographicGridmap-v1.4) loads a *result set* and a *baseline result set* and you can switch between them (currently with the 'b' key).

<table width="100%">
<tr>
<td><img src="Screenshot 2021-02-24 at 12.23.43.png" width="250"/></td>
<td>Results with commuting, over time</td>
</tr>
<tr>
<td><img src="Screenshot 2021-02-24 at 12.23.48.png" width="250"/></td>
<td>Results without commuting data, over time</td>
</tr>
</table>


There is an apparent *problem* in which the grid cells don't seem to match up with those from the demographics data. These are matched with the `/grid1km/1year/persons/Dimension_1_names` dataset in `demographics.h5` (in `../data/demographics.zip`) and the `/abundances/grid_id` dataset in the `h5` files in ``../data/2021-02-19_comparison.zip`. @Claire can you check?

<table width="100%">
<tr>
<td><img src="Screenshot 2021-02-24 at 12.19.55.png" width="250"/></td>
<td>The data don't get as far as the east coast, so don't match that from demographics.h5 Please check (details above!)</td>
</tr>
</table>



### Actions

 - [ ] See 12th Feb entry below for where to get real OD data (from the 2011 census)
 - [ ] Claire, please check the location grid ids (see above)
 - [ ] Claire/Richard, run [RAMP_DemographicGridmap-v1.4](https://github.com/aidans/ramp/releases/tag/RAMP_DemographicGridmap-v1.4)


## 22th February 2021 (non-meeting update)

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
 
### Retrospectively answering Min's questions (for Analytical Support Teams):

#### What are the major scientific questions to focus on, e.g., region-to-region comparison, real-world vaccine efficacy, intervention efficacy?

 - *Specific*: For a spatially-explicit model, support model development and help interpret the outputs
 - *General*: effective visualisation and interactions to support comparison of multivariate data in "continuous" space at multiple resolutions.
 - *Approach*: (a) Gridmaps (multi-scale, continuous geography, multivariate), (b) rapid prototyping and (c) embedding within the modelling process.

#### what analytical algorithms may be needed, e.g., multi-time-series similarity metrics., time series search and matching, etc.?

 - fast and efficient data structures
 - spatial/temporal aggregation
 - similarity mrtrics

#### how many algorithms to be studied in each category?

 - not fixed number - driven by analytical need

#### how many visual representations for each scientific questions?

 - not fixed number - driven by analytical need

#### how will these be translated to the deliverables?

 - document the co-evolution of the (a) visualisation design and (2) model development (and analytical questions that may arise)

 
