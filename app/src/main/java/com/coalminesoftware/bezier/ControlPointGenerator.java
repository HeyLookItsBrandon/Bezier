package com.coalminesoftware.bezier;

import android.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.coalminesoftware.bezier.CollectionUtils.createFilledList;

public class ControlPointGenerator {
    /**
     * Given a list of points, generates control points for a cubic Bézier curve through them.
     * <p>
     * This is adapted from Ramsundar Shandilya's Swift implementation detailed in a Medium article
     * on the topic.
     *
     * @return A map of control point pairs, keyed by the leading point (commonly referred to as
     * P3.) In the pair, {@link Pair#first} is P1 – the control point for the trailing point, P0.
     * {@link Pair#second} is point P2 – the control point for P3.
     *
     * {@see <a href="https://medium.com/@ramshandilya/draw-smooth-curves-through-a-set-of-points-in-ios-34f6d73c8f9">Medium article</a>}
     * {@see <a href="https://github.com/Ramshandilya/Bezier/blob/master/Bezier/CubicCurveAlgorithm.swift">Swift implementation</a>}
     */
    public static Map<Point, Pair<Point, Point>> generateControlPoints(List<Point> dataPoints) {
        int count = dataPoints.size() - 1;

        List<Point> firstControlPoints = createFilledList(count);
        List<Point> secondControlPoints = new ArrayList<>();

        if (count == 1 ){
            Point p0 = dataPoints.get(0);
            Point p3 = dataPoints.get(1);

            // Calculate first control point. 3P1 = 2P0 + P3

            float p1x = (2*p0.x + p3.x)/3;
            float p1y = (2*p0.y + p3.y)/3;

            firstControlPoints.add(new Point(p1x, p1y));

            // Calculate second control point. P2 = 2P1 - P0
            float p2x = (2*p1x - p0.x);
            float p2y = (2*p1y - p0.y);

            secondControlPoints.add(new Point(p2x, p2y));
        } else {
            List<Point> rhsPoints = new ArrayList<>();

            List<Float> aCoefficients = new ArrayList<>();
            List<Float> bCoefficients = new ArrayList<>();
            List<Float> cCoefficients = new ArrayList<>();

            for(int i = 0; i < count; i++) {
                float rhsX;
                float rhsY;

                Point p0 = dataPoints.get(i);
                Point p3 = dataPoints.get(i + 1);

                if(i == 0) {
                    aCoefficients.add(0f);
                    bCoefficients.add(2f);
                    cCoefficients.add(1f);

                    // RHS for the first segment
                    rhsX = p0.x + 2 * p3.x;
                    rhsY = p0.y + 2 * p3.y;

                } else if(i == count - 1) {
                    aCoefficients.add(2f);
                    bCoefficients.add(7f);
                    cCoefficients.add(0f);

                    // RHS for last segment
                    rhsX = 8 * p0.x + p3.x;
                    rhsY = 8 * p0.y + p3.y;
                } else {
                    aCoefficients.add(1f);
                    bCoefficients.add(4f);
                    cCoefficients.add(1f);

                    rhsX = 4 * p0.x + 2 * p3.x;
                    rhsY = 4 * p0.y + 2 * p3.y;
                }

                rhsPoints.add(new Point(rhsX, rhsY));
            }

            // Solve Ax = B using the tridiagonal matrix algorithm (Thomas algorithm)

            for(int i = 1; i < count; i++) {
                float rhsX = rhsPoints.get(i).x;
                float rhsY = rhsPoints.get(i).y;

                float prevRhsX = rhsPoints.get(i - 1).x;
                float prevRhsY = rhsPoints.get(i - 1).y;

                float m = aCoefficients.get(i) / bCoefficients.get(i - 1);

                float b1 = bCoefficients.get(i) - m * cCoefficients.get(i - 1);
                bCoefficients.set(i, b1);

                float r2x = rhsX - m * prevRhsX;
                float r2y = rhsY - m * prevRhsY;

                rhsPoints.set(i, new Point(r2x, r2y));
            }

            // Build first control points

            float lastControlPointX = rhsPoints.get(count - 1).x / bCoefficients.get(count - 1);
            float lastControlPointY = rhsPoints.get(count - 1).y / bCoefficients.get(count - 1);

            firstControlPoints.set(count - 1, new Point(lastControlPointX, lastControlPointY));

            for(int i = count - 2; i >= 0; --i) {
                Point nextControlPoint = firstControlPoints.get(i + 1);
                if(nextControlPoint != null) {
                    float controlPointX = (rhsPoints.get(i).x - cCoefficients.get(i) * nextControlPoint.x) / bCoefficients.get(i);
                    float controlPointY = (rhsPoints.get(i).y - cCoefficients.get(i) * nextControlPoint.y) / bCoefficients.get(i);

                    firstControlPoints.set(i, new Point(controlPointX, controlPointY));
                }
            }

            // Build second control points from first

            for(int i = 0; i < count; i++) {
                if(i == count - 1) {
                    Point p3 = dataPoints.get(i + 1);

                    Point p1 = firstControlPoints.get(i);
                    if(p1 == null) {
                        continue;
                    }

                    float controlPointX = (p3.x + p1.x) / 2f;
                    float controlPointY = (p3.y + p1.y) / 2f;

                    secondControlPoints.add(new Point(controlPointX, controlPointY));
                } else {
                    Point p3 = dataPoints.get(i + 1);

                    Point nextP1 = firstControlPoints.get(i + 1);
                    if(nextP1 == null) {
                        continue;
                    }

                    float controlPointX = 2 * p3.x - nextP1.x;
                    float controlPointY = 2 * p3.y - nextP1.y;

                    secondControlPoints.add(new Point(controlPointX, controlPointY));
                }
            }
        }

        // Now that we have both, build a map from them

        Map<Point, Pair<Point, Point>> controlPointsByPoints = new HashMap<>();
        for(int i = 0; i < count; i++) {
            controlPointsByPoints.put(dataPoints.get(i + 1), new Pair<>(
                    firstControlPoints.get(i),
                    secondControlPoints.get(i)));
        }
        return controlPointsByPoints;
    }
}
