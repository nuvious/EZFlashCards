package com.dcheeseman.spreadsheetflashcards;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * Created by DavidR on 2016-01-17.
 */
public class QAInfo {
    public String question, answer;

    public Bitmap getQuestionPicture() {
        return questionPicture;
    }

    public Bitmap getAnswerPicture() {
        return answerPicture;
    }

    public Bitmap questionPicture, answerPicture;

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public QAInfo(String question, String answer, byte[] qpicdata, byte[] apicdata) {
        if (qpicdata != null) {
            questionPicture = BitmapFactory.decodeByteArray(qpicdata, 0, qpicdata.length);
        } else {
           questionPicture = null;
        }
        if (apicdata != null) {
            answerPicture = BitmapFactory.decodeByteArray(apicdata, 0, apicdata.length);
        } else {
            answerPicture = null;
        }
        this.answer = answer;
        this.question = question;
    }
}
