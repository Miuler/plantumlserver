package controllers

import java.io.ByteArrayOutputStream
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


  def uml(file:String, bare:Boolean) = {
    val fileUml = root + file
    val fileUmlKey = fileUml + bare
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
        cacheApi.remove(fileUmlKey)
        cacheApi.remove(fileUmlTime)
        logger.info(s"set time in cache $fileUmlTime: $lastModifiedTime")
        cacheApi.set(fileUmlTime, lastModifiedTime)
      }
    }

    cached(fileUmlKey) {
      Action {
        logger.info("new action")
        val puml = new String(Files.readAllBytes(fileUmlPath))
        val sourceStringReader = new SourceStringReader(puml, Paths.get(root + file).getParent.toFile)
        val outputStream = new ByteArrayOutputStream()
        val description = sourceStringReader.generateImage(outputStream, new FileFormatOption(FileFormat.SVG))
        outputStream.close()
        if (bare) {
          Ok(outputStream.toString()).as("image/svg+xml")
        } else {
          Ok(views.html.puml(file, outputStream.toString()))
        }
      }
    }
  }

  def readme(markdown:String) = {
    val markdownUri = root + markdown
    val readmeTime = markdownUri + "lastModifiedTime"
    val readmePath = Paths.get(markdownUri)
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
        cacheApi.remove(markdownUri)
        cacheApi.remove(readmeTime)
        logger.info(s"set time in cache $readmeTime: $lastModifiedTime")
        cacheApi.set(readmeTime, lastModifiedTime)
      }
    }

    cached(markdownUri) {
      Action {
        Ok(views.html.markdown(markdown, readmeHtml))
      }
    }
  }

  def directory(directory:String) = Action {
    val directoryPath = Paths.get(root + directory)
    val prePaths = Files.walk(directoryPath, 1).collect(Collectors.toList()).toList
      .sortBy(_.getFileName)
      .filter(_ != directoryPath)
    val paths = (if(directory != "/") List(Paths.get(root + "/..")) else Nil) ++ prePaths
    Ok(views.html.directory("." + directory, paths))
  }

  def clean() = Action {
    logger.info("clean all cache")
    cacheManagerProvider.get.clearAll()
    Ok("Cache borrado")
  }

  def raw(path:String) = {
    val fileKey = root + path
    val _path = Paths.get(root + path)

    val fileKeyTime = fileKey + "lastModifiedTime"

    val lastModifiedTime = Files.getLastModifiedTime(_path)
    val lastModifiedTimeCache:Option[FileTime] = cacheApi.get(fileKeyTime)

    if(lastModifiedTimeCache.isEmpty) {
      logger.info(s"set time in cache $fileKeyTime: $lastModifiedTime")
      cacheApi.set(fileKeyTime, lastModifiedTime)
    } else {
      if (!lastModifiedTime.equals(lastModifiedTimeCache.get)) {
        logger.info(s"clear cache")
        cacheApi.remove(fileKey)
        cacheApi.remove(fileKeyTime)
        logger.info(s"set time in cache $fileKeyTime: $lastModifiedTime")
        cacheApi.set(fileKeyTime, lastModifiedTime)
      }
    }

    cached("raw" + fileKey) {
      Action{Ok(new String(Files.readAllBytes(_path)))}
    }
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
