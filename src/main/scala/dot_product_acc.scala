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

class syn_fifo(val width: Int, val depth: Int) extends Module with RequireSyncReset {
  val io = IO( new Bundle {
    val in = Flipped(Decoupled(UInt(width.W)))
    val out = Decoupled(UInt(width.W))
  })
  val queue = Queue(io.in, depth)
  io.out <> queue

//  // Explicit synchronous reset domain
//  val syncReset = this.reset.asBool // Convert to Bool for sync reset
//  
//  val queue = withClockAndReset(clock, syncReset) {
//    Module(new Queue(chiselTypeOf(io.in.bits), depth))
//  }
//  
//  queue.io.enq <> io.in
//  io.out <> queue.io.deq
}

class mac extends Module{
  val io = IO(new Bundle {
    val in_a        = Flipped(Decoupled(UInt(32.W)))
    val in_b        = Flipped(Decoupled(UInt(32.W)))
    val out         = Decoupled(UInt(32.W))
    val data_bits   = Input(UInt(2.W))
    val input_sign  = Input(Bool())
    val enable      = Input(Bool())
    val out_sum     = Output(UInt(72.W))
    val out_wb      = Input(Bool())
  })

  val in_ready = Wire(Bool())
  val two_words_loaded = RegInit(false.B)

  val mul9x9_0  = Module(new mul9x9_signed())
  val mul9x9_1  = Module(new mul9x9_signed())
  val mul9x9_2  = Module(new mul9x9_signed())
  val mul9x9_3  = Module(new mul9x9_signed())
  val mul17x9_0 = Module(new mul17x9_signed())
  val mul17x9_1 = Module(new mul17x9_signed())
  val mul17x9_2 = Module(new mul17x9_signed())
  val mul17x9_3 = Module(new mul17x9_signed())
  val mul17x17  = Module(new mul17x17_signed())

  val in_a_bits = RegInit(0.U(32.W))
  val in_b_bits = RegInit(0.U(32.W))

  when(io.in_a.ready === true.B) {
    in_a_bits := io.in_a.bits
  }

  when(io.in_b.ready === true.B) {
    in_b_bits := io.in_b.bits
  }

  val mul9x9_0_in0_s = Wire(SInt(9.W))
  val mul9x9_1_in0_s = Wire(SInt(9.W))
  val mul9x9_2_in0_s = Wire(SInt(9.W))
  val mul9x9_3_in0_s = Wire(SInt(9.W))
  val mul9x9_0_in1_s = Wire(SInt(9.W))
  val mul9x9_1_in1_s = Wire(SInt(9.W))
  val mul9x9_2_in1_s = Wire(SInt(9.W))
  val mul9x9_3_in1_s = Wire(SInt(9.W))

  when(io.input_sign===true.B) { // signed
    mul9x9_0_in0_s := Cat(in_a_bits(7),in_a_bits(7,0)).asSInt        // A0
    mul9x9_0_in1_s := Cat(in_b_bits(7),in_b_bits(7,0)).asSInt        // B0
    mul9x9_1_in0_s := Cat(in_a_bits(15),in_a_bits(15,8)).asSInt      // A1
    mul9x9_1_in1_s := Cat(in_b_bits(15),in_b_bits(15,8)).asSInt      // B1
    when(io.data_bits===0.U) { // 8-bit
      mul9x9_2_in0_s := Cat(in_a_bits(23),in_a_bits(23,16)).asSInt   // A2
      mul9x9_2_in1_s := Cat(in_b_bits(23),in_b_bits(23,16)).asSInt   // B2
      mul9x9_3_in0_s := Cat(in_a_bits(31),in_a_bits(31,24)).asSInt   // A3
      mul9x9_3_in1_s := Cat(in_b_bits(31),in_b_bits(31,24)).asSInt   // B3
    }.otherwise { // 16-bit or 32-bit
      mul9x9_2_in0_s := Cat(in_a_bits(7),in_a_bits(7,0)).asSInt      // A0
      mul9x9_2_in1_s := Cat(in_b_bits(15),in_b_bits(15,8)).asSInt    // B1
      mul9x9_3_in0_s := Cat(in_a_bits(15),in_a_bits(15,8)).asSInt    // A1
      mul9x9_3_in1_s := Cat(in_b_bits(7),in_b_bits(7,0)).asSInt      // B0
    }
  }.otherwise {   // unsigned
    mul9x9_0_in0_s := Cat(0.U(1.W),in_a_bits(7,0)).asSInt
    mul9x9_0_in1_s := Cat(0.U(1.W),in_b_bits(7,0)).asSInt
    mul9x9_1_in0_s := Cat(0.U(1.W),in_a_bits(15,8)).asSInt
    mul9x9_1_in1_s := Cat(0.U(1.W),in_b_bits(15,8)).asSInt
    when(io.data_bits===0.U) { // 8-bit
      mul9x9_2_in0_s := Cat(0.U(1.W),in_a_bits(23,16)).asSInt
      mul9x9_2_in1_s := Cat(0.U(1.W),in_b_bits(23,16)).asSInt
      mul9x9_3_in0_s := Cat(0.U(1.W),in_a_bits(31,24)).asSInt
      mul9x9_3_in1_s := Cat(0.U(1.W),in_b_bits(31,24)).asSInt
    }.otherwise { // 16-bit or 32-bit
      mul9x9_2_in0_s := Cat(0.U(1.W),in_a_bits(7,0)).asSInt
      mul9x9_2_in1_s := Cat(0.U(1.W),in_b_bits(15,8)).asSInt
      mul9x9_3_in0_s := Cat(0.U(1.W),in_a_bits(15,8)).asSInt
      mul9x9_3_in1_s := Cat(0.U(1.W),in_b_bits(7,0)).asSInt
    }
  }

  mul9x9_0.io.in0 := mul9x9_0_in0_s
  mul9x9_1.io.in0 := mul9x9_1_in0_s
  mul9x9_2.io.in0 := mul9x9_2_in0_s
  mul9x9_3.io.in0 := mul9x9_3_in0_s
  mul9x9_0.io.in1 := mul9x9_0_in1_s
  mul9x9_1.io.in1 := mul9x9_1_in1_s
  mul9x9_2.io.in1 := mul9x9_2_in1_s
  mul9x9_3.io.in1 := mul9x9_3_in1_s

  val mul17x9_0_in0_s = Wire(SInt(17.W))
  val mul17x9_1_in0_s = Wire(SInt(17.W))
  val mul17x9_2_in0_s = Wire(SInt(17.W))
  val mul17x9_3_in0_s = Wire(SInt(17.W))
  val mul17x9_0_in1_s = Wire(SInt(9.W))
  val mul17x9_1_in1_s = Wire(SInt(9.W))
  val mul17x9_2_in1_s = Wire(SInt(9.W))
  val mul17x9_3_in1_s = Wire(SInt(9.W))

  when(io.input_sign===true.B) { // signed
    mul17x9_0_in0_s := Cat(in_a_bits(31),in_a_bits(31,16)).asSInt  // A32
    mul17x9_0_in1_s := Cat(in_b_bits(15),in_b_bits(15,8)).asSInt   // B1
    mul17x9_1_in0_s := Cat(in_a_bits(31),in_a_bits(31,16)).asSInt  // A32
    mul17x9_1_in1_s := Cat(in_b_bits(7),in_b_bits(7,0)).asSInt     // B0
    mul17x9_2_in0_s := Cat(in_b_bits(31),in_b_bits(31,16)).asSInt  // B32
    mul17x9_2_in1_s := Cat(in_a_bits(15),in_a_bits(15,8)).asSInt   // A1
    mul17x9_3_in0_s := Cat(in_b_bits(31),in_b_bits(31,16)).asSInt  // B32
    mul17x9_3_in1_s := Cat(in_a_bits(7),in_a_bits(7,0)).asSInt     // A0
  }.otherwise {   // unsigned
    mul17x9_0_in0_s := Cat(0.U(1.W),in_a_bits(7,0)).asSInt
    mul17x9_0_in1_s := Cat(0.U(1.W),in_b_bits(7,0)).asSInt
    mul17x9_1_in0_s := Cat(0.U(1.W),in_a_bits(15,8)).asSInt
    mul17x9_1_in1_s := Cat(0.U(1.W),in_b_bits(15,8)).asSInt
    mul17x9_2_in0_s := Cat(0.U(1.W),in_a_bits(23,16)).asSInt
    mul17x9_2_in1_s := Cat(0.U(1.W),in_b_bits(23,16)).asSInt
    mul17x9_3_in0_s := Cat(0.U(1.W),in_a_bits(31,24)).asSInt
    mul17x9_3_in1_s := Cat(0.U(1.W),in_b_bits(31,24)).asSInt
  }

  mul17x9_0.io.in0 := mul17x9_0_in0_s
  mul17x9_1.io.in0 := mul17x9_1_in0_s
  mul17x9_2.io.in0 := mul17x9_2_in0_s
  mul17x9_3.io.in0 := mul17x9_3_in0_s
  mul17x9_0.io.in1 := mul17x9_0_in1_s
  mul17x9_1.io.in1 := mul17x9_1_in1_s
  mul17x9_2.io.in1 := mul17x9_2_in1_s
  mul17x9_3.io.in1 := mul17x9_3_in1_s

  val mul17x17_in0_s  = Wire(SInt(17.W))
  val mul17x17_in1_s  = Wire(SInt(17.W))

  when(io.input_sign===true.B) { // signed
    mul17x17_in0_s := Cat(in_a_bits(31),in_a_bits(31,16)).asSInt   // A32
    mul17x17_in1_s := Cat(in_b_bits(31),in_b_bits(31,16)).asSInt   // B32
  }.otherwise {   // unsigned
    mul17x17_in0_s := Cat(0.U(1.W),in_a_bits(31,16)).asSInt   // A32
    mul17x17_in1_s := Cat(0.U(1.W),in_b_bits(31,16)).asSInt   // B32
  }

  mul17x17.io.in0 := mul17x17_in0_s
  mul17x17.io.in1 := mul17x17_in1_s

  val mul9x9_0_out	= mul9x9_0.io.out
  val mul9x9_1_out	= mul9x9_1.io.out
  val mul9x9_2_out	= mul9x9_2.io.out
  val mul9x9_3_out	= mul9x9_3.io.out
  val mul17x9_0_out	= mul17x9_0.io.out
  val mul17x9_1_out	= mul17x9_1.io.out
  val mul17x9_2_out	= mul17x9_2.io.out
  val mul17x9_3_out	= mul17x9_3.io.out
  val mul17x17_out	= mul17x17.io.out

  val out_64_s  = Wire(SInt(64.W))
  val out_64    = Wire(UInt(64.W))

  val out_32_0  = (mul9x9_1_out<<16) +& (mul9x9_3_out<<8) +& (mul9x9_2_out<<8) +& (mul9x9_0_out)

  out_64_s  := (mul17x17_out<<32) +& (mul17x9_0_out<<24) +& (mul17x9_2_out<<24) +& (mul17x9_1_out<<16) +& (mul17x9_3_out<<16) +& out_32_0

  when(io.data_bits === 0.U) { // 8-bit
    out_64  := Cat(mul9x9_3_out(15,0).asUInt, mul9x9_2_out(15,0).asUInt, mul9x9_1_out(15,0).asUInt, mul9x9_0_out(15,0).asUInt)
  }.elsewhen(io.data_bits === 1.U) { // 16-bit
    out_64  := Cat(mul17x17_out(31,0).asUInt, out_32_0(31,0).asUInt)
  }.otherwise { // 32-bit
    out_64  := out_64_s.asUInt
  }
  
  val out_sum = RegInit(0.S(72.W))
  when(io.enable === true.B) {
    when(two_words_loaded === true.B) {
      when(io.data_bits === 0.U) { // 8-bit
        out_sum := out_sum +& mul9x9_3_out +& mul9x9_2_out +& mul9x9_1_out +& mul9x9_0_out
      }.elsewhen(io.data_bits === 1.U) { // 16-bit
        out_sum := out_sum +& mul17x17_out +& out_32_0
      }.otherwise { // 32-bit
        out_sum := out_sum +& out_64_s
      }
    }
  }.otherwise {
    out_sum := 0.S
  }
  io.out_sum  := out_sum.asUInt

  in_ready  := io.in_a.valid & io.in_b.valid & (~two_words_loaded)
  when((io.out.ready === true.B) && (two_words_loaded === true.B) && (io.out_wb === true.B)) {
    two_words_loaded  := false.B
  }.elsewhen (in_ready === true.B) {
    two_words_loaded  := true.B
  }

  val out_valid = RegInit(false.B)
  when((in_ready===true.B) || (two_words_loaded === true.B)) {
    out_valid := true.B
  }.otherwise{
    out_valid := false.B
  }

  io.in_a.ready := in_ready
  io.in_b.ready := in_ready
  io.out.valid  := Mux(io.out_wb, out_valid, 0.U)
  io.out.bits   := Mux((two_words_loaded===false.B), out_64(63,32), out_64(31,0))

}

class mul9x9_signed extends Module{
  val io = IO(new Bundle {
    val in0 = Input(SInt(9.W))
    val in1 = Input(SInt(9.W))
    val out = Output(SInt(18.W))
  })
  io.out := io.in0*io.in1
}

class mul17x9_signed extends Module{
  val io = IO(new Bundle {
    val in0 = Input(SInt(17.W))
    val in1 = Input(SInt(9.W))
    val out = Output(SInt(26.W))
  })
  io.out := io.in0*io.in1
}

class mul17x17_signed extends Module{
  val io = IO(new Bundle {
    val in0 = Input(SInt(17.W))
    val in1 = Input(SInt(17.W))
    val out = Output(SInt(34.W))
  })
  io.out := io.in0*io.in1
}

class ahb_lite_mst extends Bundle {
    val hready  = Input(Bool())
    val hrdata  = Input(UInt(32.W))
    val hwdata  = Output(UInt(32.W))
    val haddr   = Output(UInt(32.W))
    val htrans  = Output(UInt(2.W))
    val hwrite  = Output(Bool())
    val hsize   = Output(UInt(3.W))
    val hburst  = Output(UInt(3.W))
}

class ahb_ctrl extends Module{
  val io = IO(new Bundle{
    val in_a    = Decoupled(UInt(32.W))
    val in_b    = Decoupled(UInt(32.W))
    val out     = Flipped(Decoupled(UInt(32.W)))
    val ahb     = new ahb_lite_mst()
    val enable  = Input(Bool())
    val data_depth  = Input(UInt(8.W))
    val data_bits   = Input(UInt(2.W))
    val addr_a      = Input(UInt(32.W))
    val addr_b      = Input(UInt(32.W))
    val addr_out    = Input(UInt(32.W))
    val op_end      = Output(Bool())
  })

  // queue signal
  val in_a_q1 = RegInit(false.B)
  val in_a_q0 = RegInit(false.B)
  val in_b_q1 = RegInit(false.B)
  val in_b_q0 = RegInit(false.B)
  val out_q1  = RegInit(false.B)
  val out_q0  = RegInit(false.B)

  val in_a_req  = Wire(Bool())
  val in_b_req  = Wire(Bool())
  val out_req   = Wire(Bool())
  val in_a_gnt  = Wire(Bool())
  val in_b_gnt  = Wire(Bool())
  val out_gnt   = Wire(Bool())

  val reading_a = RegInit(false.B)
  val reading_b = RegInit(false.B)
  val writing   = RegInit(false.B)

  val in_a_done = RegInit(false.B)
  val in_b_done = RegInit(false.B)
  val out_done  = RegInit(false.B)

  val in_a_end_addr = Wire(UInt(32.W))
  val in_b_end_addr = Wire(UInt(32.W))
  val out_end_addr  = Wire(UInt(32.W))

  val in_end_mask = Wire(UInt(32.W))

  val ahb_enable  = RegInit(false.B)

  ahb_enable  := io.enable  // delay one cycle for initialization

  when(ahb_enable === true.B) {
    when(in_a_q1 === true.B) {
      in_a_req  := true.B
      in_b_req  := false.B
      out_req   := false.B
    }.elsewhen(in_b_q1 === true.B) {
      in_a_req  := false.B
      in_b_req  := true.B
      out_req   := false.B
    }.elsewhen(out_q1 === true.B) {
      in_a_req  := false.B
      in_b_req  := false.B
      out_req   := true.B
    }.otherwise{
      when(in_a_q0 === true.B) {
        in_a_req  := true.B
        in_b_req  := false.B
        out_req   := false.B
      }.elsewhen(in_b_q0 === true.B) {
        in_a_req  := false.B
        in_b_req  := true.B
        out_req   := false.B
      }.elsewhen(out_q0 === true.B) {
        in_a_req  := false.B
        in_b_req  := false.B
        out_req   := true.B
      }.otherwise{
        when((io.in_a.ready === true.B) && (reading_a === false.B) && (in_a_done === false.B)) {
          in_a_req  := true.B
          in_b_req  := false.B
          out_req   := false.B
        }.elsewhen((io.in_b.ready === true.B) && (reading_b === false.B) && (in_b_done === false.B)) {
          in_a_req  := false.B
          in_b_req  := true.B
          out_req   := false.B
        }.elsewhen((io.out.valid === true.B) && (out_done === false.B)) {
          in_a_req  := false.B
          in_b_req  := false.B
          out_req   := true.B
        }.otherwise {
          in_a_req  := false.B
          in_b_req  := false.B
          out_req   := false.B
        }
      }
    }
  }.otherwise {
          in_a_req  := false.B
          in_b_req  := false.B
          out_req   := false.B
  }

  //printf(p"ahb_ctrl: in_a_q1=$in_a_q1, in_a_q0=$in_a_q0, in_b_q1=$in_b_q1, in_b_q0=$in_b_q0, out_q1=$in_a_q1, out_q0=$in_a_q0 \n")
  //printf(p"ahb_ctrl: in_a_req=$in_a_req, in_b_req=$in_b_req, out_req=$out_req\n")

  val gnt_any = Wire(Bool())
  gnt_any := in_a_gnt | in_b_gnt | out_gnt
  when(ahb_enable){
    when(in_a_gnt === true.B) {
      in_a_q1 := false.B
      in_a_q0 := false.B
    }.elsewhen((gnt_any === true.B) && (io.in_a.ready === true.B) && (in_a_done === false.B)) {
      in_a_q0 := true.B
      when(in_a_q0 === true.B) {
        in_a_q1 := true.B
      }
    }
  }.otherwise {
      in_a_q1 := false.B
      in_a_q0 := false.B
  }

  when(ahb_enable){
    when(in_b_gnt === true.B) {
      in_b_q1 := false.B
      in_b_q0 := false.B
    }.elsewhen((gnt_any === true.B) && (io.in_b.ready === true.B) && (in_b_done === false.B)) {
      in_b_q0 := true.B
      when(in_b_q0 === true.B) {
        in_b_q1 := true.B
      }
    }
  }.otherwise {
      in_b_q1 := false.B
      in_b_q0 := false.B
  }

  when(out_gnt === true.B) {
    out_q1 := false.B
    out_q0 := false.B
  }.elsewhen((gnt_any === true.B) && (io.out.valid === true.B)) {
    out_q0 := true.B
    when(out_q0 === true.B) {
      out_q1 := true.B
    }
  }

  when(io.ahb.hready === true.B) {
    when(in_a_req === true.B) {
      in_a_gnt  := true.B
      in_b_gnt  := false.B
      out_gnt   := false.B
    }.elsewhen(in_b_req === true.B) {
      in_a_gnt  := false.B
      in_b_gnt  := true.B
      out_gnt   := false.B
    }.elsewhen(out_req === true.B) {
      in_a_gnt  := false.B
      in_b_gnt  := false.B
      out_gnt   := true.B
    }.otherwise {
      in_a_gnt  := false.B
      in_b_gnt  := false.B
      out_gnt   := false.B
    }
  }.otherwise{
      in_a_gnt  := false.B
      in_b_gnt  := false.B
      out_gnt   := false.B
  }
  //printf(p"ahb_ctrl: in_a_gnt=$in_a_gnt, in_b_gnt=$in_b_gnt, out_gnt=$out_gnt\n")

  val in_a_addr = RegInit(0.U(32.W))
  val in_b_addr = RegInit(0.U(32.W))
  val out_addr  = RegInit(0.U(32.W))

  when(ahb_enable) {
    when(in_a_gnt) {
      in_a_addr := in_a_addr + 4.U
    }
  }.otherwise {
    in_a_addr := io.addr_a
  }

  when(ahb_enable) {
    when(in_b_gnt) {
      in_b_addr := in_b_addr + 4.U
    }
  }.otherwise {
    in_b_addr := io.addr_b
  }

  when(ahb_enable) {
    when(out_gnt) {
      out_addr  := out_addr + 4.U
    }
  }.otherwise {
    out_addr  := io.addr_out
  }

  when(io.data_bits===0.U) { // 8-bit
    in_a_end_addr := io.addr_a +& io.data_depth
    in_b_end_addr := io.addr_b +& io.data_depth
    out_end_addr  := io.addr_out +& Cat(io.data_depth(7,1), 0.U(2.W))
    when(io.data_depth(1,0) === 0.U) {
      in_end_mask   := "h0000_00FF".U
    }.elsewhen(io.data_depth(1,0) === 0.U) {
      in_end_mask   := "h0000_FFFF".U
    }.elsewhen(io.data_depth(1,0) === 0.U) {
      in_end_mask   := "h00FF_FFFF".U
    }.otherwise{
      in_end_mask   := "hFFFF_FFFF".U
    }
  }.elsewhen(io.data_bits === 1.U) { // 16-bit
    in_a_end_addr := io.addr_a +& (io.data_depth<<1)
    in_b_end_addr := io.addr_b +& (io.data_depth<<1)
    out_end_addr  := io.addr_out +& (io.data_depth<<2)
    when(io.data_depth(0) === 0.U) {
      in_end_mask   := "h0000_FFFF".U
    }.otherwise {
      in_end_mask   := "hFFFF_FFFF".U
    }
  }.otherwise { // 32-bit
    in_a_end_addr := io.addr_a +& (io.data_depth<<2)
    in_b_end_addr := io.addr_b +& (io.data_depth<<2)
    out_end_addr  := io.addr_out +& ((io.data_depth<<3) +& 4.U)
    in_end_mask   := "hFFFF_FFFF".U
  }
  //printf(p"in_a_end_addr=${in_a_end_addr}, in_b_end_addr=${in_b_end_addr}\n")

  when(ahb_enable === true.B){
    when((in_a_gnt === true.B) && (in_a_addr === Cat(in_a_end_addr(31,2), 0.U(2.W)))) {   // lower 2 bits are 0
      in_a_done := true.B
    }
  }.otherwise{
      in_a_done := false.B
  }

  when(ahb_enable === true.B){
    when((in_b_gnt === true.B) && (in_b_addr === Cat(in_b_end_addr(31,2), 0.U(2.W)))) {   // lower 2 bits are 0
      in_b_done := true.B
    }
  }.otherwise{
      in_b_done := false.B
  }

  when(ahb_enable === true.B){
    when((out_gnt === true.B) && (out_addr === out_end_addr)) {
      out_done := true.B
    }
  }.otherwise{
      out_done := false.B
  }

  val op_end = RegInit(true.B)

  when(ahb_enable === true.B) {
    when((out_done === true.B) && (io.ahb.hready === true.B)) {
      op_end  := true.B
    }
  }.otherwise {
    op_end  := false.B
  }
  io.op_end := op_end

  io.ahb.hburst := 0.U
  io.ahb.hsize  := 2.U

  when(ahb_enable === true.B) {
    when(in_a_req === true.B) {
      io.ahb.htrans := 2.U
      io.ahb.haddr  := in_a_addr
      io.ahb.hwrite := false.B
    }.elsewhen(in_b_req === true.B) {
      io.ahb.htrans := 2.U
      io.ahb.haddr  := in_b_addr
      io.ahb.hwrite := false.B
    }.elsewhen(out_req === true.B) {
      io.ahb.htrans := 2.U
      io.ahb.haddr  := out_addr
      io.ahb.hwrite := true.B
    }.otherwise {
      io.ahb.htrans := 0.U
      io.ahb.haddr  := 0.U
      io.ahb.hwrite := false.B
    }
  }.otherwise{
      io.ahb.htrans := 0.U
      io.ahb.haddr  := 0.U
      io.ahb.hwrite := false.B
  }

  when(in_a_gnt === true.B) {
    reading_a := true.B
  }.elsewhen((reading_a === true.B) && (io.ahb.hready === true.B)) {
    reading_a := false.B
  }

  when(in_b_gnt === true.B) {
    reading_b := true.B
  }.elsewhen((reading_b === true.B) && (io.ahb.hready === true.B)) {
    reading_b := false.B
  }
  //printf(p"ahb_ctrl: reading_a=$reading_a, reading_b=$reading_b\n")

  when(out_gnt === true.B) {
    writing := true.B
  }.elsewhen((writing === true.B) && (io.ahb.hready === true.B)) {
    writing := false.B
  }

  io.in_a.bits  := Mux(in_a_done, (io.ahb.hrdata & in_end_mask), io.ahb.hrdata)
  when(reading_a === true.B) {
    //io.in_a.bits  := io.ahb.hrdata
    io.in_a.valid := io.ahb.hready
  }.otherwise{
    //io.in_a.bits  := 0.U
    io.in_a.valid := false.B
  }

  io.in_b.bits  := Mux(in_b_done, (io.ahb.hrdata & in_end_mask), io.ahb.hrdata)
  when(reading_b === true.B) {
    io.in_b.valid := io.ahb.hready
  }.otherwise{
    io.in_b.valid := false.B
  }

  val ahb_wdata = RegInit(0.U(32.W))
  when(out_gnt === true.B) {
    ahb_wdata := io.out.bits
  }

  io.ahb.hwdata := ahb_wdata
  io.out.ready  := out_gnt

}

class dp_acc extends Module with RequireAsyncReset {
//class dp_acc extends Module {
  val io = IO(new Bundle{
    val ahb         = new ahb_lite_mst()
    val enable      = Input(Bool())
    val input_sign  = Input(Bool())
    val data_depth  = Input(UInt(8.W))
    val data_bits   = Input(UInt(2.W))
    val addr_a      = Input(UInt(32.W))
    val addr_b      = Input(UInt(32.W))
    val addr_out    = Input(UInt(32.W))
    val out_wb      = Input(Bool())
    val out_sum     = Output(UInt(72.W))
    val op_end      = Output(Bool())
  })

  val in_a_fifo = withReset((~io.enable)) {Module(new syn_fifo(width=32, depth=8))}
  val in_b_fifo = withReset((~io.enable)) {Module(new syn_fifo(width=32, depth=8))}
  val out_fifo  = withReset((~io.enable)) {Module(new syn_fifo(width=32, depth=8))}
  val mac       = Module(new mac)
  val ahb_ctrl  = Module(new ahb_ctrl)

  io.ahb <> ahb_ctrl.io.ahb
  ahb_ctrl.io.in_a <> in_a_fifo.io.in
  ahb_ctrl.io.in_b <> in_b_fifo.io.in
  ahb_ctrl.io.out <> out_fifo.io.out
  mac.io.in_a <> in_a_fifo.io.out
  mac.io.in_b <> in_b_fifo.io.out
  mac.io.out <> out_fifo.io.in

  mac.io.data_bits  := io.data_bits
  mac.io.input_sign := io.input_sign
  mac.io.enable     := io.enable
  mac.io.out_wb     := io.out_wb

  ahb_ctrl.io.enable    := io.enable
  ahb_ctrl.io.data_depth:= io.data_depth
  ahb_ctrl.io.data_bits := io.data_bits
  ahb_ctrl.io.addr_a    := io.addr_a   
  ahb_ctrl.io.addr_b    := io.addr_b   
  ahb_ctrl.io.addr_out  := io.addr_out 

  io.out_sum  := mac.io.out_sum
  io.op_end   := ahb_ctrl.io.op_end
  //printf(p"in_a_fifo: not_full=${in_a_fifo.io.in.ready}\n")
  //printf(p"out_fifo: not_empty=${out_fifo.io.out.valid}\n")
}

object syn_fifo extends App {
  ChiselStage.emitSystemVerilogFile(
    new syn_fifo(width=32,depth=8),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info", "-default-layer-specialization=enable")
  )
}

object dp_acc extends App {
  ChiselStage.emitSystemVerilogFile(
    new dp_acc,
    Array("--target-dir","verilog"),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info", "-default-layer-specialization=enable")
  )
}

object ahb_ctrl extends App {
  ChiselStage.emitSystemVerilogFile(
    new ahb_ctrl,
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info", "-default-layer-specialization=enable")
  )
}
