package co.astrnt.qasdk.type;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.StringDef;

import static co.astrnt.qasdk.type.InterviewType.CLOSE_INTERVIEW;
import static co.astrnt.qasdk.type.InterviewType.CLOSE_SECTION;
import static co.astrnt.qasdk.type.InterviewType.CLOSE_TEST;

@StringDef({CLOSE_INTERVIEW, CLOSE_TEST, CLOSE_SECTION})
@Retention(RetentionPolicy.SOURCE)
public @interface InterviewTypes {
}