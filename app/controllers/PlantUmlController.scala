package controllers

import java.io.{ByteArrayOutputStream, File}
import java.nio.file.attribute.FileTime
import java.nio.file.{Files, Paths}
import java.util.stream.Collectors
import javax.inject.{Inject, Singleton}

import net.sourceforge.plantuml.{FileFormat, FileFormatOption, SourceStringReader}
import org.pegdown.PegDownProcessor
import play.Configuration
import play.api.Logger
import play.api.cache.{CacheApi, CacheManagerProvider, Cached}
import play.api.mvc._
import play.twirl.api.Html

import scala.collection.JavaConversions._

/**
  * Created by miuler on 9/21/16.
  */
@Singleton
class PlantUmlController @Inject()(cached: Cached,
                                   cacheApi: CacheApi,
                                   config: Configuration,
                                   cacheManagerProvider: CacheManagerProvider) extends Controller {

  val logger = Logger(getClass)
  val root = config.getString("puml.root")


  def uml(file:String) = {
    val fileUml = root + file
    val fileUmlTime = fileUml + "lastModifiedTime"
    logger.info(s"fileUml: ${fileUml}")

    val fileUmlPath = Paths.get(fileUml)
    val lastModifiedTime = Files.getLastModifiedTime(fileUmlPath)
    val lastModifiedTimeCache:Option[FileTime] = cacheApi.get(fileUmlTime)

    if(lastModifiedTimeCache.isEmpty) {
      logger.info(s"set time in cache $fileUmlTime: $lastModifiedTime")
      cacheApi.set(fileUmlTime, lastModifiedTime)
    } else {
      if (!lastModifiedTime.equals(lastModifiedTimeCache.get)) {
        logger.info(s"clear cache")
        cacheApi.remove(fileUml)
        cacheApi.remove(fileUmlTime)
        logger.info(s"set time in cache $fileUmlTime: $lastModifiedTime")
        cacheApi.set(fileUmlTime, lastModifiedTime)
      }
    }

    cached(fileUml) {
      Action {
        logger.info("new action")
        val puml = new String(Files.readAllBytes(fileUmlPath))
        val sourceStringReader = new SourceStringReader(puml, new File(root))
        val outputStream = new ByteArrayOutputStream()
        val description = sourceStringReader.generateImage(outputStream, new FileFormatOption(FileFormat.SVG))
        outputStream.close()
        Ok(outputStream.toString()).as("image/svg+xml")
      }
    }
  }

  def readme() = {
    val readme = root + "README.md"
    val readmeTime = readme + "lastModifiedTime"
    val readmePath = Paths.get(readme)
    val pegDownProcessor = new PegDownProcessor()
    val readmeHtml = pegDownProcessor.markdownToHtml(new String(Files.readAllBytes(readmePath)))

    val lastModifiedTime = Files.getLastModifiedTime(readmePath)
    val lastModifiedTimeCache:Option[FileTime] = cacheApi.get(readmeTime)

    if(lastModifiedTimeCache.isEmpty) {
      logger.info(s"set time in cache $readmeTime: $lastModifiedTime")
      cacheApi.set(readmeTime, lastModifiedTime)
    } else {
      if (!lastModifiedTime.equals(lastModifiedTimeCache.get)) {
        logger.info(s"clear cache")
        cacheApi.remove(readme)
        cacheApi.remove(readmeTime)
        logger.info(s"set time in cache $readmeTime: $lastModifiedTime")
        cacheApi.set(readmeTime, lastModifiedTime)
      }
    }

    cached(readme) {
      Action {
        Ok(views.html.readme(Html(readmeHtml)))
      }
    }
  }

  def umlRoot() = Action {
    val readmePath = Paths.get(root)
    val paths = Files.walk(readmePath, 1).collect(Collectors.toList()).toList.sortBy(_.getFileName)
    Ok(views.html.root(paths))
  }

  def clean() = Action {
    cacheManagerProvider.get.clearAll()
    Ok("Cache borrado")
  }

}

//trait AccessLogging {
//
//  val accessLogger = Logger("access")
//
//  object Action extends ActionBuilder[Request] {
//
//    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
//      accessLogger.info(s"method=${request.method} uri=${request.uri} remote-address=${request.remoteAddress}")
//      block(request)
//    }
//  }
//}
