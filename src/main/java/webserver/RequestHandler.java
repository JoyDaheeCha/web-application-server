package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Map;

import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(), connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {

            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String line = "";

            // 1. request 받기
            line = br.readLine();
            log.debug("request line : {}", line);

            if(line == null) {
                return;
            }


            String[] tokens = line.split(" ");

            while (!line.equals("")) {
                line = br.readLine();
                log.debug("Header : {}", line);
            }

            DataOutputStream dos = new DataOutputStream(out);
            String url = tokens[1];

            if(url.startsWith("/user/create")) {
                int index = url.indexOf("?");
                String params = url.substring(index+1);

                Map<String,String> paramsMap = HttpRequestUtils.parseQueryString(params);

                User user = new User( paramsMap.get("userId"),
                                      paramsMap.get("password"),
                                      paramsMap.get("name"),
                                      paramsMap.get("email"));
                log.debug("user : {}", user);
            }
            else {
                byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
                response200Header(dos, body.length);
                responseBody(dos, body);
            }

        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
