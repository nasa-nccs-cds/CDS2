package nasa.nccs.cds2.modules
import nasa.nccs.cdapi.kernels.{ Kernel, Port, KernelModule, ExecutionResult, DataFragment }
import nasa.nccs.cds2.kernels.KernelTools
import org.nd4j.linalg.factory.Nd4j

class CDS extends KernelModule with KernelTools {
  override val version = "1.0-SNAPSHOT"
  override val organization = "nasa.nccs"
  override val author = "Thomas Maxwell"
  override val contact = "thomas.maxwell@nasa.gov"

  class average extends Kernel {
    val inputs = List(Port("input fragment", "1"))
    val outputs = List(Port("result", "1"))
    override val description = "Average over Input Fragment"

    def execute(inputSubsets: List[DataFragment], run_args: Map[String, Any]): ExecutionResult = {
      val input_array = getNdArray(inputSubsets)
      val mean_val = Nd4j.mean(input_array).getFloat(0)
      val result = Array[Float](mean_val)
      logger.info("Kernel %s: Executed operation %s, result = %s ".format(name, operation, result.mkString("[", ",", "]")))
      new ExecutionResult(Array.emptyFloatArray)
    }
  }
}

object arrayTest extends App {
  var data = Array(1.0.toFloat, 1.0.toFloat, Float.NaN )
  var arr2 = Nd4j.create( data )
  var fmean = arr2.mean(1)
  println( "Mean: "+ fmean.toString )
}
