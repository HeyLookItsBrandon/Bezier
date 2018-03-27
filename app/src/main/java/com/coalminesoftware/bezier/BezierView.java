package com.coalminesoftware.bezier;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BezierView extends View {
	public BezierView(Context context) {
		super(context);
	}

	public BezierView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}

	public BezierView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public BezierView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		Paint paint = new Paint();
		paint.setStrokeWidth(5);
		paint.setColor(0xFFFF0000);

		List<Point> points = Arrays.asList(
				new Point(canvas.getWidth()*0.33f, canvas.getHeight()*0.33f),
				new Point(canvas.getWidth()*0.67f, canvas.getHeight()*0.33f),
				new Point(canvas.getWidth()*0.67f, canvas.getHeight()*0.67f),
				new Point(canvas.getWidth()*0.33f, canvas.getHeight()*0.67f));

		Map<Point, Pair<Point, Point>> controlPointsBySegment = generateControlPoints(points);
		Path path = buildPath(points, controlPointsBySegment);

		canvas.drawPath(path, paint);
	}

	private Path buildPath(List<Point> points, Map<Point,Pair<Point,Point>> controlPointsBySegment) {
		Path path = new Path();

		Point point = points.get(0);
		path.moveTo(point.x, point.y);

		for(int i = 1; i < points.size(); i++) {
			point = points.get(i);

			Pair<Point,Point> controlPoints = controlPointsBySegment.get(point);
			Point c1 = controlPoints.first;
			Point c2 = controlPoints.second;

			path.cubicTo(c1.x, c1.y, c2.x, c2.y, point.x, point.y);
		}

		return path;
	}

	// Lovingly stolen and adpated from https://github.com/Ramshandilya/Bezier/blob/master/Bezier/CubicCurveAlgorithm.swift,
	// from the article https://medium.com/@ramshandilya/draw-smooth-curves-through-a-set-of-points-in-ios-34f6d73c8f9
	private Map<Point, Pair<Point, Point>> generateControlPoints(List<Point> dataPoints) {
		int count = dataPoints.size() - 1;

		List<Point> firstControlPoints = new ArrayList<>();
		List<Point> secondControlPoints = new ArrayList<>();

		if (count == 1 ){
			Point P0 = dataPoints.get(0);
			Point P3 = dataPoints.get(1);

			//Calculate First Control Point
			//3P1 = 2P0 + P3

			float P1x = (2*P0.x + P3.x)/3;
			float P1y = (2*P0.y + P3.y)/3;

			firstControlPoints.add(new Point(P1x, P1y));

			//Calculate second Control Point
			//P2 = 2P1 - P0
			float P2x = (2*P1x - P0.x);
			float P2y = (2*P1y - P0.y);

			secondControlPoints.add(new Point(P2x, P2y));
		} else {
			List<Point> rhsArray = new ArrayList<>();

			//Array of Coefficients
			List<Double> a = new ArrayList<>();
			List<Double> b = new ArrayList<>();
			List<Double> c = new ArrayList<>();

			for(int i = 0; i < count; i++) {
				float rhsValueX = 0;
				float rhsValueY = 0;

				Point P0 = dataPoints.get(i);
				Point P3 = dataPoints.get(i + 1);

				if(i == 0) {
					a.add((double) 0);
					b.add((double) 2);
					c.add((double) 1);

					//rhs for first segment
					rhsValueX = P0.x + 2 * P3.x;
					rhsValueY = P0.y + 2 * P3.y;

				} else if(i == count - 1) {
					a.add((double) 2);
					b.add((double) 7);
					c.add((double) 0);

					//rhs for last segment
					rhsValueX = 8 * P0.x + P3.x;
					rhsValueY = 8 * P0.y + P3.y;
				} else {
					a.add((double) 1);
					b.add((double) 4);
					c.add((double) 1);

					rhsValueX = 4 * P0.x + 2 * P3.x;
					rhsValueY = 4 * P0.y + 2 * P3.y;
				}

				rhsArray.add(new Point(rhsValueX, rhsValueY));
			}

			//Solve Ax=B. Use Tridiagonal matrix algorithm a.k.a Thomas Algorithm

			for(int i = 1; i < count; i++) {
				double rhsValueX = rhsArray.get(i).x;
				double rhsValueY = rhsArray.get(i).y;

				double prevRhsValueX = rhsArray.get(i - 1).x;
				double prevRhsValueY = rhsArray.get(i - 1).y;

				double m = a.get(i) / b.get(i - 1);

				double b1 = b.get(i) - m * c.get(i - 1);
				b.set(i, b1);

				double r2x = rhsValueX - m * prevRhsValueX;
				double r2y = rhsValueY - m * prevRhsValueY;

				rhsArray.set(i, new Point((int) r2x, (int) r2y));
			}

			//Get First Control Points

			//Last control Point
			double lastControlPointX = rhsArray.get(count - 1).x / b.get(count - 1);
			double lastControlPointY = rhsArray.get(count - 1).y / b.get(count - 1);

			firstControlPoints.set(count - 1, new Point((int) lastControlPointX, (int) lastControlPointY));

			for(int i = count - 2; i >= 0; --i) {
				Point nextControlPoint = firstControlPoints.get(i + 1);
				if(nextControlPoint != null) {
					double controlPointX = (rhsArray.get(i).x - c.get(i) * nextControlPoint.x) / b.get(i);
					double controlPointY = (rhsArray.get(i).y - c.get(i) * nextControlPoint.y) / b.get(i);

					firstControlPoints.set(i, new Point((int) controlPointX, (int) controlPointY));
				}
			}

			//Compute second Control Points from first

			for(int i = 0; i < count; i++) {
				if(i == count - 1) {
					Point P3 = dataPoints.get(i + 1);

					Point P1 = firstControlPoints.get(i);
					if(P1 == null) {
						continue;
					}

					double controlPointX = (P3.x + P1.x) / 2.0;
					double controlPointY = (P3.y + P1.y) / 2.0;

					secondControlPoints.add(new Point((int) controlPointX, (int) controlPointY));
				} else {
					Point P3 = dataPoints.get(i + 1);

					Point nextP1 = firstControlPoints.get(i + 1);
					if(nextP1 == null) {
						continue;
					}

					double controlPointX = 2 * P3.x - nextP1.x;
					double controlPointY = 2 * P3.y - nextP1.y;

					secondControlPoints.add(new Point((int) controlPointX, (int) controlPointY));
				}
			}
		}

		// Build return doodad

		Map<Point, Pair<Point, Point>> controlPointsByPoints = new HashMap<>();
		for(int i = 1; i < count; i++) {
			controlPointsByPoints.put(dataPoints.get(i), new Pair<>(
					firstControlPoints.get(i),
					secondControlPoints.get(i)));
		}
		return controlPointsByPoints;
	}

	private static class Point {
		float x;
		float y;

		public Point(float x, float y) {
			this.x = x;
			this.y = y;
		}
	}
}
