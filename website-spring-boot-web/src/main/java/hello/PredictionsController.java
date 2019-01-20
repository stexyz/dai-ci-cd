package hello;

import ai.h2o.mojos.runtime.MojoPipeline;

import ai.h2o.mojos.runtime.frame.MojoColumn;
import ai.h2o.mojos.runtime.frame.MojoFrame;
import ai.h2o.mojos.runtime.frame.MojoFrameBuilder;
import ai.h2o.mojos.runtime.frame.MojoRowBuilder;
import ai.h2o.mojos.runtime.lic.LicenseException;
import ai.h2o.mojos.runtime.readers.InMemoryMojoReaderBackend;
import ai.h2o.mojos.runtime.readers.MojoReaderBackend;
import io.minio.MinioClient;
import io.minio.errors.*;
import org.apache.commons.collections.ArrayStack;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.xmlpull.v1.XmlPullParserException;

import java.io.*;
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
    public String greeting(@RequestParam(name="old", required=false, defaultValue="true") Boolean old, @RequestParam(name="page", required=false, defaultValue="1") Integer page, Model model) {

        List<List<String>> table = new ArrayList<List<String>>();
        MinioClient minioClient = null;
        try {
            minioClient = new MinioClient("http://localhost:9000", "accesskey", "secretkey");
            minioClient.statObject("data-bucket", "test-week1.csv");
            InputStream dataSetStream = minioClient.getObject("data-bucket", "test-week1.csv");

            BufferedReader br = new BufferedReader(new InputStreamReader(dataSetStream, "UTF-8"));


            String cvsSplitBy = ",";
            String line = br.readLine();
            assert (line!=null);

            // using array list as it will be added values during prediction phase
            List<String> headers = new ArrayList<>(Arrays.asList(line.split(cvsSplitBy)));
            headers.add("Predictions");
            while ((line = br.readLine()) != null) {
                List<String> row = new ArrayList<>(Arrays.asList(line.split(cvsSplitBy)));
                table.add(row);
            }
            double rmse = -1;
            table = predict(minioClient, table, headers, rmse);

            // Close the input stream.
            dataSetStream.close();

            assert(page > 0);

            List<List<String>> rowsToDisplay = new ArrayList<>();
            rowsToDisplay.add(headers);
            rowsToDisplay.addAll(table.stream().skip(pageSize * (page - 1)).limit(pageSize).collect(Collectors.toList()));

            Integer maxPage = table.size()/pageSize;

            model.addAttribute("rmse", rmse);
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
        } catch (LicenseException e) {
            e.printStackTrace();
        }
        return "predictions";
    }

    private List<List<String>> predict(MinioClient minioClient, List<List<String>> table, List<String> headers, Double rmse) throws IOException, InvalidKeyException, NoSuchAlgorithmException, InsufficientDataException, InvalidArgumentException, InternalException, NoResponseException, InvalidBucketNameException, XmlPullParserException, ErrorResponseException, LicenseException {

        //1. read mojo
        String model_bucket_old = "model-bucket-old";
        String mojo_file_name = "pipeline.mojo";
        //download the mojo file to local drive to use for pipeline creation
//        minioClient.getObject(model_bucket_old, mojo_file_name, "/tmp/pipeline.mojo.local");
        MojoPipeline model = MojoPipeline.loadFrom("/tmp/pipeline.mojo.local");

        List<Double> actuals = new ArrayList<>();
        //go row by row and create predictions
        MojoFrameBuilder frameBuilder = model.getInputFrameBuilder();
        for(List<String> row : table){
            MojoRowBuilder rowBuilder = frameBuilder.getMojoRowBuilder();
            int colNum = 0;
            for (String cell : row){
                rowBuilder.setValue(headers.get(colNum++), cell);
            }
            frameBuilder.addRow(rowBuilder);
            actuals.add(Double.parseDouble(row.get(4)));
        }

        MojoFrame iframe = frameBuilder.toMojoFrame();

//        iframe.debug();

        MojoFrame oframe = model.transform(iframe);
//        oframe.debug();

        //compute RMSE
        List<Double> predictions = new ArrayList<>();

        MojoColumn predictionCol = oframe.getColumn(0);
        int i = 0;
        for (String p : predictionCol.getDataAsStrings()){
            predictions.add(Double.parseDouble(p));
            table.get(i++).add(p);
        }
        rmse = get_rmse(actuals, predictions);
        System.out.println("RMSE="+rmse);
        return table;
    }

    private double get_rmse(List<Double> truth, List<Double> prediction) {
        if (truth.size() != prediction.size()) {
            throw new IllegalArgumentException(String.format("The vector sizes don't match: %d != %d.", truth.size(), prediction.size()));
        }

        int n = truth.size();
        double rss = 0.0;
        for (int i = 0; i < n; i++) {
            rss += Math.pow(truth.get(i) - prediction.get(i), 2);
        }

        return Math.sqrt(rss/n);
    }
}
