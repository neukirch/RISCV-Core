/*
RISC-V Pipelined Project in Chisel

This project implements a pipelined RISC-V processor in Chisel. The pipeline includes five stages: fetch, decode, execute, memory, and writeback.
The core is part of an educational project by the Chair of Electronic Design Automation (https:  //eit.rptu.de/fgs/eis/) at RPTU Kaiserslautern, Germany.

Supervision and Organization: Tobias Jauch, Philipp Schmitz, Alex Wezel
Student Workers: Giorgi Solomnishvili, Zahra Jenab Mahabadi, Tsotne Karchava

*/

package top_MC

import Prefetcher.Prefetcher

import Cache.DICachesAndMemory
import config.{ControlSignals, IMEMsetupSignals, Inst, Instruction}
import config.Inst._


import chisel3._
import chisel3.util._
import Piplined_RISC_V._
import Stage_ID.ID
import Stage_IF.IF
import Stage_EX.EX
import Stage_MEM.MEM
import HazardUnit.HazardUnit
import config.{MemUpdates, RegisterUpdates, SetupSignals, TestReadouts}

class top_MC(BinaryFile: String, DataFile: String) extends Module {

  val testHarness = IO(
    new Bundle {
      val setupSignals = Input(new SetupSignals)
      val testReadouts = Output(new TestReadouts)
      val regUpdates   = Output(new RegisterUpdates)
      val memUpdates   = Output(new MemUpdates)
      val currentPC    = Output(UInt(32.W))
    }
  )

  
  // Pipeline Registers
  val IFBarrier  = Module(new IFpipe).io
  val IDBarrier  = Module(new IDpipe).io
  val EXBarrier  = Module(new EXpipe).io
  val MEMBarrier = Module(new MEMpipe).io

  // Pipeline Stages
  val IF  = Module(new IF)
  val ID  = Module(new ID)
  val EX  = Module(new EX)
  val MEM = Module(new MEM)
  val writeBackData = Wire(UInt()) //! do actual WB stage?

  // Memory
  val Memory  = Module(new DICachesAndMemory(BinaryFile))

  // Hazard Unit
  val HzdUnit = Module(new HazardUnit)

  // Test Harness -> not set up correctly?
  IF.testHarness.InstructionMemorySetup := testHarness.setupSignals.IMEMsignals
  ID.testHarness.registerSetup          := testHarness.setupSignals.registerSignals
  testHarness.testReadouts.registerRead := ID.testHarness.registerPeek
  testHarness.testReadouts.DMEMread  := Memory.io.data_out
  testHarness.regUpdates                := ID.testHarness.testUpdates
  testHarness.memUpdates                := 0.U.asTypeOf(new MemUpdates)
  Memory.testHarness.setupSignals := testHarness.setupSignals.IMEMsignals
  testHarness.currentPC                 := Memory.testHarness.requestedAddress


  // IF Stage
  IF.io.branchTaken        := EX.io.branchTaken
  IF.io.IFBarrierPC        := IFBarrier.outCurrentPC
  IF.io.stall              := HzdUnit.io.stall | HzdUnit.io.stall_membusy  | HzdUnit.io.stall_load_ex 
  IF.io.newBranch          := EX.io.newBranch
  IF.io.updatePrediction   := EX.io.updatePrediction
  IF.io.entryPC            := IDBarrier.outPC
  IF.io.branchAddr         := EX.io.branchTarget
  IF.io.branchMispredicted := HzdUnit.io.branchMispredicted
  IF.io.PCplus4ExStage     := EX.io.outPCplus4
  IF.io.instructionICache  := Memory.io.instr_out.asTypeOf(new Instruction)
  IF.io.memoryPCin         := Memory.io.pcOut
  IF.io.ICACHEvalid        := Memory.io.ICACHEvalid

  // Signals to IFBarrier
  IFBarrier.inCurrentPC        := IF.io.PC
  IFBarrier.inInstruction      := IF.io.instruction
  IFBarrier.stall              := HzdUnit.io.stall | HzdUnit.io.stall_membusy | HzdUnit.io.stall_load_ex
  IFBarrier.flush              := HzdUnit.io.flushD
  IFBarrier.inBTBHit           := IF.io.btbHit
  IFBarrier.inBTBPrediction    := IF.io.btbPrediction
  IFBarrier.inBTBTargetPredict := IF.io.btbTargetPredict
  IFBarrier.branchAddr         := EX.io.branchTarget
  IFBarrier.branchTaken        := EX.io.branchTaken
  IFBarrier.branchMispredicted := HzdUnit.io.branchMispredicted

  // ID stage
  ID.io.inPC                  := IFBarrier.outCurrentPC
  ID.io.instruction           := IFBarrier.outInstruction
  ID.io.registerWriteAddress  := MEMBarrier.outRd
  ID.io.registerWriteEnable   := MEMBarrier.outControlSignals.regWrite
  ID.io.stall_load_ex         := HzdUnit.io.stall_load_ex
  ID.io.registerWriteData     := writeBackData

  // Signals to IDBarrier
  IDBarrier.inInstruction      := ID.io.instruction
  IDBarrier.inControlSignals   := ID.io.controlSignals
  IDBarrier.inBranchType       := ID.io.branchType
  IDBarrier.inPC               := IFBarrier.outCurrentPC
  IDBarrier.flush              := HzdUnit.io.flushE
  IDBarrier.stall              := HzdUnit.io.stall | HzdUnit.io.stall_membusy | HzdUnit.io.stall_load_ex
  IDBarrier.inOp1Select        := ID.io.op1Select
  IDBarrier.inOp2Select        := ID.io.op2Select
  IDBarrier.inImmData          := ID.io.immData
  IDBarrier.inRd               := IFBarrier.outInstruction.registerRd
  IDBarrier.inALUop            := ID.io.ALUop
  IDBarrier.inReadData1        := ID.io.readData1
  IDBarrier.inReadData2        := ID.io.readData2
  IDBarrier.inBTBHit           := IFBarrier.outBTBHit
  IDBarrier.inBTBPrediction    := IFBarrier.outBTBPrediction
  IDBarrier.inBTBTargetPredict := IFBarrier.outBTBTargetPredict
  IDBarrier.isADDI             := ID.io.isADDI

  // EX stage
  EX.io.instruction           := IDBarrier.outInstruction
  EX.io.controlSignals        := IDBarrier.outControlSignals
  EX.io.PC                    := IDBarrier.outPC
  EX.io.branchType            := IDBarrier.outBranchType
  EX.io.op1Select             := IDBarrier.outOp1Select
  EX.io.op2Select             := IDBarrier.outOp2Select
  EX.io.rs1Select             := HzdUnit.io.rs1Select
  EX.io.rs2Select             := HzdUnit.io.rs2Select
  EX.io.rs1                   := IDBarrier.outReadData1
  EX.io.rs2                   := IDBarrier.outReadData2
  EX.io.immData               := IDBarrier.outImmData
  EX.io.ALUop                 := IDBarrier.outALUop
  EX.io.ALUresultEXB          := EXBarrier.outALUResult
  EX.io.ALUresultMEMB         := writeBackData
  EX.io.btbHit                := IDBarrier.outBTBHit
  EX.io.btbTargetPredict      := IDBarrier.outBTBTargetPredict

  // Signals to EXBarrier
  EXBarrier.inALUResult       := EX.io.ALUResult
  EXBarrier.inControlSignals  := IDBarrier.outControlSignals
  EXBarrier.inRd              := IDBarrier.outRd
  EXBarrier.inRs2             := EX.io.Rs2Forwarded
  EXBarrier.stall             := HzdUnit.io.stall_membusy
  EXBarrier.exLoadIn          := EX.io.exLoad
  EXBarrier.inPC              := IDBarrier.outPC    
  EXBarrier.stall_load_ex     := HzdUnit.io.stall_load_ex

  // MEM stage
  MEM.io.dataIn               := EXBarrier.outRs2
  MEM.io.dataAddress          := EXBarrier.outALUResult
  MEM.io.inPC                 := EXBarrier.outPC
  MEM.io.inControlSignals     := EXBarrier.outControlSignals
  MEM.io.inALUResult          := EXBarrier.outALUResult
  MEM.io.inRd                 := EXBarrier.outRd
  MEM.io.inRs2                := EXBarrier.outRs2
  MEM.io.stall_load_ex        := HzdUnit.io.stall_load_ex //? not needed?
  //?stall?
  //Read data from DMEM
  MEM.io.Memory_data_out      := Memory.io.data_out
  MEM.io.Memory_DCACHEvalid   := Memory.io.DCACHEvalid
  MEM.io.Memory_DCACHEbusy    := Memory.io.DCACHEbusy

  // MEMBarrier
  MEMBarrier.inControlSignals := MEM.io.outControlSignals
  MEMBarrier.inALUResult      := MEM.io.outALUResult
  MEMBarrier.inRd             := MEM.io.outRd
  MEMBarrier.inMEMData        := MEM.io.dataOut
  MEMBarrier.stall            := HzdUnit.io.stall_membusy
  MEMBarrier.inPC             := MEM.io.outPC
  MEMBarrier.stall_load_ex    := HzdUnit.io.stall_load_ex //? not needed?

  //Read data from DMEM
  //!MEMBarrier.inMEMData          := Memory.io.data_out
  //!do this in MEM stage

  // MEM stage //!WB stage?
  //Mux for which data to write to register
  when(MEMBarrier.outControlSignals.memToReg){
    writeBackData := MEMBarrier.outMEMData
    ID.io.registerWriteData := MEMBarrier.outMEMData
    printf(p"WB memToReg writeBackData 0x${Hexadecimal(MEMBarrier.outMEMData)} at ${MEMBarrier.outRd}, enabled: ${MEMBarrier.outControlSignals.regWrite}\n")
  }.otherwise{
    writeBackData := MEMBarrier.outALUResult
    ID.io.registerWriteData := MEMBarrier.outALUResult
    printf(p"WB NO memToReg writeBackData 0x${Hexadecimal(MEMBarrier.outALUResult)} at ${MEMBarrier.outRd}, enabled: ${MEMBarrier.outControlSignals.regWrite}\n")
  } //!writeBackData for what? into ID and EX
  printf(p"MEMBarrier outPC: ${(MEMBarrier.outPC / 4.U) + 1.U}\n")
  printf(p"\n")

  // Signals to Hazard Unit
  HzdUnit.io.controlSignalsEXB  := EXBarrier.outControlSignals
  HzdUnit.io.controlSignalsMEMB := MEMBarrier.outControlSignals
  HzdUnit.io.rs1AddrIFB         := IFBarrier.outInstruction.registerRs1
  HzdUnit.io.rs2AddrIFB         := IFBarrier.outInstruction.registerRs2
  HzdUnit.io.rs1AddrIDB         := IDBarrier.outInstruction.registerRs1
  HzdUnit.io.rs2AddrIDB         := IDBarrier.outInstruction.registerRs2
  HzdUnit.io.rdAddrIDB          := IDBarrier.outInstruction.registerRd
  HzdUnit.io.rdAddrEXB          := EXBarrier.outRd
  HzdUnit.io.rdAddrMEMB         := MEMBarrier.outRd
  HzdUnit.io.branchTaken        := EX.io.branchTaken
  HzdUnit.io.wrongAddrPred      := EX.io.wrongAddrPred
  HzdUnit.io.btbPrediction      := IDBarrier.outBTBPrediction
  HzdUnit.io.branchType         := IDBarrier.outBranchType
  HzdUnit.io.membusy            := Memory.io.DCACHEbusy | Memory.io.ICACHEbusy //! maybe actually only if memory is accessed, not caches
  HzdUnit.io.branchToDo         := IF.io.branchToDo
  HzdUnit.io.dCacheValid        := Memory.io.DCACHEvalid
  HzdUnit.io.exLoad             := EX.io.exLoad
  HzdUnit.io.MEMBPCIn           := MEMBarrier.outPC
  HzdUnit.io.isADDI             := IDBarrier.ADDIIDPipe //! remove all ADDI signals
  HzdUnit.io.EXBPC              := EXBarrier.outPC 
  HzdUnit.io.EXPC               := EX.io.PC 
  HzdUnit.io.controlSignalsIDB  := IDBarrier.outControlSignals



  // Signals to Memory
  // Data
  Memory.io.write_data  := MEM.io.outRs2
  Memory.io.address     := MEM.io.outALUResult
  Memory.io.write_en    := MEM.io.outControlSignals.memWrite
  Memory.io.read_en     := MEM.io.outControlSignals.memRead
  Memory.io.flushed     := HzdUnit.io.flushD
  //TODO put these in MEM and properly connect MEMBarrier with MEM
  // Instruction
  Memory.io.instr_addr  := IF.io.PC




 
  

  //! in mem stage?
  // EX.io.dcacheread := Memory.io.data_out
  // EX.io.dcachevalid := Memory.io.DCACHEvalid
  


  EX.io.outMEMDataMEMB         := MEMBarrier.outMEMData

    // Prints for Debugging

  val printReg = RegInit(0.U(2.W))
  when(((IFBarrier.outCurrentPC /  4.U) + 1.U) === 1.U && printReg === 0.U){
    for(i<-0 to 100){ 
      printf(p"\n") 
    }
    printReg := 1.U
    printf(p"\n")
    printf(p"                             Start\n")
    printf(p"\n")
  }
  printf(p"\n")
  printf(p"\n")
  printf(p"------------------------------------------------------------------------\n")
  printf(p"\n")

  //printf(p"------------------------------------IF------------------------------------\n")
  printf(p"IF PC: ${(IF.io.PC/ 4.U) + 1.U}, instruction: 0x${Hexadecimal(IF.io.instruction.asUInt)}, branchTaken: ${IF.io.branchTaken}, branchAddr: ${IF.io.branchAddr}\n")
  printf(p"\n")

  //printf(p"------------------------------------IFBarrier------------------------------------\n")
  printf(p"IFBarrier outPC: ${(IFBarrier.outCurrentPC / 4.U) + 1.U}, instruction: 0x${Hexadecimal(IFBarrier.outInstruction.asUInt)}, stall: ${IFBarrier.stall}, flush: ${IFBarrier.flush}\n")
  printf(p"\n")

  //printf(p"------------------------------------ID------------------------------------\n")
  printf(p"ID instruction: 0x${Hexadecimal(ID.io.instruction.asUInt)}, Rd: ${ID.io.instruction.registerRd}, Rs1: ${ID.io.instruction.registerRs1}, Rs2: ${ID.io.instruction.registerRs2}\n")
  printf(p"\n")

  //printf(p"------------------------------------IDBarrier------------------------------------\n")
  printf(p"IDBarrier outPC: ${(IDBarrier.outPC / 4.U) + 1.U}, instruction: 0x${Hexadecimal(IDBarrier.outInstruction.asUInt)}, stall: ${IDBarrier.stall}, flush: ${IDBarrier.flush}\n")
  // printf(p"IDBarrier inImmData: 0x${Hexadecimal(IDBarrier.inImmData)}, outImmData: 0x${Hexadecimal(IDBarrier.outImmData)}, outReadData1: 0x${Hexadecimal(IDBarrier.outReadData1)}\n")
  printf(p"\n")

  //printf(p"------------------------------------EX------------------------------------\n")
  printf(p"EX PC: ${(EX.io.PC / 4.U) + 1.U}, instruction: 0x${Hexadecimal(EX.io.instruction.asUInt)}, ALUResult: 0x${Hexadecimal(EX.io.ALUResult.asUInt)}\n")
  printf(p"\n")

  //printf(p"------------------------------------EXBarrier------------------------------------\n")
  printf(p"EXBarrier PC: ${(EXBarrier.outPC / 4.U) + 1.U}, stall: ${EXBarrier.stall}, outALUResult: 0x${Hexadecimal(EXBarrier.outALUResult)}, outRd: 0x${Hexadecimal(EXBarrier.outRd)}, outRs2: 0x${Hexadecimal(EXBarrier.outRs2)}, regWrite ${EXBarrier.outControlSignals.regWrite}, memToReg ${EXBarrier.outControlSignals.memToReg}\n")
  printf(p"\n")

  //printf(p"------------------------------------MEM------------------------------------\n")
  printf(p"MEM outPC: ${(MEM.io.outPC / 4.U) + 1.U}, dataOut: 0x${Hexadecimal(MEM.io.dataOut)} ,outRd: ${MEM.io.outRd}, outALUResult: 0x${Hexadecimal(MEM.io.outALUResult)}\n")
  printf(p"\n")

  //printf(p"------------------------------------MEMBarrier------------------------------------\n")
  printf(p"MEMBarrier outPC: ${(MEMBarrier.outPC / 4.U) + 1.U},outRd: ${MEMBarrier.outRd}, outMEMData: 0x${Hexadecimal(MEMBarrier.outMEMData)}, outALUResult: 0x${Hexadecimal(MEMBarrier.outALUResult)}\n")
  printf(p"\n")

  //printf(p"------------------------------------WB STAGE?????------------------------------------\n")
  printf(p"WB???? writeBackData: 0x${Hexadecimal(writeBackData)}, ID.io.registerWriteData: 0x${Hexadecimal(ID.io.registerWriteData)}\n")
  printf(p"\n")

  //printf(p"------------------------------------BTB------------------------------------\n")
  //printf(p"BTB IF.io.btbHit: ${IF.io.btbHit}, IDBarrier.inBTBHit: ${IDBarrier.inBTBHit}, EX.io.btbHit: ${EX.io.btbHit}\n")
  //printf(p"\n")

  //printf(p"------------------------------------HzdUnit------------------------------------\n")
  printf(p"HZDUNIT stall: ${HzdUnit.io.stall}, stall_membusy: ${HzdUnit.io.stall_membusy}, stall_load_ex: ${HzdUnit.io.stall_load_ex}, flushD: ${HzdUnit.io.flushD}\n")
  printf(p"\n")


  printf(p"\n")
  printf(p"\n")
}