import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.Float;

import static java.lang.Math.sqrt;

public class SeamCarver {

    private int width;
    private final int height;
    int[][] pixels;
    private static final float[][] gx = {
            { 1.f, 0.f, -1.f },
            { 2.f, 0.f, -2.f },
            { 1.f, 0.f, -1.f },
    };
    private static final float[][] gy = {
            { 1.f, 2.f, 1.f },
            { 0.f, 0.f, 0.f },
            { -1.f, -2.f, -1.f },
    };

    SeamCarver(String filePath) {
        BufferedImage image = loadImage(filePath);
        width = image.getWidth();
        height = image.getHeight();

        pixels = new int[height][width];
        for (int y = 0; y < height; ++y) {
            image.getRGB(0, y, width, 1, pixels[y], 0, width);
        }
    }

    public void run(int numOfCarves) {
        for (int iteration = 0; iteration < numOfCarves; ++iteration, --width) {
            float[][] luminance = calculateLuminance(pixels);
            float[][] gradient = calculateEdges(luminance);
            float[][] energies = calculateEnergy(gradient);
            carve(energies);
            System.out.println(STR."[INFO] Removed seam: \{iteration + 1}");
        }

        savePixelData(pixels);
        System.out.println("[OK] Finished");
    }

    /**
     * Loads image and returns BufferedImage object. Will terminate program when cannot read file.
     */
    private BufferedImage loadImage(String filePath) {
        BufferedImage image = null;
        try {
            image = ImageIO.read(new File(filePath));
        } catch (IOException e) {
            System.out.println(STR."[ERROR] Failed to load image: \{e.getMessage()}");
            System.exit(-1);
        }
        System.out.println(STR."[OK] Loaded image: \{filePath}");
        return image;
    }

    /**
     * Converts RGB to normalized luminance value.
     */
    private float rgbToLuminance(int rgb) {
        float r = (rgb >> 16 & 0xFF) / 255.f;
        float g = (rgb >> 8 & 0xFF) / 255.f;
        float b = (rgb & 0xFF) / 255.f;

        return 0.2126f * r + 0.7152f * g + 0.0722f * b;
    }

    /**
     * Calculates luminance for every pixel in 2D array.
     */
    private float[][] calculateLuminance(int[][] pixels) {
        float[][] luminance = new float[height][width];

        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                luminance[y][x] = rgbToLuminance(pixels[y][x]);
            }
        }
        return luminance;
    }

    /**
     * Calculates edges in image using Sobel operator.
     * To calculate this gradient it needs image data converted to luminance values.
     * Gradient contains floats bigger than or equal zero.
     */
    private float[][] calculateEdges(float[][] matrix) {
        float[][] gradient = new float[height][width];
        for (int centerY = 0; centerY < height; ++centerY) {
            for (int centerX = 0; centerX < width; ++centerX) {
                float sobelX = 0.f;
                float sobelY = 0.f;
                for (int dy = -1; dy <= 1; ++dy) {
                    for (int dx = -1; dx <= 1; ++dx) {
                        int y = centerY + dy;
                        int x = centerX + dx;
                        float convolution = 0.f;
                        if (x >= 0 && x < width && y >= 0 && y < height) {
                            convolution = matrix[y][x];
                        }
                        sobelX += convolution * gx[dy + 1][dx + 1];
                        sobelY += convolution * gy[dy + 1][dx + 1];
                    }
                }
                float value = (float) sqrt(sobelX * sobelX + sobelY * sobelY);
                // round value to zero if it is too small
                if (value > 1e-6f) {
                    gradient[centerY][centerX] = value;
                } else {
                    gradient[centerY][centerX] = 0.f;
                }
            }
        }
        return gradient;
    }

    /**
     * Calculates energy of every pixel from 2D gradient array using dynamic programming.
     * Returns 2D array of sums of energies leading up to pixels.
     */
    private float[][] calculateEnergy(float[][] matrix) {
        float[][] energies = new float[height][width];
        // copy first row
        System.arraycopy(matrix[0], 0, energies[0], 0, width);

        for (int y = 1; y < height; ++y) {
            for (int centerX = 0; centerX < width; ++centerX) {
                float min = Float.MAX_VALUE;
                // finds cheapest parent pixel from three pixels above
                for (int dx = -1; dx <= 1; ++dx) {
                    int x = centerX + dx;
                    float value = Float.MAX_VALUE;  // default value when left or right parent is out of bounds
                    if (x >= 0 && x < width) {
                        value = energies[y - 1][x];
                    }
                    if (value < min) {
                        min = value;
                    }
                }
                energies[y][centerX] = matrix[y][centerX] + min;
            }
        }
        return energies;
    }

    /**
     * Finds cheapest pixel in last row of energy matrix.
     */
    private int getCheapestSeamStart(float[][] matrix) {
        int column = 0;
        for (int x = 1; x < width; ++x) {
            if (matrix[height - 1][x] < matrix[height - 1][column]) {
                column = x;
            }
        }
        return column;
    }

    /**
     * Removes one seam of lowest energy from image based on energy matrix.
     */
    private void carve(float[][] matrix) {
        int seam = getCheapestSeamStart(matrix);
        // move pixels to the left starting from seam (last row)
        for (int pos = seam; pos < width - 1; ++pos) {
            pixels[height - 1][pos] = pixels[height - 1][pos + 1];
        }

        for (int y = height - 2; y >= 0; --y) {
            for (int dx = -1; dx <= 1; ++dx) {
                int x = seam + dx;  // index of lowest energy in current row
                if (x >= 0 && x < width && matrix[y][x] < matrix[y][seam]) {
                    seam = x;
                }
            }
            // move pixels to the left starting from seam
            for (int pos = seam; pos < width - 1; ++pos) {
                pixels[y][pos] = pixels[y][pos + 1];
            }
        }
    }

    /**
     * Saves pixel data as image file.
     */
    private void savePixelData(int[][] pixels) {
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; ++y) {
            newImage.setRGB(0, y, width, 1, pixels[y], 0, width);
        }
        try {
            ImageIO.write(newImage, "png", new File("output.png"));
        } catch (IOException e) {
            System.out.println(STR."[ERROR] Failed to save image: \{e.getMessage()}");
            System.exit(1);
        }
        System.out.println("[OK] Generated: output.png");
    }

}
