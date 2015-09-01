package android.uirendering.cts.testclasses;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Typeface;
import android.test.suitebuilder.annotation.SmallTest;
import android.uirendering.cts.bitmapcomparers.MSSIMComparer;
import android.uirendering.cts.bitmapverifiers.SamplePointVerifier;
import android.uirendering.cts.testinfrastructure.ActivityTestBase;
import android.uirendering.cts.testinfrastructure.CanvasClient;
import android.uirendering.cts.testinfrastructure.ViewInitializer;
import android.view.View;
import android.view.ViewGroup;
import com.android.cts.uirendering.R;

public class PathClippingTests extends ActivityTestBase {
    // draw circle with whole in it, with stroked circle
    static final CanvasClient sCircleDrawCanvasClient = new CanvasClient() {
        @Override
        public String getDebugString() {
            return "StrokedCircleDraw";
        }

        @Override
        public void draw(Canvas canvas, int width, int height) {
            Paint paint = new Paint();
            paint.setAntiAlias(false);
            paint.setColor(Color.BLUE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(20);
            canvas.drawCircle(50, 50, 40, paint);
        }
    };

    // draw circle with whole in it, by path operations + path clipping
    static final CanvasClient sCircleClipCanvasClient = new CanvasClient() {
        @Override
        public String getDebugString() {
            return "CircleClipDraw";
        }

        @Override
        public void draw(Canvas canvas, int width, int height) {
            canvas.save();

            Path path = new Path();
            path.addCircle(50, 50, 50, Path.Direction.CW);
            path.addCircle(50, 50, 30, Path.Direction.CCW);

            canvas.clipPath(path);
            canvas.drawColor(Color.BLUE);

            canvas.restore();
        }
    };

    @SmallTest
    public void testCircleWithCircle() {
        createTest()
                .addCanvasClient(sCircleDrawCanvasClient, false)
                .addCanvasClient(sCircleClipCanvasClient)
                .runWithComparer(new MSSIMComparer(0.90));
    }

    @SmallTest
    public void testCircleWithPoints() {
        createTest()
                .addCanvasClient(sCircleClipCanvasClient)
                .runWithVerifier(new SamplePointVerifier(
                        new Point[] {
                                // inside of circle
                                new Point(50, 50),
                                // on circle
                                new Point(50 + 32, 50 + 32),
                                // outside of circle
                                new Point(50 + 38, 50 + 38),
                                new Point(100, 100)
                        },
                        new int[] {
                                Color.WHITE,
                                Color.BLUE,
                                Color.WHITE,
                                Color.WHITE,
                        }));
    }

    @SmallTest
    public void testViewRotate() {
        createTest()
                .addLayout(R.layout.blue_padded_layout, new ViewInitializer() {
                    @Override
                    public void intializeView(View view) {
                        ViewGroup rootView = (ViewGroup) view;
                        rootView.setClipChildren(true);
                        View childView = rootView.getChildAt(0);
                        childView.setPivotX(50);
                        childView.setPivotY(50);
                        childView.setRotation(45f);

                    }
                })
                .runWithVerifier(new SamplePointVerifier(
                        new Point[] {
                                // inside of rotated rect
                                new Point(50, 50),
                                new Point(50 + 32, 50 + 32),
                                // outside of rotated rect
                                new Point(50 + 38, 50 + 38),
                                new Point(100, 100)
                        },
                        new int[] {
                                Color.BLUE,
                                Color.BLUE,
                                Color.WHITE,
                                Color.WHITE,
                        }));
    }

    @SmallTest
    public void testTextClip() {
        createTest()
                .addCanvasClient(new CanvasClient() {
                    @Override
                    public void draw(Canvas canvas, int width, int height) {
                        canvas.save();

                        Path path = new Path();
                        path.addCircle(0, 50, 50, Path.Direction.CW);
                        path.addCircle(100, 50, 50, Path.Direction.CW);
                        canvas.clipPath(path);

                        Paint paint = new Paint();
                        paint.setAntiAlias(true);
                        paint.setTextSize(100);
                        paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
                        canvas.drawText("STRING", 0, 100, paint);

                        canvas.restore();
                    }
                })
                .runWithComparer(new MSSIMComparer(0.90));
    }
}
