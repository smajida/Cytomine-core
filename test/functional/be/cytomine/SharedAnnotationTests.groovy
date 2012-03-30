package be.cytomine

import be.cytomine.ontology.Annotation
import be.cytomine.ontology.AnnotationTerm
import be.cytomine.test.BasicInstance
import be.cytomine.test.Infos
import be.cytomine.test.http.AnnotationAPI
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import be.cytomine.test.http.AnnotationCommentAPI
import be.cytomine.security.User

/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 * Date: 8/02/11
 * Time: 9:01
 * To change this template use File | Settings | File Templates.
 */
class SharedAnnotationTests extends functionaltestplugin.FunctionalTestCase {

    void testGetAnnotationCommentWithCredential() {
        def sharedAnnotation = BasicInstance.createOrGetBasicSharedAnnotation()
        def result = AnnotationCommentAPI.show(sharedAnnotation.annotation.id, sharedAnnotation.id, Infos.GOODLOGIN,Infos.GOODPASSWORD)
        assertEquals(200, result.code)
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
    }

    void testListAnnotationCommentsByAnnotationWithCredential() {
        def sharedAnnotation = BasicInstance.createOrGetBasicSharedAnnotation()
        def result = AnnotationCommentAPI.list(sharedAnnotation.annotation.id, Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(200, result.code)
        def json = JSON.parse(result.data)
        assert json instanceof JSONArray
    }

    void testAddAnnotationComments() {
        def sharedAnnotation = BasicInstance.getBasicSharedAnnotationNotExist()
        def json = JSON.parse((String)sharedAnnotation.encodeAsJSON())
        json.subject = "subject for test mail"
        json.message = "message for test mail"
        json.users = [User.findByUsername('lbtd').id]
        def result = AnnotationCommentAPI.create(sharedAnnotation.annotation.id,json.encodeAsJSON(), Infos.GOODLOGIN, Infos.GOODPASSWORD)
        assertEquals(200, result.code)

        result = AnnotationCommentAPI.show(sharedAnnotation.annotation.id, sharedAnnotation.id, Infos.GOODLOGIN,Infos.GOODPASSWORD)
        assertEquals(200, result.code)

    }
}