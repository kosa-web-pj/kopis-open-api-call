package com.kosa.showfan.aws;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import java.io.*;
import java.net.URL;

public class AWSService {
    //need data
    private static final String BUCKET_NAME = "";
    private static final String ACCESS_KEY = "";
    private static final String SECRET_KEY = "";
    private AmazonS3 amazonS3;

    public AWSService() {
        AWSCredentials awsCredentials = new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY);
        amazonS3 = new AmazonS3Client(awsCredentials);
    }

    public void uploadFile(String key, String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            try (InputStream in = url.openStream()) {
                if (amazonS3 != null) {
                    try {
                        PutObjectRequest putObjectRequest =
                                new PutObjectRequest(BUCKET_NAME, key + ".jpg", convertInputStreamToFile(in));
                        putObjectRequest.setCannedAcl(CannedAccessControlList.PublicRead);

                        ObjectMetadata metadata = new ObjectMetadata();
                        metadata.setContentType("image/jpeg");
                        putObjectRequest.setMetadata(metadata);

                        amazonS3.putObject(putObjectRequest);

                    } catch (AmazonServiceException ase) {
                        ase.printStackTrace();
                    } finally {
                        amazonS3 = null;
                    }
                }

                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static File convertInputStreamToFile(InputStream inputStream) {

        File tempFile = null;
        try {
            tempFile = File.createTempFile(String.valueOf(inputStream.hashCode()), ".tmp");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        tempFile.deleteOnExit();

        copyInputStreamToFile(inputStream, tempFile);

        return tempFile;
    }

    private static void copyInputStreamToFile(InputStream inputStream, File file) {

        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            int read;
            byte[] bytes = new byte[1024];

            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        AWSService s3 = new AWSService();
        s3.uploadFile("PF154190", "https://acting.kr/data/thumb/poster/PF154190.gif");
    }
}