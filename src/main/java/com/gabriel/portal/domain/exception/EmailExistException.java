package com.gabriel.portal.domain.exception;

public class EmailExistException extends Exception{
    public EmailExistException(String message) {
        super(message);
    }
}
