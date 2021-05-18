package edu.cg;

import java.awt.image.BufferedImage;
import java.awt.Color;
import java.util.*;


public class BasicSeamsCarver extends ImageProcessor {

    // An enum describing the carving scheme used by the seams carver.
    // VERTICAL_HORIZONTAL means vertical seams are removed first.
    // HORIZONTAL_VERTICAL means horizontal seams are removed first.
    // INTERMITTENT means seams are removed intermittently : vertical, horizontal, vertical, horizontal etc.
    public static enum CarvingScheme {
        VERTICAL_HORIZONTAL("Vertical seams first"),
        HORIZONTAL_VERTICAL("Horizontal seams first"),
        INTERMITTENT("Intermittent carving");

        public final String description;

        private CarvingScheme(String description) {
            this.description = description;
        }
    }

    // A simple coordinate class which assists the implementation.
    // Y == NumOfRows, X == NumOfCols
    protected class Coordinate {
        public int Y;
        public int X;

        public Coordinate(int Y, int X) {
            this.X = X;
            this.Y = Y;
        }
    }

    // TODO :  Decide on the fields your BasicSeamsCarver should include. Refer to the recitation and homework
    // instructions PDF to make an educated decision.
    BufferedImage workingImage;
    BufferedImage originalImg;
    RGBWeights rgbWeights;

    int lastCurrentRow;
    int lastCurrentCol;

    Coordinate[][] indexMatrix;

    double[][] pixelEnergyMatrix;
    double[][] costMatrix;

    Coordinate[][] prevPixel;

    public BasicSeamsCarver(Logger logger, BufferedImage workingImage,
                            int outWidth, int outHeight, RGBWeights rgbWeights) {
        super((s) -> logger.log("Seam carving: " + s), workingImage, rgbWeights, outWidth, outHeight);
        // TODO : Include some additional initialization procedures.
        this.workingImage = workingImage;
        originalImg = duplicateWorkingImage();
        this.rgbWeights = rgbWeights;

        initIndexMatrix();
        initPixelEnergy();
    }

    public void initIndexMatrix() {
        indexMatrix = new Coordinate[originalImg.getHeight()][originalImg.getWidth()];

        for (int y = 0; y < originalImg.getHeight(); y++) {
            for (int x = 0; x < originalImg.getWidth(); x++) {
                Coordinate coordinate = new Coordinate(y, x);
                indexMatrix[y][x] = coordinate;
            }
        }
    }

    public double getPixelDifference(int y1, int x1, int y2, int x2) {
        int greyscaleIntensity1 = getGreyscaleIntensity(x1, y1);
        int greyscaleIntensity2 = getGreyscaleIntensity(x2, y2);

        return Math.abs(greyscaleIntensity1 - greyscaleIntensity2);
    }

    public double getGradientMagnitude(double dy, double dx) {
        return Math.sqrt((dx * dx) + (dy * dy));
    }

    private int getGreyscaleIntensity(int x, int y) {
        int rgb = workingImage.getRGB(x, y);

        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;

        return (red + green + blue) / 3;
    }

    public void initPixelEnergy() {
        pixelEnergyMatrix = new double[workingImage.getHeight()][workingImage.getWidth()];
        for (int y = 0; y < workingImage.getHeight(); y++) {
            for (int x = 0; x < workingImage.getWidth(); x++) {
                int nextY = (y == workingImage.getHeight() - 1) ? y - 1 : y + 1;
                int nextX = (x == workingImage.getWidth() - 1) ? x - 1 : x + 1;

                double dx = getPixelDifference(y, x, y, nextX);
                double dy = getPixelDifference(y, x, nextY, x);

                pixelEnergyMatrix[y][x] = getGradientMagnitude(dy, dx);
            }
        }
    }

    public BufferedImage carveImage(CarvingScheme carvingScheme) {
        int numberOfVerticalSeamsToCarve = Math.abs(this.outWidth - this.inWidth);
        int numberOfHorizontalSeamsToCarve = Math.abs(this.outHeight - this.inHeight);
        // TODO :  Perform Seam Carving. Overall you need to remove 'numberOfVerticalSeamsToCarve' vertical seams
        // and 'numberOfHorizontalSeamsToCarve' horizontal seams from the image.
        // Note you must consider the 'carvingScheme' parameter in your procedure.
        // Return the resulting image.

        if (carvingScheme == CarvingScheme.VERTICAL_HORIZONTAL) {
            carveVerticalSeams(numberOfVerticalSeamsToCarve);
            carveHorizontalSeams(numberOfHorizontalSeamsToCarve);
        }
        else if (carvingScheme == CarvingScheme.HORIZONTAL_VERTICAL) {
            carveHorizontalSeams(numberOfHorizontalSeamsToCarve);
            carveVerticalSeams(numberOfVerticalSeamsToCarve);
        }
        else if (carvingScheme == CarvingScheme.INTERMITTENT) {
            while (numberOfVerticalSeamsToCarve > 0 || numberOfHorizontalSeamsToCarve > 0) {
                if (numberOfVerticalSeamsToCarve > 0) {
                    carveVerticalSeams(1);
                    numberOfVerticalSeamsToCarve--;
                }
                if (numberOfHorizontalSeamsToCarve > 0) {
                    carveHorizontalSeams(1);
                    numberOfHorizontalSeamsToCarve--;
                }
            }
        }

        return workingImage;
    }

    private void carveVerticalSeams(int numOfSeams) {
        if (numOfSeams > 0) {
            lastCurrentRow = workingImage.getHeight();
            lastCurrentCol = workingImage.getWidth();

            for (int i = 0; i < numOfSeams; i++) {
                findAndRemoveSeam();
            }
        }
    }

    private void carveHorizontalSeams(int numOfSeams) {
        if (numOfSeams > 0) {
            workingImage = rotateImageClockwise(workingImage);
            lastCurrentRow = workingImage.getHeight();
            lastCurrentCol = workingImage.getWidth();
            pixelEnergyMatrix = rotatePixelEnergyClockwise();
            indexMatrix = rotateIndexArrayClockwise();

            for (int i = 0; i < numOfSeams; i++) {
                findAndRemoveSeam();
            }

            workingImage = rotateImageCounterClockwise(workingImage);
            pixelEnergyMatrix = rotatePixelEnergyCounterClockwise();
            indexMatrix = rotateIndexArrayCounterClockwise();
        }
    }

    public BufferedImage showSeams(boolean showVerticalSeams, int seamColorRGB) {
        int numberOfVerticalSeamsToCarve = Math.abs(this.outWidth - this.inWidth);
        int numberOfHorizontalSeamsToCarve = Math.abs(this.outHeight - this.inHeight);
        // TODO :  Present either vertical or horizontal seams on the input image.
        // If showVerticalSeams = true, carve 'numberOfVerticalSeamsToCarve' vertical seams from the image.
        // Then, generate a new image from the input image in which you mark all of the vertical seams that
        // were chosen in the Seam Carving process.
        // This is done by painting each pixel in each seam with 'seamColorRGB' (overriding its' previous value).
        // Similarly, if showVerticalSeams = false, carve 'numberOfHorizontalSeamsToCarve' horizontal seams
        // from the image.
        // Then, generate a new image from the input image in which you mark all of the horizontal seams that
        // were chosen in the Seam Carving process.

        BufferedImage paintedImg = duplicateWorkingImage();
        if (showVerticalSeams) {
            for (int i = 0; i < numberOfVerticalSeamsToCarve; i++) {
                lastCurrentRow = workingImage.getHeight();
                lastCurrentCol = workingImage.getWidth();

                List<Coordinate> seamToPaint = findAndRemoveSeam();
                paintedImg = paintVerticalSeam(paintedImg, seamToPaint, i, seamColorRGB);
            }

        }
        else {
            for (int i = 0; i < numberOfHorizontalSeamsToCarve; i++) {
                if (i == 0) {
                    workingImage = rotateImageClockwise(workingImage);
                    lastCurrentRow = workingImage.getHeight();
                    lastCurrentCol = workingImage.getWidth();
                    pixelEnergyMatrix = rotatePixelEnergyClockwise();
                    indexMatrix = rotateIndexArrayClockwise();
                    paintedImg = rotateImageClockwise(paintedImg);
                }
                List<Coordinate> seamToPaint = findAndRemoveSeam();
                paintedImg = paintVerticalSeam(paintedImg, seamToPaint, i, seamColorRGB);
            }
            paintedImg = rotateImageCounterClockwise(paintedImg);
        }

        return paintedImg;
    }

    public List<Coordinate> findAndRemoveSeam() {
        fillCostMatrix();
        Coordinate minErrorCoordinate = findMinError();
        List<Coordinate> seam = createSeam(minErrorCoordinate);
        updateMatricesAfterRemoveVerticalSeam(seam);
        workingImage = createCurrentImg();
        lastCurrentCol--;

        return seam;
    }

    public double[][] rotatePixelEnergyClockwise() {
        double[][] rotate = new double[lastCurrentRow][lastCurrentCol];
        double[] rowToCol = new double[lastCurrentRow];
        for (int y = 0; y < lastCurrentCol; y++) {
            for (int x = 0; x < lastCurrentRow; x++) {
                rowToCol[x] = pixelEnergyMatrix[y][x];
            }
            for (int newRow = 0; newRow < lastCurrentRow; newRow++) {
                rotate[newRow][lastCurrentCol - 1 - y] = rowToCol[newRow];
            }
        }
        return rotate;
    }

    public double[][] rotatePixelEnergyCounterClockwise() {
        double[][] rotate = new double[lastCurrentCol][lastCurrentRow];
        double[] rowToCol = new double[lastCurrentCol];
        for (int y = 0; y < lastCurrentRow; y++) {
            for (int x = 0; x < lastCurrentCol; x++) {
                rowToCol[x] = pixelEnergyMatrix[y][x];
            }
            for (int newRow = lastCurrentCol - 1; newRow >= 0; newRow--) {
                rotate[newRow][y] = rowToCol[lastCurrentCol - 1 - newRow];
            }
        }
        return rotate;
    }

    public Coordinate[][] rotateIndexArrayClockwise() {
        Coordinate[][] rotate = new Coordinate[lastCurrentRow][lastCurrentCol];
        Coordinate[] rowToCol = new Coordinate[lastCurrentRow];
        for (int y = 0; y < lastCurrentCol; y++) {
            for (int x = 0; x < lastCurrentRow; x++) {
                rowToCol[x] = indexMatrix[y][x];
            }
            for (int newRow = 0; newRow < lastCurrentRow; newRow++) {
                rotate[newRow][lastCurrentCol - 1 - y] = rowToCol[newRow];
            }
        }
        return rotate;
    }

    public Coordinate[][] rotateIndexArrayCounterClockwise() {
        Coordinate[][] rotate = new Coordinate[lastCurrentCol][lastCurrentRow];
        Coordinate[] rowToCol = new Coordinate[lastCurrentCol];
        for (int y = 0; y < lastCurrentRow; y++) {
            for (int x = 0; x < lastCurrentCol; x++) {
                rowToCol[x] = indexMatrix[y][x];
            }
            for (int newRow = lastCurrentCol - 1; newRow >= 0; newRow--) {
                rotate[newRow][y] = rowToCol[lastCurrentCol - 1 - newRow];
            }
        }
        return rotate;
    }

    public BufferedImage rotateImageClockwise(BufferedImage img) {
        BufferedImage rotateImg = newEmptyImage(img.getHeight(), img.getWidth());
        for (int y = 0; y < img.getHeight(); y++) {
            Color[] pixelRowToPixelCol = new Color[img.getWidth()];
            for (int x = 0; x < img.getWidth(); x++) {
                pixelRowToPixelCol[x] = new Color(img.getRGB(x, y));
            }

            for (int newRow = 0; newRow < img.getWidth(); newRow++) {
                rotateImg.setRGB(img.getHeight() - 1 - y, newRow, pixelRowToPixelCol[newRow].getRGB());
            }
        }
        return rotateImg;
    }

    public BufferedImage rotateImageCounterClockwise(BufferedImage img) {
        BufferedImage rotateImg = newEmptyImage(img.getHeight(), img.getWidth());
        for (int y = 0; y < img.getHeight(); y++) {
            Color[] pixelRowToPixelCol = new Color[img.getWidth()];
            for (int x = 0; x < img.getWidth(); x++) {
                pixelRowToPixelCol[x] = new Color(img.getRGB(x, y));
            }
            for (int newCol = img.getWidth() - 1; newCol >= 0; newCol--) {
                rotateImg.setRGB(y, newCol, pixelRowToPixelCol[img.getWidth() - 1 - newCol].getRGB());
            }
        }
        return rotateImg;
    }

    public void fillCostMatrix() {
        costMatrix = new double[lastCurrentRow][lastCurrentCol];
        prevPixel = new Coordinate[lastCurrentRow][lastCurrentCol];

        for (int x = 0; x < lastCurrentCol; x++) {
            costMatrix[0][x] = pixelEnergyMatrix[0][x];
        }

        for (int y = 1; y < lastCurrentRow; y++) {
            for (int x = 0; x < lastCurrentCol; x++) {
                double mv = Double.MAX_VALUE;
                double mr = Double.MAX_VALUE;
                double ml = Double.MAX_VALUE;

                if (x == 0) {
                    // calc CV CR
                    double cv = 0;
                    mv = costMatrix[y - 1][x] + cv;

                    double cr = getPixelDifference(y, x + 1, y - 1, x);
                    mr = costMatrix[y - 1][x + 1] + cr;
                }

                if (x == lastCurrentCol - 1) {
                    // calc CL CV
                    double cv = 0;
                    mv = costMatrix[y - 1][x] + cv;

                    double cl = getPixelDifference(y, x - 1, y - 1, x);
                    ml = costMatrix[y - 1][x - 1] + cl;
                }

                if (x > 0 && x < lastCurrentCol - 1) {
                    // calc CL CV CR
                    double cl = getPixelDifference(y, x - 1, y, x + 1) + getPixelDifference(y, x - 1, y - 1, x);
                    ml = costMatrix[y - 1][x - 1] + cl;

                    double cv = getPixelDifference(y, x - 1, y, x + 1);
                    mv = costMatrix[y - 1][x] + cv;

                    double cr = getPixelDifference(y, x + 1, y, x - 1) + getPixelDifference(y, x + 1, y - 1, x);
                    mr = costMatrix[y - 1][x + 1] + cr;
                }

                double min = Math.min(mv, Math.min(mr, ml));
                if (min == mv) {
                    prevPixel[y][x] = new Coordinate(y - 1, x);
                } else if (min == ml) {
                    prevPixel[y][x] = new Coordinate(y - 1, x - 1);
                } else if (min == mr) {
                    prevPixel[y][x] = new Coordinate(y - 1, x + 1);
                }

                costMatrix[y][x] = pixelEnergyMatrix[y][x] + min;
            }
        }
    }

    public Coordinate findMinError() {
        double minError = costMatrix[lastCurrentRow - 1][0];
        Coordinate minCoordinate = new Coordinate(lastCurrentRow - 1, 0);

        for (int x = 1; x < lastCurrentCol; x++) {
            if (costMatrix[lastCurrentRow - 1][x] < minError) {
                minError = costMatrix[lastCurrentRow - 1][x];
                minCoordinate.X = x;
            }
        }

        return minCoordinate;
    }

    public List<Coordinate> createSeam(Coordinate startCoordinate) {
        List<Coordinate> seam = new LinkedList<>();
        Coordinate currentCoordinate = startCoordinate;
        while (currentCoordinate.Y > 0) {
            ((LinkedList<Coordinate>) seam).addFirst(currentCoordinate);
            currentCoordinate = prevPixel[currentCoordinate.Y][currentCoordinate.X];
        }
        ((LinkedList<Coordinate>) seam).addFirst(currentCoordinate);
        return seam;
    }


    public void updateMatricesAfterRemoveVerticalSeam(List<Coordinate> seam) {
        for (int y = 0; y < lastCurrentRow; y++) {
            Coordinate toRemove = seam.get(y);
            for (int x = 0; x < lastCurrentCol; x++) {
                double dx;
                double dy;
                if (x == toRemove.X - 1) {
                    if (x < lastCurrentCol - 2) {
                        dx = getPixelDifference(y, x, y,x+2);
                    } else {
                        dx = getPixelDifference(y, x-1, y,x);
                    }
                    if (y < lastCurrentRow - 1) {
                        Coordinate nextToRemove = seam.get(y + 1);
                        if (nextToRemove.X == x) {
                            if (x == lastCurrentCol - 1) {
                                dy = getPixelDifference(y, x, y + 1, x - 1);
                            } else {
                                dy = getPixelDifference(y, x, y + 1, x + 1);
                            }
                        } else {
                            dy = getPixelDifference(y, x, y + 1, x);
                        }
                    } else {
                        Coordinate prevToRemove = seam.get(y - 1);
                        if (prevToRemove.X == x) {
                            if (x == lastCurrentCol - 1) {
                                dy = getPixelDifference(y, x, y - 1, x - 1);
                            } else {
                                dy = getPixelDifference(y, x, y - 1, x + 1);
                            }
                        } else {
                            dy = getPixelDifference(y, x, y - 1, x);
                        }
                    }
                    pixelEnergyMatrix[y][x] = getGradientMagnitude(dy, dx);
                }
                if (x == toRemove.X + 1) {
                    if (x < lastCurrentCol - 1) {
                        dx = getPixelDifference(y, x, y,x+1);
                    } else {
                        dx = getPixelDifference(y, x-2, y, x);
                    }
                    if (y < lastCurrentRow - 1) {
                        Coordinate nextToRemove = seam.get(y + 1);
                        if (nextToRemove.X == x) {
                            if (x == lastCurrentCol - 1) {
                                dy = getPixelDifference(y, x, y+1, x-1);
                            } else {
                                dy = getPixelDifference(y, x, y+1, x+1);
                            }
                        } else {
                            dy = getPixelDifference(y, x, y+1, x);
                        }
                    } else {
                        Coordinate prevToRemove = seam.get(y - 1);
                        if (prevToRemove.X == x) {
                            if (x == lastCurrentCol - 1) {
                                dy = getPixelDifference(y, x, y - 1, x - 1);
                            } else {
                                dy = getPixelDifference(y, x, y - 1, x + 1);
                            }
                        } else {
                            dy = getPixelDifference(y, x, y - 1, x);
                        }
                    }
                    pixelEnergyMatrix[y][x - 1] = getGradientMagnitude(dy, dx);
                    indexMatrix[y][x - 1] = indexMatrix[y][x];
                }
                if (x > toRemove.X + 1) {
                    pixelEnergyMatrix[y][x - 1] =  pixelEnergyMatrix[y][x];
                    indexMatrix[y][x - 1] = indexMatrix[y][x];
                }
            }
            // put invalid value on removed cols
            indexMatrix[y][lastCurrentCol - 1] = null;
            pixelEnergyMatrix[y][lastCurrentCol - 1] = -1;
        }
    }

    public BufferedImage createCurrentImg() {
        BufferedImage ans = newEmptyImage(lastCurrentCol - 1, lastCurrentRow);
        for (int y = 0; y < lastCurrentRow; y++) {
            for (int x = 0; x < lastCurrentCol - 1; x++) {
                ans.setRGB(x, y, originalImg.getRGB(indexMatrix[y][x].X, indexMatrix[y][x].Y));
            }
        }
        return ans;
    }

    public BufferedImage paintVerticalSeam(BufferedImage paintedImg, List<Coordinate> seamToPaint, int offset, int color) {
        for (int y = 0; y < paintedImg.getHeight(); y++) {
            Coordinate toPaint = seamToPaint.get(y);
            toPaint.X += offset;
            for (int x = 0; x < paintedImg.getWidth(); x++) {
                if (toPaint.X == x) {
                    paintedImg.setRGB(x, y, color);
                } else {
                    paintedImg.setRGB(x, y, paintedImg.getRGB(x, y));
                }
            }
        }
        return paintedImg;
    }
}
