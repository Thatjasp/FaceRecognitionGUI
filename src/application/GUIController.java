package application;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.videoio.VideoCapture;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.*;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class GUIController {
	public ImageView currentFrame;
	@FXML
	public Button end_btn;
	@FXML
	public Button start_btn;
	@FXML
	public CheckBox greyScale;
	@FXML
	public CheckBox logoBox;
	@FXML
	public ImageView histogram;
	@FXML
	public CheckBox eyeReckingBox;

	// Histogram Data
	Mat histImage;

	final int widthOfHIST = 512;

	final int heightOfHIST = 500;

	ArrayList<Mat> histBins = new ArrayList<>();

	Mat bHistory = new Mat(), gHistory = new Mat(), rHistory = new Mat();

	float[] range = { 0, 256 };

	MatOfFloat histRange = new MatOfFloat(range);

	int histSize = 256;

	boolean gray = false;
	// End of Histogram

	// Video Capture/Face Detetcting
	private CascadeClassifier faceCasc = new CascadeClassifier();
	private CascadeClassifier eyeCasc = new CascadeClassifier();
	private Mat frame = new Mat();// the actual frame being show
	private Mat grayFrame = new Mat();// gray frame transfer
	private int absoluteFaceSize;
	private float faceSize;

	private int absoluteEyeSize;
	private float eyeSize;
	// Logo Box Shit
	private Mat logo = new Mat();// Danny devito logo
	Size frameSize = new Size(150, 160);
	Rect roi;
	Mat imageROI;
	// Logo Box End

	private ScheduledExecutorService timer; // Loop for the VideoCapture

	VideoCapture capture = new VideoCapture();// VideoCapture that receives mats

	/*
	 * Disabling greyScale when start camera is pressed and FaceSize is set to 0
	 */
	protected void init() {
		absoluteFaceSize = 0;
		greyScale.setDisable(false);
	}

	/*
	 * Lines of code executed when FaceRecognition check box is set.
	 * 
	 * Loads Face recognition classifiers and enables start button.
	 */
	public void eyeReckingBox() {
		loadClassifier("resources/haarcascade/haarcascade_frontalface_alt.xml",
				"resources/haarcascade/haarcascade_righteye_2splits.xml");

		start_btn.setDisable(false);

	}

	/*
	 * Method executed when Start Camera button is pressed
	 * 
	 * Grabs frame and executes run inner method every 12ms
	 */
	@FXML
	public void startCamera(ActionEvent event) {
		init();

		this.capture.open(0);

		if (this.capture.isOpened()) {

			Runnable frameGrabber = new Runnable() {

				@Override
				public void run() {
					Image imageToShow = grabFrame();
					try {
						histogram();
					} catch (Exception e) {
					} // Catches when turn grey
					currentFrame.setImage(imageToShow);

				}

			};

			this.timer = Executors.newSingleThreadScheduledExecutor();

			timer.scheduleAtFixedRate(frameGrabber, 0, 13, TimeUnit.MILLISECONDS);
		}
	}

	/*
	 * Grabs frame from OpenCV and determines if logo box has been selected or if
	 * gray box has been selected. Adds rectangles if Face or eye has been found
	 * 
	 * @return The complete frame as an image
	 */
	@SuppressWarnings("exports")
	public Image grabFrame() {
		this.capture.read(frame);

		if (!frame.empty()) {

			if (greyScale.isSelected()) {

				Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);

				gray = true;
			}

			if (logoBox.isSelected() && logo != null)
				logoBoxLogic();

			detectAndDisplay();

			return matIntoImage(frame);
		}
		return null;
	}

	/*
	 * Method executed when logo box check box is pressed
	 * 
	 * Determines if logo is shown or not
	 */
	@FXML
	public void loadLogo() {

		if (logoBox.isSelected()) {

			this.logo = Imgcodecs.imread("TheLorax.jpg");

			Imgproc.resize(logo, logo, frameSize);

		} else {
			this.logo = null;
		}

	}

	/*
	 * Method executed when check box is executed
	 */
	@FXML
	public void greyBox() {

		if (greyScale.isSelected() && logoBox.isSelected()) {

			Imgproc.cvtColor(logo, logo, Imgproc.COLOR_BGR2GRAY);

			gray = true;

		}
		// TODO a certain expression for when greyscale isSelected. Check grabFrame for

		else if (!greyScale.isSelected() && logoBox.isSelected()) {
			
			System.out.println("bruh");
			try {
				this.logo = Imgcodecs.imread("TheLorax.jpg");

				Imgproc.resize(logo, logo, frameSize);

				gray = false;
			} catch (Exception e) {
				e.printStackTrace();
			}

		} else if (!greyScale.isSelected())
			gray = false;

		else {

		}

	}

	/*
	 * Adds logo onto the frame of the captured video
	 */
	public void logoBoxLogic() {

		roi = new Rect(488, 320, 150, 160);

		try {

			imageROI = frame.submat(roi);

			if (imageROI.size().equals(logo.size()))

				Core.addWeighted(imageROI, 1, logo, .7, 0, imageROI);

		} catch (Exception e) {

			e.printStackTrace();

		}

	}

	/*
	 * Method that finds the amount RGB in each frame and displays it on the right
	 */
	@FXML
	public void histogram() {

		splitHistory();

		int binWidth = (int) Math.round((double) widthOfHIST / histSize);

		histImage = new Mat(heightOfHIST, widthOfHIST, CvType.CV_8UC3, new Scalar(0, 0, 0));
		
		normHist(histImage.rows());
		

		float[] bHistData = new float[(int) (bHistory.total() * bHistory.channels())];
		bHistory.get(0, 0, bHistData);

		float[] gHistData = new float[(int) (gHistory.total() * gHistory.channels())];
		gHistory.get(0, 0, gHistData);

		float[] rHistData = new float[(int) (rHistory.total() * rHistory.channels())];
		rHistory.get(0, 0, rHistData);

		for (int i = 1; i < histSize; i++) {

			Imgproc.line(histImage, new Point(binWidth * (i - 1), heightOfHIST - Math.round(bHistData[i - 1])),
					new Point(binWidth * (i), heightOfHIST - Math.round(bHistData[i])), new Scalar(255, 0, 0), 2, 8, 0);

			if (!gray) {

				Imgproc.line(histImage, new Point(binWidth * (i - 1), heightOfHIST - Math.round(gHistData[i - 1])),
						new Point(binWidth * (i), heightOfHIST - Math.round(gHistData[i])), new Scalar(0, 255, 0), 2, 8,
						0);

				Imgproc.line(histImage, new Point(binWidth * (i - 1), heightOfHIST - Math.round(rHistData[i - 1])),
						new Point(binWidth * (i), heightOfHIST - Math.round(rHistData[i])), new Scalar(0, 0, 255), 2, 8,
						0);

			}
		}
		Image hist = matIntoImage(histImage);

		histogram.setImage(hist);
	}
	
	private void splitHistory() {
		Core.split(frame, histBins);
		if (histBins.size() > 0) {
			Imgproc.calcHist(histBins, new MatOfInt(0), new Mat(), bHistory, new MatOfInt(histSize), histRange, false);

			if (!gray) {

				Imgproc.calcHist(histBins, new MatOfInt(1), new Mat(), gHistory, new MatOfInt(histSize), histRange,
						false);

				Imgproc.calcHist(histBins, new MatOfInt(2), new Mat(), rHistory, new MatOfInt(histSize), histRange,
						false);
			}
		}
	}
	
	public void normHist(int rows) {
		Core.normalize(bHistory, bHistory, 0, rows, Core.NORM_MINMAX);

		if (!gray) {

			Core.normalize(gHistory, gHistory, 0, histImage.rows(), Core.NORM_MINMAX);

			Core.normalize(rHistory, rHistory, 0, histImage.rows(), Core.NORM_MINMAX);

		}
	}

	/*
	 * Method executed when end Camera button is pressed cleans up code
	 */
	@FXML
	public void endCamera() {
		if (capture.isOpened()) {
			this.capture.release();
			timer.shutdown();
		}
	}

	/*
	 * Method executed when close the window cleans up code
	 */
	@FXML
	public void exitApplication() {
		if (capture.isOpened())
			capture.release();

		Platform.exit();

	}

	/*
	 * ' A helper method to make mat data into an Image
	 * 
	 * @params frame - The mat needed to become an image
	 */
	private Image matIntoImage(Mat frame) {

		MatOfByte buffer;

		Image oneFrame;

		buffer = new MatOfByte();

		Imgcodecs.imencode(".jpg", frame, buffer);

		oneFrame = new Image(new ByteArrayInputStream(buffer.toArray()));

		return oneFrame;
	}

	/*
	 * helper method to load 2 classifiers for Face recognition
	 * 
	 * @param classifierPath The primary face classifer
	 * 
	 * @param secondPath The secondary face classifier
	 */
	private void loadClassifier(String classifierPath, String secondPath) {

		faceCasc.load(classifierPath);
		eyeCasc.load(secondPath);

	}

	/*
	 * Detects face and makes a rectangle around the face face if recognized
	 */
	public void detectAndDisplay() {
		MatOfRect face = new MatOfRect();

		if (!gray)
			Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
		else
			grayFrame = frame.clone();

		Imgproc.equalizeHist(grayFrame, grayFrame);

		Rect[] faceArray = detectAndDisplay(grayFrame, face);

		Rect biggest = new Rect(0, 0, 0, 0);

		for (Rect faces : faceArray)
			if (biggest.area() < faces.area())
				biggest = faces;

		Imgproc.rectangle(frame, biggest.tl(), biggest.br(), new Scalar(0, 255, 0), 3);
		

		if (faceArray.length != 0) {
			Mat upperprofile = frame.submat(faceArray[0]);
			System.out.println("face" + biggest.tl().toString());
			detectEye(upperprofile, faceArray[0]);
		}
	}

	/*
	 * A helper method to return the faces found in the image
	 * 
	 * @param grayFrame The gray frame version of the frame
	 * 
	 * @param face An empty MatOfRect data type
	 * 
	 * @return The list of faces represented as rectangles in the frame.
	 */
	private Rect[] detectAndDisplay(Mat grayFrame, MatOfRect face) {
		faceSize = .15f;

		if (this.absoluteFaceSize == 0) {
			int height = grayFrame.rows();
			if (Math.round(height * faceSize) > 0) {
				this.absoluteFaceSize = Math.round(height * faceSize);
			}
		}
		this.faceCasc.detectMultiScale(grayFrame, face, 1.1, 2, 0 | Objdetect.CASCADE_FIND_BIGGEST_OBJECT,
				new Size(this.absoluteFaceSize, absoluteFaceSize), new Size());
		return face.toArray();

	}

	/*
	 * A Helper method to find eyes inside of a subframe
	 * 
	 * @param upperProfile The submat that is being searched for
	 * 
	 * @param The rectangle that the submat is in
	 */
	private void detectEye(Mat upperProfile, Rect upper) {
		MatOfRect eye = new MatOfRect();
		eyeSize = .20f;

		if (this.absoluteEyeSize == 0) {
			int height = upperProfile.rows();

			if (Math.round(height * eyeSize) > 0)
				this.absoluteEyeSize = Math.round(height * eyeSize);
		}
		// Find out a way to get eye translate to big frame
		eyeCasc.detectMultiScale(upperProfile, eye, 1.1, 2, 0 | Objdetect.CASCADE_FIND_BIGGEST_OBJECT,
				new Size(this.absoluteEyeSize, absoluteEyeSize), new Size());
		Rect biggest = new Rect(0, 0, 0, 0);
		for (Rect eyes : eye.toArray()) {
			if (biggest.area() < eyes.area())
				biggest = eyes;
		}

		if (biggest.area() != 0) {
			Imgproc.rectangle(upperProfile, biggest.tl(), biggest.br(), new Scalar(255, 0, 0), 3);
			System.out.println(biggest.tl().toString());
			}	
		}

}
