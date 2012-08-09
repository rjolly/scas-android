package scas.android

import _root_.android.app.Activity
import _root_.android.os.Bundle
import scas.application.MyApp

class MainActivity extends Activity with TypedActivity {
  override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)
    setContentView(R.layout.main)
    MyApp.main(Array.empty[String])
    findView(TR.textview).setText("hello, world!")
  }
}
