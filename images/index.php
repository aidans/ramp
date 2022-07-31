<html>
<head>
<title>GlyphMaps: Population and COVID</title>
<style>
h1,h2,h3,h4 {
    font-family: sans-serif;
    color:333333;
}
p,td,li {
    font-family: sans-serif;
    color:333333;
    text-align: justify;
    font-size:1em;
}
ul{
	margin-bottom:0px;
	margin-top:0px;
}
code{
    border-style: solid; padding:1px; border-width:1px; border-color:#ccc;
}
</style>
</head>
<body>

<div style="max-width: 800px">
<h1 style="text-align:center;background-color:#eee">GlyphMaps: Population and COVID</h1>

<div style="text-align:center;border-style: solid none solid none;border-width:1px"><b>Interactive demo that demonstrates use of Glyphmaps for population and COVID modelling</b></div>

<p>Aidan Slingsby
<br>Homepage: <a href="http://www.staff.city.ac.uk/~sbbb717/">http://www.staff.city.ac.uk/~sbbb717/</a>
<br>Email: <a href="mailto:a.slingsby@city.ac.uk">a.slingsby@city.ac.uk</a></p>

<p>This is joint work with <a href="https://www.bioss.ac.uk/people/claire.html">Claire Harris</a> of <a href="bioss.ac.uk">BIOSS</a> and <a hre="https://www.gla.ac.uk/researchinstitutes/bahcm/staff/richardreeve/">Richard Reeve</a> of <a href="https://www.gla.ac.uk/">Glasgow University</a>. It was carried out as part of the <a href="https://sites.google.com/view/rampvis/">RAMPVIS</a> (reported <a href="https://arxiv.org/abs/2012.04757">here</a>) funded by UKRI/EPSRC, involving most UK visualisation academics in a project coordinated by <a href="https://eng.ox.ac.uk/people/min-chen/">Min Chen</a> at <a href="https://www.ox.ac.uk/">Oxford University</a>.</p>

<p>The work used glyphmaps to help tune and interpret the spread of COVID through the Scottish population. However, please note that the model never developed to the stage of informing pandemic response and the results should be treated as <b>examples of what are possible</b>.</p>

<ul>
	<li><a href="https://observablehq.com/@aidans/rampvis-idiom-gridded-glyphmaps"><b>Observable page</b> that describes the work</a>
	<li><a href="https://observablehq.com/@aidans/zoomable-gridded-glyphmap-of-scottish-population"><b>Observable page</b> with a simple implementation for age-banded Scottish population.</a>
	<li><a href="RAMP_DemographicGridmap-v1.10_withdata.zip"><b>Java implementation</b> which implements the 4 glyph designs below.</a>
</ul>



<h2>Zoomable Gridded Glyphmaps</h2>

<p>Zoomable Gridded Glyphmaps dynamically grids spatial data with a fixed screen-size. This enables data to explored at multiple scales. Glyphs placed in cells summarise the data using a glyph. A wide range of glyph designs is possible, which can provide rich summaries that may include multiclass classes, multivariate data and statistical summaries.</p>

<p><img src="anim.gif"</p>

<p>In the table below, we provide 4 glyph types (rows) and 3 scaling types (columns).</p>

<table>
<tr><td width=200></td><td style="text-align:center" width=150><b>Absolute</b><br><i>Raw numbers of people</i></td><td style="text-align:center" width=150><b>Relative</b><br><i>Proportions</i></td><td style="text-align:center" width=150><b>Relative with low populations deemphasised</b><br><i>Low populations deemphasised through fading</i></td></tr>
<tr valign="top"><td style="text-align:right"><b>(Inverted) population pyramids</b><br><i>Resident population in 10-year age-bands with the youngest at the top.</i></td><td><img src="gridded1a.png" width=150 height=150></td><td><img src="gridded1b.png" width=150 height=150></td><td><img src="gridded1c.png" width=150 height=150></td></tr>
<tr valign="top"><td style="text-align:right"><b>Animated COVID spread over time</b><br><i></i></td><td><img src="gridded2a.gif" width=150 height=150></td><td><img src="gridded2b.gif" width=150 height=150></td><td><img src="gridded2c.gif" width=150 height=150></td><td valign="bottom"><img src="legend.png" width=80/></td></tr>
<tr valign="top"><td style="text-align:right"><b>COVID spread over time</b><br></td><td><img src="gridded3a.png" width=150 height=150></td><td><img src="gridded3b.png" width=150 height=150></td><td><img src="gridded3c.png" width=150 height=150></td><td valign="bottom"><img src="legend.png" width=80/></td></tr>
<tr valign="top"><td style="text-align:right"><b>Comparison of COVID spread</b><br><i>Compared to another scenario, with more above the horizontal centre and less below the horizontal centre</i></td><td><img src="gridded4a.png" width=150 height=150></td><td><img src="gridded4b.png" width=150 height=150></td><td><img src="gridded4c.png" width=150 height=150></td><td valign="bottom"><img src="legend.png" width=80/></td></tr>
</table>


<p>In the map below, note the younger populations in the centres of Edinburgh and Glasgow.</p>

<p><img src="pyramids.png"></p>


<h2>Gridmap Glyphmaps</h2>

<p>Here, instead dynamically gridding the data, gridmaps present the data in existing administrative units (Local Authorities here) in a layout which tries to maintain good relative geographical positioning whilst maintaining large non-overlapping cells are large enough to resolve a detailed glyph.</p>

<p><img src="morph.gif" width="48%"> <img src="mouse.gif" width="48%"></p>

<p>On the right (above), the temporal reference line indicates, for example, that that infection starts sooner in Glasgow that in Edinburgh.</p>

<table>
<tr><td width=200></td><td style="text-align:center" width=150><b>Absolute</b><br><i>Raw numbers of people</i></td><td style="text-align:center" width=150><b>Relative</b><br><i>Proportions</i></td><td style="text-align:center" width=150><b>Relative with low populations deemphasised</b><br><i>Low populations deemphasised through fading</i></td></tr>
<tr valign="top"><td style="text-align:right"><b>(Inverted) population pyramids</b><br><i>Resident population in 10-year age-bands with the youngest at the top.</i></td><td><img src="gridmap1a.png" width=150 height=150></td><td><img src="gridmap1b.png" width=150 height=150></td><td><img src="gridmap1c.png" width=150 height=150></td></tr>
<tr valign="top"><td style="text-align:right"><b>Animated COVID spread over time</b><br><i></i></td><td><img src="gridmap2a.gif" width=150 height=150></td><td><img src="gridmap2b.gif" width=150 height=150></td><td><img src="gridmap2c.gif" width=150 height=150></td><td valign="bottom"><img src="legend.png" width=80/></td></tr>
<tr valign="top"><td style="text-align:right"><b>COVID spread over time</b><br></td><td><img src="gridmap3a.png" width=150 height=150></td><td><img src="gridmap3b.png" width=150 height=150></td><td><img src="gridmap3c.png" width=150 height=150></td><td valign="bottom"><img src="legend.png" width=80/></td></tr>
<tr valign="top"><td style="text-align:right"><b>Comparison of COVID spread</b><br><i>Compared to another scenario, with more above the horizontal centre and less below the horizontal centre</i></td><td><img src="gridmap4a.png" width=150 height=150></td><td><img src="gridmap4b.png" width=150 height=150></td><td><img src="gridmap4c.png" width=150 height=150></td><td valign="bottom"><img src="legend.png" width=80/></td></tr>
</table>

<p><img src="RAMPVIS.figures.AS.composite.jpg" width="100%"></p>

<p>The image should help interpret the graphics (thanks to Jason Dykes for his input into this image).</i></p>


<p style="text-align:right"><i>&copy;Aidan Slingsby<br>October, 2021</i></p>

</div>


</body>
</html>