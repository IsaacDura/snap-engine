/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.visat.toolviews.stat;

/**
 * The tool view containing a density plot
 *
 * @author Marco Zuehlke
 */
public class DensityPlotToolView extends AbstractStatisticsToolView {

    public static final String ID = DensityPlotToolView.class.getName();

    @Override
    protected PagePanel createPagePanel() {
        return new DensityPlotPanel(this, getDescriptor().getHelpId());
    }
}
