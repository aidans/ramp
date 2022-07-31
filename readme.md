# Interactive visualisation that uses of Glyphmaps for population and COVID modelling

[This information is also] here(http://www.staff.city.ac.uk/~sbbb717/">http://www.staff.city.ac.uk/~sbbb717/)

This is joint work a [Claire Harris](https://www.bioss.ac.uk/people/claire.html") of [BIOSS](http://bioss.ac.uk) and [Richard Reeve](https://www.gla.ac.uk/researchinstitutes/bahcm/staff/richardreeve/) of [Glasgow University](https://www.gla.ac.uk/"). It was carried out as part of the [RAMPVIS](https://sites.google.com/view/rampvis/) - outputs of which are [published here](https://openaccess.city.ac.uk/id/eprint/28123/) (including outputs of this work) -  funded by EPSRC, involving most UK visualisation academics in a project coordinated by [Min Chen](https://eng.ox.ac.uk/people/min-chen/)</a> at [Oxford University](https://www.ox.ac.uk/)

The work used glyphmaps to help tune and interpret the spread of COVID through the Scottish population. However, please note that the model never developed to the stage of informing pandemic response and the results should be treated as **examples of what are possible**.

* **[Observable page](https://observablehq.com/@aidans/rampvis-idiom-gridded-glyphmaps)** that describes the work
* **[Observable page](https://observablehq.com/@aidans/zoomable-gridded-glyphmap-of-scottish-population)** with a simple implementation for age-banded Scottish population.
* **[Java implementation](https://www.staff.city.ac.uk/~sbbb717/glyphmaps/covid/RAMP_DemographicGridmap-v1.10_withdata.zip)** which implements the 4 glyph designs below.



## Zoomable Gridded Glyphmaps

Zoomable Gridded Glyphmaps dynamically grids spatial data with a fixed screen-size. This enables data to explored at multiple scales. Glyphs placed in cells summarise the data using a glyph. A wide range of glyph designs is possible, which can provide rich summaries that may include multiclass classes, multivariate data and statistical summaries.

<p><img src="images/anim.gif"></p>

In the table below, we provide 4 glyph types (rows) and 3 scaling types (columns).

<table>
<tr><td width=200></td><td style="text-align:center" width=150><b>Absolute</b><br><i>Raw numbers of people</i></td><td style="text-align:center" width=150><b>Relative</b><br><i>Proportions</i></td><td style="text-align:center" width=150><b>Relative with low populations deemphasised</b><br><i>Low populations deemphasised through fading</i></td></tr>
<tr valign="top"><td style="text-align:right"><b>(Inverted) population pyramids</b><br><i>Resident population in 10-year age-bands with the youngest at the top.</i></td><td><img src="images/gridded1a.png" width=150 height=150></td><td><img src="images/gridded1b.png" width=150 height=150></td><td><img src="images/gridded1c.png" width=150 height=150></td></tr>
<tr valign="top"><td style="text-align:right"><b>Animated COVID spread over time</b><br><i></i></td><td><img src="images/gridded2a.gif" width=150 height=150></td><td><img src="images/gridded2b.gif" width=150 height=150></td><td><img src="images/gridded2c.gif" width=150 height=150></td><td valign="bottom"><img src="images/legend.png" width=80/></td></tr>
<tr valign="top"><td style="text-align:right"><b>COVID spread over time</b><br></td><td><img src="images/gridded3a.png" width=150 height=150></td><td><img src="images/gridded3b.png" width=150 height=150></td><td><img src="images/gridded3c.png" width=150 height=150></td><td valign="bottom"><img src="images/legend.png" width=80/></td></tr>
<tr valign="top"><td style="text-align:right"><b>Comparison of COVID spread</b><br><i>Compared to another scenario, with more above the horizontal centre and less below the horizontal centre</i></td><td><img src="images/gridded4a.png" width=150 height=150></td><td><img src="images/gridded4b.png" width=150 height=150></td><td><img src="images/gridded4c.png" width=150 height=150></td><td valign="bottom"><img src="images/legend.png" width=80/></td></tr>
</table>


In the map below, note the younger populations in the centres of Edinburgh and Glasgow.

<img src="images/pyramids.png">


## Gridmap Glyphmaps

Here, instead dynamically gridding the data, gridmaps present the data in existing administrative units (Local Authorities here) in a layout which tries to maintain good relative geographical positioning whilst maintaining large non-overlapping cells are large enough to resolve a detailed glyph.

<img src="images/morph.gif" width="48%"> <img src="images/mouse.gif" width="48%">

On the right (above), the temporal reference line indicates, for example, that that infection starts sooner in Glasgow that in Edinburgh.

<table>
<tr><td width=200></td><td style="text-align:center" width=150><b>Absolute</b><br><i>Raw numbers of people</i></td><td style="text-align:center" width=150><b>Relative</b><br><i>Proportions</i></td><td style="text-align:center" width=150><b>Relative with low populations deemphasised</b><br><i>Low populations deemphasised through fading</i></td></tr>
<tr valign="top"><td style="text-align:right"><b>(Inverted) population pyramids</b><br><i>Resident population in 10-year age-bands with the youngest at the top.</i></td><td><img src="images/gridmap1a.png" width=150 height=150></td><td><img src="images/gridmap1b.png" width=150 height=150></td><td><img src="images/gridmap1c.png" width=150 height=150></td></tr>
<tr valign="top"><td style="text-align:right"><b>Animated COVID spread over time</b><br><i></i></td><td><img src="images/gridmap2a.gif" width=150 height=150></td><td><img src="images/gridmap2b.gif" width=150 height=150></td><td><img src="images/gridmap2c.gif" width=150 height=150></td><td valign="bottom"><img src="images/legend.png" width=80/></td></tr>
<tr valign="top"><td style="text-align:right"><b>COVID spread over time</b><br></td><td><img src="images/gridmap3a.png" width=150 height=150></td><td><img src="images/gridmap3b.png" width=150 height=150></td><td><img src="images/gridmap3c.png" width=150 height=150></td><td valign="bottom"><img src="images/legend.png" width=80/></td></tr>
<tr valign="top"><td style="text-align:right"><b>Comparison of COVID spread</b><br><i>Compared to another scenario, with more above the horizontal centre and less below the horizontal centre</i></td><td><img src="images/gridmap4a.png" width=150 height=150></td><td><img src="images/gridmap4b.png" width=150 height=150></td><td><img src="images/gridmap4c.png" width=150 height=150></td><td valign="bottom"><img src="images/legend.png" width=80/></td></tr>
</table>

<p><img src="images/RAMPVIS.figures.AS.composite.jpg" width="100%"></p>

The image should help interpret the graphics (thanks to Jason Dykes for his input into this image).</i>


