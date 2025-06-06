import chisel3._
import chisel3.util._
import _root_.circt.stage.ChiselStage

//class syn_fifo(val width: Int, val depth: Int) extends Module {
//  val io = IO(new Bundle {
//    val clock   = Input(Clock())
//    val reset   = Input(AsyncReset())
//    val push    = Input(Bool())
//    val wdata   = Input(UInt(width.W))
//    val pop     = Input(Bool())
//    val rdata   = Output(UInt(width.W))
//    val full    = Output(Bool())
//    val empty   = Output(Bool())
//  })
//  
//  withClockAndReset(clock, reset.asAsyncReset) {
//
//  }
//}
class syn_fifo(val width: Int, val depth: Int) extends Module{
  val io = IO( new Bundle {
    val in = Flipped(Decoupled(UInt(width.W)))
    val out = Decoupled(UInt(width.W))
  })
  val queue = Queue(io.in, depth)
  io.out <> queue
}

class mac extends Module{
  val io = IO(new Bundle {
    val in_a    = Flipped(Decoupled(UInt(32.W)))
    val in_b    = Flipped(Decoupled(UInt(32.W)))
    val out     = Decoupled(UInt(32.W))
    val bit_sel = Input(UInt(2.W))
  })

  val mul_a0_b0 = Module(new mul8x8())
  val mul_a0_b1 = Module(new mul8x8())
  val mul_a0_b2 = Module(new mul8x8())
  val mul_a0_b3 = Module(new mul8x8())
  val mul_a1_b0 = Module(new mul8x8())
  val mul_a1_b1 = Module(new mul8x8())
  val mul_a1_b2 = Module(new mul8x8())
  val mul_a1_b3 = Module(new mul8x8())
  val mul_a2_b0 = Module(new mul8x8())
  val mul_a2_b1 = Module(new mul8x8())
  val mul_a2_b2 = Module(new mul8x8())
  val mul_a2_b3 = Module(new mul8x8())
  val mul_a3_b0 = Module(new mul8x8())
  val mul_a3_b1 = Module(new mul8x8())
  val mul_a3_b2 = Module(new mul8x8())
  val mul_a3_b3 = Module(new mul8x8())

  mul_a0_b0.io.a := io.in_a.bits(7,0)
  mul_a0_b1.io.a := io.in_a.bits(7,0)
  mul_a0_b2.io.a := io.in_a.bits(7,0)
  mul_a0_b3.io.a := io.in_a.bits(7,0)
  mul_a1_b0.io.a := io.in_a.bits(15,8)
  mul_a1_b1.io.a := io.in_a.bits(15,8)
  mul_a1_b2.io.a := io.in_a.bits(15,8)
  mul_a1_b3.io.a := io.in_a.bits(15,8)
  mul_a2_b0.io.a := io.in_a.bits(23,16)
  mul_a2_b1.io.a := io.in_a.bits(23,16)
  mul_a2_b2.io.a := io.in_a.bits(23,16)
  mul_a2_b3.io.a := io.in_a.bits(23,16)
  mul_a3_b0.io.a := io.in_a.bits(31,24)
  mul_a3_b1.io.a := io.in_a.bits(31,24)
  mul_a3_b2.io.a := io.in_a.bits(31,24)
  mul_a3_b3.io.a := io.in_a.bits(31,24)

  mul_a0_b0.io.b := io.in_b.bits(7,0)
  mul_a0_b1.io.b := io.in_b.bits(15,8)
  mul_a0_b2.io.b := io.in_b.bits(23,16)
  mul_a0_b3.io.b := io.in_b.bits(31,24)
  mul_a1_b0.io.b := io.in_b.bits(7,0)
  mul_a1_b1.io.b := io.in_b.bits(15,8)
  mul_a1_b2.io.b := io.in_b.bits(23,16)
  mul_a1_b3.io.b := io.in_b.bits(31,24)
  mul_a2_b0.io.b := io.in_b.bits(7,0)
  mul_a2_b1.io.b := io.in_b.bits(15,8)
  mul_a2_b2.io.b := io.in_b.bits(23,16)
  mul_a2_b3.io.b := io.in_b.bits(31,24)
  mul_a3_b0.io.b := io.in_b.bits(7,0)
  mul_a3_b1.io.b := io.in_b.bits(15,8)
  mul_a3_b2.io.b := io.in_b.bits(23,16)
  mul_a3_b3.io.b := io.in_b.bits(31,24)

  val out_a0_b0 = mul_a0_b0.io.c
  val out_a0_b1 = mul_a0_b1.io.c
  val out_a0_b2 = mul_a0_b2.io.c
  val out_a0_b3 = mul_a0_b3.io.c
  val out_a1_b0 = mul_a1_b0.io.c
  val out_a1_b1 = mul_a1_b1.io.c
  val out_a1_b2 = mul_a1_b2.io.c
  val out_a1_b3 = mul_a1_b3.io.c
  val out_a2_b0 = mul_a2_b0.io.c
  val out_a2_b1 = mul_a2_b1.io.c
  val out_a2_b2 = mul_a2_b2.io.c
  val out_a2_b3 = mul_a2_b3.io.c
  val out_a3_b0 = mul_a3_b0.io.c
  val out_a3_b1 = mul_a3_b1.io.c
  val out_a3_b2 = mul_a3_b2.io.c
  val out_a3_b3 = mul_a3_b3.io.c

  val out_64  = Wire(UInt(64.W))

  //TODO
  io.in_a.ready := false.B
  io.in_b.ready := false.B
  io.out.valid := true.B
  io.out.bits := Mux((io.bit_sel===0.U), out_64(63,32), out_64(31,0))

  when(io.bit_sel === 0.U) { // 8-bit
    out_64 := Cat(out_a3_b3, out_a2_b2, out_a1_b1, out_a0_b0)
  }.elsewhen(io.bit_sel === 1.U) { // 16-bit
    val out_32_0 = (out_a1_b1<<16) + (out_a1_b0<<8) + (out_a0_b1<<8) + (out_a0_b0)
    val out_32_1 = (out_a3_b3<<16) + (out_a3_b2<<8) + (out_a2_b3<<8) + (out_a2_b2)
    out_64 := Cat(out_32_1, out_32_0)
  }.otherwise { // 32-bit
    out_64 := (out_a0_b0) +& (out_a0_b1<<8) +& (out_a0_b2<<16) +& (out_a0_b3<<24) +& (out_a1_b0<<8) +& (out_a1_b1<<16) +& (out_a1_b2<<24) +& (out_a1_b3<<32) +& (out_a2_b0<<16) +& (out_a2_b1<<24) +& (out_a2_b2<<32) +& (out_a2_b3<<40) +& (out_a3_b0<<24) +& (out_a3_b1<<32) +& (out_a3_b2<<40) +& (out_a3_b3<<48)
  }

}

class mul8x8 extends Module{
  val io = IO(new Bundle {
    val a = Input(UInt(8.W))
    val b = Input(UInt(8.W))
    val c = Output(UInt(16.W))
  })
  io.c := io.a*io.b
}

object syn_fifo extends App {
  ChiselStage.emitSystemVerilogFile(
    new syn_fifo(width=32,depth=8),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info", "-default-layer-specialization=enable")
  )
}

object mac extends App {
  ChiselStage.emitSystemVerilogFile(
    new mac,
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info", "-default-layer-specialization=enable")
  )
}
