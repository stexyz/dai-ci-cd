package hello;

import io.minio.MinioClient;
import io.minio.errors.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class PredictionsController {

    private Integer pageSize = 20;

    @GetMapping("/predictions")
    public String greeting(@RequestParam(name="old", required=false, defaultValue="true") Boolean old, @RequestParam(name="page", required=false, defaultValue="1") Integer page ,Model model) {

        List<List<String>> table = new ArrayList<List<String>>();
        MinioClient minioClient = null;
        try {
            minioClient = new MinioClient("http://localhost:9000", "accesskey", "secretkey");
            minioClient.statObject("data-bucket", "creditcard_train_cat.csv");
            InputStream dataSetStream = minioClient.getObject("data-bucket", "creditcard_train_cat.csv");

            BufferedReader br = new BufferedReader(new InputStreamReader(dataSetStream, "UTF-8"));


            String line = "";
            String cvsSplitBy = ",";
            List<String> headers = new ArrayList<String>();
            line = br.readLine();
            assert (line!=null);
            table.add(Arrays.asList(line.split(cvsSplitBy)));
            while ((line = br.readLine()) != null) {
                List<String> row = Arrays.asList(line.split(cvsSplitBy));
                table.add(row);
            }

            // Close the input stream.
            dataSetStream.close();

            assert(page > 0);

            List<List<String>> rowsToDisplay = table.stream().skip(pageSize * (page - 1)).limit(pageSize).collect(Collectors.toList());

            Integer maxPage = table.size()/pageSize;

            model.addAttribute("old", old);
            model.addAttribute("rows", rowsToDisplay);
            model.addAttribute("page", page);
            model.addAttribute("maxPage", maxPage);
        } catch (InvalidEndpointException e) {
            e.printStackTrace();
        } catch (InvalidPortException e) {
            e.printStackTrace();
        } catch (InvalidBucketNameException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InsufficientDataException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoResponseException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (ErrorResponseException e) {
            e.printStackTrace();
        } catch (InternalException e) {
            e.printStackTrace();
        } catch (InvalidArgumentException e) {
            e.printStackTrace();
        }
        return "predictions";
    }
}
