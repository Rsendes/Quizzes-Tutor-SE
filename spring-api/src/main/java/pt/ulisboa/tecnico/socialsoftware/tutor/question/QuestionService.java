package pt.ulisboa.tecnico.socialsoftware.tutor.question;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.TutorException;
import pt.ulisboa.tecnico.socialsoftware.tutor.impexp.domain.QuestionsXmlExport;
import pt.ulisboa.tecnico.socialsoftware.tutor.impexp.domain.QuestionsXmlImport;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.domain.Image;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.domain.Question;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.dto.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.dto.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.repository.QuestionRepository;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.repository.TopicRepository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.ExceptionError.QUESTION_NOT_FOUND;

@Service
public class QuestionService {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private TopicRepository topicRepository;

    @PersistenceContext
    EntityManager entityManager;

    @Transactional
    public QuestionDto findQuestionById(Integer questionId) {
        return questionRepository.findById(questionId).map(QuestionDto::new)
                .orElseThrow(() -> new TutorException(QUESTION_NOT_FOUND, questionId));
    }

    @Transactional
    public QuestionDto findQuestionByNumber(Integer number) {
        return questionRepository.findByNumber(number).map(QuestionDto::new)
                .orElseThrow(() -> new TutorException(QUESTION_NOT_FOUND, number));
    }

    @Transactional
    public List<QuestionDto> findAllQuestions(Integer pageIndex, Integer pageSize) {
        return questionRepository.findAll(PageRequest.of(pageIndex, pageSize)).getContent().stream().map(QuestionDto::new).collect(Collectors.toList());
    }

    @Transactional
    public List<QuestionDto> findActiveQuestions() {
        return questionRepository.getActiveQuestions().stream().map(QuestionDto::new).collect(Collectors.toList());
    }

    @Transactional
    public QuestionDto createQuestion(QuestionDto questionDto) {
        if (questionDto.getNumber() == null) {
            int maxQuestionNumber = questionRepository.getMaxQuestionNumber() != null ?
                    questionRepository.getMaxQuestionNumber() : 0;
            questionDto.setNumber(maxQuestionNumber + 1);
        }

        Question question = new Question(questionDto);
        this.entityManager.persist(question);
        return new QuestionDto(question);
    }

    @Transactional
    public void updateQuestion(Integer questionId, QuestionDto questionDto) {
        Question question = questionRepository.findById(questionId).orElseThrow(() -> new TutorException(QUESTION_NOT_FOUND, questionId));
        question.update(questionDto);
    }

    @Transactional
    public void removeQuestion(Integer questionId) {
        Question question = questionRepository.findById(questionId).orElseThrow(() -> new TutorException(QUESTION_NOT_FOUND, questionId));
        question.remove();
        entityManager.remove(question);
    }

    @Transactional
    public void questionSwitchActive(Integer questionId) {
        Question question = questionRepository.findById(questionId).orElseThrow(() -> new TutorException(QUESTION_NOT_FOUND, questionId));
        question.switchActive();
    }

    @Transactional
    public void uploadImage(Integer questionId, String type) {
        Question question = questionRepository.findById(questionId).orElseThrow(() -> new TutorException(QUESTION_NOT_FOUND, questionId));

        Image image = question.getImage();

        if (image == null) {
            image = new Image();

            question.addImage(image);

            entityManager.persist(image);
        }

        question.getImage().setUrl(question.getNumber() + "." + type);
    }

    @Transactional
    public void updateQuestionTopics(Integer questionId, TopicDto[] topics) {
        Question question = questionRepository.findById(questionId).orElseThrow(() -> new TutorException(QUESTION_NOT_FOUND, questionId));

        question.updateTopics(Arrays.stream(topics).map(topicDto -> topicRepository.findByName(topicDto.getName())).collect(Collectors.toSet()));
    }

    public String exportQuestions() {
        QuestionsXmlExport xmlExporter = new QuestionsXmlExport();

        return xmlExporter.export(questionRepository.findAll());
    }

    @Transactional
    public void importQuestions(String questionsXML) {
        QuestionsXmlImport xmlImporter = new QuestionsXmlImport();

        xmlImporter.importQuestions(questionsXML, this);
    }
}
