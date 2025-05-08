/*
RISC-V Pipelined Project in Chisel

This project implements a pipelined RISC-V processor in Chisel. The pipeline includes five stages: fetch, decode, execute, memory, and writeback.
The core is part of an educational project by the Chair of Electronic Design Automation (https://eit.rptu.de/fgs/eis/) at RPTU Kaiserslautern, Germany.

Supervision and Organization: Tobias Jauch, Philipp Schmitz, Alex Wezel
Student Workers: Giorgi Solomnishvili, Zahra Jenab Mahabadi, Tsotne Karchava, Abdullah Shaaban Saad Allam.

*/

package HazardUnit
import config._
import chisel3.{Output, _}
import chisel3.util._

class HazardUnit extends Module
{
  val io = IO(
    new Bundle {
        val branchType          = Input(UInt(3.W))
        val controlSignalsEXB   = Input(new ControlSignals)
        val controlSignalsMEMB  = Input(new ControlSignals)
        val rs1AddrIFB          = Input(UInt(32.W))
        val rs1AddrIDB          = Input(UInt(32.W))
        val rs2AddrIFB          = Input(UInt(32.W))
        val rs2AddrIDB          = Input(UInt(32.W))
        val rdAddrIDB           = Input(UInt(32.W))
        val rdAddrEXB           = Input(UInt(32.W))
        val rdAddrMEMB          = Input(UInt(32.W))
        val branchTaken         = Input(Bool())
        val btbPrediction       = Input(Bool())
        val wrongAddrPred       = Input(Bool())
        val membusy             = Input(Bool())
        val branchMispredicted  = Output(Bool())
        val stall               = Output(Bool())
        val stall_membusy       = Output(Bool())
        val flushE              = Output(Bool())
        val flushD              = Output(Bool())
        val rs1Select           = Output(UInt(2.W))     //Used to select input to ALU in case of forwarding -- output from FwdUint
        val rs2Select           = Output(UInt(2.W))

        //! Added for Loop_Test_0
        val branchToDo           = Input(Bool())

        //!for load stalls?
        val exLoad           = Input(Bool())
        val dCacheValid      = Input(Bool())
        // val IDBarrierLoad      = Input(Bool())
        val controlSignalsIDB   = Input(new ControlSignals)
        val EXBPC           = Input(UInt(32.W))
        val EXPC           = Input(UInt(32.W))
        val isADDI           = Input(Bool())

        val MEMBPCIn           = Input(UInt(32.W))

        val stall_load_ex           = Output(Bool())
    }
  )



  val stall_load_ex       = Wire(Bool())
  stall_load_ex := false.B
  
  val stall_load_ex_final       = Wire(Bool())
  stall_load_ex_final := false.B


  val stall_load_ex_Reg       = RegInit(false.B)

  io.stall_load_ex := false.B

  



  val stall       = Wire(Bool())

  val loadPC = RegInit(io.EXPC)
  loadPC := io.EXPC

  // Forwarding Unit
    // Handling source register 1
    when((io.rs1AddrIFB =/= 0.U) && (io.rs1AddrIFB === io.rdAddrIDB) && io.exLoad){
      io.stall_load_ex := true.B
      when(io.MEMBPCIn === loadPC){
        io.stall_load_ex := false.B
      }
    }.elsewhen((io.rs2AddrIFB =/= 0.U) && (io.rs2AddrIFB === io.rdAddrIDB) && io.exLoad){
      io.stall_load_ex := true.B
      when(io.MEMBPCIn === loadPC){
        io.stall_load_ex := false.B
      }
    }

    
  
    when((io.rs1AddrIDB =/= 0.U) && (io.rs1AddrIDB === io.rdAddrEXB) && io.controlSignalsEXB.regWrite && !io.controlSignalsEXB.memRead){
        // Normal forward 
        io.rs1Select  := 1.asUInt(2.W)  // Forward from EX/MEM pipeline register (EX Barrier)
        printf(p"Forward from EX/MEM pipeline register (EX Barrier) rs1AddrIDB ${io.rs1AddrIDB}===${io.rdAddrEXB} rdAddrEXB, EXB.regWrite ${io.controlSignalsEXB.regWrite}\n")
        printf(p"\n")
    }  
    .elsewhen((io.rs1AddrIDB =/= 0.U) && (io.rs1AddrIDB === io.rdAddrMEMB) && io.controlSignalsMEMB.regWrite){
      io.rs1Select  := 2.asUInt(2.W)  // Forward from MEM/WB pipeline register (MEM Barrier)
      printf(p"Forward from MEM/WB pipeline register (MEM Barrier)\n")
      printf(p"\n")
    }
    .otherwise{
      io.rs1Select  := 0.asUInt(2.W)
      printf(p"No Forwarding rs1AddrIDB 0x${Hexadecimal(io.rs1AddrIDB)}, rdAddrEXB 0x${Hexadecimal(io.rdAddrEXB)}, rdAddrMEMB 0x${Hexadecimal(io.rdAddrMEMB)}\n")
      printf(p"EXB.regWrite${io.controlSignalsEXB.regWrite}, MEMB.regWrite${io.controlSignalsMEMB.regWrite}\n")
      printf(p"\n")
    }


    // Handling source register 2
    when((io.rs2AddrIDB =/= 0.U) && (io.rs2AddrIDB === io.rdAddrEXB) && io.controlSignalsEXB.regWrite){
        // Normal forward 
        io.rs2Select  := 1.asUInt(2.W)  // Forward from EX/MEM pipeline register (EX Barrier)
        printf(p"Forward from EX/MEM pipeline register (EX Barrier)\n")
      printf(p"\n")
    }
    .elsewhen((io.rs2AddrIDB =/= 0.U) && (io.rs2AddrIDB === io.rdAddrMEMB) && io.controlSignalsMEMB.regWrite){
      io.rs2Select  := 2.asUInt(2.W)  // Forward from MEM/WB pipeline register (MEM Barrier)
      printf(p"Forward from MEM/WB pipeline register (MEM Barrier)\n")
      printf(p"\n")
    }
    .otherwise{
      io.rs2Select  := 0.asUInt(2.W)
      printf(p"No Forwarding rs2AddrIDB 0x${Hexadecimal(io.rs2AddrIDB)}, rdAddrEXB 0x${Hexadecimal(io.rdAddrEXB)}, rdAddrMEMB 0x${Hexadecimal(io.rdAddrMEMB)}\n")
      printf(p"EXB.regWrite${io.controlSignalsEXB.regWrite}, MEMB.regWrite${io.controlSignalsMEMB.regWrite}\n")
      printf(p"\n")
    }


  // Stalling for Load
    when(  (io.rs1AddrIFB =/= 0.U || io.rs2AddrIFB =/= 0.U) 
          && (io.rs1AddrIFB === io.rdAddrIDB || io.rs2AddrIFB === io.rdAddrIDB) 
          && io.controlSignalsEXB.regWrite 
          && io.controlSignalsEXB.memToReg) {
          //!&& !stall_load_ex_Reg) { //!3.4. 
      stall := true.B
    }.otherwise{
      stall := false.B
    }
    stall := false.B//!4.4. 
    // Stall when memory unit is busy
    io.stall_membusy := io.membusy

    // Outputs: Data Hazard -> stall ID & IF stages, and Flush EX stage (Load) ___ Control Hazard -> flush ID & EX stages (Branch Taken)
    // *NOTE*: If io.branchType = DC, this means the branch/jump instruction currently in EX is invalid (flushed!) --> correcting misprediction is invalid too!

    io.stall    := stall //!|| io.membusy
    

    when((io.branchTaken =/= io.btbPrediction &&  io.branchType =/= branch_types.DC) || io.wrongAddrPred){
      io.branchMispredicted := 1.B
    }
    .otherwise{
      io.branchMispredicted := 0.B
    }
    io.flushD   := io.branchMispredicted
    io.flushE   := io.branchMispredicted//!3.4.io.stall | io.branchMispredicted //!io.branchMispredicted

}
