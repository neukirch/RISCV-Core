package UnifiedMemory
import chisel3._
import chisel3.util._
import config.{IMEMsetupSignals, DMEMsetupSignals, MemUpdates}
import chisel3.experimental.{ChiselAnnotation, annotate}
import chisel3.util.experimental.loadMemoryFromFileInline
import firrtl.transforms.NoDedupAnnotation

class MemArbiter(memFile: String) extends Module {

//TODO
//!
/*
//TODO
finish arbiter with IOs and connect caches to arbiter and arbiter to mem
remove mem IOs to have single port through arbiter
handle stalls in caches depending on arbiter signals
clean up prints etc and add comments to explain changes
//TODO
*/
//!

  // val testHarness = IO(new Bundle {
  //   val imemSetup = Input(new IMEMsetupSignals)
  //   val dmemSetup = Input(new DMEMsetupSignals)
  //   val testUpdatesDMEM = Output(new MemUpdates)
  //   val requestedAddressIMEM = Output(UInt(32.W))
  // })
  val testHarness = IO(
    new Bundle {
      //Instruction
      val setupSignals     = Input(new IMEMsetupSignals)
      val requestedAddress = Output(UInt())
    }
  )

val io = IO(new Bundle {

  //Signals for caches
  //Instructions
  val iAddr = Input(UInt(32.W)) //req addr from I cache
  val iReq = Input(Bool())  //req asserted from I cache on miss

  //Data
  val dAddr = Input(UInt(32.W)) //req addr from D cache
  val dData = Input(UInt(32.W)) //data to write 
  val dWrite = Input(Bool())    //asserted when D cache wants to write
  val dReq = Input(Bool())  //req asserted from D cache on miss

  //Outputs
  val dataRead = Output(UInt(32.W)) //data from mem to send to caches
  val grantData = Output(Bool())        //is data for D cache from mem valid
  val grantInst = Output(Bool())        //is data for I cache from mem valid


  //Signals for memory //! not needed, merged mem directly instantiated in this module
  // val memData = Input(UInt(32.W))   //data received from memory
  // val memAddr = Output(UInt(32.W))  //req addr for memory
  // val memWrite = Output
  })

  //TODO maybe also cache and prefetch connections here????
  val mem  = Module(new UnifiedMemory("src/main/scala/DataMemory/dataMemVals"))

  //default inputs for memory module
  mem.io.addr := 0.U
  mem.io.write := false.B
  mem.io.wdata := 0.U
  mem.io.req := false.B

  //prio data>instruction
  // val grantData = Wire(Bool())
  // grantData := io.dReq
  // val grantInst = Wire(Bool())
  // grantInst := !io.dReq && io.iReq

  //set memory input signals for memory request
  // mem.io.req := grantData || grantInst //request to memory
  // mem.io.addr := Mux(grantData, io.dAddr, io.iAddr) //set req addr according to prio
  // mem.io.write := Mux(grantData, io.dWrite, false.B) // set write if data cache requests write
  // mem.io.wdata := Mux(grantData, io.dData, 0.U) //set data to write
  mem.io.req := io.dReq || io.iReq //request to memory
  mem.io.addr := Mux(io.dReq, io.dAddr, io.iAddr) //set req addr according to prio
  mem.io.write := Mux(io.dReq, io.dWrite, false.B) // set write if data cache requests write
  mem.io.wdata := Mux(io.dReq, io.dData, 0.U) //set data to write

  //set outputs to caches
  io.dataRead := mem.io.dataRead
  // io.grantData := grantData
  // io.grantInst := grantInst
  io.grantData := io.dReq
  io.grantInst := !io.dReq && io.iReq


  //test harness //TODO
  mem.testHarness.dmemSetup.setup := 0.B
  mem.testHarness.dmemSetup.dataIn := 0.U
  mem.testHarness.dmemSetup.dataAddress := 0.U
  mem.testHarness.dmemSetup.readEnable := 0.B
  mem.testHarness.dmemSetup.writeEnable := 0.B
  mem.testHarness.imemSetup := testHarness.setupSignals
  testHarness.requestedAddress := mem.testHarness.requestedAddressIMEM


}
// package UnifiedMemory
// import chisel3._
// import chisel3.util._
// import config.{IMEMsetupSignals, DMEMsetupSignals, MemUpdates}
// import chisel3.experimental.{ChiselAnnotation, annotate}
// import chisel3.util.experimental.loadMemoryFromFileInline
// import firrtl.transforms.NoDedupAnnotation

// class MemoryArbiter extends Bundle {
//     // val io = IO(
//     //     new Bundle{
//     //     //Inputs
//     //         //From Instruction Cache
//     //         val instrAddress        = Input(UInt(32.W))
//     //         val instrReq            = Input(Bool())
//     //         //From Data Cache
//     //         val dataReq             = Input(Bool())
//     //         val dataWriteEnable     = Input(Bool())
//     //         val dataReadEnable      = Input(Bool())
//     //         val dataAddress         = Input(UInt(32.W))
//     //         val writeData           = Input(UInt(32.W))
//     //         //From Memory
//     //         val mem              = Input(UInt(32.W))
//     //         val memValid            = Input(Bool())
//     //     //Outputs
//     //         //To Memory
//     //         val memAddress          = Output(UInt(32.W))
//     //         val memWR               = Output(UInt(32.W))
//     //         //To Caches
//     //         val memOut              = Output(UInt(32.W))
//     //         val memValid            = Output(Bool())
//     //     }

// /*
// //TODO
// finish arbiter with IOs and connect caches to arbiter and arbiter to mem
// remove mem IOs to have single port through arbiter
// handle stalls in caches depending on arbiter signals
// clean up prints etc and add comments to explain changes
// //TODO
// */

// val io = IO(new Bundle {
//     val i_req = Flipped(Decoupled(new Bundle {
//       val addr = UInt(32.W)
//     }))

//     val d_req = Flipped(Decoupled(new Bundle {
//       val addr  = UInt(32.W)
//       val write = Bool()
//       val wdata = UInt(32.W)
//     }))

//     val mem_req = Decoupled(new Bundle {
//       val addr  = UInt(32.W)
//       val write = Bool()
//       val wdata = UInt(32.W)
//     })

//     val mem_resp = Flipped(Valid(UInt(32.W)))

//     val resp = Valid(UInt(32.W))
//   })

//   // prio: data>inst
//   val grantData = io.d_req.valid
//   val grantInst = !grantData && io.i_req.valid

//   // set output signals for mem_req
//   io.mem_req.valid := grantData || grantInst //request
//   io.mem_req.bits.addr := Mux(grantData, io.d_req.bits.addr, io.i_req.bits.addr) //set req addr
//   io.mem_req.bits.write := Mux(grantData, io.d_req.bits.write, false.B) // set write if data cache requests write
//   io.mem_req.bits.wdata := Mux(grantData, io.d_req.bits.wdata, 0.U) //set data to write

//   // Set the ready signals for the caches if req granted and mem is ready
//   io.d_req.ready := grantData && io.mem_req.ready
//   io.i_req.ready := grantInst && io.mem_req.ready

//   // mem_resp is valid when mem_req is valid
//   io.mem_resp.ready := true.B //always valid

//   // Send response to cache
//   io.resp.valid := io.mem_req.valid && io.mem_resp.valid
//   io.resp.bits := io.mem_resp.bits
// }
