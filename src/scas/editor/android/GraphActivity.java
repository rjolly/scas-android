package scas.editor.android;

import scas.Graph;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.view.View;

public class GraphActivity extends Activity {
    Graph graph;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();
        graph = (Graph)intent.getSerializableExtra(getPackageName() + ".graph");
        setContentView(R.layout.graph);
    }

    public static class GraphView extends View {
        Graph graph = ((GraphActivity)getContext()).graph;
        Paint paint = new Paint();
        Paint red = new Paint();
        int w = getWidth();
        int h = getHeight();
        double z = 1.0;
        double x0 = 0.0;
        double y0 = 0.0;
        float x1;
        float y1;

        OnGestureListener glistener = new SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                x1 = e.getX();
                y1 = e.getY();
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                shift(e2.getX() - x1, e2.getY() - y1);
                invalidate();
                x1 = e2.getX();
                y1 = e2.getY();
                return true;
            }
        };
        GestureDetector gdetector = new GestureDetector(getContext(), glistener);

        OnScaleGestureListener sglistener = new SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                shift(detector.getFocusX() - x1, detector.getFocusY() - y1);
                scale(detector.getScaleFactor());
                invalidate();
                x1 = detector.getFocusX();
                y1 = detector.getFocusY();
                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                x1 = detector.getFocusX();
                y1 = detector.getFocusY();
                return true;
            }
        };
        ScaleGestureDetector sgdetector = new ScaleGestureDetector(getContext(), sglistener);

        void shift(double x, double y) {
            x0 += x / (double)h * 2.0 / z;
            y0 += y / (double)h * 2.0 / z;
        }

        void scale(double a) {
            z *= a;
        }

        public GraphView(Context context, AttributeSet attrs) {
            super(context, attrs);
            paint.setColor(Color.WHITE);
            red.setColor(Color.RED);
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed, left, top, right, bottom);
            w = getWidth();
            h = getHeight();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            super.onTouchEvent(event);
            return gdetector.onTouchEvent(event) | sgdetector.onTouchEvent(event);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int m1 = 0;
            float p[] = new float[w << 2];
            for (int n = 0 ; n <= w ; n++) {
                double x = (2.0 * (double)n - (double)w) / (double)h / z - x0;
                double y = graph.apply(x);
                int m = (int)(((double)h + (y0 - y) * z * (double)h) / 2.0);
                if (n > 0) {
                    int k = (n - 1) << 2;
                    p[k] = (float)(n - 1);
                    p[k + 1] = (float)m1;
                    p[k + 2] = (float)n;
                    p[k + 3] = (float)m;
                }
                m1 = m;
            }
            int x2 = (int)(((double)w + x0 * z * (double)h) / 2.0);
            int y2 = (int)(((double)h + y0 * z * (double)h) / 2.0);
            canvas.drawLine(0.0f, (float)y2, (float)w, (float)y2, red);
            canvas.drawLine((float)x2, 0.0f, (float)x2, (float)h, red);
            canvas.drawLines(p, paint);
        }
    }
}
