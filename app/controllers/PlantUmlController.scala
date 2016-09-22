package controllers

import java.io.{ByteArrayOutputStream, File}
import java.nio.file.attribute.FileTime
import java.nio.file.{Files, Paths}
import javax.inject.{Inject, Singleton}

import net.sourceforge.plantuml.{FileFormat, FileFormatOption, SourceStringReader}
import play.api.Logger
import play.api.cache.{CacheApi, Cached}
import play.api.mvc.{Action, Controller}

/**
  * Created by miuler on 9/21/16.
  */
@Singleton
class PlantUmlController @Inject() (cached: Cached, cacheApi: CacheApi) extends Controller {

  val logger = Logger(getClass)
  val root = "/home/miuler/"

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
        logger.info(s"uml: ${cacheApi.get(fileUml)}")
        cacheApi.remove(fileUml)
        logger.info(s"uml: ${cacheApi.get(fileUml)}")
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

}

