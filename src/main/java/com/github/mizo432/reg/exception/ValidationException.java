package com.github.mizo432.reg.exception;

/**
 * ビジネスルール違反や入力検証エラーを表す例外。
 * 呼び出し側で適切なエラーハンドリングができるように、
 * 技術的エラーとビジネスルール違反を区別するためのカスタム例外。
 */
public class ValidationException extends IllegalArgumentException {

    private final String field;

    public ValidationException(String message) {
        super(message);
        this.field = null;
    }

    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
