package nasa.nccs.cds2.engine.spark

import nasa.nccs.cdapi.kernels.DataFragment
import nasa.nccs.cdapi.cdm
import nasa.nccs.cdapi.cdm.PartitionedFragment
import nasa.nccs.cds2.engine.{CollectionDataLoader, SampleTaskRequests, CDS2ExecutionManager}
import nasa.nccs.esgf.process._
import org.apache.spark.rdd.RDD

import scala.collection.mutable

class RDDataManager( val cdsContext: CDSparkContext, domainMap: Map[String,DomainContainer] ) extends DataManager( domainMap, new CollectionDataLoader() ) {
  var prdds = mutable.Map[String, RDD[PartitionedFragment]]()

  def loadRDData( data_container: DataContainer, nPart: Int ):  RDD[PartitionedFragment] = {
    val uid: String = data_container.uid
    val data_source: DataSource = data_container.getSource
    val axisConf: List[OperationSpecs] = data_container.getOpSpecs
    prdds.get(uid) match {
      case Some(prdd) => prdd
      case None =>
        val dataset: cdm.CDSDataset = dataLoader.getDataset(data_source)
        domainMap.get(data_source.domain) match {
          case Some(domain_container) =>
            val variable = dataset.loadVariable(uid, data_source.name)
            val partAxis = 't'   // TODO: Compute this
            val pRDD = cdsContext.makeFragmentRDD( variable, domain_container.axes, partAxis, nPart, axisConf )
            prdds += uid -> pRDD
            logger.info("Loaded variable %s (%s:%s) subset data, shape = %s ".format(uid, data_source.collection, data_source.name, "") ) // pRDD.shape.toString) )
            pRDD
          case None =>
            throw new Exception("Undefined domain for dataset " + data_source.name + ", domain = " + data_source.domain)
        }
    }
  }
}

class CDSparkExecutionManager( val cdsContext: CDSparkContext ) extends CDS2ExecutionManager {

  override def execute( request: TaskRequest, run_args: Map[String,String] ): xml.Elem = {
    logger.info("Execute { request: " + request.toString + ", runargs: " + run_args.toString + "}"  )
    val data_manager = new RDDataManager( cdsContext, request.domainMap )
    val nPart = 4  // TODO: Compute this
    for( data_container <- request.variableMap.values; if data_container.isSource )  data_manager.loadRDData( data_container, nPart )
    executeWorkflows( request.workflows, data_manager, run_args ).toXml
  }
}

object sparkExecutionTest extends App {
  import org.apache.spark.{SparkContext, SparkConf}
  // Run with: spark-submit  --class "nasa.nccs.cds2.engine.sparkExecutionTest"  --master local[4] /usr/local/web/Spark/CDS2/target/scala-2.11/cds2_2.11-1.0-SNAPSHOT.jar
  val conf = new SparkConf().setAppName("SparkExecutionTest")
  val sc = new CDSparkContext(conf)
  val npart = 4
  val request = SampleTaskRequests.getAveArray
  val run_args = Map[String,String]()
  val cdsExecutionManager = new CDSparkExecutionManager( sc )
  val result = cdsExecutionManager.execute( request, run_args )
  println( result.toString )
}


