package co.astrnt.qasdk.repository;

import co.astrnt.qasdk.core.AstronautApi;
import co.astrnt.qasdk.dao.InterviewResultApiDao;
import co.astrnt.qasdk.dao.post.RegisterPost;
import io.reactivex.Observable;

/**
 * Created by deni rohimat on 06/04/18.
 */
public class InterviewRepository extends BaseRepository {
    private final AstronautApi mAstronautApi;

    public InterviewRepository(AstronautApi astronautApi) {
        mAstronautApi = astronautApi;
    }

    public Observable<InterviewResultApiDao> enterCode(String interviewCode, int version) {
        return mAstronautApi.getApiService().enterCode(interviewCode, "android", version);
    }

    public Observable<InterviewResultApiDao> registerUser(RegisterPost param) {
        return mAstronautApi.getApiService().registerUser(param);
    }

}
