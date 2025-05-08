/*
RISC-V Pipelined Project in Chisel

This project implements a pipelined RISC-V processor in Chisel. The pipeline includes five stages: fetch, decode, execute, memory, and writeback.
The core is part of an educational project by the Chair of Electronic Design Automation (https://eit.rptu.de/fgs/eis/) at RPTU Kaiserslautern, Germany.

Supervision and Organization: Tobias Jauch, Philipp Schmitz, Alex Wezel
Student Workers: Giorgi Solomnishvili, Zahra Jenab Mahabadi, Tsotne Karchava, Abdullah Shaaban Saad Allam.

*/

// package Stage_MEM

// //!import DCache.CacheAndMemory
// //!import DataMemory.DataMemory
// import Cache.DICachesAndMemory
// import chisel3._
// import chisel3.util._
// import chisel3.experimental.{ChiselAnnotation, annotate}
// import chisel3.util.experimental.loadMemoryFromFileInline
// import firrtl.annotations.MemorySynthInit
// import config.{DMEMsetupSignals, MemUpdates}
// class MEM(DataFile: String) extends Module {

//   val io = IO(
//     new Bundle {
//       val dataIn      = Input(UInt())
//       val dataAddress = Input(UInt(32.W))
//       val writeEnable = Input(Bool())
//       val readEnable  = Input(Bool())
//       val dataOut     = Output(UInt())
//       val dataValid   = Output(Bool())
//       val memBusy     = Output(Bool())
//     })


//   // //val DMEM = Module(new DataMemory())
//   // val DMEM = Module(new DICachesAndMemory(DataFile))//CacheAndMemory())//TODO DataFile is Instr!!!


//   // //DMEM
//   // DMEM.io.write_data  := io.dataIn
//   // DMEM.io.address     := io.dataAddress
//   // DMEM.io.write_en    := io.writeEnable
//   // DMEM.io.read_en     := io.readEnable
//   // //Read data from DMEM
//   // io.dataOut          := DMEM.io.data_out
//   // io.dataValid        := DMEM.io.DCACHEvalid
//   // io.memBusy          := DMEM.io.DCACHEbusy

// }

//!6.3. implemented to achieve correct control flow / propagate signals correctly
//! MEM stage in top_mc looks like WB stage not MEM stage


package Stage_MEM

//!import DCache.CacheAndMemory
//!import DataMemory.DataMemory
import Cache.DICachesAndMemory
import chisel3._
import chisel3.util._
import chisel3.experimental.{ChiselAnnotation, annotate}
import chisel3.util.experimental.loadMemoryFromFileInline
import firrtl.annotations.MemorySynthInit
import config.{DMEMsetupSignals, MemUpdates, ControlSignals, ControlSignalsOB}
class MEM extends Module {

  val io = IO(
    new Bundle {
      val dataIn      = Input(UInt()) //! = inRs2
      val dataAddress = Input(UInt(32.W))
      val dataOut     = Output(UInt())
      val dataValid   = Output(Bool())
      val memBusy     = Output(Bool())

      val inPC = Input(UInt(32.W))
      val outPC = Output(UInt(32.W))

      val inControlSignals  = Input(new ControlSignals)
      val outControlSignals = Output(new ControlSignals)

      val inALUResult       = Input(UInt(32.W))
      val outALUResult      = Output(UInt(32.W))

      val inRd             = Input(UInt())
      val inRs2            = Input(UInt())
      val outRd             = Output(UInt())
      val outRs2            = Output(UInt())

      val Memory_data_out     = Input(UInt())
      val Memory_DCACHEvalid   = Input(Bool())
      val Memory_DCACHEbusy     = Input(Bool())

      val stall_load_ex             = Input(Bool())
    })

  io.outRd := io.inRd
  io.outRs2 := io.inRs2
  io.outPC := io.inPC
  io.outControlSignals := io.inControlSignals
  io.outALUResult := io.inALUResult

  io.dataOut := io.Memory_data_out
  io.dataValid := io.Memory_DCACHEvalid
  io.memBusy := io.Memory_DCACHEbusy

  // val PCReg = RegInit(0.U(32.W))
  // when(PCReg === (io.inPC + 1.U)){
  //   PCReg := PCReg + 1.U
  //   io.outControlSignals.memRead := false.B
  // }.elsewhen(io.stall_load_ex && io.Memory_DCACHEvalid){
  //   io.outControlSignals.memRead := false.B
  // }.elsewhen(io.stall_load_ex){
  //   PCReg := io.inPC
  //   io.outControlSignals.memRead := io.inControlSignals.memRead
  // }//!comb loop

  //!only want to have 1 load per instruction
  // val loadIssued = RegInit(false.B)
  // io.outControlSignals.memRead := io.inControlSignals.memRead && !loadIssued


  // when(io.inControlSignals.memRead && !loadIssued){
  //   loadIssued := true.B 
  // }

  // when(io.Memory_DCACHEvalid){
  //   loadIssued := false.B 
  // }
val requestSent = RegInit(false.B)

val lastPC = RegNext(io.inPC)
val newInstr = io.inPC =/= lastPC

io.outControlSignals.memToReg := io.inControlSignals.memToReg// && !requestSent
io.outControlSignals.regWrite := io.inControlSignals.regWrite// && !requestSent
io.outControlSignals.memRead := io.inControlSignals.memRead && !requestSent
io.outControlSignals.memWrite := io.inControlSignals.memWrite && !requestSent
io.outControlSignals.branch := io.inControlSignals.branch && !requestSent
io.outControlSignals.jump := io.inControlSignals.jump && !requestSent

when(io.outControlSignals.memRead && io.Memory_DCACHEvalid){
  requestSent := true.B
  printf(p"MEM request done for lastPC${(lastPC / 4.U) + 1.U}\n")
  printf(p"regWrite ${io.outControlSignals.regWrite}, memToReg ${io.outControlSignals.memToReg}, memRead ${io.outControlSignals.memRead}, memWrite ${io.outControlSignals.memWrite}\n")
}

when(newInstr){
  requestSent := false.B
}

val dataReg = RegInit(0.U(32.W))
val dataRegPC = RegInit(io.inPC)
when(io.Memory_DCACHEvalid){
  dataReg := io.Memory_data_out
  dataRegPC := io.inPC
}

when(dataRegPC === io.inPC){
  io.dataOut := dataReg
}

}