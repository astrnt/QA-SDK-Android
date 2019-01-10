package co.astrnt.qasdk;

import android.content.Context;
import android.content.res.Resources;
import android.os.Environment;
import android.os.StatFs;

import com.orhanobut.hawk.Hawk;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import co.astrnt.qasdk.core.AstronautApi;
import co.astrnt.qasdk.dao.InformationApiDao;
import co.astrnt.qasdk.dao.InterviewApiDao;
import co.astrnt.qasdk.dao.InterviewResultApiDao;
import co.astrnt.qasdk.dao.MultipleAnswerApiDao;
import co.astrnt.qasdk.dao.PrevQuestionStateApiDao;
import co.astrnt.qasdk.dao.QuestionApiDao;
import co.astrnt.qasdk.dao.QuestionInfoApiDao;
import co.astrnt.qasdk.dao.QuestionInfoMcqApiDao;
import co.astrnt.qasdk.dao.SectionApiDao;
import co.astrnt.qasdk.type.InterviewType;
import co.astrnt.qasdk.type.SectionType;
import co.astrnt.qasdk.type.UploadStatusState;
import co.astrnt.qasdk.type.UploadStatusType;
import co.astrnt.qasdk.utils.QuestionInfo;
import co.astrnt.qasdk.utils.SectionInfo;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmList;
import timber.log.Timber;

public class AstrntSDK {

    private static AstronautApi mAstronautApi;
    private static String mApiUrl;
    private static String mAwsBucket;
    private static boolean isPractice = false;
    private Realm realm;
    private boolean isDebuggable;

    public AstrntSDK(Context context, String apiUrl, String awsBucket, boolean debug, String appId) {
        mApiUrl = apiUrl;
        mAwsBucket = awsBucket;
        isDebuggable = debug;

        if (debug) {
            Timber.plant(new Timber.DebugTree());
        }
        Realm.init(context);

        realm = Realm.getInstance(getRealmConfig());
    }

    public AstrntSDK() {
        this.realm = Realm.getInstance(getRealmConfig());
    }

    private static RealmConfiguration getRealmConfig() {
        return new RealmConfiguration.Builder()
                .name("astrntdb")
                .schemaVersion(BuildConfig.VERSION_CODE)
                .deleteRealmIfMigrationNeeded()
                .build();
    }

    private static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    private static int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }

    public Realm getRealm() {
        return realm;
    }

    public String getApiUrl() {
        return mApiUrl;
    }

    public String getAwsBucket() {
        return mAwsBucket;
    }

    public void saveInterviewResult(InterviewResultApiDao resultApiDao, InterviewApiDao interviewApiDao, boolean isContinue) {
        if (!realm.isInTransaction()) {
            realm.beginTransaction();
            if (resultApiDao.getInformation() != null) {
                realm.copyToRealmOrUpdate(resultApiDao.getInformation());
            }
            if (resultApiDao.getInvitation_video() != null) {
                realm.copyToRealmOrUpdate(resultApiDao.getInvitation_video());
            }
            realm.commitTransaction();
            if (interviewApiDao != null) {
                saveInterview(interviewApiDao, resultApiDao.getToken(), resultApiDao.getInterview_code());
                updateSectionOrQuestionInfo(interviewApiDao);
            } else {

                saveInterview(resultApiDao.getInterview(), resultApiDao.getToken(), resultApiDao.getInterview_code());
                updateSectionOrQuestionInfo(resultApiDao.getInterview());
            }
            InterviewApiDao currentInterview = getCurrentInterview();
            if (resultApiDao.getInformation() != null && currentInterview != null && isContinue) {
                updateInterview(currentInterview, resultApiDao.getInformation());
            }
        }

    }

    public InterviewApiDao updateQuestionData(InterviewApiDao currentInterview, InterviewApiDao newInterview) {

        if (!realm.isInTransaction()) {
            realm.beginTransaction();
            if (isSectionInterview()) {

                RealmList<SectionApiDao> sectionList = new RealmList<>();
                for (SectionApiDao newSection : newInterview.getSections()) {

                    for (SectionApiDao section : currentInterview.getSections()) {

                        if (newSection.getId() == section.getId()) {
                            RealmList<QuestionApiDao> questionList = new RealmList<>();

                            for (QuestionApiDao newQuestion : newSection.getSectionQuestions()) {
                                for (QuestionApiDao question : section.getSectionQuestions()) {
                                    if (newQuestion.getId() == question.getId()) {
                                        if (newSection.getType().equals(SectionType.INTERVIEW)) {
                                            newQuestion.setUploadStatus(question.getUploadStatus());
                                            newQuestion.setVideoPath(question.getVideoPath());
                                            newQuestion.setUploadProgress(question.getUploadProgress());
                                        } else {
                                            newQuestion.setSelectedAnswer(question.getSelectedAnswer());
                                            newQuestion.setAnswered(question.isAnswered());
                                        }
                                    }
                                }
                                questionList.add(newQuestion);
                            }
                            newSection.setSectionQuestions(questionList);
                        }
                    }
                    sectionList.add(newSection);
                }
                if (!sectionList.isEmpty()) {
                    newInterview.setSections(sectionList);
                }
            } else {
                RealmList<QuestionApiDao> questionList = new RealmList<>();
                for (QuestionApiDao newQuestion : newInterview.getQuestions()) {
                    for (QuestionApiDao question : currentInterview.getQuestions()) {
                        if (newQuestion.getId() == question.getId()) {
                            if (newInterview.getType().equals(InterviewType.CLOSE_INTERVIEW)) {
                                newQuestion.setUploadStatus(question.getUploadStatus());
                                newQuestion.setVideoPath(question.getVideoPath());
                                newQuestion.setUploadProgress(question.getUploadProgress());
                            } else {
                                newQuestion.setSelectedAnswer(question.getSelectedAnswer());
                                newQuestion.setAnswered(question.isAnswered());
                            }
                        }
                    }
                    questionList.add(newQuestion);
                }
                newInterview.setQuestions(questionList);
            }

            realm.copyToRealmOrUpdate(newInterview);
            realm.commitTransaction();
        }
        return newInterview;
    }

    private void updateSectionOrQuestionInfo(InterviewApiDao interviewApiDao) {

        if (isSectionInterview()) {
            saveSectionInfo();
            saveQuestionInfo();
        }

        if (interviewApiDao.getQuestions() != null && !interviewApiDao.getQuestions().isEmpty()) {
            saveQuestionInfo();
        }
    }

    public void saveInterview(InterviewApiDao interview, String token, String interviewCode) {
        if (!realm.isInTransaction()) {
            realm.beginTransaction();
            interview.setToken(token);
            interview.setInterviewCode(interviewCode);
            realm.copyToRealmOrUpdate(interview);
            realm.commitTransaction();
        }
    }

    private void updateInterview(InterviewApiDao interview, InformationApiDao informationApiDao) {
        if (isSectionInterview()) {
            updateSectionInfo(informationApiDao.getSectionIndex());

            if (informationApiDao.getQuestionsInfo() != null && !informationApiDao.getQuestionsInfo().isEmpty()) {
                for (QuestionInfoApiDao questionInfoApiDao : informationApiDao.getQuestionsInfo()) {
                    updateQuestionInfo(questionInfoApiDao.getInterviewIndex(), questionInfoApiDao.getInterviewAttempt());
                }
            }
        } else {
            updateQuestionInfo(informationApiDao.getInterviewIndex(), informationApiDao.getInterviewAttempt());
        }

        if (!realm.isInTransaction()) {
            realm.beginTransaction();

            if (isSectionInterview()) {

                RealmList<SectionApiDao> sectionList = new RealmList<>();

                for (int i = 0; i < interview.getSections().size(); i++) {
                    SectionApiDao section = interview.getSections().get(i);
                    RealmList<QuestionApiDao> questionApiDaos = new RealmList<>();

                    if (section != null) {
                        if (i == informationApiDao.getSectionIndex() && !informationApiDao.getSectionInfo().equals("start")) {

                            section.setPrepTimeLeft(informationApiDao.getPreparationTime());
                            section.setPreparationTime(informationApiDao.getPreparationTime());
                            section.setTimeLeft(informationApiDao.getSectionDurationLeft());
                            section.setDuration(informationApiDao.getSectionDurationLeft());
                            section.setOnGoing(informationApiDao.isOnGoing());
                        }
                        if (section.isOnGoing()) {

                            if (section.getType().equals(InterviewType.INTERVIEW)) {

                                for (QuestionApiDao question : section.getSectionQuestions()) {

                                    if (informationApiDao.getQuestionsInfo() != null && !informationApiDao.getQuestionsInfo().isEmpty()) {
                                        for (QuestionInfoApiDao questionInfoApiDao : informationApiDao.getQuestionsInfo()) {

                                            if (questionInfoApiDao.getPrevQuestStates() != null) {
                                                for (PrevQuestionStateApiDao questionState : questionInfoApiDao.getPrevQuestStates()) {

                                                    if (question.getId() == questionState.getQuestionId()) {
                                                        if (questionState.isAnswered()) {
                                                            question.setAnswered(true);
                                                        } else {
                                                            question.setAnswered(false);
                                                        }
                                                        question.setTimeLeft(questionState.getDurationLeft());
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    questionApiDaos.add(question);
                                }
                            } else {
                                for (QuestionApiDao question : section.getSectionQuestions()) {

                                    if (informationApiDao.getQuestionsMcqInfo() != null && !informationApiDao.getQuestionsMcqInfo().isEmpty()) {
                                        for (QuestionInfoMcqApiDao questionInfoMcqApiDao : informationApiDao.getQuestionsMcqInfo()) {

                                            if (question.getId() == questionInfoMcqApiDao.getId()) {
                                                for (Integer answerId : questionInfoMcqApiDao.getAnswer_ids()) {
                                                    question = addSelectedAnswer(question, answerId);
                                                }
                                            }
                                        }
                                    }
                                    questionApiDaos.add(question);
                                }
                            }
                        }

                        if (!questionApiDaos.isEmpty()) {
                            section.setSectionQuestions(questionApiDaos);
                        }
                        sectionList.add(section);
                    }
                }

                if (!sectionList.isEmpty()) {
                    interview.setSections(sectionList);
                }
            } else {

                if (interview.getQuestions() != null && informationApiDao.getPrevQuestStates() != null) {
                    RealmList<QuestionApiDao> questions = interview.getQuestions();
                    for (PrevQuestionStateApiDao questionState : informationApiDao.getPrevQuestStates()) {
                        for (QuestionApiDao question : questions) {
                            if (question.getId() == questionState.getQuestionId()) {
                                if (questionState.isAnswered()) {
                                    question.setAnswered(true);
                                } else {
                                    question.setAnswered(false);
                                }
                                question.setTimeLeft(questionState.getDurationLeft());
                            }
                        }
                    }

                    interview.setQuestions(questions);
                }
            }

            interview.setFinished(informationApiDao.isFinished());

            realm.copyToRealmOrUpdate(interview);
            realm.commitTransaction();

            updateSectionOrQuestionInfo(interview);
        }
    }

    public void updateSectionTimeLeft(SectionApiDao currentSection, int timeLeft) {
        currentSection = getSectionById(currentSection.getId());
        if (currentSection != null) {
            if (!realm.isInTransaction()) {
                realm.beginTransaction();
                currentSection.setDuration(timeLeft);
                realm.copyToRealmOrUpdate(currentSection);
                realm.commitTransaction();
            }
        }
    }

    public void updateSectionPrepTimeLeft(SectionApiDao currentSection, int timeLeft) {
        currentSection = getSectionById(currentSection.getId());
        if (currentSection != null) {
            if (!realm.isInTransaction()) {
                realm.beginTransaction();
                currentSection.setPreparationTime(timeLeft);
                realm.copyToRealmOrUpdate(currentSection);
                realm.commitTransaction();
            }
        }
    }

    public void updateQuestionTimeLeft(QuestionApiDao currentQuestion, int timeLeft) {
        currentQuestion = getQuestionById(currentQuestion.getId());
        if (currentQuestion != null) {
            if (!realm.isInTransaction()) {
                realm.beginTransaction();
                currentQuestion.setTimeLeft(timeLeft);
                realm.copyToRealmOrUpdate(currentQuestion);
                realm.commitTransaction();
            }
        }
    }

    public void updateInterviewTimeLeft(int timeLeft) {
        InterviewApiDao interviewApiDao = getCurrentInterview();
        if (interviewApiDao != null) {
            if (!realm.isInTransaction()) {
                realm.beginTransaction();
                interviewApiDao.setDuration_left(timeLeft);
                realm.copyToRealmOrUpdate(interviewApiDao);
                realm.commitTransaction();
            }
        }
    }

    private void saveSectionInfo() {
        SectionInfo sectionInfo = new SectionInfo(getSectionIndex());
        if (!realm.isInTransaction()) {
            realm.beginTransaction();
            realm.copyToRealmOrUpdate(sectionInfo);
            realm.commitTransaction();
        }
    }

    private void updateSectionInfo(int sectionIndex) {
        SectionInfo questionInfo = new SectionInfo(sectionIndex);
        if (!realm.isInTransaction()) {
            realm.beginTransaction();
            realm.copyToRealmOrUpdate(questionInfo);
            realm.commitTransaction();
        }
    }

    private SectionInfo getSectionInfo() {
        return realm.where(SectionInfo.class).findFirst();
    }

    private void saveQuestionInfo() {
        QuestionInfo questionInfo = new QuestionInfo(getQuestionIndex(), getQuestionAttempt(), false);
        if (!realm.isInTransaction()) {
            realm.beginTransaction();
            realm.copyToRealmOrUpdate(questionInfo);
            realm.commitTransaction();
        }
    }

    private void updateQuestionInfo(int questionIndex, int questionAttempt) {
        if (!realm.isInTransaction()) {
            realm.beginTransaction();
            QuestionApiDao currentQuestion = getQuestionByIndex(questionIndex);
            questionAttempt = currentQuestion.getTakesCount() - questionAttempt;
            QuestionInfo questionInfo = new QuestionInfo(questionIndex, questionAttempt, false);
            realm.copyToRealmOrUpdate(questionInfo);
            realm.commitTransaction();
        }
    }

    private QuestionInfo getQuestionInfo() {
        return realm.where(QuestionInfo.class).equalTo("isPractice", isPractice()).findFirst();
    }

    private InformationApiDao getInformation() {
        return realm.where(InformationApiDao.class).findFirst();
    }

    public boolean isResume() {
        InformationApiDao informationApiDao = getInformation();
        if (isSectionInterview()) {
            return getCurrentSection().isOnGoing() || informationApiDao.getSectionDurationLeft() > 0;
        } else {
            return informationApiDao.getPrevQuestStates() != null && !informationApiDao.getPrevQuestStates().isEmpty();
        }
    }

    public int getQuestionIndex() {
        if (isPractice()) {
            return 0;
        }
        QuestionInfo questionInfo = getQuestionInfo();
        if (questionInfo != null) {
            return questionInfo.getIndex();
        } else {
            InformationApiDao information = getInformation();
            if (information == null) {
                return 0;
            } else {
                InterviewApiDao interviewApiDao = getCurrentInterview();
                if (isSectionInterview()) {
                    int questionIndex = 0;

                    SectionApiDao section = getCurrentSection();
                    if (section.getType().equals(InterviewType.INTERVIEW)) {

                        for (QuestionInfoApiDao questionInfoApiDao : information.getQuestionsInfo()) {
                            if (questionInfoApiDao != null) {
                                questionIndex = questionInfoApiDao.getInterviewIndex();
                                updateQuestionInfo(questionIndex, questionInfoApiDao.getInterviewAttempt());

                                for (int i = 0; i < questionInfoApiDao.getPrevQuestStates().size(); i++) {
                                    PrevQuestionStateApiDao prevQuestionState = questionInfoApiDao.getPrevQuestStates().get(i);
                                    assert prevQuestionState != null;
                                    if (prevQuestionState.getDurationLeft() > 0) {
                                        updateQuestion(interviewApiDao, prevQuestionState);
                                        return i;
                                    }
                                }
                            }
                        }
                    } else {
                        RealmList<QuestionInfoMcqApiDao> questionsMcqInfo = information.getQuestionsMcqInfo();
                        for (int i = 0; i < questionsMcqInfo.size(); i++) {
                            QuestionInfoMcqApiDao questionInfoMcqApiDao = questionsMcqInfo.get(i);
                            if (questionInfoMcqApiDao != null) {
                                if (questionInfoMcqApiDao.getAnswer_ids().isEmpty()) {
                                    return i;
                                }
                            }
                        }
                    }

                    return questionIndex;
                } else {
                    if (interviewApiDao.getType().equals(InterviewType.OPEN_TEST) ||
                            interviewApiDao.getType().equals(InterviewType.CLOSE_TEST)) {

                        for (int i = 0; i < information.getPrevQuestStates().size(); i++) {
                            PrevQuestionStateApiDao prevQuestionState = information.getPrevQuestStates().get(i);
                            assert prevQuestionState != null;
                            if (prevQuestionState.getDurationLeft() > 0) {
                                updateQuestion(interviewApiDao, prevQuestionState);
                                return i;
                            }
                        }

                        return information.getInterviewIndex();
                    } else {
                        return information.getInterviewIndex();
                    }
                }
            }
        }
    }

    public int getSectionIndex() {
        if (isPractice()) {
            return 0;
        }
        SectionInfo sectionInfo = getSectionInfo();
        if (sectionInfo != null) {
            return sectionInfo.getIndex();
        } else {
            InformationApiDao information = getInformation();
            if (information == null) {
                return 0;
            } else {
                return information.getSectionIndex();
            }
        }
    }

    private void updateQuestion(InterviewApiDao interview, PrevQuestionStateApiDao questionState) {
        for (QuestionApiDao question : interview.getQuestions()) {
            if (question.getId() == questionState.getQuestionId()) {
                if (!realm.isInTransaction()) {
                    realm.beginTransaction();
                    if (questionState.isAnswered()) {
                        question.setAnswered(true);
                    } else {
                        question.setAnswered(false);
                    }
                    question.setTimeLeft(questionState.getDurationLeft());

                    realm.copyToRealmOrUpdate(question);
                    realm.commitTransaction();
                }
            }
        }
    }

    public int getQuestionAttempt() {
        QuestionInfo questionInfo = getQuestionInfo();
        if (questionInfo != null) {
            return questionInfo.getAttempt();
        } else {
            InformationApiDao information = getInformation();
            if (information == null) {
                return 1;
            } else {

                if (isSectionInterview()) {
                    SectionApiDao currentSection = getCurrentSection();
                    if (currentSection != null) {
                        if (currentSection.getSectionQuestions() != null) {
                            assert currentSection.getSectionQuestions().first() != null;
                            return currentSection.getSectionQuestions().first().getTakesCount();
                        }
                        return 1;
                    } else {
                        return 1;
                    }

                } else {
                    QuestionApiDao currentQuestion = getCurrentQuestion();
                    if (currentQuestion != null) {
                        return currentQuestion.getTakesCount() - information.getInterviewAttempt();
                    } else {
                        return 1;
                    }
                }
            }
        }
    }

    public List<QuestionApiDao> getAllVideoQuestion() {
        InterviewApiDao interviewApiDao = getCurrentInterview();
        if (interviewApiDao != null) {
            if (isSectionInterview()) {
                List<QuestionApiDao> pendingUpload = new ArrayList<>();

                for (SectionApiDao section : interviewApiDao.getSections()) {
                    if (section.getType().equals(SectionType.INTERVIEW)) {
                        pendingUpload.addAll(section.getSectionQuestions());
                    }
                }

                return pendingUpload;
            } else {

                List<QuestionApiDao> pendingUpload = new ArrayList<>();

                if (interviewApiDao.getType().equals(InterviewType.CLOSE_INTERVIEW)) {
                    pendingUpload.addAll(interviewApiDao.getQuestions());
                }

                return pendingUpload;
            }
        } else {
            return null;
        }
    }

    public QuestionApiDao searchQuestionById(long id) {
        return realm.where(QuestionApiDao.class).equalTo("id", id).findFirst();
    }

    public InterviewApiDao getCurrentInterview() {
        InterviewApiDao currentInterview = realm.where(InterviewApiDao.class).findFirst();
        if (currentInterview != null) {
            return currentInterview;
        } else {
            return null;
        }
    }

    public int getTotalQuestion() {
        if (isPractice()) {
            return 1;
        }
        InterviewApiDao interviewApiDao = getCurrentInterview();
        if (interviewApiDao != null) {
            if (isSectionInterview()) {
                SectionApiDao currentSection = getCurrentSection();
                return currentSection.getTotalQuestion();
            } else {
                return interviewApiDao.getTotalQuestion();
            }
        } else {
            return 0;
        }
    }

    public List<QuestionApiDao> getPending(@UploadStatusState String uploadStatusType) {
        InterviewApiDao interviewApiDao = getCurrentInterview();
        if (interviewApiDao != null) {
            if (isSectionInterview()) {
                List<QuestionApiDao> pendingUpload = new ArrayList<>();

                for (SectionApiDao section : interviewApiDao.getSections()) {
                    if (section.getType().equals(SectionType.INTERVIEW)) {
                        for (QuestionApiDao item : section.getSectionQuestions()) {
                            if (item.getUploadStatus().equals(uploadStatusType)) {
                                pendingUpload.add(item);
                            }
                        }
                    }
                }

                return pendingUpload;
            } else {

                List<QuestionApiDao> pendingUpload = new ArrayList<>();

                if (interviewApiDao.getType().equals(InterviewType.CLOSE_INTERVIEW)) {
                    for (QuestionApiDao item : interviewApiDao.getQuestions()) {
                        if (item.getUploadStatus().equals(uploadStatusType)) {
                            pendingUpload.add(item);
                        }
                    }
                }

                return pendingUpload;
            }
        } else {
            return null;
        }
    }

    public int getTotalSection() {
        if (isPractice()) {
            return 1;
        }
        InterviewApiDao interviewApiDao = getCurrentInterview();
        if (interviewApiDao != null) {
            return interviewApiDao.getSections().size();
        } else {
            return 0;
        }
    }

    public boolean isSectionHasVideo() {
        for (SectionApiDao item : getCurrentInterview().getSections()) {
            if (item.getType().equals(InterviewType.INTERVIEW)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAllUploaded() {
        InterviewApiDao interviewApiDao = getCurrentInterview();
        if (interviewApiDao == null) {
            return true;
        } else {
            if (isSectionInterview()) {
                if (interviewApiDao.getSections().isEmpty()) {
                    return true;
                } else {
                    return getQuestionIndex() >= getCurrentSection().getTotalQuestion() && getQuestionAttempt() == 0;
                }
            } else {
                if (interviewApiDao.getQuestions().isEmpty()) {
                    return true;
                } else {
                    int totalQuestion = getTotalQuestion();
                    return getQuestionIndex() >= totalQuestion && getQuestionAttempt() == 0;
                }
            }
        }
    }

    public boolean isFinished() {
        InterviewApiDao interviewApiDao = getCurrentInterview();
        InformationApiDao informationApiDao = getInformation();
        if (interviewApiDao == null || informationApiDao == null) {
            return true;
        } else {
            return (interviewApiDao.isFinished() || informationApiDao.isFinished()) && isFinishInterview();
        }
    }

    public boolean isCanContinue() {
        if (isSectionInterview()) {
            return isNotLastSection() && isNotLastQuestion();
        } else {
            return isNotLastQuestion();
        }
    }

    private QuestionApiDao getPracticeQuestion() {
        QuestionApiDao questionApiDao = new QuestionApiDao();
        questionApiDao.setTakesCount(3);
        questionApiDao.setMaxTime(45);
        questionApiDao.setTitle("What are your proudest achievements, and why?");
        return questionApiDao;
    }

    public SectionApiDao getCurrentSection() {
        InterviewApiDao interviewApiDao = getCurrentInterview();
        assert interviewApiDao != null;
        int sectionIndex = getSectionIndex();
        if (sectionIndex < interviewApiDao.getSections().size()) {
            return interviewApiDao.getSections().get(sectionIndex);
        } else {
            return interviewApiDao.getSections().last();
        }
    }

    public SectionApiDao getSectionByIndex(int sectionIndex) {
        InterviewApiDao interviewApiDao = getCurrentInterview();
        assert interviewApiDao != null;
        if (sectionIndex < interviewApiDao.getSections().size()) {
            return interviewApiDao.getSections().get(sectionIndex);
        } else {
            return interviewApiDao.getSections().last();
        }
    }

    public SectionApiDao getSectionById(long sectionId) {
        InterviewApiDao interviewApiDao = getCurrentInterview();
        assert interviewApiDao != null;
        for (SectionApiDao item : interviewApiDao.getSections()) {
            if (item.getId() == sectionId) {
                return item;
            }
        }
        return null;
    }

    private SectionApiDao getNextSection() {
        InterviewApiDao interviewApiDao = getCurrentInterview();
        assert interviewApiDao != null;
        int sectionIndex = getSectionIndex();
        if (sectionIndex < interviewApiDao.getSections().size()) {
            return interviewApiDao.getSections().get(sectionIndex);
        } else {
            return interviewApiDao.getSections().last();
        }
    }

    public QuestionApiDao getCurrentQuestion() {
        if (isPractice()) {
            return getPracticeQuestion();
        }
        InterviewApiDao interviewApiDao = getCurrentInterview();
        assert interviewApiDao != null;
        int questionIndex = getQuestionIndex();
        if (isSectionInterview()) {
            SectionApiDao currentSection = getCurrentSection();
            if (questionIndex < currentSection.getSectionQuestions().size()) {
                return currentSection.getSectionQuestions().get(questionIndex);
            } else {
                return currentSection.getSectionQuestions().last();
            }
        } else {
            if (questionIndex < interviewApiDao.getQuestions().size()) {
                return interviewApiDao.getQuestions().get(questionIndex);
            } else {
                return interviewApiDao.getQuestions().last();
            }
        }
    }

    private QuestionApiDao getQuestionByIndex(int questionIndex) {
        InterviewApiDao interviewApiDao = getCurrentInterview();
        assert interviewApiDao != null;
        if (isSectionInterview()) {
            SectionApiDao currentSection = getCurrentSection();
            if (questionIndex < currentSection.getSectionQuestions().size()) {
                return currentSection.getSectionQuestions().get(questionIndex);
            } else {
                return currentSection.getSectionQuestions().last();
            }
        } else {
            if (questionIndex < interviewApiDao.getQuestions().size()) {
                return interviewApiDao.getQuestions().get(questionIndex);
            } else {
                return interviewApiDao.getQuestions().last();
            }
        }
    }

    public QuestionApiDao getQuestionById(long questionId) {
        InterviewApiDao interviewApiDao = getCurrentInterview();
        assert interviewApiDao != null;
        if (isSectionInterview()) {

            for (SectionApiDao section : interviewApiDao.getSections()) {
                for (QuestionApiDao item : section.getSectionQuestions()) {
                    if (item.getId() == questionId) {
                        return item;
                    }
                }
            }
        } else {
            for (QuestionApiDao question : interviewApiDao.getQuestions()) {
                if (question.getId() == questionId) {
                    return question;
                }
            }
        }
        return null;
    }

    private QuestionApiDao getNextQuestion() {
        InterviewApiDao interviewApiDao = getCurrentInterview();
        assert interviewApiDao != null;
        int questionIndex = getQuestionIndex();
        if (isSectionInterview()) {
            SectionApiDao currentSection = getCurrentSection();
            if (questionIndex < currentSection.getSectionQuestions().size()) {
                return currentSection.getSectionQuestions().get(questionIndex);
            } else {
                return currentSection.getSectionQuestions().last();
            }
        } else {
            if (questionIndex < interviewApiDao.getQuestions().size()) {
                return interviewApiDao.getQuestions().get(questionIndex);
            } else {
                return interviewApiDao.getQuestions().last();
            }
        }
    }

    public void increaseQuestionIndex() {
        if (isPractice()) {
            return;
        }
        if (!realm.isInTransaction()) {
            realm.beginTransaction();

            QuestionInfo questionInfo = getQuestionInfo();

            QuestionApiDao nextQuestion = getNextQuestion();
            if (nextQuestion != null) {
                questionInfo.increaseIndex();
                questionInfo.setAttempt(nextQuestion.getTakesCount());
            } else {
                questionInfo.resetAttempt();
            }

            realm.copyToRealmOrUpdate(questionInfo);
            realm.commitTransaction();
        }
    }

    public void decreaseQuestionIndex() {
        if (isPractice()) {
            return;
        }
        if (!realm.isInTransaction()) {
            realm.beginTransaction();

            QuestionInfo questionInfo = getQuestionInfo();
            questionInfo.decreaseIndex();

            realm.copyToRealmOrUpdate(questionInfo);
            realm.commitTransaction();
        }
    }

    public void increaseSectionIndex() {
        if (isPractice()) {
            return;
        }
        if (!realm.isInTransaction()) {
            realm.beginTransaction();

            SectionInfo sectionInfo = getSectionInfo();
            sectionInfo.increaseIndex();

            realm.copyToRealmOrUpdate(sectionInfo);
            realm.commitTransaction();
        }

        SectionApiDao nextSection = getNextSection();
        if (nextSection != null) {
            if (nextSection.getSectionQuestions() != null) {
                assert nextSection.getSectionQuestions().first() != null;
                updateQuestionInfo(0, 0);
            } else {
                updateQuestionInfo(0, 0);
            }
        } else {
            updateQuestionInfo(0, 0);
        }
    }

    public void decreaseQuestionAttempt() {

        QuestionInfo questionInfo = getQuestionInfo();
        if (questionInfo == null) {
            updateQuestionInfo(0, 0);
            questionInfo = getQuestionInfo();
        }

        if (!realm.isInTransaction() && questionInfo != null) {
            realm.beginTransaction();

            questionInfo.decreaseAttempt();
            int attempt = questionInfo.getAttempt();

            if (attempt <= 0) {
                realm.commitTransaction();
            } else {
                realm.copyToRealmOrUpdate(questionInfo);
                realm.commitTransaction();
            }
        }
    }

    public boolean isLastAttempt() {
        return getQuestionAttempt() <= 0;
    }

    public boolean isNotLastQuestion() {
        if (isSectionInterview()) {
            SectionApiDao sectionApiDao = getSectionByIndex(getSectionIndex());
            return getQuestionIndex() < sectionApiDao.getTotalQuestion();
        } else {
            return getQuestionIndex() < getTotalQuestion();
        }
    }

    public boolean isNotLastSection() {
        return getSectionIndex() < getTotalSection();
    }

    public void updateCompressing(QuestionApiDao questionApiDao) {

        if (!realm.isInTransaction()) {
            realm.beginTransaction();

            questionApiDao.setUploadStatus(UploadStatusType.COMPRESSING);

            realm.copyToRealmOrUpdate(questionApiDao);
            realm.commitTransaction();
        } else {
            updateCompressing(questionApiDao);
        }
    }

    public void updateVideoPath(QuestionApiDao questionApiDao, String videoPath) {

        if (!realm.isInTransaction()) {
            realm.beginTransaction();

            questionApiDao.setVideoPath(videoPath);
            questionApiDao.setUploadStatus(UploadStatusType.COMPRESSED);

            realm.copyToRealmOrUpdate(questionApiDao);
            realm.commitTransaction();
        } else {
            updateVideoPath(questionApiDao, videoPath);
        }
    }

    public void updateProgress(QuestionApiDao questionApiDao, double progress) {

        if (!realm.isInTransaction()) {
            realm.beginTransaction();

            questionApiDao.setUploadProgress(progress);

            realm.copyToRealmOrUpdate(questionApiDao);
            realm.commitTransaction();

            Timber.d("Video with Question Id %s on progress uploading %s / 100", questionApiDao.getId(), progress);
        } else {
            updateProgress(questionApiDao, progress);
        }
    }

    public void markUploading(QuestionApiDao questionApiDao) {

        if (!realm.isInTransaction()) {
            realm.beginTransaction();

            questionApiDao.setUploadStatus(UploadStatusType.UPLOADING);

            realm.copyToRealmOrUpdate(questionApiDao);
            realm.commitTransaction();

            Timber.d("Video with Question Id %s is now uploading", questionApiDao.getId());
        } else {
            markUploading(questionApiDao);
            Timber.e("Video with Question Id %s is failed to marked uploading", questionApiDao.getId());
        }
    }

    public void markNotAnswer(QuestionApiDao questionApiDao) {

        if (!realm.isInTransaction()) {
            realm.beginTransaction();

            questionApiDao.setUploadStatus(UploadStatusType.NOT_ANSWER);

            realm.copyToRealmOrUpdate(questionApiDao);
            realm.commitTransaction();

            Timber.d("Video with Question Id %s is now uploading", questionApiDao.getId());
        } else {
            markNotAnswer(questionApiDao);
        }
    }

    public void markUploaded(QuestionApiDao questionApiDao) {

        if (!realm.isInTransaction()) {
            realm.beginTransaction();

            questionApiDao.setUploadStatus(UploadStatusType.UPLOADED);

            realm.copyToRealmOrUpdate(questionApiDao);
            realm.commitTransaction();

            Timber.d("Video with Question Id %s has been uploaded", questionApiDao.getId());
        } else {
            markUploaded(questionApiDao);
            Timber.e("Video with Question Id %s is failed to marked uploaded", questionApiDao.getId());
        }
    }

    public void markAsCompressed(QuestionApiDao questionApiDao) {

        if (!realm.isInTransaction()) {
            realm.beginTransaction();

            questionApiDao.setUploadStatus(UploadStatusType.COMPRESSED);

            realm.copyToRealmOrUpdate(questionApiDao);
            realm.commitTransaction();

            Timber.d("Video with Question Id %s mark as pending", questionApiDao.getId());
        } else {
            markAsCompressed(questionApiDao);
        }
    }

    public void markAsPending(QuestionApiDao questionApiDao, String rawFilePath) {

        if (!realm.isInTransaction()) {
            realm.beginTransaction();

            questionApiDao.setUploadStatus(UploadStatusType.PENDING);
            questionApiDao.setVideoPath(rawFilePath);

            realm.copyToRealmOrUpdate(questionApiDao);
            realm.commitTransaction();

            Timber.d("Video with Question Id %s mark as pending", questionApiDao.getId());
        } else {
            markAsPending(questionApiDao, rawFilePath);
        }
    }

    public void clearDb() {
        if (!realm.isInTransaction()) {
            realm.beginTransaction();
            realm.deleteAll();
            realm.commitTransaction();
        } else {
            clearDb();
        }
    }

    public void clearVideoFile(Context context) {
        File filesDir = context.getFilesDir();

        File[] files = filesDir.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.getName().equals("video")) {
                    deleteRecursive(file);
                }
            }
        }
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }

        fileOrDirectory.delete();
    }

    public boolean isPractice() {
        return isPractice;
    }

    public boolean isSectionInterview() {
        InterviewApiDao interviewApiDao = getCurrentInterview();
        return interviewApiDao != null && interviewApiDao.getSections() != null && !interviewApiDao.getSections().isEmpty();
    }

    public void setInterviewFinished() {
        if (!realm.isInTransaction()) {
            realm.beginTransaction();
            InterviewApiDao interviewApiDao = getCurrentInterview();
            interviewApiDao.setFinished(true);
            realm.copyToRealmOrUpdate(interviewApiDao);
            realm.commitTransaction();

            Timber.d("Interview marked as finished in local");
        } else {
            setInterviewFinished();
        }
    }

    public void setPracticeMode() {
        isPractice = true;
        if (!realm.isInTransaction()) {
            realm.beginTransaction();
            QuestionInfo questionInfo = new QuestionInfo(0, 3, isPractice);
            questionInfo.setId(20180427);
            realm.copyToRealmOrUpdate(questionInfo);
            realm.commitTransaction();
        }
    }

    public void finishPracticeMode() {
        isPractice = false;
    }

    public long getAvailableStorage() {
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long bytesAvailable = stat.getBlockSizeLong() * stat.getAvailableBlocksLong();
        long megAvailable = bytesAvailable / (1024 * 1024);
        Timber.d("Available MB : %s", megAvailable);
        return megAvailable;
    }

    public boolean isStorageEnough() {
        return getAvailableStorage() > 300 + (getTotalQuestion() * 30);
    }

    public void addSelectedAnswer(QuestionApiDao questionApiDao, MultipleAnswerApiDao answer) {
        if (!realm.isInTransaction()) {

            RealmList<MultipleAnswerApiDao> selectedAnswer = new RealmList<>();
            realm.beginTransaction();

            RealmList<MultipleAnswerApiDao> multipleAnswer = questionApiDao.getMultiple_answers();
            for (MultipleAnswerApiDao item : multipleAnswer) {
                if (questionApiDao.isMultipleChoice()) {
                    if (item.getId() == answer.getId()) {
                        item.setSelected(!answer.isSelected());
                    }
                } else {
                    if (item.getId() == answer.getId()) {
                        item.setSelected(!answer.isSelected());
                    } else {
                        item.setSelected(false);
                    }
                }
            }

            for (MultipleAnswerApiDao item : multipleAnswer) {
                if (item.isSelected()) {
                    selectedAnswer.add(item);
                }
            }

            questionApiDao.setSelectedAnswer(selectedAnswer);
            questionApiDao.setMultiple_answers(multipleAnswer);
            if (selectedAnswer.isEmpty()) {
                questionApiDao.setAnswered(false);
            } else {
                questionApiDao.setAnswered(true);
            }
            realm.copyToRealmOrUpdate(questionApiDao);
            realm.commitTransaction();
        } else {
            addSelectedAnswer(questionApiDao, answer);
        }
    }

    private QuestionApiDao addSelectedAnswer(QuestionApiDao questionApiDao, int answerId) {

        RealmList<MultipleAnswerApiDao> selectedAnswer = new RealmList<>();

        RealmList<MultipleAnswerApiDao> multipleAnswer = questionApiDao.getMultiple_answers();
        for (MultipleAnswerApiDao item : multipleAnswer) {
            if (questionApiDao.isMultipleChoice()) {
                if (item.getId() == answerId) {
                    item.setSelected(!item.isSelected());
                }
            } else {
                if (item.getId() == answerId) {
                    item.setSelected(!item.isSelected());
                } else {
                    item.setSelected(false);
                }
            }
        }

        for (MultipleAnswerApiDao item : multipleAnswer) {
            if (item.isSelected()) {
                selectedAnswer.add(item);
            }
        }

        questionApiDao.setSelectedAnswer(selectedAnswer);
        questionApiDao.setMultiple_answers(multipleAnswer);
        if (selectedAnswer.isEmpty()) {
            questionApiDao.setAnswered(false);
        } else {
            questionApiDao.setAnswered(true);
        }
        return questionApiDao;
    }

    public void markAnswered(QuestionApiDao questionApiDao) {

        if (!realm.isInTransaction()) {
            realm.beginTransaction();

            questionApiDao.setAnswered(true);

            realm.copyToRealmOrUpdate(questionApiDao);
            realm.commitTransaction();

            Timber.d("Video with Question Id %s has been uploaded", questionApiDao.getId());
        } else {
            markAnswered(questionApiDao);
            Timber.e("Video with Question Id %s is failed to marked uploaded", questionApiDao.getId());
        }
    }

    public AstronautApi getApi() {
        if (mAstronautApi == null) {
            mAstronautApi = new AstronautApi(mApiUrl, isDebuggable);
        }
        return mAstronautApi;
    }

    public boolean isContinueInterview() {
        return Hawk.get("ContinueInterview", false);
    }

    public void setContinueInterview(boolean isContinue) {
        Hawk.put("ContinueInterview", isContinue);
    }

    public boolean isFinishInterview() {
        return Hawk.get("FinishInterview", true);
    }

    public void setFinishInterview(boolean isFinish) {
        Hawk.put("FinishInterview", isFinish);
    }

    public boolean isShowUpload() {
        return Hawk.get("ShowUpload", false);
    }

    public void setShowUpload(boolean showUpload) {
        Hawk.put("ShowUpload", showUpload);
    }

}
