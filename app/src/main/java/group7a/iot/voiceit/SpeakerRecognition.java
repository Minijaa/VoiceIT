package group7a.iot.voiceit;

import java.io.File;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SpeakerRecognition {
    //VerificationProfileID för Samuel är = 5ce07214-c2c8-4799-83b2-45c334750e52
    //VerificationProfileID för Samuel2 är = 6e54c829-e388-4444-946c-10d388f8960c
    private OkHttpClient client = new OkHttpClient();

    public void createProfile() {
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, "{\"locale\":\"en-us\"}");
        Request request = new Request.Builder()
                .url("https://westus.api.cognitive.microsoft.com/spid/v1.0/verificationProfiles")
                .post(body)
                .addHeader("Ocp-Apim-Subscription-Key", "ac4d7534bab4421fb7116f7013811296")
                .addHeader("Content-Type", "application/json")
                .build();

        try {
            Response response = client.newCall(request).execute();
            System.out.println(response);
            System.out.println(response.body().string());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void getPossiblePhrases() {
        String locale = "en-us";
        MediaType mediaType = MediaType.parse("application/json");
        Request request = new Request.Builder()
                .url("https://westus.api.cognitive.microsoft.com/spid/v1.0/verificationPhrases?locale=en-us")
                .addHeader("Ocp-Apim-Subscription-Key", "ac4d7534bab4421fb7116f7013811296")
                .build();

        try {
            Response response = client.newCall(request).execute();
            System.out.println(response);
            System.out.println(response.body().string());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createEnrollment(File file) {
        MediaType mediaType = MediaType.parse("multipart/form-data");
        RequestBody body = RequestBody.create(mediaType, file);
        Request request = new Request.Builder()
                .url("https://westus.api.cognitive.microsoft.com/spid/v1.0/verificationProfiles/{6e54c829-e388-4444-946c-10d388f8960c}/enroll")
                .post(body)
                .addHeader("Ocp-Apim-Subscription-Key", "ac4d7534bab4421fb7116f7013811296")
                .addHeader("Content-Type", "multipart/form-data")
                .build();

        try {
            Response response = client.newCall(request).execute();
            System.out.println(response);
            System.out.println(response.body().string());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean verifySpeaker(File file) {
        MediaType mediaType = MediaType.parse("multipart/form-data");
        RequestBody body = RequestBody.create(mediaType, file);
        Request request = new Request.Builder()
                .url("https://westus.api.cognitive.microsoft.com/spid/v1.0/verify?verificationProfileId={6e54c829-e388-4444-946c-10d388f8960c}")
                .post(body)
                .addHeader("Ocp-Apim-Subscription-Key", "ac4d7534bab4421fb7116f7013811296")
                .addHeader("Content-Type", "multipart/form-data")
                .build();
        try {
            Response response = client.newCall(request).execute();
            System.out.println(response);
            String responseString = response.body() != null ? response.body().string() : null;
            System.out.println(responseString);
            if ((responseString != null && responseString.contains("Accept")) && responseString.contains("High")){
                return true;
            }
            response.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
