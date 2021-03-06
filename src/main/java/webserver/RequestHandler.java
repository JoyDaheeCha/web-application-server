package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import db.DataBase;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.IOUtils;

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
            DataOutputStream dos = new DataOutputStream(out);
            String url = tokens[1];

            int contentLength = 0;
            Map<String, String> cookieMap = new HashMap<>();

            while (!line.equals("")) {
                line = br.readLine();
                if(line.contains("Content-Length")) {
                    contentLength = Integer.parseInt(line.split(": ")[1]);
                }
                else if(line.contains("Cookie")) {
                    cookieMap = HttpRequestUtils.parseCookies(line.split(": ")[1]);
                }
                log.debug("Header : {}", line);
            }

            if(url.equals("/user/create")) {
                String params = IOUtils.readData(br, contentLength);
                Map<String,String> paramsMap = HttpRequestUtils.parseQueryString(params);

                User user = new User( paramsMap.get("userId"),
                                      paramsMap.get("password"),
                                      paramsMap.get("name"),
                                      paramsMap.get("email"));
                log.debug("user : {}", user);

                DataBase.addUser(user);

                response302Header(dos, "/index.html");
            }
            else if(url.equals("/user/login")) {
                String params = IOUtils.readData(br, contentLength);
                Map<String,String> paramsMap = HttpRequestUtils.parseQueryString(params);

                // 사용자 계정 얻기
                String userId = paramsMap.get("userId");
                User user = DataBase.findUserById(userId);

                // 로그인 성공
                if (user != null  && user.getPassword().equals(paramsMap.get("password"))) {
                    responseLoginSucceedHeader(dos, "/index.html");
                } else {
                    // 로그인 실패
                    responseLoginFailHeader(dos, "/user/login_failed.html");
                }
            }
            else if(url.equals("/user/list")) {
                Boolean isLogined = Boolean.parseBoolean(cookieMap.get("logined"));

                if (isLogined) {
                    Collection<User> users = DataBase.findAll();
                    StringBuilder sb = new StringBuilder();
                    sb.append("<table border='1'>");
                    for(User user : users) {
                        sb.append("<tr>");
                        sb.append("<td>" + user.getUserId() + "</td>");
                        sb.append("<td>" + user.getName() + "</td>");
                        sb.append("<td>" + user.getEmail() + "</td>");
                        sb.append("</td>");
                    }
                    sb.append("</table>");

                    byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
                    response200Header(dos, body.length);
                    responseBody(dos, body);
                } else  {
                    response302Header(dos, "/user/login.html");
                }

            }
            else if (url.endsWith(".css")) {
                byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
                responseCSSHeader(dos);
                responseBody(dos, body);
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

    private void responseCSSHeader(DataOutputStream dos) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/css \r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseLoginFailHeader(DataOutputStream dos, String url) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Content-Type: text/html \r\n");
            dos.writeBytes("Location:" + url + "\r\n");
            dos.writeBytes("Set-Cookie: logined=false \r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseLoginSucceedHeader(DataOutputStream dos, String url) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Content-Type: text/html \r\n");
            dos.writeBytes("Location:" + url + "\r\n");
            dos.writeBytes("Set-Cookie: logined=true \r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response302Header(DataOutputStream dos, String url) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location:" + url + "\r\n");
            dos.writeBytes("\r\n");
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
