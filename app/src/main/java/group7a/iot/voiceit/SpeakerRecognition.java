package group7a.iot.voiceit;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.http.entity.mime.MultipartEntityBuilder;



public class SpeakerRecognition {

    public void httpRequest(File file) {
        System.out.println("Innan kommer vi hit.");
        CloseableHttpClient httpclient = null;
        try {
            httpclient = HttpClients.createDefault();
            System.out.println("Den senasdte");
        }catch (Exception e){
            System.out.println("KRASH!!!1");
            e.printStackTrace();
            System.out.println("KRASH!!!2");

        }
        System.out.println("Kommer vi hit=?");
        try {
            System.out.println("Booo or Baaa?");
            URIBuilder builder = new URIBuilder("https://westus.api.cognitive.microsoft.com/spid/v1.0/verify?verificationProfileId={21787816-261d-4703-97c2-e866f54c5975}");
            MultipartEntityBuilder mpBuilder = MultipartEntityBuilder.create();
            mpBuilder.addBinaryBody("file", new FileInputStream(file), ContentType.APPLICATION_OCTET_STREAM, file.getName());
            URI uri = builder.build();

            HttpPost request = new HttpPost(uri);
            request.setHeader("Content-Type", "multipart/form-data");
            request.setHeader("Ocp-Apim-Subscription-Key", "{b7efa04012d54db2abea9a10494cfca2}");
            HttpEntity multipart = mpBuilder.build();

            request.setEntity(multipart);
            // Request body
//            StringEntity reqEntity = new StringEntity("{body}");
//            request.setEntity(reqEntity);

            HttpResponse response = httpclient.execute(request);
            HttpEntity responseEntity = response.getEntity();

            if (responseEntity != null) {
                System.out.println(EntityUtils.toString(responseEntity));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("BOBOOO!!!!");
        }
    }
}