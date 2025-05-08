/*
RISC-V Pipelined Project in Chisel

This project implements a pipelined RISC-V processor in Chisel. The pipeline includes five stages: fetch, decode, execute, memory, and writeback.
The core is part of an educational project by the Chair of Electronic Design Automation (https://eit.rptu.de/fgs/eis/) at RPTU Kaiserslautern, Germany.

Supervision and Organization: Tobias Jauch, Philipp Schmitz, Alex Wezel
Student Workers: Giorgi Solomnishvili, Zahra Jenab Mahabadi, Tsotne Karchava, Abdullah Shaaban Saad Allam.

*/

package Piplined_RISC_V

import chisel3._
import chisel3.util._
import config.{ControlSignals}
class MEMpipe extends Module
{
  val io = IO(
    new Bundle {
      //val inPCBranch      = Input(UInt())
      val inControlSignals  = Input(new ControlSignals)
      val inRd              = Input(UInt(32.W))
      val inMEMData         = Input(UInt(32.W))
      val inALUResult       = Input(UInt(32.W))
      val stall             = Input(Bool())
      val outMEMData        = Output(UInt(32.W))
      val outControlSignals = Output(new ControlSignals)
      val outRd             = Output(UInt(32.W))
      val outALUResult      = Output(UInt(32.W))


      val inPC      = Input(UInt(32.W))
      val outPC      = Output(UInt(32.W))


      val stall_load_ex             = Input(Bool())
    }
  )
  val pcReg             = RegEnable(io.inPC, 0.U, !io.stall) //RegInit(UInt(), 0.U)
  io.outPC := pcReg



  val ALUResultReg      = RegEnable(io.inALUResult, 0.U, !io.stall) //RegInit(UInt(), 0.U)
  val controlSignalsReg = RegEnable(io.inControlSignals, !io.stall) //Reg(new ControlSignals)
  val rdReg             = RegEnable(io.inRd, 0.U, !io.stall) //RegInit(UInt(), 0.U)

  //Control singals register
  io.outControlSignals := controlSignalsReg

  //immediate data register
  io.outRd             := rdReg

  //MEM data
  val memDataReg        = RegEnable(io.inMEMData, 0.U, !io.stall)
  io.outMEMData        := memDataReg
  // val memDataReg        = RegEnable(io.inMEMData, 0.U, !io.stall)
  // val bufferReg = RegEnable(memDataReg, 0.U, !io.stall)//RegInit(io.inMEMData)
  // //bufferReg := memDataReg
  // // when(memDataReg === 0.U){
  // //   io.outMEMData        := io.inMEMData
  // // }.otherwise{
  // //   io.outMEMData        := memDataReg
  // // }
  // when(bufferReg === 0.U){
  //   io.outMEMData        := io.inMEMData//memDataReg
  // }.otherwise{
  //   io.outMEMData        := bufferReg
  // }

  //!io.outMEMData        := io.inMEMData

  //ALU result register
  io.outALUResult      := ALUResultReg
  
  // printf(p"MEMBarrier io.inALUResult 0x${Hexadecimal(io.inALUResult)}, io.inRd ${io.inRd},  io.inMEMData 0x${Hexadecimal(io.inMEMData)}, io.stall ${io.stall}, in_memToReg ${io.inControlSignals.memToReg}\n")
  // printf(p"MEMBarrier ALUResultReg 0x${Hexadecimal(ALUResultReg)}, rdReg ${rdReg},  io.outMEMData 0x${Hexadecimal(io.outMEMData)}, memToRegRegister ${controlSignalsReg.memToReg}\n")
  //printf(p"MEMBarrier io.outALUResult 0x${Hexadecimal(io.outALUResult)}, io.outRd ${io.outRd},  io.outMEMData 0x${Hexadecimal(io.outMEMData)}, out_memToReg ${io.outControlSignals.memToReg}\n")
  //printf(p"\n")

}