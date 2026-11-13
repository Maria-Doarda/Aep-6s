package fz.exemple.aep.controllers;

import com.mongodb.MongoSocketException;
import com.mongodb.MongoTimeoutException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class MongoUnavailableHandler {

    @ExceptionHandler({
            DataAccessResourceFailureException.class,
            MongoTimeoutException.class,
            MongoSocketException.class
    })
    public ProblemDetail handleMongoDown(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Banco de dados indisponível no momento. Tente novamente em instantes."
        );
        problem.setTitle("Serviço indisponível");
        return problem;
    }
}
