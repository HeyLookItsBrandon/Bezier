package com.coalminesoftware.bezier.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.View;

import com.coalminesoftware.bezier.BezierUtils;
import com.coalminesoftware.bezier.Point;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class BezierView extends View {
	/** How many segments to create between points on a curve. */
	private static final int CURVE_SEGMENT_DIVISION_COUNT = 3;
	private Paint curvePaint = buildStrokePaint(5, 0x44CC0000);
	private Paint dotPaint = buildFillPaint(0xFF0000CC);
	private Paint controlPointLinePaint = buildStrokePaint(5, 0x4400CC00);
	private Paint controlPointDotPaint = buildFillPaint(0xFF00CC00);
	private Paint generateCurvePaint = buildStrokePaint(5, 0xFFFFAA00);

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

		List<Point> points = buildTestPoints(canvas);

		Map<Point, Pair<Point, Point>> controlPointsByLeadingPoint =
				BezierUtils.generateControlPoints(points);

		Point trailingPoint = null;
		for (Point leadingPoint : points) {
			Pair<Point, Point> controlPoints = controlPointsByLeadingPoint.get(leadingPoint);

			if(controlPoints != null) {
                drawLine(canvas, trailingPoint, controlPoints.first, controlPointLinePaint);
                drawLine(canvas, controlPoints.second, leadingPoint, controlPointLinePaint);
				drawDot(canvas, controlPoints.first, controlPointDotPaint);
				drawDot(canvas, controlPoints.second, controlPointDotPaint);
			}

			drawDot(canvas, leadingPoint, dotPaint);

			trailingPoint = leadingPoint;
		}

		Path path = buildCubicPath(points, controlPointsByLeadingPoint);
		canvas.drawPath(path, curvePaint);

		List<Point> curvePointString = BezierUtils.buildCurveLineString(
				points, controlPointsByLeadingPoint, CURVE_SEGMENT_DIVISION_COUNT);
		canvas.drawPath(buildLineStringPath(curvePointString), generateCurvePaint);
	}

	private Path buildLineStringPath(List<Point> curvePointString) {
		Path path = new Path();

		Point point = curvePointString.get(0);
		path.moveTo(point.x, point.y);
		for(int i = 1; i < curvePointString.size(); i++) {
			point = curvePointString.get(i);
			path.lineTo(point.x, point.y);
		}

		return path;
	}

	private static List<Point> buildTestPoints(Canvas canvas) {
		return Arrays.asList(
				new Point(canvas.getWidth()*0.33f, canvas.getHeight()*0.33f),
				new Point(canvas.getWidth()*0.67f, canvas.getHeight()*0.33f),
				new Point(canvas.getWidth()*0.67f, canvas.getHeight()*0.67f),
				new Point(canvas.getWidth()*0.33f, canvas.getHeight()*0.67f),
				new Point(canvas.getWidth()*0.5f, canvas.getHeight()*0.5f));
	}

	private static void drawDot(Canvas canvas, Point point, Paint paint) {
		canvas.drawCircle(point.x, point.y, 10, paint);
	}

	private static void drawLine(Canvas canvas, Point start, Point end, Paint paint) {
		canvas.drawLine(start.x, start.y,
				end.x, end.y, paint);
	}

	private static Path buildCubicPath(List<Point> points, Map<Point,Pair<Point,Point>> controlPointsBySegment) {
		Path path = new Path();

		Point point = points.get(0);
		path.moveTo(point.x, point.y);

		for(int i = 1; i < points.size(); i++) {
			point = points.get(i);

			Pair<Point,Point> controlPoints = controlPointsBySegment.get(point);
			if(controlPoints == null) {
				path.lineTo(point.x, point.y);
			} else {
				Point c1 = controlPoints.first;
				Point c2 = controlPoints.second;

				path.cubicTo(c1.x, c1.y, c2.x, c2.y, point.x, point.y);

			}
		}

		return path;
	}

	private static Paint buildStrokePaint(int width, @ColorInt int color) {
		return buildPaint(width, color, Style.STROKE);
	}

	private static Paint buildFillPaint(@ColorInt int color) {
		return buildPaint(0, color, Style.FILL);
	}

	private static Paint buildPaint(int width, @ColorInt int color, Style style) {
		Paint paint = new Paint();
		paint.setStyle(style);
		paint.setStrokeWidth(width);
		paint.setColor(color);

		return paint;
	}
}
