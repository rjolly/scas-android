import java.io.{File, FileReader, FileWriter}
import java.sql.{DriverManager, Date}

val converter = new jscl.converter.Converter("mmltxt.xsl")
val cmd = System.getProperty("note_pad.cmd")
val dirname = System.getProperty("note_pad.dir")
val dir = new File(dirname)
val created = dir.mkdir

def filter(file: File) = !file.isDirectory && file.getName.endsWith(".txt")

Class.forName("org.sqlite.JDBC")
val db = new File("note_pad.db")
val url = "jdbc:sqlite:" + db.getCanonicalPath
val db_path = "/data/data/scas.editor.android/databases/note_pad.db"
val runtime = Runtime.getRuntime

if (cmd == "pull") {
  runtime.exec("adb pull " + db_path)
  dir.listFiles.filter(filter).foreach { file =>
    file.delete
  }
  val conn = DriverManager.getConnection(url)
  val stmt = conn.createStatement
  val rs = stmt.executeQuery("select * from notes")
  var row = rs.next
  while (row) {
    val id = rs.getLong("_id")
    val title = rs.getString("title")
    val text = rs.getString("note")
    val note = if (text.lastIndexOf("\n") < text.length - 1) text + "\n" else text
    val created = rs.getDate("created")
    val modified = rs.getDate("modified")
    val filename = title.trim + ".txt"
    println(filename + "(" + note.length + ", " + created + ", " + modified + ", " + id + ")")
    val file = new File(dir, filename)
    val writer = new FileWriter(file)
    writer.write(note, 0, note.length)
    writer.close
    file.setLastModified(modified.getTime)
    row = rs.next
  }
  rs.close
  stmt.close
  conn.close
} else if (cmd == "push") {
  val conn = DriverManager.getConnection(url)
  val stmt = conn.createStatement
  val del = stmt.executeUpdate("delete from notes")
  stmt.close
  val pstmt = conn.prepareStatement("insert into notes (\"_id\", title, note, created, modified) VALUES (?, ?, ?, ?, ?)")
  var id = 0l
  dir.listFiles.filter(filter).foreach { file =>
    val reader = new FileReader(file)
    val note = converter(reader)
    id += 1
    val name = file.getName
    val title = name.substring(0, name.lastIndexOf(".txt"))
    val modified = new Date(file.lastModified)
    println(title + "(" + note.length + ", " + modified + ", " + id + ")")
    pstmt.setLong(1, id)
    pstmt.setString(2, title)
    pstmt.setString(3, note)
    pstmt.setDate(4, modified)
    pstmt.setDate(5, modified)
    val ins = pstmt.executeUpdate
  }
  stmt.close
  conn.close
  runtime.exec("adb push " + db.getName + " " + db_path)
}
