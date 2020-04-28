package co.astrnt.qasdk.dao;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by deni rohimat on 06/04/18.
 */
public class InterviewApiDao extends RealmObject {

    @PrimaryKey
    private long invite_id;
    private String type;
    private String sub_type;
    private int is_allowed_preview;
    private String first_time;
    private int duration_left;
    private int total_video_question;
    private JobApiDao job;
    private CompanyApiDao company;
    private CandidateApiDao candidate;
    private CustomFieldResultApiDao custom_fields;
    private RealmList<SectionApiDao> sections;
    private RealmList<QuestionApiDao> questions;
    private String lang;
    private boolean finished;
    private int self_pace;
    //this temporary for save code
    private String temp_code;
    private String token;
    private String interviewCode;

    public long getInvite_id() {
        return invite_id;
    }

    public void setInvite_id(long invite_id) {
        this.invite_id = invite_id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSub_type() {
        return sub_type;
    }

    public void setSub_type(String sub_type) {
        this.sub_type = sub_type;
    }

    public int getIs_allowed_preview() {
        return is_allowed_preview;
    }

    public void setIs_allowed_preview(int is_allowed_preview) {
        this.is_allowed_preview = is_allowed_preview;
    }

    public boolean isFirst_time() {
        return first_time == null || first_time.equals("yes");
    }

    public void setFirst_time(String first_time) {
        this.first_time = first_time;
    }

    public int getDuration_left() {
        return duration_left;
    }

    public void setDuration_left(int duration_left) {
        this.duration_left = duration_left;
    }

    public JobApiDao getJob() {
        return job;
    }

    public void setJob(JobApiDao job) {
        this.job = job;
    }

    public CompanyApiDao getCompany() {
        return company;
    }

    public void setCompany(CompanyApiDao company) {
        this.company = company;
    }

    public CandidateApiDao getCandidate() {
        return candidate;
    }

    public void setCandidate(CandidateApiDao candidate) {
        this.candidate = candidate;
    }

    public CustomFieldResultApiDao getCustom_fields() {
        return custom_fields;
    }

    public void setCustom_fields(CustomFieldResultApiDao custom_fields) {
        this.custom_fields = custom_fields;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public RealmList<SectionApiDao> getSections() {
        return sections;
    }

    public void setSections(RealmList<SectionApiDao> sections) {
        this.sections = sections;
    }

    public RealmList<QuestionApiDao> getQuestions() {
        return questions;
    }

    public void setQuestions(RealmList<QuestionApiDao> questions) {
        this.questions = questions;
    }

    public int getTotalQuestion() {
        return getQuestions().size();
    }

    public int getTotalUpload() {
        return getTotalQuestion() * 5;
    }

    // in milis
    public int getEstimatedTime() {
        int totalTime = 0;

        for (QuestionApiDao item : getQuestions()) {
            totalTime += item.getMaxTime() + 30;
        }
        return totalTime;
    }

    public String getTemp_code() {
        return temp_code;
    }

    public void setTemp_code(String temp_code) {
        this.temp_code = temp_code;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getInterviewCode() {
        return interviewCode;
    }

    public void setInterviewCode(String interviewCode) {
        this.interviewCode = interviewCode;
    }

    public int getTotalVideoQuestion() {
        return total_video_question;
    }

    public void setTotalVideoQuestion(int total_video_question) {
        this.total_video_question = total_video_question;
    }

    public boolean isSelfPace() {
        return self_pace == 1;
    }

    public void setSelfPace(int selfPace) {
        this.self_pace = selfPace;
    }
}
