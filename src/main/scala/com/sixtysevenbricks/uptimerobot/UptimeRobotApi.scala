package com.sixtysevenbricks.uptimerobot

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Scanner

import org.apache.http.client.fluent.Request
import org.apache.http.message.BasicNameValuePair
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.xml.XML

/**
  * Access the UptimeRobot API to create monitors - see http://uptimerobot.com/apiv2.
  */
class UptimeRobotApi(apiKey: String) {
  private val log = LoggerFactory.getLogger(classOf[UptimeRobotApi])

  private val baseUrl = "https://api.uptimerobot.com/v2/"
  private val standardFields = Map("api_key" -> apiKey, "format" -> "xml")

//  def getAccountDetails
//  def listMonitors
//  def editMonitor
//  def resetMonitor

//  def listAlertContacts
//  def newAlertContact
//  def editAlertContact
//  def deleteAlertContact

//  def getMWindows
//  def newMWindow
//  def editMWindow


  def newMonitor(monitorDescription: Monitor):Either[ApiError, MonitorId] = {
    val fields = monitorDescription.toQueryMap ++ standardFields
    val response = postRequest("newMonitor", fields)
    parseResponse(response)
  }

  def listMonitorIds(): Either[ApiError, Seq[MonitorId]] = {
    val fields = standardFields
    val response = postRequest("getMonitors", fields)
    parseResponse(response)
  }

  def listMonitorDetails(): Either[ApiError, Seq[MonitorId]] = {
    val fields = standardFields
    val response = postRequest("getMonitors", fields)
    parseResponse(response)
  }

  def deleteMonitor(monitorId: MonitorId):Either[ApiError, MonitorId] = {
    val fields = Map("id" -> monitorId.id) ++ standardFields
    val response = postRequest("deleteMonitor", fields)
    parseResponse(response)
  }

  private def parseResponse[T](response: String):Either[ApiError, T] = {
    try {
      XML.loadString(response) match {
        case m @ <monitor/> =>
          val monitorId = (m \ "@id").toString()
          log.debug("Succeeded with " + m)
          Right(MonitorId(monitorId).asInstanceOf[T])
        case m @ <monitors>{ monitors @ _* }</monitors> =>
          val monitorIds = monitors.map( _ \ "@id").map(id => MonitorId(id.toString()))
          log.debug("Succeeded with "+m)
          Right(monitorIds.asInstanceOf[T])
        case error @ <error/> =>
          val errorId = (error \ "@id").toString
          val details = (error \ "@message").toString
          log.debug("Failed with " + error)
          Left(ApiError(errorId, details))
      }
    } catch {
      case e: Exception => Left(ApiError("---", e.getMessage))
    }
  }

  private def postRequest(action: String, fields: Map[String, String])= {
    log.debug(s"Sending request to $action with fields "+fields)
    val requestUrl = baseUrl+action
    val formValues = fields.map(kv => new BasicNameValuePair(kv._1, kv._2)).asJava
    val response = Request.Post(requestUrl)
      .bodyForm(formValues, StandardCharsets.UTF_8)
      .execute()
      .returnResponse()
    val scanner = new Scanner(response.getEntity.getContent, StandardCharsets.UTF_8.name()).useDelimiter("\\A")
    val content = if (scanner.hasNext()) scanner.next() else ""
    log.debug("Response is "+content)
    content
  }

  private def toQueryString(fields: Map[String, String]): String = {
    fields.map{case (key, value) => key+"="+URLEncoder.encode(value, StandardCharsets.UTF_8.toString) }.mkString("&")
  }
}

// @todo Monitors have monitor windows
abstract class Monitor(friendlyName: String, url: String, alertContacts: Seq[AlertContact], intervalInSeconds: Option[Int]) {
  def toQueryMap: Map[String, String] = {
    val values = Seq("friendly_name" -> friendlyName,
              "url" -> url,
              "alert_contacts" -> alertContacts.map(_.id).mkString("-")) ++
          intervalInSeconds.map( "interval" -> _.toString )
    values.toMap
  }
}

case class HttpMonitor(friendlyName: String, url: String, alertContacts: Seq[AlertContact], intervalInSeconds: Option[Int] = None) extends Monitor(friendlyName, url, alertContacts, intervalInSeconds) {
  override def toQueryMap = super.toQueryMap + ("type" -> "1")
}
case class KeywordMonitor(friendlyName: String, url: String, alertContacts: Seq[AlertContact], alertWhenKeywordMissing: Boolean, keywordValue: String, intervalInSeconds: Option[Int] = None)
    extends Monitor(friendlyName, url, alertContacts, intervalInSeconds) {
  override def toQueryMap = super.toQueryMap + ("type" -> "2", "keyword_type" -> (if (alertWhenKeywordMissing) "2" else "1"), "keyword_value" -> keywordValue)
}
case class PingMonitor(friendlyName: String, url: String, alertContacts: Seq[AlertContact], intervalInSeconds: Option[Int] = None)
    extends Monitor(friendlyName, url, alertContacts, intervalInSeconds) {
  override def toQueryMap = super.toQueryMap + ("type" -> "3")
}
case class PortMonitor(friendlyName: String, url: String, alertContacts: Seq[AlertContact], port: Int, intervalInSeconds: Option[Int] = None)
    extends Monitor(friendlyName, url, alertContacts, intervalInSeconds) {
  override def toQueryMap = super.toQueryMap + ("type" -> "4", "port" -> port.toString, "sub_type" -> "99")
}

// @todo Alert contacts should have their threshold and recurrence fixed
case class AlertContact(id: String, threshold: Int = 0, recurrence: Int = 0)
case class MonitorId(id: String)
case class ApiError(errorId: String, details: String)