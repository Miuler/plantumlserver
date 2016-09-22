package filters

import javax.inject.{Inject, Singleton}

import akka.stream.Materializer
import play.api.Logger
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by miuler on 9/22/16.
  */
@Singleton
class AccessLoggingFilter @Inject() (implicit override val mat: Materializer) extends Filter {

  val accessLogger = Logger(getClass)

  override def apply(nextFilter: RequestHeader => Future[Result])
                    (requestHeader: RequestHeader): Future[Result] = {
    val resultFuture = nextFilter(requestHeader)

    resultFuture.foreach(result => {
      val msg = s"method=${requestHeader.method} uri=${requestHeader.uri} remote-address=${requestHeader.remoteAddress}" +
        s" status=${result.header.status}";
      accessLogger.info(msg)
    })

    resultFuture
  }
}