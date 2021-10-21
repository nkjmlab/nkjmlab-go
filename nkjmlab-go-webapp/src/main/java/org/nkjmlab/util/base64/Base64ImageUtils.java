package org.nkjmlab.util.base64;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;

public class Base64ImageUtils {
  private static org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();

  public static BufferedImage decode(String encodedImage, String formatName) {
    byte[] decoded = Base64.getDecoder().decode(encodedImage.replaceFirst("data:.*?base64,", ""));
    try {
      BufferedImage image = ImageIO.read(new ByteArrayInputStream(decoded));
      return image;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static BufferedImage decodeAndWrite(String encodedImage, String formatName, File output) {
    BufferedImage image;
    try {
      image = decode(encodedImage, formatName);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    try {
      ImageIO.write(image, formatName, output);
      return image;
    } catch (NullPointerException e) {
      log.error("{},{},{}", formatName, output, image);
      log.error("The program may not have auth for write to {}", output);
      throw new RuntimeException(e);
    } catch (Exception e) {
      log.error("{},{},{}", formatName, output, image);
      throw new RuntimeException(e);
    }
  }

  public static String encode(RenderedImage image, String formatName) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(baos);) {
      ImageIO.write(image, formatName, bos);
      bos.flush();
      return Base64.getEncoder().encodeToString(baos.toByteArray());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static String encodedImageToImgSrcValue(String encodedImage, String formatName) {
    return String.format("data:image/%s;base64,%s", formatName, encodedImage);
  }

  public static String encodedImageToImgElement(String encodedImage, String formatName) {
    return String.format("<img src='%s'/>", encodedImage);
  }

  public static String encodeToImgSrcValue(BufferedImage image, String formatName) {
    return encodedImageToImgSrcValue(encode(image, formatName), formatName);
  }

  public static String encodeToImgElement(BufferedImage image, String formatName) {
    return encodedImageToImgElement(encode(image, formatName), formatName);
  }

}
