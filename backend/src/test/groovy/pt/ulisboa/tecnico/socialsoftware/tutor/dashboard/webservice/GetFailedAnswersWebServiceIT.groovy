package pt.ulisboa.tecnico.socialsoftware.tutor.dashboard.webservice

import groovyx.net.http.HttpResponseException
import groovyx.net.http.RESTClient
import org.apache.http.HttpStatus
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import pt.ulisboa.tecnico.socialsoftware.tutor.SpockTest

import pt.ulisboa.tecnico.socialsoftware.tutor.answer.dto.MultipleChoiceStatementAnswerDetailsDto
import pt.ulisboa.tecnico.socialsoftware.tutor.answer.dto.StatementAnswerDto
import pt.ulisboa.tecnico.socialsoftware.tutor.answer.dto.StatementQuizDto
import pt.ulisboa.tecnico.socialsoftware.tutor.auth.domain.AuthUser
import pt.ulisboa.tecnico.socialsoftware.tutor.dashboard.domain.Dashboard
import pt.ulisboa.tecnico.socialsoftware.tutor.dashboard.service.FailedAnswersSpockTest
import pt.ulisboa.tecnico.socialsoftware.tutor.question.dto.MultipleChoiceQuestionDto
import pt.ulisboa.tecnico.socialsoftware.tutor.question.dto.OptionDto
import pt.ulisboa.tecnico.socialsoftware.tutor.question.domain.Question
import pt.ulisboa.tecnico.socialsoftware.tutor.question.dto.QuestionDto
import pt.ulisboa.tecnico.socialsoftware.tutor.quiz.domain.Quiz
import pt.ulisboa.tecnico.socialsoftware.tutor.quiz.dto.QuizDto
import pt.ulisboa.tecnico.socialsoftware.tutor.user.domain.Student

import java.time.LocalDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GetFailedAnswersWebServiceIT extends FailedAnswersSpockTest {
    @LocalServerPort
    private int port

    def response
    def courseExecution


    def setup() {
        given: 'a rest client'
        restClient = new RESTClient("http://localhost:" + port)

        and: 'a demo course execution'
        courseExecution = courseService.getDemoCourse()

        and: 'a quiz with future conclusionDate'
        createQuizAndQuestionIT()

        and: 'a student login'
        demoStudentLogin()
        student = authUserService.demoStudentAuth(false).getUser()

        and: 'a dashboard'
        def dashboardDto = dashboardService.createDashboard(courseExecution.getCourseExecutionId(), student.getId())
    }

    def "student gets failed answers from dashboard"() {

        given: 'a failed answer to the quiz'
        def answer = answerQuizIT()

        when: 'the web service is invoked'
        response = restClient.get(
                path: '/students/dashboards/executions/' + courseExecution.getCourseExecutionId() + '/failedanswers/',
                requestContentType: 'application/json'
        )

        then: "the request returns 200"
        response != null
        response.status == 200
        response.data.id != null

        and: 'it is in the database'
        failedAnswerRepository.findAll().size() == 1

        and: 'it is the right failed answer'
        def fa = failedAnswerRepository.findAll().get(0)
        fa.getQuestionAnswer().getId() == answer.getQuestionAnswerId()

        and: 'it is in the dashboard'
        def dashboard = dashboardRepository.findAll().get(0)
        dashboard.getAllFailedAnswers().get(0).getId() == fa.getId()

        cleanup:
        dashboardRepository.deleteAll()
        quizAnswerRepository.deleteAll()
        userRepository.deleteAll()
        failedAnswerRepository.deleteAll()
    }

    def "teacher can't get student's failed answers from dashboard"() {

        given: 'a teacher login'
        demoTeacherLogin()

        when: 'the web service is invoked'
        response = restClient.get(
                path: '/students/dashboards/executions/' + courseExecution.getCourseExecutionId() + '/failedanswers/',
                requestContentType: 'application/json'
        )

        then: "no permission to do the request"
        response != null
        response.status == 403

        cleanup:
        dashboardRepository.deleteAll()
        quizAnswerRepository.deleteAll()
        userRepository.deleteAll()
        failedAnswerRepository.deleteAll()
    }

    def "student can't get another student's failed answers from dashboard"() {

        given: 'a failed answer to the quiz'
        answerQuizIT()

        and: 'another student login'
        demoStudentLogin()
        def newStudent = authUserService.demoStudentAuth(true).getUser()

        and: 'a new dashboard'
        def newDashboardDto = dashboardService.createDashboard(courseExecution.getCourseExecutionId(), newStudent.getId())

        when: 'the web service is invoked'
        response = restClient.get(
                path: '/students/dashboards/executions/' + courseExecution.getCourseExecutionId() + '/failedanswers/',
                requestContentType: 'application/json'
        )

        then: "the request returns 200"
        response != null
        response.status == 200
        and: "has value"
        response.data.id != null

        and: 'it is not in the new student dashboard'
        def dashboard = dashboardRepository.getById(newDashboardDto.getId())
        dashboard.getFailedAnswers().isEmpty()

        cleanup:
        dashboardRepository.deleteAll()
        quizAnswerRepository.deleteAll()
        userRepository.deleteAll()
        failedAnswerRepository.deleteAll()
    }

    def cleanup(){
        failedAnswerRepository.deleteAll()
        dashboardRepository.deleteAll()
        quizAnswerRepository.deleteAll()
        userRepository.deleteAll()
        quizQuestionRepository.deleteAll()
        questionRepository.deleteAll()
        questionDetailsRepository.deleteAll()
        quizRepository.deleteAll()
        courseExecutionRepository.deleteAll()
        courseRepository.deleteAll()
    }


}