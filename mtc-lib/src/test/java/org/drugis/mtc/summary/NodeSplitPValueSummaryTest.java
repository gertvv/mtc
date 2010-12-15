/*
 * This file is part of drugis.org MTC.
 * MTC is distributed from http://drugis.org/mtc.
 * Copyright (C) 2009-2010 Gert van Valkenhoef.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.drugis.mtc.summary;

import static org.drugis.common.JUnitUtil.assertAllAndOnly;
import static org.junit.Assert.assertEquals;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.drugis.mtc.Parameter;
import org.junit.Before;
import org.junit.Test;

import scala.actors.threadpool.Arrays;

public class NodeSplitPValueSummaryTest {
	private Parameter[] d_parameters;
	private ExampleResults d_results;

	private double EPSILON = 0.000000000000001;
	
	@Before
	public void setUp() throws IOException {
		d_results = new ExampleResults();
		d_parameters = d_results.getParameters();
	}
	
	@Test
	public void testCreation() {
		NodeSplitPValueSummary cs = new NodeSplitPValueSummary(d_results, d_parameters[0], d_parameters[1]);
		assertEquals(false, cs.getDefined());
	}
	
	@Test
	public void testCalculations() {
		d_results.makeSamplesAvailable();
		NodeSplitPValueSummary nps = new NodeSplitPValueSummary(d_results, d_parameters[0], d_parameters[1]);
		assertEquals(true, nps.getDefined());
		assertEquals(0.052, nps.getPvalue(), EPSILON);
	}

	
	@Test
	public void testResultsPreservedOnClear() {
		d_results.makeSamplesAvailable();
		NodeSplitPValueSummary nps = new NodeSplitPValueSummary(d_results, d_parameters[0], d_parameters[1]);
		double pval = nps.getPvalue();
		d_results.clear();
		assertEquals(true, nps.getDefined());
		assertEquals(pval, nps.getPvalue(), EPSILON);
	}
		
	@Test
	public void testResultsCalculatedOnAvailable() {
		NodeSplitPValueSummary nps = new NodeSplitPValueSummary(d_results, d_parameters[0], d_parameters[1]);
		d_results.makeSamplesAvailable();
		assertEquals(true, nps.getDefined());
		assertEquals(0.052, nps.getPvalue(), EPSILON);
	}

	@Test
	public void testPropertyChangeOnAvailable() {
		NodeSplitPValueSummary nps = new NodeSplitPValueSummary(d_results, d_parameters[0], d_parameters[1]);
		final List<String> properties = new ArrayList<String>();
		nps.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				properties.add(evt.getPropertyName());
			}
		});
		d_results.makeSamplesAvailable();

		assertAllAndOnly(
				Arrays.asList(new String[] {
						Summary.PROPERTY_DEFINED, 
						NodeSplitPValueSummary.PROPERTY_PVALUE }),
				properties);
	}
	
	/**
		> dir <- c(window(x$x, start=251, end=500), window(x$x, start=751, end=1000))
		> ind <- c(window(x$y, start=251, end=500), window(x$y, start=751, end=1000))
		> prop <- sum(dir > ind)/length(dir)
		> p <- 2*min(prop, 1-prop)
	 */
	
}
