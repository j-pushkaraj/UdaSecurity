package com.udacity.catpoint.image;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsRequest;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsResponse;
import software.amazon.awssdk.services.rekognition.model.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Image Recognition Service that can identify cats. Requires aws credentials to be entered in config.properties to work.
 */
public final class AwsImageService implements ImageService {

    private static final Logger log = LoggerFactory.getLogger(AwsImageService.class);

    //aws recommendation is to maintain only a single instance of client objects
    private static RekognitionClient rekognitionClient;

    private static synchronized void initRekognitionClient() {
        if (rekognitionClient != null) {
            return;
        }
        Properties props = new Properties();
        try (InputStream is = AwsImageService.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (is == null) {
                log.error("Unable to initialize AWS Rekognition, config.properties not found in classpath");
                return;
            }
            props.load(is);
        } catch (IOException ioe) {
            log.error("Unable to initialize AWS Rekognition, no properties file found", ioe);
            return;
        }

        String awsId = props.getProperty("aws.id");
        String awsSecret = props.getProperty("aws.secret");
        String awsRegion = props.getProperty("aws.region");

        if (awsId == null || awsSecret == null || awsRegion == null) {
            log.error("AWS credentials/region missing in config.properties");
            return;
        }

        AwsCredentials awsCredentials = AwsBasicCredentials.create(awsId, awsSecret);
        rekognitionClient = RekognitionClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .region(Region.of(awsRegion))
                .build();
    }

    public AwsImageService() {
        initRekognitionClient();
    }


    /**
     * Returns true if the provided image contains a cat.
     * @param image Image to scan
     * @param confidenceThreshold Minimum threshold to consider for cat. For example, 90.0f would require 90% confidence minimum
     * @return
     */
    @Override
    public boolean imageContainsCat(BufferedImage image, float confidenceThreshold) {
        if (rekognitionClient == null) {
            log.error("AWS Rekognition client not initialized.");
            return false;
        }
        Image awsImage = null;
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpg", os);
            awsImage = Image.builder().bytes(SdkBytes.fromByteArray(os.toByteArray())).build();
        } catch (IOException ioe) {
            log.error("Error building image byte array", ioe);
            return false;
        }
        DetectLabelsRequest detectLabelsRequest = DetectLabelsRequest.builder().image(awsImage).minConfidence(confidenceThreshold).build();
        DetectLabelsResponse response = rekognitionClient.detectLabels(detectLabelsRequest);
        logLabelsForFun(response);
        return response.labels().stream().filter(l -> l.name().toLowerCase().contains("cat")).findFirst().isPresent();
    }

    private void logLabelsForFun(DetectLabelsResponse response) {
        log.info(response.labels().stream()
                .map(label -> String.format("%s(%.1f%%)", label.name(), label.confidence()))
                .collect(Collectors.joining(", ")));
    }
}
