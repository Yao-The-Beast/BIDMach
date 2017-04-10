package BIDMach.networks.layers


import BIDMat.{Mat,SBMat,CMat,DMat,FMat,IMat,LMat,HMat,GMat,GDMat,GIMat,GLMat,GSMat,GSDMat,SMat,SDMat,TMat,FFilter,Filter,GFilter}
import BIDMat.MatFunctions._
import BIDMat.SciFunctions._
import BIDMach.datasources._
import BIDMach.updaters._
import BIDMach.mixins._
import BIDMach.models._
import BIDMach._
import edu.berkeley.bid.CPUMACH
import edu.berkeley.bid.CUMACH
import scala.util.hashing.MurmurHash3
import java.util.HashMap
import BIDMach.networks._
import java.util.Arrays

/* Many issues to think of    
   How to consider bias...(maybe very difficult?)
*/

class ConvLayer(override val net:Net, override val opts:ConvNodeOpts = new ConvNode ) extends ModelLayer(net, opts, 2) {
    var filter:FMat = null; 
    var ffilter:Filter = null;
    var updateFilter:FMat = null;
    var updateFFilter:Filter = null;
    var bias_mat:FMat = null; // it should be size (channel_out*1*1*1), to better broadcast?
    var update_bias_mat:FMat = null;
    var inputDim:IMat = null; // Should be three numbers
//    var outputDim:IMat = null; //Should be three numbers

  def initModelMats = {
    inputDim = inputData.dims;
    val channel_in = inputDim(0);
    val filter_h = opts.kernel(0); // 3;0
    val filter_w = opts.kernel(1); // 3;
    val npad = opts.pad(0); //1;
    val nstride = opts.stride(0); // 1;
    val channel_out = opts.noutputs; // actually # of filters;

    
    if (modelmats(imodel).asInstanceOf[AnyRef] == null) {
    	modelmats(imodel) = if (net.opts.useGPU && Mat.hasCUDA > 0 && Mat.hasCUDNN) {
    		val x = GFilter.GFilter2Ddn(filter_h,filter_w,channel_in,channel_out,nstride,npad); 
    		x.setTensorFormat(Net.getCUDNNformat(opts.tensorFormat, net.opts.tensorFormat));
    		x;
    	} else {
    		FFilter2Ddn(filter_h,filter_w,channel_in,channel_out,nstride,npad);
    	}
    	modelmats(imodel).asInstanceOf[Filter].xavier(1f);   	
    	updatemats(imodel) = modelmats(imodel).zeros(modelmats(imodel).dims);
    	
    	val biasDim = irow(channel_out,filter_w,filter_h,1);
    	modelmats(imodel+1) = modelmats(imodel).zeros(biasDim);
    	updatemats(imodel+1) = modelmats(imodel).zeros(biasDim);
    	
    }
    filter = modelmats(imodel).asInstanceOf[FMat];
    ffilter = filter.asInstanceOf[Filter];
    updateFFilter = ffilter.copy;
    updateFilter = updateFFilter.asInstanceOf[FMat];
    updateFilter.clear;
    
    bias_mat = modelmats(imodel+1).asInstanceOf[FMat];
    update_bias_mat = updatemats(imodel+1).asInstanceOf[FMat];
  }

  override def forward = {
    val start = toc;
    
    // Create filter model, filter update and bias model if needed
    if (inputDim.asInstanceOf[AnyRef] == null) initModelMats;
    if (output.asInstanceOf[AnyRef] == null){ 
    	var outputBatchDim = Filter.getOutputDims(inputData.dims, ffilter.inDims, ffilter.outDims, ffilter.stride, ffilter.pad, ffilter.outPad);
    	output = filter.zeros(outputBatchDim)
    }
    
    ffilter.convolve(inputData, output, true);
    if (opts.hasBias) output ~ output + bias_mat;

    clearDeriv
    forwardtime += toc - start
  }

  override def backward = {
    val start = toc;
    val ndims = output.dims.length;
    
    if(opts.hasBias){
      update_bias_mat ~ update_bias_mat + (deriv.sum(irow(ndims - 1)) / inputData.ncols);
    }

    if (inputDeriv.asInstanceOf[AnyRef] != null) {      
        ffilter.convolveT(deriv, inputDeriv, false)
    }

    updateFFilter.convolveM(inputData, deriv, false)

    backwardtime += toc - start;
  }

  override def toString = {
    "conv@" + Integer.toHexString(hashCode() % 0x10000)
  }

}

trait ConvNodeOpts extends ModelNodeOpts {
  var noutputs:Int = 0
  var hasBias:Boolean = true
  var pad:IMat = null
  var kernel:IMat = null
  var stride:IMat = null
  var dilation:IMat = null //was dilation:List[Integer] = Arrays.asList(1)
  var tensorFormat:Int = Net.UseNetFormat;

  def copyOpts(opts:ConvNodeOpts):ConvNodeOpts = {
  		super.copyOpts(opts);
  		opts.noutputs = noutputs;
  		opts.hasBias = hasBias;
  		opts.pad = pad;
  		opts.kernel = kernel;
  		opts.stride = stride;
  		opts.dilation = dilation;
  		opts.tensorFormat = tensorFormat;
  		opts;
  }

}

class ConvNode extends Node with ConvNodeOpts {

  def copyTo(opts:ConvNode):ConvNode = {
    this.asInstanceOf[Node].copyTo(opts);
    copyOpts(opts);
    opts
  }

  override def clone:ConvNode = {
    copyTo(new ConvNode ).asInstanceOf[ConvNode]
  }
  
  override def create(net:Net):ConvLayer = {
    ConvLayer(net, this)
  }

  override def toString = {
    "conv@" + Integer.toHexString(hashCode() % 0x10000)
  }

}

object ConvLayer {
  
  def apply(net:Net) = new ConvLayer(net, new ConvNode)
  
  def apply(net:Net, opts:ConvNodeOpts) = new ConvLayer(net, opts)

}
