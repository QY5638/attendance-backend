package com.quyong.attendance.module.face.support;

import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class LocalFaceRecognitionProvider implements FaceRecognitionProvider {

    private static final String FEATURE_PREFIX = "LITE:";
    private static final int HASH_SIZE = 8;
    private static final int DIFF_HASH_WIDTH = HASH_SIZE + 1;
    private static final int DIFF_HASH_HEIGHT = HASH_SIZE;

    @Override
    public String extractFeature(String imageData) {
        String normalizedImageData = normalize(imageData);
        BufferedImage bufferedImage = readImage(normalizedImageData);
        if (bufferedImage == null) {
            return DigestUtils.md5DigestAsHex(normalizedImageData.getBytes(StandardCharsets.UTF_8));
        }

        return FEATURE_PREFIX + buildAverageHash(bufferedImage) + ":" + buildDifferenceHash(bufferedImage);
    }

    @Override
    public BigDecimal compare(String imageData, String storedFeatureData) {
        if (storedFeatureData == null || storedFeatureData.trim().isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        String currentFeature = extractFeature(imageData);

        if (!isLiteFeature(currentFeature) || !isLiteFeature(storedFeatureData)) {
            if (currentFeature.equals(storedFeatureData)) {
                return new BigDecimal("99.99");
            }
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        String[] currentParts = currentFeature.split(":");
        String[] storedParts = storedFeatureData.split(":");
        if (currentParts.length != 3 || storedParts.length != 3) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal averageHashScore = calculateHashSimilarity(currentParts[1], storedParts[1]);
        BigDecimal differenceHashScore = calculateHashSimilarity(currentParts[2], storedParts[2]);
        BigDecimal combined = averageHashScore.add(differenceHashScore)
                .divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        if (currentFeature.equals(storedFeatureData)) {
            return new BigDecimal("99.99");
        }
        return combined;
    }

    private String normalize(String imageData) {
        return imageData == null ? "" : imageData.replaceAll("\\s+", "");
    }

    private boolean isLiteFeature(String featureData) {
        return featureData != null && featureData.startsWith(FEATURE_PREFIX);
    }

    private BigDecimal calculateHashSimilarity(String currentHash, String storedHash) {
        if (currentHash == null || storedHash == null || currentHash.length() != storedHash.length()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        int differentBits = 0;
        for (int index = 0; index < currentHash.length(); index++) {
            int currentNibble = Character.digit(currentHash.charAt(index), 16);
            int storedNibble = Character.digit(storedHash.charAt(index), 16);
            if (currentNibble < 0 || storedNibble < 0) {
                return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
            differentBits += Integer.bitCount(currentNibble ^ storedNibble);
        }

        int totalBits = currentHash.length() * 4;
        BigDecimal matchedBits = BigDecimal.valueOf(totalBits - differentBits)
                .multiply(new BigDecimal("100"));
        return matchedBits.divide(BigDecimal.valueOf(totalBits), 2, RoundingMode.HALF_UP);
    }

    private BufferedImage readImage(String normalizedImageData) {
        try {
            byte[] imageBytes = decodeImageBytes(normalizedImageData);
            if (imageBytes.length == 0) {
                return null;
            }
            return ImageIO.read(new ByteArrayInputStream(imageBytes));
        } catch (Exception exception) {
            return null;
        }
    }

    private byte[] decodeImageBytes(String normalizedImageData) {
        if (normalizedImageData == null || normalizedImageData.trim().isEmpty()) {
            return new byte[0];
        }

        int separatorIndex = normalizedImageData.indexOf(',');
        String base64Payload = separatorIndex >= 0
                ? normalizedImageData.substring(separatorIndex + 1)
                : normalizedImageData;
        return Base64.getDecoder().decode(base64Payload);
    }

    private String buildAverageHash(BufferedImage sourceImage) {
        BufferedImage resizedImage = resize(sourceImage, HASH_SIZE, HASH_SIZE);
        int[][] grayMatrix = toGrayMatrix(resizedImage);
        long total = 0;
        for (int row = 0; row < HASH_SIZE; row++) {
            for (int column = 0; column < HASH_SIZE; column++) {
                total += grayMatrix[row][column];
            }
        }

        double average = total / (double) (HASH_SIZE * HASH_SIZE);
        StringBuilder builder = new StringBuilder(HASH_SIZE * HASH_SIZE / 4);
        int nibble = 0;
        int bitCount = 0;
        for (int row = 0; row < HASH_SIZE; row++) {
            for (int column = 0; column < HASH_SIZE; column++) {
                nibble = (nibble << 1) | (grayMatrix[row][column] >= average ? 1 : 0);
                bitCount++;
                if (bitCount == 4) {
                    builder.append(Integer.toHexString(nibble));
                    nibble = 0;
                    bitCount = 0;
                }
            }
        }
        return builder.toString();
    }

    private String buildDifferenceHash(BufferedImage sourceImage) {
        BufferedImage resizedImage = resize(sourceImage, DIFF_HASH_WIDTH, DIFF_HASH_HEIGHT);
        int[][] grayMatrix = toGrayMatrix(resizedImage);
        StringBuilder builder = new StringBuilder(HASH_SIZE * HASH_SIZE / 4);
        int nibble = 0;
        int bitCount = 0;
        for (int row = 0; row < DIFF_HASH_HEIGHT; row++) {
            for (int column = 0; column < HASH_SIZE; column++) {
                nibble = (nibble << 1) | (grayMatrix[row][column] >= grayMatrix[row][column + 1] ? 1 : 0);
                bitCount++;
                if (bitCount == 4) {
                    builder.append(Integer.toHexString(nibble));
                    nibble = 0;
                    bitCount = 0;
                }
            }
        }
        return builder.toString();
    }

    private int[][] toGrayMatrix(BufferedImage image) {
        int[][] grayMatrix = new int[image.getHeight()][image.getWidth()];
        for (int row = 0; row < image.getHeight(); row++) {
            for (int column = 0; column < image.getWidth(); column++) {
                int rgb = image.getRGB(column, row);
                int red = (rgb >> 16) & 0xff;
                int green = (rgb >> 8) & 0xff;
                int blue = rgb & 0xff;
                grayMatrix[row][column] = (red * 299 + green * 587 + blue * 114) / 1000;
            }
        }
        return grayMatrix;
    }

    private BufferedImage resize(BufferedImage sourceImage, int width, int height) {
        Image scaledImage = sourceImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage targetImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = targetImage.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(scaledImage, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return targetImage;
    }
}
