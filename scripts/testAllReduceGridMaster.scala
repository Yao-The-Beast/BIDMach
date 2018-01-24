import BIDMach.allreduce.{AllreduceGridMaster, MasterConfig, MetaDataConfig, ThresholdConfig}
import scala.concurrent.duration._

val nodeNum = 2

val masterConfig = MasterConfig(nodeNum = nodeNum, discoveryTimeout = 5.seconds)

AllreduceGridMaster.startUp("2551", masterConfig = masterConfig)