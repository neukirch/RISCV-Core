/*
RISC-V Pipelined Project in Chisel

This project implements a pipelined RISC-V processor in Chisel. The pipeline includes five stages: fetch, decode, execute, memory, and writeback.
The core is part of an educational project by the Chair of Electronic Design Automation (https://eit.rptu.de/fgs/eis/) at RPTU Kaiserslautern, Germany.

Supervision and Organization: Tobias Jauch, Philipp Schmitz, Alex Wezel
Student Workers: Giorgi Solomnishvili, Zahra Jenab Mahabadi, Tsotne Karchava, Abdullah Shaaban Saad Allam, Kamal Baghirli.

*/

package Cache

import UnifiedMemory.MemArbiter
import chisel3._
import chisel3.util._
import chisel3.experimental._
import chisel3.util.experimental._
import firrtl.annotations.MemoryLoadFileType
import ICache.ICache
import DCache.Cache
import Prefetcher.Prefetcher
import config.IMEMsetupSignals

//TODO Add memory files here?
class DICachesAndMemory (I_memoryFile: String, cacheOnly : Boolean = true) extends Module{
  val testHarness = IO(
    new Bundle {
      //Instruction
      val setupSignals     = Input(new IMEMsetupSignals)
      val requestedAddress = Output(UInt())
    }
  )
  val io = IO(
    new Bundle{
      //Instruction
      val instr_addr = Input(UInt(32.W))
      val instr_out = Output(UInt(32.W))
      val ICACHEvalid = Output(Bool())
      val ICACHEbusy = Output(Bool())
      //Data
      val write_data = Input(UInt(32.W))
      val address = Input(UInt(32.W))
      val write_en = Input(Bool())
      val read_en = Input(Bool())
      val data_out = Output(UInt(32.W))
      val DCACHEvalid = Output(Bool())
      val DCACHEbusy = Output(Bool())
    }
  )

  //!xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
  /*

  Hier statt memory die connections zu Arbiter gemacht
  dann im Arbiter das memory erstellen und connecten

  */
  //!xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx


  //Modules
  val arbiter  = Module(new MemArbiter("src/main/scala/DataMemory/dataMemVals"))
  val dcache = Module(new Cache("src/main/scala/DCache/CacheContent.bin", read_only = false))
  val icache = Module(new Cache("src/main/scala/ICache/ICacheContent.bin", read_only = true))
  
  val ipref = Module(new Prefetcher(I_memoryFile, cacheOnly))
  


  //!Arbiter Connections
  //Instruction
  arbiter.io.iAddr := icache.io.mem_data_addr
  arbiter.io.iReq := icache.io.mem_read_en
  //Data
  arbiter.io.dAddr := dcache.io.mem_data_addr
  arbiter.io.dData := dcache.io.mem_data_in
  arbiter.io.dWrite := dcache.io.mem_write_en
  arbiter.io.dReq := dcache.io.mem_read_en || dcache.io.mem_write_en
  //Outputs
  dcache.io.mem_data_out := arbiter.io.dataRead
  dcache.io.mem_granted:= arbiter.io.grantData
  icache.io.mem_data_out := arbiter.io.dataRead
  icache.io.mem_granted:= arbiter.io.grantInst


  //!
  // arbiter should be connected to caches
  // missing: cache logic changes
  //!

  //xxxxxxxxxxx
  //Data

  dcache.io.data_in.foreach(_ := io.write_data)
  dcache.io.data_addr := io.address
  dcache.io.write_en.foreach(_ := io.write_en)
  dcache.io.read_en := io.read_en
  io.DCACHEvalid := dcache.io.valid
  io.data_out := dcache.io.data_out
  io.DCACHEbusy := dcache.io.busy

  //Prefetcher not for data
  dcache.io.hit := false.B
  dcache.io.prefData := 0.U

  //xxxxxxxxxxx
  //Instruction
          //Prefetcher signals
  ipref.io.missAddress :=   io.instr_addr
  ipref.io.cacheBusy   :=   icache.io.busy
  ipref.io.miss        :=   icache.io.miss
  icache.io.hit        :=   ipref.io.hit
  icache.io.prefData   :=   ipref.io.result


  icache.io.read_en := true.B // Always reading for instruction cache
  icache.io.data_addr := io.instr_addr
  io.ICACHEvalid := icache.io.valid
  io.instr_out := icache.io.data_out
  io.ICACHEbusy := icache.io.busy



  //test harness
  // arbiter.testHarness.dmemSetup.setup := 0.B
  // arbiter.testHarness.dmemSetup.dataIn := 0.U
  // arbiter.testHarness.dmemSetup.dataAddress := 0.U
  // arbiter.testHarness.dmemSetup.readEnable := 0.B
  // arbiter.testHarness.dmemSetup.writeEnable := 0.B

  arbiter.testHarness.setupSignals := testHarness.setupSignals
  testHarness.requestedAddress := arbiter.testHarness.requestedAddress
}




// /*
// RISC-V Pipelined Project in Chisel

// This project implements a pipelined RISC-V processor in Chisel. The pipeline includes five stages: fetch, decode, execute, memory, and writeback.
// The core is part of an educational project by the Chair of Electronic Design Automation (https://eit.rptu.de/fgs/eis/) at RPTU Kaiserslautern, Germany.

// Supervision and Organization: Tobias Jauch, Philipp Schmitz, Alex Wezel
// Student Workers: Giorgi Solomnishvili, Zahra Jenab Mahabadi, Tsotne Karchava, Abdullah Shaaban Saad Allam, Kamal Baghirli.

// */

// package Cache

// import UnifiedMemory.UnifiedMemory
// import chisel3._
// import chisel3.util._
// import chisel3.experimental._
// import chisel3.util.experimental._
// import firrtl.annotations.MemoryLoadFileType
// import ICache.ICache
// import DCache.Cache
// import Prefetcher.Prefetcher
// import config.IMEMsetupSignals

// //TODO Add memory files here?
// class DICachesAndMemory (I_memoryFile: String, cacheOnly : Boolean = true) extends Module{
//   val testHarness = IO(
//     new Bundle {
//       //Instruction
//       val setupSignals     = Input(new IMEMsetupSignals)
//       val requestedAddress = Output(UInt())
//     }
//   )
//   val io = IO(
//     new Bundle{
//       //Instruction
//       val instr_addr = Input(UInt(32.W))
//       val instr_out = Output(UInt(32.W))
//       val ICACHEvalid = Output(Bool())
//       val ICACHEbusy = Output(Bool())
//       //Data
//       val write_data = Input(UInt(32.W))
//       val address = Input(UInt(32.W))
//       val write_en = Input(Bool())
//       val read_en = Input(Bool())
//       val data_out = Output(UInt(32.W))
//       val DCACHEvalid = Output(Bool())
//       val DCACHEbusy = Output(Bool())
//     }
//   )


//   //Modules
//   val mem  = Module(new UnifiedMemory("src/main/scala/DataMemory/dataMemVals"))
//   val dcache = Module(new Cache("src/main/scala/DCache/CacheContent.bin", read_only = false))
//   val icache = Module(new Cache("src/main/scala/ICache/ICacheContent.bin", read_only = true))
  
//   val ipref = Module(new Prefetcher(I_memoryFile, cacheOnly))
  
//   //xxxxxxxxxxx
//   //Data
//   mem.testHarness.dmemSetup.setup := 0.B
//   mem.testHarness.dmemSetup.dataIn := 0.U
//   mem.testHarness.dmemSetup.dataAddress := 0.U
//   mem.testHarness.dmemSetup.readEnable := 0.B
//   mem.testHarness.dmemSetup.writeEnable := 0.B

//   dcache.io.data_in.foreach(_ := io.write_data)
//   dcache.io.data_addr := io.address
//   dcache.io.write_en.foreach(_ := io.write_en)
//   dcache.io.read_en := io.read_en
//   io.DCACHEvalid := dcache.io.valid
//   io.data_out := dcache.io.data_out
//   io.DCACHEbusy := dcache.io.busy

//   mem.io.dataWriteEnable := dcache.io.mem_write_en
//   mem.io.dataReadEnable := dcache.io.mem_read_en
//   mem.io.dataIn := dcache.io.mem_data_in
//   mem.io.dataAddr := dcache.io.mem_data_addr / 4.U
//   dcache.io.mem_data_out := mem.io.dataOut

//   //Prefetcher not for data
//   dcache.io.hit := false.B
//   dcache.io.prefData := 0.U

//   //xxxxxxxxxxx
//   //Instruction
//           //Prefetcher signals
//   ipref.io.missAddress :=   io.instr_addr
//   ipref.io.cacheBusy   :=   icache.io.busy
//   ipref.io.miss        :=   icache.io.miss
//   icache.io.hit        :=   ipref.io.hit
//   icache.io.prefData   :=   ipref.io.result



//   icache.io.read_en := true.B // Always reading for instruction cache
//   icache.io.data_addr := io.instr_addr
//   io.ICACHEvalid := icache.io.valid
//   io.instr_out := icache.io.data_out
//   io.ICACHEbusy := icache.io.busy

//   mem.io.instAddr := icache.io.mem_data_addr // input to memory /4.U
//   icache.io.mem_data_out := mem.io.instOut // output from memory

//   mem.testHarness.imemSetup := testHarness.setupSignals
//   testHarness.requestedAddress := mem.testHarness.requestedAddressIMEM
// }