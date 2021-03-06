package co.astrnt.qasdk.utils;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class QuestionInfo extends RealmObject {

    @PrimaryKey
    private long id;
    private int index;
    private int attempt;
    private boolean isPractice;

    public QuestionInfo() {
    }

    public QuestionInfo(int index, int attempt, boolean isPractice) {
        this.index = index;
        this.attempt = attempt;
        this.isPractice = isPractice;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getAttempt() {
        return attempt;
    }

    public void setAttempt(int attempt) {
        this.attempt = attempt;
    }

    public boolean isPractice() {
        return isPractice;
    }

    public void setPractice(boolean practice) {
        isPractice = practice;
    }

    public void increaseIndex() {
        this.index++;
    }

    public void decreaseIndex() {
        this.index--;
    }

    public void resetAttempt() {
        this.attempt = 0;
    }

    public void decreaseAttempt() {
        this.attempt--;
    }
}
