/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.util.converters;

import com.bc.ceres.binding.converters.StringConverter;

/**
 *
 * Empty marker subclass for StringConverter.
 *
 * Purpose: use as converter for band maths expressions in the
 * {@link org.esa.beam.framework.gpf.annotations.Parameter}-annotation in order to allow for generating a decent
 * UI-component.
 *
 * @author Thomas Storm
 */
public class GeneralExpressionConverter extends StringConverter {

}
