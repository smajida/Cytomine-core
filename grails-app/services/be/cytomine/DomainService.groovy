package be.cytomine

import be.cytomine.Exception.CytomineException
import be.cytomine.Exception.InvalidRequestException
import be.cytomine.Exception.WrongArgumentException

class DomainService {

    static transactional = true

    def saveDomain(def newObject) {
        newObject.checkAlreadyExist()
        if (!newObject.validate()) {
            println newObject.errors
            println newObject.retrieveErrors().toString()
            CytomineException cyto = new WrongArgumentException(newObject.retrieveErrors().toString())
            throw new WrongArgumentException(newObject.retrieveErrors().toString())
        }
        def modifiedFieldNames = newObject.getDirtyPropertyNames()
        for (fieldName in modifiedFieldNames) {
           println "field change = " + fieldName
        }
        if (!newObject.save(flush: true)) throw new InvalidRequestException(newObject.retrieveErrors().toString())
    }

    def deleteDomain(def oldObject) {
        try {
//            println "*** deleteDomain.vesion=" + oldObject.version
//            println "*** object=" + oldObject
//            println "*** recup=" + oldObject.read(oldObject.id)
//            println "*** refresh=" + oldObject.refresh()
            oldObject.refresh()
            oldObject.delete(flush: true, failOnError: true)
        } catch (Exception e) {
            log.error e.toString()
            throw new InvalidRequestException(e.toString())
        }

    }

    def editDomain(def newObject, def postData) {
        if (postData.id instanceof String) {
            newObject.id = Long.parseLong(postData.id)
        } else {
            newObject.id = postData.id
        }
        newObject.checkAlreadyExist()
        if (!newObject.validate()) throw new WrongArgumentException(newObject.errors.toString())
        if (!newObject.save(flush: true)) throw new InvalidRequestException(newObject.errors.toString())
    }
}
