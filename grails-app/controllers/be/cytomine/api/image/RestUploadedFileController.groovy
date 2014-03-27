package be.cytomine.api.image

import be.cytomine.SecurityACL
import be.cytomine.api.RestController
import be.cytomine.image.AbstractImage
import be.cytomine.image.ImageInstance
import be.cytomine.image.Mime
import be.cytomine.image.UploadedFile
import be.cytomine.image.server.Storage
import be.cytomine.laboratory.Sample
import be.cytomine.project.Project
import be.cytomine.utils.JSONUtils
import grails.converters.JSON
import jsondoc.annotation.ApiMethodLight
import jsondoc.annotation.ApiParamLight
import jsondoc.annotation.ApiResponseObjectLight
import org.jsondoc.core.annotation.Api

import jsondoc.annotation.ApiParamsLight
import jsondoc.annotation.ApiResponseObjectLight
import org.jsondoc.core.pojo.ApiParamType

import javax.activation.MimetypesFileTypeMap

/**
 * Controller that handle request on file uploading (when a file is uploaded, list uploaded files...)
 */
@Api(name = "uploaded file services", description = "Methods for managing an uploaded image file.")
class RestUploadedFileController extends RestController {

    def imageProcessingService
    def cytomineService
    def imagePropertiesService
    def projectService
    def storageService
    def grailsApplication
    def uploadedFileService
    def storageAbstractImageService
    def imageInstanceService
    def abstractImageService
    def notificationService

    static allowedMethods = [image: 'POST']

    @ApiMethodLight(description="Get all uploaded file made by the current user")
    def list() {
        //get all uploaded file for this user
        def uploadedFiles = uploadedFileService.list(cytomineService.getCurrentUser())

        //if view is datatables, change way to store data
        if (params.dataTables) {
            uploadedFiles = ["aaData" : uploadedFiles]
        }
        responseSuccess(uploadedFiles)
    }

    @ApiMethodLight(description="Delete all file properties for an image")
    @ApiParamsLight(params=[
    @ApiParamLight(name="id", type="long", paramType = ApiParamType.PATH, description = "The image id")
    ])
    @ApiResponseObjectLight(objectIdentifier = "empty")
    def clearProperties () {
        AbstractImage abstractImage = abstractImageService.read(params.long('id'))
        imagePropertiesService.clear(abstractImage)
        responseSuccess([:])
    }

    @ApiMethodLight(description="Get all file properties for an image")
    @ApiParamsLight(params=[
    @ApiParamLight(name="id", type="long", paramType = ApiParamType.PATH, description = "The image id")
    ])
    @ApiResponseObjectLight(objectIdentifier = "empty")
    def populateProperties () {
        AbstractImage abstractImage = abstractImageService.read(params.long('id'))
        imagePropertiesService.populate(abstractImage)
        responseSuccess([:])
    }

    @ApiMethodLight(description="Fill image field (magn, width,...) with all file properties")
    @ApiParamsLight(params=[
    @ApiParamLight(name="id", type="long", paramType = ApiParamType.PATH, description = "The image id")
    ])
    @ApiResponseObjectLight(objectIdentifier = "empty")
    def extractProperties () {
        AbstractImage abstractImage = abstractImageService.read(params.long('id'))
        imagePropertiesService.extractUseful(abstractImage)
        responseSuccess([:])
    }


    @ApiMethodLight(description="Get an uploaded file")
    @ApiParamsLight(params=[
    @ApiParamLight(name="id", type="long", paramType = ApiParamType.PATH, description = "The uploaded file id")
    ])
    def show () {
        UploadedFile up = uploadedFileService.read(params.long('id'))
        if (up) {
            responseSuccess(up)
        } else {
            responseNotFound("UploadedFile", params.id)
        }
    }

    /**
     * Add a new image
     * TODO:: how to manage security here?
     *
     */
    @ApiMethodLight(description="Add a new uploaded file. This DOES NOT upload the file, just create the domain.")
    def add () {
        add(uploadedFileService, request.JSON)
    }

    /**
     * Update a new image
     * TODO:: how to manage security here?
     */
    @ApiMethodLight(description="Edit an uploaded file domain (usefull to edit its status during upload)")
    @ApiParamsLight(params=[
    @ApiParamLight(name="id", type="long", paramType = ApiParamType.PATH,description = "The uploaded file id")
    ])
    def update () {
        update(uploadedFileService, request.JSON)
    }

    /**
     * Delete a new image
     * TODO:: how to manage security here?
     */
    @ApiMethodLight(description="Delete an uploaded file domain. This do not delete the file on disk.")
    @ApiParamsLight(params=[
    @ApiParamLight(name="id", type="long", paramType = ApiParamType.PATH,description = "The uploaded file id")
    ])
    def delete () {
        delete(uploadedFileService, JSON.parse("{id : $params.id}"),null)
    }

    def upRedirect () {
        redirect(url: "http://localhost:9090/upload")
    }

    @ApiMethodLight(description="Create an image thanks to an uploaded file domain. THis add the image in the good storage and the project (if needed). This send too an email at the end to the uploader and the project admins.")
    @ApiParamsLight(params=[
    @ApiParamLight(name="uploadedFile", type="long", paramType = ApiParamType.PATH,description = "The uploaded file id")
    ])
    @ApiResponseObjectLight(objectIdentifier = "[abstractimage.|abstract image|]")
    def createImage () {
        long timestamp = new Date().getTime()
        def currentUser = cytomineService.currentUser
        SecurityACL.checkUser(currentUser)
        UploadedFile uploadedFile = UploadedFile.read(params.long('uploadedFile'))
        Collection<Storage> storages = []
        uploadedFile.getStorages()?.each {
            storages << storageService.read(it)
        }

        Sample sample = new Sample(name : timestamp.toString() + "-" + uploadedFile.getOriginalFilename())

        def projects = []
        //create domains instance
        def ext = uploadedFile.getExt()
        Mime mime = Mime.findByExtension(ext)
        if (!mime) {
            MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
            String mimeType = mimeTypesMap.getContentType(uploadedFile.getAbsolutePath())
            mime = new Mime(extension: ext, mimeType : mimeType)
            mime.save(failOnError: true)
        }
        log.info "#################################################################"
        log.info "#################################################################"
        log.info "##############CREATE IMAGE#################"
        log.info "#################################################################"
        log.info "#################################################################"
        AbstractImage abstractImage = new AbstractImage(
                filename: uploadedFile.getFilename(),
                originalFilename:  uploadedFile.getOriginalFilename(),
                scanner: null,
                sample: sample,
                path: uploadedFile.getFilename(),
                mime: mime)

        if (sample.validate() && abstractImage.validate()) {
            sample.save(flush : true,failOnError: true)
            sample.refresh()
            abstractImage.setSample(sample)
            abstractImage.save(flush: true,failOnError: true)

            storages.each { storage ->
                storageAbstractImageService.add(JSON.parse(JSONUtils.toJSONString([storage : storage.id, abstractimage : abstractImage.id])))
            }

            imagePropertiesService.clear(abstractImage)
            imagePropertiesService.populate(abstractImage)
            imagePropertiesService.extractUseful(abstractImage)
            abstractImage.save(flush: true,failOnError: true)

            uploadedFile.getProjects()?.each { project_id ->
                Project project = projectService.read(project_id)
                projects << project
                ImageInstance imageInstance = new ImageInstance( baseImage : abstractImage, project:  project, user :currentUser)
                imageInstanceService.add(JSON.parse(imageInstance.encodeAsJSON()))
            }

        } else {
            sample.errors?.each {
                log.info "Sample error : " + it
            }
            abstractImage.errors?.each {
                log.info "Sample error : " + it
            }
        }
        notificationService.notifyNewImageAvailable(currentUser,abstractImage,projects)
        responseSuccess([abstractimage: abstractImage])
    }

    def secUserService



}
