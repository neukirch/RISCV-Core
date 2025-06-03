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

  // Use test harness during setup
  when(testHarness.imemSetup.setup) {
    addrSource   := testHarness.imemSetup.address
    wdataSource  := testHarness.imemSetup.instruction
    writeEnable  := true.B
    readEnable   := false.B
  }.elsewhen(testHarness.dmemSetup.setup) {
    addrSource   := testHarness.dmemSetup.dataAddress
    wdataSource  := testHarness.dmemSetup.dataIn
    writeEnable  := testHarness.dmemSetup.writeEnable
    readEnable   := testHarness.dmemSetup.readEnable
  }.otherwise {
    addrSource   := io.addr
    wdataSource  := io.wdata
    writeEnable  := io.write
    readEnable   := io.req
  }

  //TODO word align?
  // Write to memory
  when(writeEnable) {
    memory(addrSource >> 2) := wdataSource
  }.otherwise{
  // Read from memory
    io.dataRead := memory(addrSource >> 2)
  }




  // Test harness outputs
  testHarness.testUpdatesDMEM.writeEnable  := writeEnable
  testHarness.testUpdatesDMEM.readEnable   := readEnable
  testHarness.testUpdatesDMEM.writeData    := wdataSource
  testHarness.testUpdatesDMEM.writeAddress := addrSource

  testHarness.requestedAddressIMEM := addrSource
}









// package UnifiedMemory
// import chisel3._
// import chisel3.util._
// import config.{IMEMsetupSignals, DMEMsetupSignals, MemUpdates}
// import chisel3.experimental.{ChiselAnnotation, annotate}
// import chisel3.util.experimental.loadMemoryFromFileInline
// import firrtl.annotations.{Annotation, MemorySynthInit}
// import chisel3.experimental.annotate
// import firrtl.transforms.NoDedupAnnotation



// class UnifiedMemory(memFile: String) extends Module {
//   val testHarness = IO(new Bundle {
//     val imemSetup = Input(new IMEMsetupSignals)
//     val dmemSetup = Input(new DMEMsetupSignals)
//     val testUpdatesDMEM = Output(new MemUpdates)
//     val requestedAddressIMEM = Output(UInt(32.W))
//   })

//   val io = IO(new Bundle {
//     // Inst fetch port (R)
//     val instAddr = Input(UInt(32.W))
//     val instOut  = Output(UInt(32.W))

//     // Data mem port (R/W)
//     val dataWriteEnable = Input(Bool())
//     val dataReadEnable  = Input(Bool())
//     val dataIn          = Input(UInt(32.W))
//     val dataAddr        = Input(UInt(32.W))
//     val dataOut         = Output(UInt(32.W))

//     //Arbitration
//     //TODO signals to test harness????
//     val DMEM_gnt         = Output(Bool())
//     val IMEM_gnt         = Output(Bool())
//   })

// //   annotate(new ChiselAnnotation {
// //     override def toFirrtl = MemorySynthInit
// //   })
// //annotate(new MemoryFileInlineAnnotation(memory, memFile, "Hex"))
// //TODO ?????
// val self = this
// annotate(new ChiselAnnotation {
//   override def toFirrtl = NoDedupAnnotation(self.toNamed)
// })
  


 
//   //! changed to size of data mem: val memory = SyncReadMem(2097152, UInt(32.W)) // sizes merged, next power of to
//   val memory = SyncReadMem(1048576, UInt(32.W))
//   loadMemoryFromFileInline(memory, memFile)

//   //set up mem connections to IO
//   testHarness.requestedAddressIMEM := io.instAddr

//   // Instruction Address Source
//   val instAddrSource = Wire(UInt(32.W))
//   when(testHarness.imemSetup.setup) {
//     instAddrSource := testHarness.imemSetup.address
//   }.otherwise {
//     instAddrSource := io.instAddr
//   }

//   // Data Address Source
//   val dataAddrSource     = Wire(UInt(32.W))
//   val dataInSource       = Wire(UInt(32.W))
//   val writeEnableSource  = Wire(Bool())
//   val readEnableSource   = Wire(Bool())

//   when(testHarness.dmemSetup.setup) {
//     dataAddrSource    := testHarness.dmemSetup.dataAddress 
//     dataInSource      := testHarness.dmemSetup.dataIn
//     writeEnableSource := testHarness.dmemSetup.writeEnable
//     readEnableSource  := testHarness.dmemSetup.readEnable
//   }.otherwise {
//     dataAddrSource    := io.dataAddr //dataAccessAddr //!io.dataAddr
//     dataInSource      := io.dataIn
//     writeEnableSource := io.dataWriteEnable
//     readEnableSource  := io.dataReadEnable
//   }

//   // Handle testHarness output
//   // Instr 
  
//   // Data
//   testHarness.testUpdatesDMEM.writeEnable      := writeEnableSource
//   testHarness.testUpdatesDMEM.readEnable       := readEnableSource
//   testHarness.testUpdatesDMEM.writeData        := dataInSource
//   testHarness.testUpdatesDMEM.writeAddress     := dataAddrSource




//   // For loading data for Instr
//   when(testHarness.imemSetup.setup){
//     memory(instAddrSource) := testHarness.imemSetup.instruction
//   }


//   //set default outputs
//   //TODO valid signal for stalling in case of simultaneous accesses
//   //io.instOut := 0.U
//   //io.dataOut := 0.U
//   io.DMEM_gnt := false.B
//   io.IMEM_gnt := false.B
    
//   //TODO prio for write and read of data 
//   //Arbitration for fixed prio of data > instr
//   // when(writeEnableSource){
//   //   // Write to memory
//   //   memory(dataAddrSource) := dataInSource
//   //   io.DMEM_gnt := true.B
//   // }.elsewhen(readEnableSource){
//   //   // Read from memory
//   //   io.dataOut := memory(dataAddrSource)
//   //   io.DMEM_gnt := true.B
//   // }.otherwise{
//   //   // All other times(no specific request for instr as IF always happens)
//   //   io.instOut := memory(instAddrSource(31,2))
//   //   io.IMEM_gnt := true.B
//   // }

//   //TODO word align? of dataAddrSource, instrAddrSource->(31,2)
//   //TODO arbitration
//   //! no arbitration:
//   when(writeEnableSource){
//     // Write to memory
//     memory(dataAddrSource) := dataInSource
//   }
//     // Read from memory
//     io.dataOut := memory(dataAddrSource)
//     // All other times(no specific request for instr as IF always happens)
//     io.instOut := memory(instAddrSource(31,2))
// }
