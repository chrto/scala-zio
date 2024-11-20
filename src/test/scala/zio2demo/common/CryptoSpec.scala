package zio2demo.common

import zio.test.ZIOSpecDefault

import zio._
import zio.test._

object CryptoSpec extends ZIOSpecDefault {
  def spec = suite("Crypto") {
    suite("hashPwd") {
      zio.Chunk(
        test("Should hash password 'joeDoe'") {
          assertTrue(Crypto.Crypto.hashPwd("joeDoe") == "7a9ed1520e93d2d3d4448153c210e27c1638ae249e554821fec70930ca4cafea")
        },

        test("Should hash password 'jackBlack'") {
          assertTrue(Crypto.Crypto.hashPwd("jackBlack") == "5df0b691934789abae24f73ab730611c6e12385604d07f0a29150f20d3f29d74")
        }
      )
    }
  }
}