/*
*   PlotWindow  -- A Swing frame for displaying a plot panel.
*
*   Copyright (C) 2000-2002 by Joseph A. Huwaldt <jhuwaldt@knology.net>.
*   All rights reserved.
*   
*   This library is free software; you can redistribute it and/or
*   modify it under the terms of the GNU Library General Public
*   License as published by the Free Software Foundation; either
*   version 2 of the License, or (at your option) any later version.
*   
*   This library is distributed in the hope that it will be useful,
*   but WITHOUT ANY WARRANTY; without even the implied warranty of
*   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
*   Library General Public License for more details.
**/
package plot;

import jahuwaldt.plot.*;

import java.awt.*;
import javax.swing.*;


/**
*  <p> A simple Swing frame that can be used to display a plot panel.
*  </p>
*
*  <p>  Modified by:  Joseph A. Huwaldt  </p>
*
*  @author  Joseph A. Huwaldt   Date:  September 22, 2000
*  @version December 12, 2000
**/
public class PlotExample extends JFrame {

	
	//-------------------------------------------------------------------------
	/**
	*  Creates a plot window that displays the specified plot panel.
	*
	*  @param  name   The title to show in the window's title bar.
	*  @param  plot   The plot panel to be displayed in this window.
	**/
	public PlotExample( String name, PlotPanel plot ) {
		super(name);

		getContentPane().add(plot);
	}

	//-------------------------------------------------------------------------

	/**
	*  A simple method to test this PlotWindow by creating a couple of Plot2D
	*  plots and putting them in windows.
	**/
	public static void main(String[] args) {
	
		XYPlotExample();
		//ContourPlotExample();		
	}

	public static void XYPlotExample() {
		
		//	Create a Simple 2D XY plot window.
		double[] xArr = {1., 2., 10, 20, 30, 40, 50};
		double[] yArr = {0.06, 0.01, -0.01, -0.02, -0.03, -0.05, -0.075};
		double[] yArrAlt = {0.08, 0.03, 0.01, 0.0, -0.01, -0.03, -0.045};
		
		Plot2D aPlot = new SimplePlotXY(xArr, yArr, "Test SimpleXY Plot", "X Axis", "Y Axis",
										null, null, new CircleSymbol());
		
		//	Make the horizontal axis a log axis.
		PlotAxis xAxis = aPlot.getHorizontalAxis();
		xAxis.setScale(new Log10AxisScale());
		
		//	Create a 2nd run and add it to the plot.
		PlotRunList runs = aPlot.getRuns();
		PlotRun run = (PlotRun)runs.get(0);
		run.setLineColor(Color.red);
		//runs.add(new PlotRun(xArr, yArrAlt, true, new SquareSymbol()));
		
		PlotPanel panel = new PlotPanel(aPlot);
		panel.setBackground( Color.white );
		
		
		PlotExample window = new PlotExample("SimplePlotXY Plot Window", panel);
		window.setSize(500, 300);
		window.setLocation(50,50);
		window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		window.show();		

	}
	
	public static void ContourPlotExample() {

		//	Create a more complicated 2D contour plot of 3D data.
		//	zArr is the Z component of the data points.
		double[][] zArr = {	{0.5,1.1,1.5,1,2.0,3,3,2,1,0.1},
							{1.0,1.5,3.0,5,6.0,2,1,1.2,1,4},
							{0.9,2.0,2.1,3,6.2,7,3,2,1,1.4},
							{1.0,1.5,3.0,4,6.0,5,2,1.5,1,2},
							{0.8,2.0,3.0,3,4.0,4,3,2.4,2,3},
							{0.6,1.1,1.5,1,4.0,3.5,3,2,3,4},
							{1.0,1.5,3.0,5,6.0,2,1,1.2,2.7,4},
							{0.8,2.0,3.0,3,5.5,6,3,2,1,1.4},
							{1.0,1.5,3.0,4,6.0,5,2,1,0.5,0.2} };
		
		//	An alternate set of test data.
/*		double[][] zArr = {	{-0.44, -0.44, -0.44, -0.44, -0.44, -0.45, -0.48, -0.51, -0.52, -0.50, -0.49, -0.51, -0.55, -0.59, -0.60},
							{-0.45, -0.48, -0.50, -0.49, -0.47, -0.44, -0.44, -0.44, -0.41, -0.40, -0.43, -0.43, -0.47, -0.55, -0.59},
							{-0.52, -0.57, -0.60, -0.59, -0.56, -0.50, -0.44, -0.37, -0.33, -0.46, -0.56, -0.45, -0.36, -0.50, -0.58},
							{-0.59, -0.58, -0.53, -0.54, -0.59, -0.58, -0.47, -0.32, -0.33, -0.52, -0.35, -0.55, -0.47, -0.46, -0.57},
							{-0.58, -0.40, -0.20, -0.25, -0.47, -0.60, -0.51, -0.32, -0.35, -0.39,  0.23, -0.33, -0.55, -0.44, -0.56},
							{-0.52, -0.18,  0.14,  0.06, -0.31, -0.58, -0.54, -0.34, -0.33, -0.46, -0.10, -0.47, -0.53, -0.45, -0.56},
							{-0.52, -0.19,  0.12,  0.05, -0.32, -0.58, -0.55, -0.37, -0.28, -0.46, -0.55, -0.57, -0.45, -0.48, -0.58},
							{-0.58, -0.41, -0.23, -0.27, -0.49, -0.60, -0.53, -0.40, -0.31, -0.35, -0.43, -0.44, -0.45, -0.54, -0.59},
							{-0.59, -0.59, -0.54, -0.55, -0.60, -0.57, -0.49, -0.42, -0.40, -0.41, -0.43, -0.47, -0.53, -0.58, -0.60},
							{-0.52, -0.57, -0.59, -0.58, -0.55, -0.50, -0.44, -0.44, -0.48, -0.51, -0.53, -0.56, -0.58, -0.60, -0.60} };
*/
		//	xArr2 & yArr2 are the X & Y components of the data points (evenly gridded in this case).
		double[][] xArr2, yArr2;
		
		//	Create a simple grid of X & Y to go with our Z data.
		int ni = zArr.length;
		int nj = zArr[0].length;
		xArr2 = new double[ni][nj];
		yArr2 = new double[ni][nj];
		for (int i=0; i < ni; ++i) {
			for (int j=0; j < nj; ++j) {
				xArr2[i][j] = j;
				yArr2[i][j] = i;
			}
		}
		
		Plot2D aPlot = new ContourPlot(xArr2, yArr2, zArr, 12, false,
										"Test Contour Plot", "X Axis", "Y Axis",
										null, null);
		
		//	Colorize the contours.
		((ContourPlot)aPlot).colorizeContours(Color.blue, Color.red);
		
		//	Create a run that contains the original XY data points we just put contours through.
		//	We'll plot it with symbols so we can see the location of the original data points.
		PlotSymbol symbol = new CircleSymbol();
		symbol.setBorderColor(Color.gray);
		symbol.setSize(4);
		PlotRun run = new PlotRun();
		for (int i=0; i < ni; ++i) {
			for (int j=0; j < nj; ++j) {
				run.add( new PlotDatum(xArr2[i][j], yArr2[i][j], false, symbol) );
			}
		}
		
		//	Add this new run of points to the plot.
		aPlot.getRuns().add(run);
		
		//	Now proceed with creating the plot window.
		PlotPanel panel = new PlotPanel(aPlot);
		panel.setBackground( Color.white );
		PlotExample window = new PlotExample("ContourPlot Plot Window", panel);
		window.setSize(500, 300);
		window.setLocation(100,100);
		window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		window.show();
	}
}


