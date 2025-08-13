package scas.editor.android;

import android.app.Activity;
import android.content.Intent;

import java.io.IOException;
import java.io.StringReader;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import jscl.editor.Code;
import jscl.editor.rendering.MathObject;
import jscl.editor.rendering.Plot;

class Eval implements Runnable {
    String in;
    String out;
    Throwable error;
    private final Activity activity;
    private final ScriptEngine engine;
    private final Code code;

    Eval(final ScriptEngineFactory factory, final Code code, final Activity activity) {
        this.activity = activity;
        this.engine = factory.getScriptEngine();
        this.code = code;
    }

    public void run() {
        try {
            Object obj = engine.eval(in);
            if (obj instanceof Plot) {
                start((Plot)obj);
            } else if (obj instanceof MathObject) {
                out = apply((MathObject)obj);
            } else {
                out = apply(obj);
            }
        } catch (Throwable e) {
            error = e;
        }
    }

    String apply(MathObject obj) throws IOException {
        return code.apply(new StringReader("<math>" + obj.toMathML() + "</math>"));
    }

    String apply(Object obj) {
        return obj == null?null:obj.toString();
    }

    void start(Plot graph) {
        final Intent intent = new Intent(activity, GraphActivity.class);
        intent.putExtra(activity.getPackageName() + ".graph", graph);
        activity.startActivity(intent);
    }
}
