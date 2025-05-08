/*
RISC-V Pipelined Project in Chisel

This project implements a pipelined RISC-V processor in Chisel. The pipeline includes five stages: fetch, decode, execute, memory, and writeback.
The core is part of an educational project by the Chair of Electronic Design Automation (https://eit.rptu.de/fgs/eis/) at RPTU Kaiserslautern, Germany.

Supervision and Organization: Tobias Jauch, Philipp Schmitz, Alex Wezel
Student Workers: Giorgi Solomnishvili, Zahra Jenab Mahabadi, Tsotne Karchava, Abdullah Shaaban Saad Allam.

*/

package ALU

import chisel3._
import chisel3.util._
import config.ALUOps._


class ALU extends Module {

  val io = IO(new Bundle {
    val src1   = Input(UInt(32.W))
    val src2   = Input(UInt(32.W))
    val ALUop  = Input(UInt(32.W))
    val aluRes = Output(UInt(32.W))
  })

  val ALU_SLT  = Wire(UInt(32.W))
  val ALU_SLTU = Wire(UInt(32.W))

  //SLT operation
  when (io.src1.asSInt < io.src2.asSInt){
    ALU_SLT := 1.U
    // printf(p"SLT\n")
    // printf(p"\n")
  }.otherwise{
    ALU_SLT := 0.U
  }

  //SLTU operation
  when (io.src1 < io.src2){
    ALU_SLTU:= 1.U
    // printf(p"SLTU\n")
    // printf(p"\n")
  }.otherwise{
    ALU_SLTU:= 0.U
  }


  val shamt = io.src2(4,0)
  io.aluRes := 0.U

  
  val aluResReg = RegInit(0.U(32.W))//! 3.4.


  switch(io.ALUop){
    is(ADD){ io.aluRes := (io.src1 + io.src2)//}
      printf(p"ADD io.src1 0x${Hexadecimal(io.src1)}, io.src2 0x${Hexadecimal(io.src2)}, io.aluRes 0x${Hexadecimal(io.aluRes)}\n")
      printf(p"\n")}  // Add
    is(SUB){ io.aluRes := (io.src1 - io.src2)
      printf(p"SUB io.src1 0x${Hexadecimal(io.src1)}, io.src2 0x${Hexadecimal(io.src2)}, io.aluRes 0x${Hexadecimal(io.aluRes)}\n")
      printf(p"\n")}  // Sub

    is(SLL){ io.aluRes := (io.src1 << shamt)
      printf(p"SLL\n")
      printf(p"\n")}   // SLL, SLLI
    is(SRL){ io.aluRes := (io.src1 >> shamt)
      printf(p"SRL\n")
      printf(p"\n")}   // SRL, SRLI
    is(SRA){ io.aluRes := (Fill(32,io.src1(31)) << (31.U(5.W) - (shamt - 1.U(5.W)))) | (io.src1 >> shamt)
      printf(p"SRA\n")
      printf(p"\n")}  // SRA, SRAI

    is(OR){ io.aluRes := io.src1 | io.src2
      printf(p"OR\n")
      printf(p"\n")}     // OR
    is(AND){ io.aluRes := io.src1 & io.src2
      printf(p"AND\n")
      printf(p"\n")}    // AND
    is(XOR){ io.aluRes := io.src1 ^ io.src2
      printf(p"XORL\n")
      printf(p"\n")}    // XOR

    is(SLT){ io.aluRes := ALU_SLT
      printf(p"SLT\n")
      printf(p"\n")}              // SLT,  SLTI, BLT,
    is(SLTU){ io.aluRes := ALU_SLTU
      printf(p"SLTU\n")
      printf(p"\n")}             // SLTU, SLTIU,BLTU

    is(INC_4){ io.aluRes := io.src1 + 4.U
      printf(p"INC_4L\n")
      printf(p"\n")}      // PC increment
    is(COPY_B){ io.aluRes := io.src2
      printf(p"COPY_B\n")
      printf(p"\n")}           //Pass B

    is(LUI){ io.aluRes := (0.U + io.src2)
      printf(p"LUI\n")
      printf(p"\n")}    //LUI
    is(AUIPC){ io.aluRes := (io.src1 + io.src2)
      printf(p"AUIPC\n")
      printf(p"\n")}      // AUIPC
    is(DC){ io.aluRes := aluResReg //! 3.4. io.aluRes := io.src1 - io.src2
      printf(p"DC\n")
      printf(p"\n")}
  }

  aluResReg := io.aluRes //! 3.4.
  //printf(p"\n")

}

