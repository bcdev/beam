/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.cluster;

/**
 * Probability calculator factory.
 * <p/>
 * todo - extract interface
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class ProbabilityCalculatorFactory {

    public ProbabilityCalculator createProbabilityCalculator(final EMCluster[] clusters) {
        final Distribution[] distributions = new Distribution[clusters.length];
        final double[] priors = new double[clusters.length];

        for (int i = 0; i < clusters.length; i++) {
            distributions[i] = new MultinormalDistribution(clusters[i].getMean(), clusters[i].getCovariances());
            priors[i] = clusters[i].getPriorProbability();
        }

        return new ProbabilityCalculator(distributions, priors);
    }
}
