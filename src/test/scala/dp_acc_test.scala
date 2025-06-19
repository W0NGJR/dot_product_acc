import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

//import chiseltest._
//import org.scalatest.flatspec.AnyFlatSpec
//import org.scalatest.matchers.should.Matchers

class dp_acc_test extends AnyFreeSpec with Matchers with ChiselSim {
  "dp_acc should calculate proper mac result" in {
    simulate(new dp_acc) { dut =>
//class dp_acc_test extends AnyFlatSpec with ChiselScalatestTester {
//  "dp_acc" should "compute properly" in {
//    test(new dp_acc) { dut =>
      // Same test logic but using newer syntax:
      dut.io.input_sign.poke(true.B)
      dut.io.data_depth.poke(1.U)
      dut.io.data_bits.poke(0.U)
      dut.io.addr_a.poke("h400".U)
      dut.io.addr_b.poke("h800".U)
      dut.io.addr_out.poke("h1000".U)
      dut.io.out_wb.poke(true.B)

      dut.reset.poke(true.B)
      dut.clock.step()
      dut.reset.poke(false.B)

      dut.io.ahb.hready.poke(true.B)
      dut.io.enable.poke(true.B)
      dut.clock.step()

      //println(s"cycle 1")
      dut.clock.step()
      //println(f"dut: haddr=0x${dut.io.ahb.haddr.peek().litValue}%X, htrans=${dut.io.ahb.htrans.peek().litValue}, hrdata=0x${dut.io.ahb.hrdata.peek().litValue}%X, hwdata=0x${dut.io.ahb.hwdata.peek().litValue}%X, hready=${dut.io.ahb.hready.peek()}\n")
      dut.io.ahb.hrdata.poke("h47D790F".U)

      //println(s"cycle 2")
      dut.clock.step()
      //println(f"dut: haddr=0x${dut.io.ahb.haddr.peek().litValue}%X, htrans=${dut.io.ahb.htrans.peek().litValue}, hrdata=0x${dut.io.ahb.hrdata.peek().litValue}%X, hwdata=0x${dut.io.ahb.hwdata.peek().litValue}%X, hready=${dut.io.ahb.hready.peek()}\n")
      dut.io.ahb.hrdata.poke("h1D4C376F".U)

      //println(s"cycle 3")
      dut.clock.step()
      //println(f"dut: haddr=0x${dut.io.ahb.haddr.peek().litValue}%X, htrans=${dut.io.ahb.htrans.peek().litValue}, hrdata=0x${dut.io.ahb.hrdata.peek().litValue}%X, hwdata=0x${dut.io.ahb.hwdata.peek().litValue}%X, hready=${dut.io.ahb.hready.peek()}\n")
      dut.io.ahb.hrdata.poke("h00060005".U)

      //println(s"cycle 4")
      dut.clock.step()
      //println(f"dut: haddr=0x${dut.io.ahb.haddr.peek().litValue}%X, htrans=${dut.io.ahb.htrans.peek().litValue}, hrdata=0x${dut.io.ahb.hrdata.peek().litValue}%X, hwdata=0x${dut.io.ahb.hwdata.peek().litValue}%X, hready=${dut.io.ahb.hready.peek()}\n")
      dut.io.ahb.hrdata.poke("h00080007".U)

      //println(s"cycle 5")
      dut.clock.step()
      //println(f"dut: haddr=0x${dut.io.ahb.haddr.peek().litValue}%X, htrans=${dut.io.ahb.htrans.peek().litValue}, hrdata=0x${dut.io.ahb.hrdata.peek().litValue}%X, hwdata=0x${dut.io.ahb.hwdata.peek().litValue}%X, hready=${dut.io.ahb.hready.peek()}\n")
      dut.io.ahb.hrdata.poke("h000a0009".U)

      //println(s"cycle 6")
      dut.clock.step()
      //println(f"dut: haddr=0x${dut.io.ahb.haddr.peek().litValue}%X, htrans=${dut.io.ahb.htrans.peek().litValue}, hrdata=0x${dut.io.ahb.hrdata.peek().litValue}%X, hwdata=0x${dut.io.ahb.hwdata.peek().litValue}%X, hready=${dut.io.ahb.hready.peek()}\n")
      dut.io.ahb.hrdata.poke("h000c000b".U)

      dut.clock.step()
      dut.io.ahb.hready.poke(false.B)
      dut.clock.step()
      dut.io.ahb.hready.poke(true.B)


      for (i<-0 until 10){
        dut.clock.step()
        //dut.io.ahb.hrdata.poke(Cat((i*2+1).U(16.W),(i*2).U(16.W)))
        dut.io.ahb.hrdata.poke(((i*2+1) << 16) | (i*2))
      }

      dut.io.enable.poke(false.B)
      dut.clock.step(5)

      dut.clock.step()
      dut.io.ahb.hrdata.poke("h47D790F".U)

      dut.clock.step()
      dut.io.ahb.hrdata.poke("h1D4C376F".U)



     /* 
        for(i<-0 until 10) {
          println(s"dut: haddr=${dut.io.ahb.haddr.peek()}, htrans=${dut.io.ahb.htrans.peek()}")
          //println(s"in_a_fifo: not_full=${dut.mac.in_a_bits.peek()}")
          //println(s"in_a_fifo: not_full=${dut.in_a_fifo.io.in.bits.peek()}")
          dut.clock.step()
        }
        */
    }
  }
}
