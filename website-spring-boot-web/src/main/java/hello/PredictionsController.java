package hello;

import ai.h2o.mojos.runtime.MojoPipeline;

import ai.h2o.mojos.runtime.frame.MojoColumn;
import ai.h2o.mojos.runtime.frame.MojoFrame;
import ai.h2o.mojos.runtime.frame.MojoFrameBuilder;
import ai.h2o.mojos.runtime.frame.MojoRowBuilder;
import ai.h2o.mojos.runtime.lic.LicenseException;
import io.minio.MinioClient;
import io.minio.errors.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.xmlpull.v1.XmlPullParserException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class PredictionsController {

    private static final Integer pageSize = 20;
    private static final String oldDataSetName = "test-week1.csv";
    private static final String newDataSetName = "test-week2.csv";
    private static final String minioUrl = "http://localhost:9000";
    private static final String accessKey = "accesskey";
    private static final String secretKey = "secretkey";
    private static final String dataBucket = "data-bucket";
    private static final String cvsSplitBy = ",";
    private static final String model_bucket = "model-bucket";
    private static final String mojo_file_name_minio = "pipeline.mojo";
    private static final String mojo_local_path = "/tmp/pipeline.mojo.local";


    @GetMapping("/predictions")
    public void predictions(@RequestParam(name="old", required=false, defaultValue="true") Boolean old, @RequestParam(name="page", required=false, defaultValue="1") Integer page, Model model) throws InvalidPortException, InvalidEndpointException, IOException, InvalidKeyException, NoSuchAlgorithmException, InsufficientDataException, InternalException, NoResponseException, InvalidBucketNameException, XmlPullParserException, ErrorResponseException, InvalidArgumentException, LicenseException {
        List<List<String>> table = new ArrayList<>();
        List<String> headers;

        MinioClient minioClient = new MinioClient(minioUrl, accessKey, secretKey);
        String datasetName = old ? oldDataSetName : newDataSetName;
        minioClient.statObject(dataBucket, datasetName);
        //TODO: add request param "cache" for pagination
        try(InputStream dataSetStream = minioClient.getObject(dataBucket, datasetName)) {
            BufferedReader br = new BufferedReader(new InputStreamReader(dataSetStream, "UTF-8"));

            // read headers
            String line = br.readLine();
            assert (line != null);
            // using array list as it will be added values during prediction phase
            headers = new ArrayList<>(Arrays.asList(line.split(cvsSplitBy)));
            headers.add("Predictions");

            //read rest of the data set
            while ((line = br.readLine()) != null) {
                List<String> row = new ArrayList<>(Arrays.asList(line.split(cvsSplitBy)));
                table.add(row);
            }

        }

        List<Double> predictions = predict(minioClient, table, headers);
        List<Double> actuals = getActual(table);
        double rmse = get_rmse(actuals, predictions);
        table = appendPredictions(predictions, table);

        assert (page > 0);

        List<List<String>> rowsToDisplay = new ArrayList<>();
        rowsToDisplay.add(headers);
        rowsToDisplay.addAll(table.stream().skip(pageSize * (page - 1)).limit(pageSize).collect(Collectors.toList()));

        Integer maxPage = table.size() / pageSize;

        model.addAttribute("rmse", rmse);
        model.addAttribute("old", old);
        model.addAttribute("rows", rowsToDisplay);
        model.addAttribute("page", page);
        model.addAttribute("maxPage", maxPage);
    }

    private List<Double> predict(MinioClient minioClient, List<List<String>> table, List<String> headers) throws IOException, LicenseException, InvalidKeyException, NoSuchAlgorithmException, InsufficientDataException, InvalidArgumentException, InternalException, NoResponseException, InvalidBucketNameException, XmlPullParserException, ErrorResponseException {
        Files.deleteIfExists(Paths.get(mojo_local_path));

        //download the mojo file to local drive to use for pipeline creation
        minioClient.getObject(model_bucket, mojo_file_name_minio, mojo_local_path);
        MojoPipeline model = MojoPipeline.loadFrom(mojo_local_path);

        MojoFrameBuilder frameBuilder = model.getInputFrameBuilder();
        for(List<String> row : table){
            MojoRowBuilder rowBuilder = frameBuilder.getMojoRowBuilder();
            int colNum = 0;
            for (String cell : row){
                rowBuilder.setValue(headers.get(colNum++), cell);
            }
            frameBuilder.addRow(rowBuilder);
        }

        MojoFrame iFrame = frameBuilder.toMojoFrame();
        MojoFrame oFrame = model.transform(iFrame);

        List<Double> predictions = new ArrayList<>();

        MojoColumn predictionCol = oFrame.getColumn(0);
        for (String p : predictionCol.getDataAsStrings()){
            predictions.add(Double.parseDouble(p));
        }
        return predictions;
    }

    private List<Double> getActual(List<List<String>> table){
        List<Double> actual = new ArrayList<>();
        for(List<String> row : table) {
            actual.add(Double.parseDouble(row.get(4)));
        }
        return actual;
    }

    private List<List<String>> appendPredictions(List<Double> predictions, List<List<String>> table){
        for(int i = 0; i < table.size(); i++){
            table.get(i).add(predictions.get(i).toString());
        }
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
