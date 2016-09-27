package controllers

import java.io.ByteArrayOutputStream
import java.nio.file.attribute.FileTime
import java.nio.file.{Files, Paths}
import java.util.stream.Collectors
import java.net.URLDecoder
import javax.inject.{Inject, Singleton}

import net.sourceforge.plantuml.{FileFormat, FileFormatOption, SourceStringReader}
import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
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
    val _file = URLDecoder.decode(file)
    val fileUml = root + _file
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
        logger.info(s"puml: $puml")
        logger.info(s"root + file: ${root + _file}")
        logger.info(s"Paths.get(root + file).getParent: ${Paths.get(root + _file).getParent}")
        val sourceStringReader = new SourceStringReader(puml, Paths.get(root + _file).getParent.toFile)
        logger.info(s"sourceStringReader: $sourceStringReader")
        val outputStream = new ByteArrayOutputStream()
        logger.info(s"outputStream: $outputStream")
        val description = sourceStringReader.generateImage(outputStream, new FileFormatOption(FileFormat.SVG))
        outputStream.close()
        if (bare) {
          Ok(outputStream.toString()).as("image/svg+xml")
        } else {
          Ok(views.html.puml(_file, outputStream.toString()))
        }
      }
    }
  }

  def markdown(markdown:String) = {
    val _markdown = URLDecoder.decode(markdown)
    val markdownUri = root + _markdown
    val readmeTime = markdownUri + "lastModifiedTime"
    val readmePath = Paths.get(markdownUri)
//    val pegDownProcessor = new PegDownProcessor()

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

      //      val readmeHtml = pegDownProcessor.markdownToHtml(new String(Files.readAllBytes(readmePath)))
      val extensions = List(
        TablesExtension.create(),
        AutolinkExtension.create(),
        StrikethroughExtension.create(),
        HeadingAnchorExtension.create()
      )

      val parser = Parser.builder().extensions(extensions).build()
      val node = parser.parse(new String(Files.readAllBytes(readmePath)))
      val htmlRenderer = HtmlRenderer.builder().extensions(extensions).build()
      val readmeHtml = htmlRenderer.render(node)
      Action {
        Ok(views.html.markdown(_markdown, readmeHtml))
      }
    }
  }

  def directory(directory:String) = Action {
    val _directory = URLDecoder.decode(directory)
    val directoryPath = Paths.get(root + _directory)
    val prePaths = Files.walk(directoryPath, 1).collect(Collectors.toList()).toList
      .sortBy(_.getFileName)
      .filter(_ != directoryPath)
    val paths = (if(_directory != "/") List(Paths.get(root + "/..")) else Nil) ++ prePaths
    Ok(views.html.directory("." + _directory, paths))
  }

  def clean() = Action {
    logger.info("clean all cache")
    cacheManagerProvider.get.clearAll()
    Ok("Cache borrado")
  }

  def raw(path:String) = {
    val fileKey = root + URLDecoder.decode(path)
    val _path = Paths.get(root + URLDecoder.decode(path))

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
