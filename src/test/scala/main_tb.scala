package main_tb
import RISCV_TOP.RISCV_TOP
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import top_MC._
import chisel3._
import DataTypes.Data._
import java.sql.Driver

class main_tb extends AnyFlatSpec with ChiselScalatestTester {

  "main_tb" should "pass" in {
    //test(new RISCV_TOP("src/test/programs/test0")).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
    test(new RISCV_TOP("src/main/resources/mem.hex")).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(0)
      for(i <- 0 until 591){//beq 800, bge  800, bgeu 850, blt 720, bltu 760, bne 730
      //add   pc 373->379,380.... end success, 374 fail         pass at 1121 cycles
      //beq   pc 239->245,246.... end success, 242 fail         pass at 688 cycles
      //bge   pc 263->269,270.... end success, 264 fail         pass at 778 cycles
      //bgeu  pc 276->282,283.... end success, 277 fail         pass at 833 cycles
      //blt   pc 239->245,246.... end success, 240 fail         pass at 688 cycles
      //bltu  pc 252->258,259.... end success, 253 fail         pass at 743 cycles
      //bne   pc 240->246,247.... end success, 241 fail         pass at 697 cycles

      //lui   pc  88 -> 94,95... end success, 89 fail           pass at 77 cycles
      //lw    pc  227 -> 233,234... end success, 228 fail       pass at 591 cycles
      //sw    pc  353 -> 359,360... end success, 354 fail                    ----- fails
  




      //not implemented?
      //lb    pc  -> ... end success,  fail               ----- fails
      //lbu   pc  -> ... end success,  fail               ----- fails
      //lh    pc  -> ... end success,  fail               ----- fails
      //lhu   pc  -> ... end success,  fail               ----- fails
      //sb    pc  -> ... end success,  fail               ----- fails
      //sh    pc  -> ... end success,  fail               ----- fails


      //sw
      //test 2 done at 37
        dut.clock.step()
      }
    }
  }
}