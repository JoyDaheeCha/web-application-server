package controller;

import http.HttpMethod;
import http.HttpRequest;
import http.HttpResponse;

/**
 * 동일 URL라도 요청 메소드(do/post)가 다르다면 다른 처리 가능.
 */
public abstract class AbstractController implements Controller {
    @Override
    public void service(HttpRequest request, HttpResponse response) {
        HttpMethod method = request.getMethod();

        if (method.isPost()) {
            doPost(request, response);
        } else {
            doGet(request, response);
        }
    }

    protected void doPost(HttpRequest request, HttpResponse response) {
    }

    protected void doGet(HttpRequest request, HttpResponse response) {
    }
}
