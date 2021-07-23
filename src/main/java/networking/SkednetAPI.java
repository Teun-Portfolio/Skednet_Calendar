package networking;

import com.google.api.client.http.HttpStatusCodes;
import org.apache.http.Consts;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.InvalidCredentialsException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import utils.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class SkednetAPI {
    private final static String LOGIN = "https://mijn.skednet.com/Account/Login";
    private final static String SCHEDULE = "https://mijn.skednet.com/Schedule";
    private final CloseableHttpClient httpClient;

    public SkednetAPI(String username, String password) throws AuthenticationException, IOException {
        // use old cookie standard
        httpClient = HttpClients.custom().setDefaultRequestConfig(RequestConfig.custom()
                .setCookieSpec(CookieSpecs.STANDARD).build()).build();

        // Get __RequestVerificationToken from login page
        HttpGet getToken = new HttpGet(LOGIN);
        String token = "";
        try (CloseableHttpResponse response = httpClient.execute(getToken)) {
            Document html = Jsoup.parse(EntityUtils.toString(response.getEntity()));
            token = html.select("[name=\"__RequestVerificationToken\"]").attr("value");
            if(token.isEmpty()){
                throw new HttpResponseException(response.getStatusLine().getStatusCode()
                        , "__RequestVerificationToken not found in response");
            }
        }

        // POST Login credentials to login page
        List<NameValuePair> form = new ArrayList<>();
        form.add(new BasicNameValuePair("Email", username));
        form.add(new BasicNameValuePair("Password", password));
        form.add(new BasicNameValuePair("__RequestVerificationToken", token));
        form.add(new BasicNameValuePair("RememberMe", "true"));
        UrlEncodedFormEntity encodedForm = new UrlEncodedFormEntity(form, Consts.UTF_8);
        // send post
        HttpPost postLogin = new HttpPost(LOGIN);
        postLogin.setEntity(encodedForm);
        // check response
        try (CloseableHttpResponse response = httpClient.execute(postLogin)) {
            // 302 found means success as it tries to redirect us to the home page
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatusCodes.STATUS_CODE_FOUND) {
                Logger.i("SkednetAPI:authorize", "Authorized successfully");
            }
            // 200 ok usually is caused by a incorrect set of credentials
            else if (statusCode == HttpStatusCodes.STATUS_CODE_OK) {
                throw new InvalidCredentialsException("Response did not redirect (200 OK) " +
                        "is your user and pass set correctly?");
            }
            // failed to authorize
            else {
                throw new HttpResponseException(statusCode, String.format("Failed to authorize: %s"
                        , response.getStatusLine()));
            }
        }
    }

    /**
     * returns response from mijn.skednet.com/schedule
     */
    public String getSchedule() {
    //  Get response containing schedule
        HttpGet getSchedule = new HttpGet(SCHEDULE);
        try (CloseableHttpResponse response = httpClient.execute(getSchedule)) {
            // check if response is ok
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatusCodes.STATUS_CODE_OK) {
                // returns response
                return EntityUtils.toString(response.getEntity());
            }
            throw new HttpResponseException(statusCode, "Incorrect response from skednet schedule");
        } catch (IOException e) {
            Logger.e("SkednetAPI:getSchedule", e.getMessage());
        }
        Logger.w("SkednetApi:getSchedule", "Schedule not received returning empty string!");
        return "";
    }
}

