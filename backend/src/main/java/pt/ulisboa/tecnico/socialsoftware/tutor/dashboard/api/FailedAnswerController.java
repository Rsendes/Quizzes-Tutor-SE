package pt.ulisboa.tecnico.socialsoftware.tutor.dashboard.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.ulisboa.tecnico.socialsoftware.tutor.dashboard.dto.UpdatedFailedAnswersDto;
import pt.ulisboa.tecnico.socialsoftware.tutor.dashboard.services.FailedAnswerService;
import pt.ulisboa.tecnico.socialsoftware.tutor.dashboard.dto.FailedAnswerDto;

import java.util.List;

@RestController
public class FailedAnswerController {
    private static final Logger logger = LoggerFactory.getLogger(FailedAnswerController.class);

    @Autowired
    private FailedAnswerService failedAnswerService;

    FailedAnswerController(FailedAnswerService failedAnswerService) {
        this.failedAnswerService = failedAnswerService;
    }

    @PutMapping("/students/dashboards/{dashboardId}/failedanswers")
    @PreAuthorize("hasRole('ROLE_STUDENT') and hasPermission(#dashboardId, 'DASHBOARD.ACCESS')")
    public List<FailedAnswerDto> updateFailedAnswers(@PathVariable int dashboardId) {
        return this.failedAnswerService.updateFailedAnswers(dashboardId);
    }

    @DeleteMapping("/students/failedanswers/{failedAnswerId}")
    @PreAuthorize("hasRole('ROLE_STUDENT') and hasPermission(#failedAnswerId, 'FAILEDANSWER.ACCESS')")
    public void removeFailedAnswer(@PathVariable int failedAnswerId) {
        this.failedAnswerService.removeFailedAnswer(failedAnswerId);
    }
}