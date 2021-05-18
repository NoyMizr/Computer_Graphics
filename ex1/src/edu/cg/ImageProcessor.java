package edu.cg;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class ImageProcessor extends FunctioalForEachLoops {
	
	//MARK: Fields
	public final Logger logger;
	public final BufferedImage workingImage;
	public final RGBWeights rgbWeights;
	public final int inWidth;
	public final int inHeight;
	public final int workingImageType;
	public final int outWidth;
	public final int outHeight;
	
	//MARK: Constructors
	public ImageProcessor(Logger logger, BufferedImage workingImage,
						  RGBWeights rgbWeights, int outWidth, int outHeight) {
		super(); //Initializing for each loops...
		
		this.logger = logger;
		this.workingImage = workingImage;
		this.rgbWeights = rgbWeights;
		inWidth = workingImage.getWidth();
		inHeight = workingImage.getHeight();
		workingImageType = workingImage.getType();
		this.outWidth = outWidth;
		this.outHeight = outHeight;
		setForEachInputParameters();
	}
	
	public ImageProcessor(Logger logger,
						  BufferedImage workingImage,
						  RGBWeights rgbWeights) {
		this(logger, workingImage, rgbWeights,
				workingImage.getWidth(), workingImage.getHeight());
	}
	
	//MARK: Change picture hue - example
	public BufferedImage changeHue() {
		logger.log("Prepareing for hue changing...");
		
		int r = rgbWeights.redWeight;
		int g = rgbWeights.greenWeight;
		int b = rgbWeights.blueWeight;
		int max = rgbWeights.maxWeight;
		
		BufferedImage ans = newEmptyInputSizedImage();
		
		forEach((y, x) -> {
			Color c = new Color(workingImage.getRGB(x, y));
			int red = r*c.getRed() / max;
			int green = g*c.getGreen() / max;
			int blue = b*c.getBlue() / max;
			Color color = new Color(red, green, blue);
			ans.setRGB(x, y, color.getRGB());
		});
		
		logger.log("Changing hue done!");
		
		return ans;
	}
	
	//MARK: Nearest neighbor - example
	public BufferedImage nearestNeighbor() {
		logger.log("applies nearest neighbor interpolation.");
		BufferedImage ans = newEmptyOutputSizedImage();
		
		pushForEachParameters();
		setForEachOutputParameters();
		
		forEach((y, x) -> {
			int imgX = (int)Math.round((x*inWidth) / ((float)outWidth));
			int imgY = (int)Math.round((y*inHeight) / ((float)outHeight));
			imgX = Math.min(imgX,  inWidth-1);
			imgY = Math.min(imgY, inHeight-1);
			ans.setRGB(x, y, workingImage.getRGB(imgX, imgY));
		});
		
		popForEachParameters();
		
		return ans;
	}
	
	//MARK: Unimplemented methods
	public BufferedImage greyscale() {
		//TODO: Implement this method, remove the exception.

		int r = rgbWeights.redWeight;
		int g = rgbWeights.greenWeight;
		int b = rgbWeights.blueWeight;
		int sum = rgbWeights.weightsSum;

		BufferedImage ansGrey = newEmptyImage(workingImage.getWidth(), workingImage.getHeight());

		forEach((y, x) -> {
			Color c = new Color(workingImage.getRGB(x, y));
			int red = r*c.getRed() / sum;
			int green = g*c.getGreen() / sum;
			int blue = b*c.getBlue() / sum;
			int grey = red + green + blue;
			Color color = new Color(grey, grey, grey);
			ansGrey.setRGB(x, y, color.getRGB());
		});

		logger.log("Changing greyscale done!");

		return ansGrey;
	}

	public BufferedImage gradientMagnitude() {
		//TODO: Implement this method, remove the exception.
		BufferedImage workingImageOnGreyScale = greyscale();

		BufferedImage ans = newEmptyInputSizedImage();

		forEach((y, x) -> {
			Color c = new Color(workingImageOnGreyScale.getRGB(x, y));
			Color cTheNextX;
			Color cTheNextY;
			if(x >= outWidth - 1 ){
				cTheNextX = new Color(workingImageOnGreyScale.getRGB(x - 1, y));
			}
			else {
				cTheNextX = new Color(workingImageOnGreyScale.getRGB(x + 1, y));
			}
			if(y >= outHeight - 1 ){
				cTheNextY = new Color(workingImageOnGreyScale.getRGB(x, y - 1));

			}else{
				cTheNextY = new Color(workingImageOnGreyScale.getRGB(x, y + 1));
			}
			int dxSqr = (int)Math.pow(differenceCurrentAndNext(c, cTheNextX),2);
			int dySqr =(int)Math.pow(differenceCurrentAndNext(c, cTheNextY),2);
			int magnitude =(int)(Math.sqrt((dxSqr+dySqr)/2));
			Color color = new Color(magnitude,magnitude,magnitude);
			ans.setRGB(x, y, color.getRGB());
		});

		logger.log("Changing gradientMagnitude done!");

		return ans;
	}

	public int differenceCurrentAndNext(Color c, Color cNext){
		int greyDif = cNext.getRed()- c.getRed();
		return greyDif;
	}

	public BufferedImage bilinear() {
		//TODO: Implement this method, remove the exception.
		logger.log("applies bilinear interpolation.");
		BufferedImage ans = newEmptyOutputSizedImage();

		pushForEachParameters();
		setForEachOutputParameters();

		forEach((y, x) -> {
			float cx = (x*inWidth) / ((float)outWidth);
			float cy = (y*inHeight) / ((float)outHeight);
			cx = Math.min(cx,  inWidth-1);
			cy = Math.min(cy, inHeight-1);

			int x2 = Math.round(cx);
			int y2 = Math.round(cy);
			x2 = Math.min(x2,  inWidth-1);
			y2 = Math.min(y2, inHeight-1);

			// we want to consider the center of each pixel
			// we will take the +0.5 from each up right corner value
			// c1 = (x1+0.5, y1+0.5), c2 = (x2+0.5, y1+0.5)
			// c3 = (x1+0.5, y2+0.5), c4 = (x2+0.5, y2+0.5)

			int x1 = x2; // temporary assign as x2
			int y1 = y2; // temporary assign as y2

			double t;
			double s;

			Color c1;
			Color c2;
			Color c3;
			Color c4;

			if (x2 == 0 && y2 == 0) {
				x1 = x2 + 1;
				y1 = y2 + 1;

				c1 = new Color(workingImage.getRGB(x2, y2));
				c2 = new Color(workingImage.getRGB(x1, y2));
				c3 = new Color(workingImage.getRGB(x2, y1));
				c4 = new Color(workingImage.getRGB(x1, y1));

				t = calcRatio(cx, x2+0.5, x1+0.5);
				s = calcRatio(cy, y2+0.5, y1+0.5);

			} else if (x2 == 0 && y2 > 0) {
				x1 = x2 + 1;
				y1 = y2 - 1;

				c1 = new Color(workingImage.getRGB(x2, y1));
				c2 = new Color(workingImage.getRGB(x1, y1));
				c3 = new Color(workingImage.getRGB(x2, y2));
				c4 = new Color(workingImage.getRGB(x1, y2));

				t = calcRatio(cx, x2+0.5, x1+0.5);
				s = calcRatio(cy, y1+0.5, y2+0.5);

			} else if (x2 > 0 && y2 == 0) {
				x1 = x2 - 1;
				y1 = y2 + 1;

				t = calcRatio(cx, x1+0.5, x2+0.5);
				s = calcRatio(cy, y2+0.5, y1+0.5);

				c1 = new Color(workingImage.getRGB(x1, y2));
				c2 = new Color(workingImage.getRGB(x2, y2));
				c3 = new Color(workingImage.getRGB(x1, y1));
				c4 = new Color(workingImage.getRGB(x2, y1));

			} else {
				x1 = x2 - 1;
				y1 = y2 - 1;

				c1 = new Color(workingImage.getRGB(x1, y1));
				c2 = new Color(workingImage.getRGB(x2, y1));
				c3 = new Color(workingImage.getRGB(x1, y2));
				c4 = new Color(workingImage.getRGB(x2, y2));

				t = calcRatio(cx, x1+0.5, x2+0.5);
				s = calcRatio(cy, y1+0.5, y2+0.5);
			}

			Color c12 = doLinearInterpolation(t, c1, c2);
			Color c34 = doLinearInterpolation(t, c3, c4);
			Color c = doLinearInterpolation(s, c12, c34);

			ans.setRGB(x, y, c.getRGB());

		});

		popForEachParameters();

		return ans;
	}

	public double calcRatio(float x, double x1, double x2) {
		return Math.sqrt(Math.pow((x-x1)/(x2-x1),2));
	}

	public Color doLinearInterpolation(double t, Color c1, Color c2) {
		int red = (int)((1-t) * c1.getRed() + t * c2.getRed());
		int green = (int)((1-t) * c1.getGreen() + t * c2.getGreen());
		int blue = (int)((1-t) * c1.getBlue() + t * c2.getBlue());
		Color c = new Color(red,green,blue);
		return c;
	}
	
	//MARK: Utilities
	public final void setForEachInputParameters() {
		setForEachParameters(inWidth, inHeight);
	}
	
	public final void setForEachOutputParameters() {
		setForEachParameters(outWidth, outHeight);
	}
	
	public final BufferedImage newEmptyInputSizedImage() {
		return newEmptyImage(inWidth, inHeight);
	}
	
	public final BufferedImage newEmptyOutputSizedImage() {
		return newEmptyImage(outWidth, outHeight);
	}
	
	public final BufferedImage newEmptyImage(int width, int height) {
		return new BufferedImage(width, height, workingImageType);
	}
	
	public final BufferedImage duplicateWorkingImage() {
		BufferedImage output = newEmptyInputSizedImage();
		
		forEach((y, x) -> 
			output.setRGB(x, y, workingImage.getRGB(x, y))
		);
		
		return output;
	}
}
