package UnifiedMemory
import chisel3._
import chisel3.util._
import config.{IMEMsetupSignals, DMEMsetupSignals, MemUpdates}
import chisel3.experimental.{ChiselAnnotation, annotate}
import chisel3.util.experimental.loadMemoryFromFileInline
import firrtl.annotations.{Annotation, MemorySynthInit}
import chisel3.experimental.annotate
import firrtl.transforms.NoDedupAnnotation



class UnifiedMemory(memFile: String) extends Module {
  val testHarness = IO(new Bundle {
    val imemSetup = Input(new IMEMsetupSignals)
    val dmemSetup = Input(new DMEMsetupSignals)
    val testUpdatesDMEM = Output(new MemUpdates)
    val requestedAddressIMEM = Output(UInt(32.W))
  })

  val io = IO(new Bundle {
    //input signals
    val req = Input(Bool())
    val addr = Input(UInt(32.W))
    val write = Input(Bool())
    val wdata = Input(UInt(32.W))

    //set data output
    val dataRead = Output(UInt(32.W))
  })

  // //TODO ?????
  val self = this
  annotate(new ChiselAnnotation {
    override def toFirrtl = NoDedupAnnotation(self.toNamed)
  })

  val memory = SyncReadMem(2097152, UInt(32.W))
  loadMemoryFromFileInline(memory, memFile)

  //default output
  io.dataRead := 0.U

  // Address/data/write enable logic
  val addrSource     = Wire(UInt(32.W))
  val wdataSource    = Wire(UInt(32.W))
  val writeEnable    = Wire(Bool())
  val readEnable     = Wire(Bool())



  //! use base for 0x80000000
  val base = "h80000000".U
  //val base = 0.U




  // Use test harness during setup
  when(testHarness.imemSetup.setup) {
    addrSource   := testHarness.imemSetup.address - base
    wdataSource  := testHarness.imemSetup.instruction 
    writeEnable  := true.B
    readEnable   := false.B
  }.elsewhen(testHarness.dmemSetup.setup) {
    addrSource   := testHarness.dmemSetup.dataAddress - base
    wdataSource  := testHarness.dmemSetup.dataIn
    writeEnable  := testHarness.dmemSetup.writeEnable
    readEnable   := testHarness.dmemSetup.readEnable
  }.otherwise {
    addrSource   := io.addr - base
    wdataSource  := io.wdata
    writeEnable  := io.write
    readEnable   := io.req
  }

  //TODO word align?
  // Write to memory
  when(writeEnable) {
    memory(addrSource >> 2) := wdataSource
    printf(p"\n")
    printf(p"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\n")
    printf(p"memory write 0x${Hexadecimal(wdataSource)} at 0x${Hexadecimal(addrSource >> 2)}, not shifted: 0x${Hexadecimal(addrSource)}\n")
    printf(p"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\n")
    printf(p"\n")
  }.otherwise{
  // Read from memory
    io.dataRead := memory(addrSource >> 2)
    
    //printf(p"memory read 0x${Hexadecimal(memory(addrSource >> 2))} at 0x${Hexadecimal(addrSource >> 2)}, not shifted: 0x${Hexadecimal(memory(addrSource))} at 0x${Hexadecimal(addrSource)}\n")

  }

  
  

  //printf(p"memory 0x${Hexadecimal(memory(0x00002000.U >> 2))} at 0x${Hexadecimal(0x00002000.U >> 2)}, not shifted: 0x${Hexadecimal(memory(0x00002000.U))} at 0x${Hexadecimal(0x00002000.U)}\n")
  
  // Test harness outputs
  testHarness.testUpdatesDMEM.writeEnable  := writeEnable
  testHarness.testUpdatesDMEM.readEnable   := readEnable
  testHarness.testUpdatesDMEM.writeData    := wdataSource
  testHarness.testUpdatesDMEM.writeAddress := addrSource

  testHarness.requestedAddressIMEM := addrSource
  // printf(p"memory at 0x${Hexadecimal(addrSource >> 2)}: 0x${Hexadecimal(memory(addrSource >> 2))}\n")
  // printf(p"memory at 0x${Hexadecimal(addrSource)}: 0x${Hexadecimal(memory(addrSource))}\n")
  // printf(p"memory t4 at 4080 0x${Hexadecimal(memory(4080))}\n")
  // printf(p"memory t5 at 8192 0x${Hexadecimal(memory(8192))}\n")
  // printf(p"memory t4 at 4080 >> 2 0x${Hexadecimal(memory(4080 >> 2))}, t5 at 8192 >> 2 0x${Hexadecimal(memory(8192 >> 2))}\n")
  // printf(p"memory actual read 0x${Hexadecimal(io.dataRead)}\n")
  

  // val count       = RegInit(0.U(32.W))
  // count := count + 1.U
  // val check       = RegInit(0.U(32.W))
  // when(memory(count) === 0x00FF00FF.U){
  //   //printf(p"count ${count} mem: 0x${Hexadecimal(memory(count))}\n")
  //   check := count
  // }
  // when(check =/= 0.U){
  //   //printf(p"count ${count} mem: 0x${Hexadecimal(memory(count))}\n")
  //   check := 0.U
  // }
// val count = RegInit(760000.U(32.W))
// count := count + 1.U
// val startVal = RegInit(760000.U(32.W))
// val memData = memory(count) 
// val addrReg = RegNext(count)

// when(memData === 0x00FF00FF.U ) {
//   //printf(p"\n")
//   //printf(p"------------------------------------------\n")
//   //printf(p"FOUND data=0x${Hexadecimal(memData)} at addr 0x${Hexadecimal(addrReg)} with count ${count}\n")
//   //printf(p"should be 0x${Hexadecimal(0x00002000.U >> 2)}\n")
//   //printf(p"------------------------------------------\n")
//   //printf(p"\n")
// }
// when(startVal + 40000.U === count){
//   //printf(p"count ${count}\n")
//   //printf(p"count 0x${Hexadecimal(count)}\n")
//   //printf(p"until 0x00${Hexadecimal(2097152.U)}\n")
// }
// FOUND data=0x00ff00ff at addr 0x00000f14 with count        3861  -> -1 -> 3860


printf(p"\n")

}


