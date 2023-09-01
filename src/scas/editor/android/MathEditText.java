package scas.editor.android;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.widget.EditText;
import android.widget.Scroller;
import android.text.Selection;

public class MathEditText extends EditText implements OnGestureListener {
	Eval eval = ((NoteEditor)getContext()).getEval();
	AlertDialog dialog = new AlertDialog.Builder(getContext()).setTitle(R.string.error_title).create();
	GestureDetector gestureDetector = new GestureDetector(getContext(), this);
	Scroller scroller = new Scroller(getContext());
	Rect drawingRect = new Rect();
	Rect lineBounds = new Rect();
	Point maxSize = new Point();

	public MathEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public void onDraw(Canvas canvas) {
		int count = getLineCount();
		getDrawingRect(drawingRect);
		getLineBounds(count - 1, lineBounds);
		maxSize.y = Math.max(lineBounds.bottom - drawingRect.height(), 0);
		super.onDraw(canvas);
	}

	@Override
	public void computeScroll() {
		if (scroller.computeScrollOffset()) {
			scrollTo(scroller.getCurrX(), scroller.getCurrY());
		}
	}
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		super.onTouchEvent(event);
		return gestureDetector.onTouchEvent(event);
	}

	public boolean onDown(MotionEvent e) {
		return true;
	}

	public boolean onSingleTapUp(MotionEvent e) {
		return true;
	}

	public void onShowPress(MotionEvent e) {}

	public void onLongPress(MotionEvent e) {}

	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		return true;
	}

	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		scroller.fling(getScrollX(), getScrollY(), -(int) velocityX, -(int) velocityY, 0, maxSize.x, 0, maxSize.y);
		return true;
	}

	@Override
	protected void onCreateContextMenu(ContextMenu menu) {
		MathMenuHandler handler = new MathMenuHandler();
		if (canEval()) {
			menu.add(0, ID_EVAL, 0, R.string.eval).
				 setOnMenuItemClickListener(handler).
				 setAlphabeticShortcut('e');
		}
		super.onCreateContextMenu(menu);
	}

	@Override
	public boolean onTextContextMenuItem(int id) {
		int min = 0;
		int max = getText().length();

		if (isFocused()) {
			final int selStart = getSelectionStart();
			final int selEnd = getSelectionEnd();

			min = Math.max(0, Math.min(selStart, selEnd));
			max = Math.max(0, Math.max(selStart, selEnd));
		}

		switch (id) {

			case ID_EVAL:
				final String data = getText().subSequence(min, max).toString();
				final int n = data.length();
				final boolean newline = n > 0 && "\n".equals(data.substring(n - 1));
				eval.in = data;
				eval.out = null;
				eval.error = null;
				Thread t0 = Thread.currentThread();
				Thread t = new Thread(t0.getThreadGroup(), eval, t0.getName(), 16384l);
				t.start();
				try {
					t.join();
				} catch (InterruptedException e) {}
				if (eval.error != null) {
					dialog.setMessage(eval.error.getMessage());
					dialog.show();
				} else {
					if (eval.out != null && !eval.out.isEmpty()) {
						getText().replace(newline?max:min, max, eval.out);
					}
					Selection.setSelection(getText(), getSelectionEnd());
				}
				return true;
			}

		return super.onTextContextMenuItem(id);
	}

	private static final int ID_EVAL = R.id.eval;

	private boolean canEval() {
		if (getText().length() > 0 && hasSelection()) {
			return true;
		}
		return false;
	}

	private class MathMenuHandler implements MenuItem.OnMenuItemClickListener {
		public boolean onMenuItemClick(MenuItem item) {
			return onTextContextMenuItem(item.getItemId());
		}
	}
}
