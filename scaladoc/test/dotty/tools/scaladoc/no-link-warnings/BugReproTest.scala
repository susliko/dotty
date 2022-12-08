package dotty.tools.scaladoc
package bugRepro

import org.junit.Assert.assertEquals

class BugReproTest extends ScaladocTest("bugRepro"):

  override def args = Scaladoc.Args(
    name = "test",
    tastyFiles = tastyFiles(name),
    output = getTempDir().getRoot,
    projectVersion = Some("1.0")
  )

  override def runTest = afterRendering {
    val diagnostics = summon[DocContext].compilerContext.reportedDiagnostics
    println(diagnostics)
    assertEquals("There should be exactly one warning", 1, diagnostics.warningMsgs.size)
    assertNoErrors(diagnostics)
  }
