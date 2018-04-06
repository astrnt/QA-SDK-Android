package co.astrnt.qasdk.repository;

import co.astrnt.qasdk.core.AstronautApi;
import co.astrnt.qasdk.dao.InterviewApiDao;
import rx.Observable;

/**
 * Created by deni rohimat on 06/04/18.
 */
public class InterviewRepository extends BaseRepository {
    AstronautApi mAstronautApi;

    public InterviewRepository(AstronautApi astronautApi) {
        mAstronautApi = astronautApi;
    }

    public Observable<InterviewApiDao> enterCode(String interviewCode) {
        return mAstronautApi.getApiService().enterCode(interviewCode);
    }

}
