package co.astrnt.qasdk.type;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static co.astrnt.qasdk.type.UploadStatusType.COMPRESSED;
import static co.astrnt.qasdk.type.UploadStatusType.NOT_ANSWER;
import static co.astrnt.qasdk.type.UploadStatusType.PENDING;
import static co.astrnt.qasdk.type.UploadStatusType.UPLOADED;
import static co.astrnt.qasdk.type.UploadStatusType.UPLOADING;

@StringDef({PENDING, NOT_ANSWER, UPLOADING, UPLOADED, COMPRESSED})
@Retention(RetentionPolicy.SOURCE)
public @interface UploadStatusState {
}