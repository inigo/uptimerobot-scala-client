package com.sixtysevenbricks.uptimerobot

import java.util.UUID

import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import org.specs2.mutable.Specification

import scala.language.postfixOps

class UptimeRobotApiSpec extends Specification {
  private val log = LoggerFactory.getLogger(classOf[UptimeRobotApi])

  sequential

  val config = ConfigFactory.parseResources(classOf[UptimeRobotApiSpec], "/test.conf")
  val apiKey = config.getString("apiKey")
  val api = new UptimeRobotApi(apiKey)

  "Creating a new monitor" should {
    "succeed for a ping monitor" in {
      val result = api.newMonitor(PingMonitor("Testmonitor", "8.8.8.8", Seq(AlertContact("0365765"))))
      println(result)
      result must beRight
      val deleteResult = api.deleteMonitor( result.right.get)
      deleteResult must beRight
    }
    "succeed for a keyword monitor" in {
      val result = api.newMonitor(KeywordMonitor("TestmonitorKeyword", "http://example.com/", Seq(), alertWhenKeywordMissing = true, "Some keyword"))
      println(result)
      result must beRight
      val deleteResult = api.deleteMonitor( result.right.get)
      deleteResult must beRight
    }
  }

  "Listing monitors" should {
    "not throw an exception" in {
      api.listMonitorIds() must not throwAn[Exception]()
    }
    "include a newly generated monitor" in {
      val result = api.newMonitor(KeywordMonitor(uniqueId(), "http://"+uniqueId()+"67bricks.com/", Seq(), alertWhenKeywordMissing = true, "Some keyword"))
      val newId = result.right.get
      api.listMonitorIds().right.get must contain(newId)
      val deleteResult = api.deleteMonitor( newId )
      deleteResult must beRight
      api.listMonitorIds().right.get must not contain newId
    }
  }

  def uniqueId() = UUID.randomUUID().toString

//  "Delete all existing monitors" in {
//    val monitorIds = api.listMonitorIds().right.get
//    monitorIds.map( id => api.deleteMonitor(id))
//    1 mustEqual 1
//  }

}
