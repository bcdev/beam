/* Copyright (C) 2002-2008 by Brockmann Consult
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

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/**
 * Expectation maximization (EM) cluster algorithm.
 * <p/>
 * todo - logging
 * todo - make algorithm use tiles
 *
 * @author Ralf Quast
 * @version $Revision: 2221 $ $Date: 2008-06-16 11:19:52 +0200 (Mo, 16 Jun 2008) $
 */
public class EMClusterer {

    private final int pointCount;
    private final int dimensionCount;
    private final int clusterCount;

    private final double[][] points;

    // prior cluster probabilities
    private final double[] priors;
    // posterior cluster probabilities
    private final double[][] posteriors;

    // cluster means
    private final double[][] means;
    // cluster covariances
    private final double[][][] covariances;
    // cluster distributions
    private final Distribution[] distributions;

    // strategy for calculating posterior cluster probabilities
    private final ProbabilityCalculator calculator;

    /**
     * Finds a collection of clusters for a given set of data points.
     *
     * @param points         the data points.
     * @param clusterCount   the number of clusters.
     * @param iterationCount the number of EM iterations to be made.
     * @param randomSeed     the seed used to initialize the cluster algorithm
     *
     * @return the cluster decomposition.
     */
    static EMCluster[] findClusters(double[][] points, int clusterCount, int iterationCount, int randomSeed) {
        return new EMClusterer(points, clusterCount, randomSeed).findClusters(iterationCount);
    }

    /**
     * Constructs a new instance of this class.
     *
     * @param points       the data points.
     * @param clusterCount the number of clusters.
     * @param randomSeed   the seed used to initialize the cluster algorithm
     */
    public EMClusterer(double[][] points, int clusterCount, int randomSeed) {
        pointCount = points.length;
        dimensionCount = points[0].length;

        this.points = points;
        this.clusterCount = clusterCount;

        priors = new double[clusterCount];
        posteriors = new double[pointCount][clusterCount];

        means = new double[clusterCount][dimensionCount];
        covariances = new double[clusterCount][dimensionCount][dimensionCount];
        distributions = new MultinormalDistribution[clusterCount];
        calculator = new ProbabilityCalculator(distributions, priors);

        initialize(new Random(randomSeed));
    }

    /**
     * Finds a collection of clusters.
     *
     * @param iterationCount the number of EM iterations to be made.
     *
     * @return the cluster decomposition.
     */
    private EMCluster[] findClusters(int iterationCount) {
        while (iterationCount > 0) {
            iterate();
            iterationCount--;
            // todo - logging
        }

        return getClusters();
    }

    /**
     * Carries out a single EM iteration.
     */
    public void iterate() {
        stepE();
        stepM();
    }

    /**
     * Returns the clusters found.
     *
     * @return the clusters found.
     */
    public EMCluster[] getClusters() {
        return getClusters(new PriorProbabilityClusterComparator());
    }

    public EMCluster[] getClusters(Comparator<EMCluster> clusterComparator) {
        final EMCluster[] clusters = new EMCluster[clusterCount];

        for (int k = 0; k < clusterCount; ++k) {
            clusters[k] = new EMCluster(means[k], covariances[k], priors[k]);
        }
        Arrays.sort(clusters, clusterComparator);

        return clusters;
    }

    /**
     * Randomly initializes the clusters.
     *
     * @param random the random number generator used for initialization.
     */
    private void initialize(Random random) {
        for (int k = 0; k < clusterCount; ++k) {
            System.arraycopy(points[random.nextInt(pointCount)], 0, means[k], 0, dimensionCount);
        }

        for (int k = 0; k < clusterCount; ++k) {
            priors[k] = 1.0; // same prior probability for all clusters

            for (int l = 0; l < dimensionCount; ++l) {
                // initialization of diagonal elements with unity comes close
                // to an initial run with the k-means algorithm
                covariances[k][l][l] = 1.0;
            }

            distributions[k] = new MultinormalDistribution(means[k], covariances[k]);
        }
    }

    /**
     * Performs an E-step.
     * <p/>
     * Calculates posterior cluster probabilities.
     */
    private void stepE() {
        for (int i = 0; i < pointCount; ++i) {
            calculator.calculate(points[i], posteriors[i]);

            // ensure non-zero probabilities everywhere to prevent the covariance
            // matrixes from getting singular 
            double sum = 0.0;
            for (int k = 0; k < clusterCount; ++k) {
                posteriors[i][k] += 1.0E-4;
                sum += posteriors[i][k];
            }
            for (int k = 0; k < clusterCount; ++k) {
                posteriors[i][k] /= sum;
            }
        }
    }

    /**
     * Performs an M-step.
     * <p/>
     * Calculates statistical moments, prior cluster probabilities and cluster distributions.
     */
    private void stepM() {
        for (int k = 0; k < clusterCount; ++k) {
            for (int l = 0; l < dimensionCount; ++l) {
                for (int m = l; m < dimensionCount; ++m) {
                    covariances[k][l][m] = 0.0;
                }
                means[k][l] = 0.0;
            }
            double sum = 0.0;
            for (int i = 0; i < pointCount; ++i) {
                for (int l = 0; l < dimensionCount; ++l) {
                    means[k][l] += posteriors[i][k] * points[i][l];
                }
                sum += posteriors[i][k];
            }
            for (int l = 0; l < dimensionCount; ++l) {
                means[k][l] /= sum;
            }
            for (int i = 0; i < pointCount; ++i) {
                for (int l = 0; l < dimensionCount; ++l) {
                    for (int m = l; m < dimensionCount; ++m) {
                        covariances[k][l][m] += posteriors[i][k] * (points[i][l] - means[k][l]) * (points[i][m] - means[k][m]);
                    }
                }
            }
            for (int l = 0; l < dimensionCount; ++l) {
                for (int m = l; m < dimensionCount; ++m) {
                    covariances[k][l][m] /= sum;
                    covariances[k][m][l] = covariances[k][l][m];
                }
            }

            priors[k] = sum / pointCount;
        }
        for (int k = 0; k < clusterCount; ++k) {
            distributions[k] = new MultinormalDistribution(means[k], covariances[k]);
        }
    }

    /**
     * Cluster comparator.
     * <p/>
     * Compares two clusters according to their prior probability.
     */
    private static class PriorProbabilityClusterComparator implements Comparator<EMCluster> {

        @Override
        public int compare(EMCluster c1, EMCluster c2) {
            return Double.compare(c2.getPriorProbability(), c1.getPriorProbability());
        }
    }
}
