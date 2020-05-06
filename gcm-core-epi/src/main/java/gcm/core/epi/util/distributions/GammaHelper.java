package gcm.core.epi.util.distributions;

/**
 * Helper utilities for Gamma distributions
 */

public class GammaHelper {

    public static double getShapeFromCOV(double cov) {
        return 1 / (cov * cov);
    }

    public static double getScaleFromMeanAndCOV(double mean, double cov) {
        return mean * cov * cov;
    }

    public static double getShapeFromMeanAndSD(double mean, double sd) {
        return mean * mean / (sd * sd);
    }

    public static double getScaleFromMeanAndSD(double mean, double sd) {
        return sd * sd / mean;
    }

}
