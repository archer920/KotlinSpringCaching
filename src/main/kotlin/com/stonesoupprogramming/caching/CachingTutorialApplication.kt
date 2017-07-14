package com.stonesoupprogramming.caching

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.EnableCaching
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Service
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Lob

@SpringBootApplication
@EnableJpaRepositories
@EnableCaching  //Spring Boot provides a CacheManager our of the box
                //but it only turns on when this annotation is present
class CachingTutorialApplication

//This is a persisted class that represents a file
//saved to the database. It has a @Lob property (bytes)
//which stores the bytes of the file. The file could be
//as large as 25MB
@Entity
data class PersistedFile(
        @field: Id @field: GeneratedValue var id : Long = 0,
        var fileName : String = "",
        var mime : String = "",
        @field : Lob var bytes : ByteArray? = null)

//Define an extension function to MultipartFile to easily
//convert to our PersistedFile class
fun MultipartFile.toPersistedFile() : PersistedFile =
        PersistedFile(fileName = this.originalFilename, mime = this.contentType, bytes = this.bytes)

//Make a respository class using JpaRepository
interface PersistedFileRepository : JpaRepository<PersistedFile, Long>

//We are going to use this class to handle caching of our PersistedFile object
//Normally, we would encapsulate our repository, but we are leaving it public to keep the code down
@Service
class PersistedFileService(@Autowired val persistedFileRepository: PersistedFileRepository){

    //This annotation will cause the cache to store a persistedFile in memory
    //so that the program doesn't have to hit the DB each time for the file.
    //This will result in faster page load times. Since we know that managed objects
    //have unique primary keys, we can just use the primary key for the cache key
    @Cacheable(cacheNames = arrayOf("persistedFile"), key="#id")
    fun findOne(id : Long) : PersistedFile = persistedFileRepository.findOne(id)

    //This annotation will cause the cache to store persistedFile ids
    //By storing the ids, we don't need to hit the DB to know if a file exists first
    @Cacheable(cacheNames = arrayOf("persistedIds"))
    fun exists(id: Long?): Boolean = persistedFileRepository.exists(id)
}

@Controller
@RequestMapping("/")
class IndexController(@Autowired private val persistedFileService: PersistedFileService){

    @GetMapping()
    fun doGet() : String = "index"

    @PostMapping("/upload")
    fun doUpload(
            @RequestParam("file")
            multipartFile: MultipartFile) : String {
        persistedFileService.persistedFileRepository.save(multipartFile.toPersistedFile())
        return "index"
    }

    @PostMapping("/load")
    fun load(@RequestParam("id") id : Long,
             model : Model) : String {
        if (persistedFileService.exists(id)){
            val persistedFile = persistedFileService.findOne(id)
            model.apply {
                addAttribute("found", true)
                addAttribute("id", persistedFile.id)
                addAttribute("name", persistedFile.fileName)
                addAttribute("mime", persistedFile.mime)
            }
        } else {
            model.addAttribute("found", false)
        }
        return "index"
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(CachingTutorialApplication::class.java, *args)
}
