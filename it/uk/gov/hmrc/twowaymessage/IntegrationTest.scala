package uk.gov.hmrc.twowaymessage

import java.util.UUID
import java.util.concurrent.TimeUnit

import org.scalatest.Ignore
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSClient
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.integration.ServiceSpec
import uk.gov.hmrc.test.it.CanCreateAuthority

import scala.concurrent.duration.{Duration, FiniteDuration}

@Ignore
class IntegrationTest extends WordSpec with Matchers with  CanCreateAuthority with ServiceSpec  {

  def externalServices: Seq[String] = Seq("datastream", "auth", "message", "auth-login-api")

  implicit val defaultTimeout: FiniteDuration = Duration(15, TimeUnit.SECONDS)

  override def authResource(r: String) = externalResource("auth-login-api", r)

  override def httpClient = app.injector.instanceOf[WSClient]
  override def additionalConfig: Map[String, _] = Map("auditing.consumer.baseUri.port" -> externalServicePorts("datastream"))

  val nino = Nino("CE100000D")

  val authHeader =
    governmentGatewayAuthority()
      .withNino(nino)
      .bearerTokenHeader()

  "Creating a message" should {
    "be successful given valid json" in {
      val content = UUID.randomUUID().toString

      val jsonString =
        s"""
           | {
           |   "email": "test@test.com",
           |   "subject": "subject",
           |   "content": "$content",
           |   "replyTo": "replyTo"
           | }
    """.stripMargin

      val message = Json.parse(jsonString).as[JsObject]
      val response = httpClient.url(resource("/two-way-message/message/customer/0/submit")).withHeaders(authHeader).post(message).futureValue

      response.status shouldBe 201
    }

    "fail given invalid json" in {
      val content = UUID.randomUUID().toString
      val jsonString =
        s"""
         | {
         |   "email": "test@test.com",
         |   "content": "$content",
         |   "replyTo": "replyTo"
         | }
    """.stripMargin

      val message = Json.parse(jsonString).as[JsObject]
      val response = httpClient.url(resource("/two-way-message/message/customer/0/submit")).withHeaders(authHeader).post(message).futureValue

      response.status shouldBe 400
    }
  }
}

@Ignore
class IntegrationTestWithoutMessage extends WordSpec with Matchers with  CanCreateAuthority with ServiceSpec  {

  def externalServices: Seq[String] = Seq("datastream", "auth", "auth-login-api")

  implicit val defaultTimeout: FiniteDuration = Duration(15, TimeUnit.SECONDS)
  override def authResource(r: String) = externalResource("auth-login-api", r)

  override def httpClient = app.injector.instanceOf[WSClient]

  val nino = Nino("CE100000D")

  val authHeader =
    governmentGatewayAuthority()
      .withNino(nino)
      .bearerTokenHeader()

  override def additionalConfig: Map[String, _] = Map("auditing.consumer.baseUri.port" -> externalServicePorts("datastream"))

  "Creating a message if message microservice is unavailable" should {
    "fail even with a valid json" in {
      val content = UUID.randomUUID().toString
      val jsonString =
        s"""
           | {
           |   "email": "test@test.com",
           |   "subject": "subject",
           |   "content": "$content",
           |   "replyTo": "replyTo"
           | }
    """.stripMargin

      val message = Json.parse(jsonString).as[JsObject]
      val response = httpClient.url(resource("/two-way-message/message/customer/0/submit")).withHeaders(authHeader).post(message).futureValue

      response.status shouldBe 502
    }
  }
}
